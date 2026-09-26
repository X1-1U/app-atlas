package local.appatlas;

import android.app.*;
import android.app.usage.*;
import android.content.pm.*;
import android.os.storage.StorageManager;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Offline, user-selected local document trees only. No broad storage permission. */
public final class MainActivity extends Activity {
    private static final String ORIGIN="https://appassets.androidplatform.net/";
    private static final String LOCAL="com.android.externalstorage.documents";
    private WebView web;
    private final android.util.LruCache<String,byte[]> thumbnailCache=new android.util.LruCache<String,byte[]>(8*1024*1024){@Override protected int sizeOf(String key,byte[] data){return data.length;}};
    private final android.util.LruCache<String,byte[]> appIconCache=new android.util.LruCache<String,byte[]>(2*1024*1024){@Override protected int sizeOf(String key,byte[] data){return data.length;}};
    private final java.util.concurrent.Semaphore thumbnailSlots=new java.util.concurrent.Semaphore(2);
    private final CancellationSignal thumbnailCancel=new CancellationSignal();
    private FrameLayout frame;
    private volatile JSONArray installed=new JSONArray();
    private volatile JSONArray indexed=new JSONArray();
    private volatile String uiPage="clean";
    private volatile boolean ready=false,returning=false,cancelScan=false,scanDone=false;
    private final ExecutorService detailsWorker=Executors.newSingleThreadExecutor();
    private volatile String sourceLookup="尚未查詢";
    private final Map<String,String> mediaOwners=new HashMap<>();
    private final ConcurrentHashMap<String,String> appNames=new ConcurrentHashMap<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean scanning=new AtomicBoolean(false);
    private final ConcurrentHashMap<String,JSONObject> files=new ConcurrentHashMap<>();
    private volatile boolean destroyed=false;
    private volatile String pendingApp="";
    private volatile String snapshot="{\"files\":[],\"roots\":[],\"busy\":false,\"errors\":[],\"limited\":false}";
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle saved){
        prefs=getSharedPreferences("atlas",MODE_PRIVATE);
        String mode=prefs.getString("theme","system");boolean systemDark=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
        boolean dark=mode.equals("dark")||(mode.equals("system")&&systemDark);
        setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(saved);getWindow().setStatusBarColor(dark?0xff111916:0xfff6f7f9);getWindow().setNavigationBarColor(dark?0xff111916:0xfff6f7f9);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(saved!=null){pendingApp=saved.getString("pendingApp","");uiPage=saved.getString("page","clean");}
        web=new WebView(this);web.setBackgroundColor(dark?0xff111916:0xfff6f7f9);
        getWindow().setDecorFitsSystemWindows(false);
        frame=new FrameLayout(this);
        frame.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsets.CONSUMED;});
        frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(false);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setBlockNetworkLoads(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                String url=request.getUrl().toString();
                if(url.startsWith(ORIGIN+"app-icon/")){byte[] bytes=applicationIcon(request.getUrl().getLastPathSegment());if(bytes!=null)return new WebResourceResponse("image/png","binary",new ByteArrayInputStream(bytes));return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
                if(url.startsWith(ORIGIN+"thumb/")){byte[] bytes=thumbnail(request.getUrl().getLastPathSegment());if(bytes!=null)return new WebResourceResponse("image/jpeg","binary",new ByteArrayInputStream(bytes));return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
                String asset=url.equals(ORIGIN+"index.html")?"index.html":url.equals(ORIGIN+"app.js")?"app.js":url.equals(ORIGIN+"cleanup.js")?"cleanup.js":url.equals(ORIGIN+"file-intelligence.js")?"file-intelligence.js":null;
                try{if(asset!=null)return new WebResourceResponse(asset.endsWith("js")?"application/javascript":"text/html","UTF-8",getAssets().open(asset));}catch(IOException ignored){}
                return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
            }
        });
        web.addJavascriptInterface(new Bridge(),"Android");setContentView(frame);applyNativeTheme();frame.post(()->frame.requestApplyInsets());web.loadUrl(ORIGIN+"index.html");
    }
    private JSONArray roots(){try{return new JSONArray(prefs.getString("roots","[]"));}catch(Exception e){return new JSONArray();}}
    private void saveRoots(JSONArray data){prefs.edit().putString("roots",data.toString()).commit();}
    private void js(String code){runOnUiThread(()->{if(!destroyed)web.evaluateJavascript(code,null);});}
    private void toast(String text){js("window.nativeToast("+JSONObject.quote(text)+")");}
    private void send(JSONArray entries,JSONArray errors,boolean busy,boolean limited){
        try{JSONObject data=new JSONObject();data.put("files",entries);data.put("installed",installed);data.put("allFiles",Environment.isExternalStorageManager());data.put("usageAccess",usageAccess());data.put("scanDone",scanDone);data.put("sourceLookup",sourceLookup);data.put("roots",roots());data.put("busy",busy);data.put("errors",errors);data.put("limited",limited);data.put("theme",prefs.getString("theme","system"));data.put("osDark",(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);
            try{StatFs stat=new StatFs(Environment.getDataDirectory().getAbsolutePath());JSONObject storage=new JSONObject();storage.put("total",stat.getTotalBytes());storage.put("available",stat.getAvailableBytes());data.put("storage",storage);}catch(Exception ignored){}
            snapshot=data.toString();js("window.receiveState(JSON.parse("+JSONObject.quote(snapshot)+"))");}catch(Exception ignored){}
    }
    public final class Bridge {
        @JavascriptInterface public String sourceLayout(){return prefs.getString("sourceLayout","grid");}
        @JavascriptInterface public void setSourceLayout(String mode){if(mode.equals("grid")||mode.equals("list"))prefs.edit().putString("sourceLayout",mode).apply();}
        @JavascriptInterface public String theme(){return prefs.getString("theme","system");}
        @JavascriptInterface public boolean systemDark(){return (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
        @JavascriptInterface public void ready(){ready=true;scan();}
        @JavascriptInterface public void exit(){runOnUiThread(()->moveTaskToBack(true));}
        @JavascriptInterface public String page(){return uiPage;}
        @JavascriptInterface public void rememberPage(String page){if(Arrays.asList("clean","browse","apps","settings").contains(page))uiPage=page;}
        @JavascriptInterface public void cancelScan(){cancelScan=true;}
        @JavascriptInterface public void scan(){startScan();}
        @JavascriptInterface public void grantFiles(){runOnUiThread(()->launchSettings(new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName()))));}
        @JavascriptInterface public void grantUsage(){runOnUiThread(()->launchSettings(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS,Uri.parse("package:"+getPackageName()))));}
        @JavascriptInterface public void appDetails(String pkg){detailsWorker.execute(()->loadAppDetails(pkg));}
        @JavascriptInterface public void appSettings(String pkg){runOnUiThread(()->{if(appNames.containsKey(pkg))launchSettings(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg)));});}
        @JavascriptInterface public void pickFolder(String app){
            if(app==null||app.trim().isEmpty()||app.length()>40||scanning.get())return;
            runOnUiThread(()->{pendingApp=app.trim();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);try{startActivityForResult(i,10);}catch(ActivityNotFoundException e){toast("找不到系統資料夾選擇器。");}});
        }
        @JavascriptInterface public void openFile(String id){runOnUiThread(()->open(id));}
        @JavascriptInterface public void deleteFile(String id){runOnUiThread(()->confirmDelete(id));}
        @JavascriptInterface public void deleteFiles(String ids){runOnUiThread(()->confirmBatch(ids));}
        @JavascriptInterface public void clearClipboard(){runOnUiThread(()->{
            new AlertDialog.Builder(MainActivity.this).setTitle("清空目前剪貼簿？").setMessage("清除目前可貼上的內容，不讀取或上傳內容。這不會清除三星鍵盤或其他 App 自行保存的歷史，也不會刪除原始檔案。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{try{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).clearPrimaryClip();toast("已清空目前系統剪貼簿；鍵盤歷史未處理。");}catch(Exception e){toast("無法清空剪貼簿，請到鍵盤的剪貼簿面板處理。");}}).show();
        });}
        @JavascriptInterface public void storageSettings(){runOnUiThread(()->{try{startActivity(new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));}catch(Exception e){toast("無法開啟儲存設定，請從手機設定手動開啟。");}});}
        @JavascriptInterface public void setTheme(String mode){if(!mode.equals("system")&&!mode.equals("light")&&!mode.equals("dark"))return;runOnUiThread(()->{prefs.edit().putString("theme",mode).commit();applyNativeTheme();js("window.themeChanged("+JSONObject.quote(mode)+","+systemDark()+")");});}
        @JavascriptInterface public void removeRoot(int index){runOnUiThread(()->remove(index));}
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putString("pendingApp",pendingApp);out.putString("page",uiPage);super.onSaveInstanceState(out);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=10||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();if(!LOCAL.equals(uri.getAuthority())){toast("第一版只支援手機本機儲存空間，請選擇內部儲存空間的資料夾。");return;}
        if(pendingApp.isEmpty()){toast("請重新指定分類名稱。");return;}
        try{
            JSONArray all=roots();String docId=DocumentsContract.getTreeDocumentId(uri);
            for(int n=0;n<all.length();n++){String prior=DocumentsContract.getTreeDocumentId(Uri.parse(all.getJSONObject(n).getString("uri")));if(docId.equals(prior)||docId.startsWith(prior+"/")||prior.startsWith(docId+"/")){toast("這個資料夾已關聯或與現有來源重疊，請先解除原關聯。");return;}}
            int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getContentResolver().takePersistableUriPermission(uri,flags);
            JSONObject root=new JSONObject();root.put("uri",uri.toString());root.put("app",pendingApp);root.put("name",DocumentsContract.getTreeDocumentId(uri));all.put(root);saveRoots(all);startScan();
        }catch(Exception e){toast("無法保留資料夾授權，請重新選擇。");}
    }
    private void startScan(){
        if(destroyed||!scanning.compareAndSet(false,true))return;
        cancelScan=false;scanDone=false;thumbnailCache.evictAll();send(indexed,new JSONArray(),true,false);
        worker.execute(()->{
            JSONArray result=new JSONArray(),errors=new JSONArray();files.clear();boolean limited=false;
            loadInstalled();
            HashSet<String> seen=new HashSet<>();HashMap<String,String> packageStates=new HashMap<>();int visited=0;
            try{
                if(Environment.isExternalStorageManager()){loadMediaOwners();limited=scanShared(result,errors);return;}
                JSONArray rs=roots();
                for(int i=0;i<rs.length();i++){
                    JSONObject r=rs.getJSONObject(i);Uri tree=Uri.parse(r.getString("uri"));String app=r.getString("app");
                    ArrayDeque<String[]> dirs=new ArrayDeque<>();dirs.add(new String[]{DocumentsContract.getTreeDocumentId(tree),r.getString("name")});
                    HashSet<String> visitedDirs=new HashSet<>();
                    try{
                        while(!dirs.isEmpty()&&!cancelScan&&!destroyed&&!Thread.currentThread().isInterrupted()){
                            if(visited>=25000||result.length()>=20000){limited=true;break;}
                            String[] dir=dirs.removeFirst();if(!visitedDirs.add(dir[0]))continue;
                            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,dir[0]);
                            String[] columns={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED,DocumentsContract.Document.COLUMN_FLAGS};
                            try(Cursor c=getContentResolver().query(children,columns,null,null,null)){
                                if(c==null){errors.put(app+"：有資料夾無法讀取");continue;}
                                while(c.moveToNext()){
                                    if(destroyed||Thread.currentThread().isInterrupted())break;
                                    if(visited++>=25000||result.length()>=20000){limited=true;break;}
                                    String doc=c.getString(0),name=c.getString(1),mime=c.getString(2);if(doc==null)continue;if(name==null)name="未命名";if(mime==null)mime="application/octet-stream";
                                    if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){dirs.add(new String[]{doc,dir[1]+" / "+name});continue;}
                                    String key=app+"|"+tree.getAuthority()+"|"+doc;if(!seen.add(key))continue;
                                    Uri uri=DocumentsContract.buildDocumentUriUsingTree(tree,doc);JSONObject f=new JSONObject();String id=UUID.randomUUID().toString();f.put("id",id);f.put("app",app);f.put("name",name);f.put("mime",mime);f.put("size",c.isNull(3)?-1:c.getLong(3));f.put("date",c.isNull(4)?0:c.getLong(4));f.put("path",dir[1]);f.put("uri",uri.toString());
                                    for(String part:dir[1].split("[/ :]+")){if(part.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){2,}")){f.put("packageName",part);if(!packageStates.containsKey(part)){try{getPackageManager().getApplicationInfo(part,0);packageStates.put(part,"installed");}catch(android.content.pm.PackageManager.NameNotFoundException e){packageStates.put(part,"unknown");}}f.put("packageState",packageStates.get(part));break;}}
                                    f.put("deletable",(c.getInt(5)&DocumentsContract.Document.FLAG_SUPPORTS_DELETE)!=0&&checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED);result.put(f);files.put(id,f);
                                }
                            }catch(Exception e){errors.put(app+"：部分目錄無法讀取，請檢查授權");}
                        }
                    }catch(Exception e){errors.put(app+"：資料夾無法存取，請解除後重新關聯");}
                    if(limited)break;
                }
            }catch(Exception e){errors.put("掃描未完成，請重新掃描或檢查資料夾授權");}
            finally{scanning.set(false);scanDone=!cancelScan;indexed=result;if(cancelScan)errors.put("掃描已停止，目前為部分結果。");send(result,errors,false,limited);}
        });
    }
    private void open(String id){
        JSONObject f=files.get(id);if(f==null||scanning.get()){toast("檔案清單已更新，請重新選擇。");return;}
        Uri uri=f.has("raw")?LocalFiles.share(new File(f.optString("raw"))):Uri.parse(f.optString("uri"));Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,f.optString("mime","application/octet-stream"));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("file",uri));
        try{startActivity(Intent.createChooser(i,"選擇開啟工具"));}catch(Exception e){toast("沒有可開啟此檔案的應用程式，或檔案已移動。");}
    }
    private void confirmDelete(String id){JSONArray ids=new JSONArray();ids.put(id);confirmBatch(ids.toString());}
    private void confirmBatch(String encoded){
        if(scanning.get()){toast("請等候掃描完成。");return;}
        final ArrayList<JSONObject> chosen=new ArrayList<>();HashSet<String> seen=new HashSet<>();StringBuilder message=new StringBuilder();
        try{JSONArray ids=new JSONArray(encoded);if(ids.length()==0||ids.length()>100){toast("每次請選擇 1–100 個檔案。");return;}
            for(int i=0;i<ids.length();i++){String id=ids.getString(i);JSONObject f=files.get(id);if(f==null||!f.optBoolean("deletable")||!seen.add(id)){toast("清單已更新或檔案不可刪除，請重新選擇。");return;}chosen.add(f);message.append(f.optString("name")).append("\n").append(f.optString("path")).append("\n\n");}
        }catch(Exception e){toast("無法讀取所選檔案。");return;}
        message.append("候選不代表垃圾。永久刪除無法復原，也可能影響原 App。若檔案大小或修改時間已改變，會略過該檔案。");
        new AlertDialog.Builder(this).setTitle("永久刪除 "+chosen.size()+" 個檔案？").setMessage(message.toString()).setNegativeButton("保留",null).setPositiveButton("永久刪除",(dialog,which)->{
            if(destroyed||!scanning.compareAndSet(false,true))return;
            worker.execute(()->{int deleted=0,skipped=0;JSONArray removed=new JSONArray();
                for(JSONObject f:chosen){if(destroyed||Thread.currentThread().isInterrupted())break;
                    try{if(f.has("raw")){File file=new File(f.optString("raw"));boolean safe=isAllowed(file)&&file.isFile()&&file.length()==f.optLong("size",-1)&&file.lastModified()==f.optLong("date",-1);if(safe&&file.delete()){deleted++;removed.put(f.optString("id"));}else skipped++;continue;}Uri uri=Uri.parse(f.optString("uri"));boolean same=false;
                        try(Cursor c=getContentResolver().query(uri,new String[]{DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
                            if(c!=null&&c.moveToFirst())same=!c.isNull(0)&&!c.isNull(1)&&c.getLong(0)==f.optLong("size",-1)&&c.getLong(1)==f.optLong("date",-1)&&!DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2));
                        }
                        if(same&&DocumentsContract.deleteDocument(getContentResolver(),uri)){deleted++;removed.put(f.optString("id"));}else skipped++;
                    }catch(Exception e){skipped++;}
                }
                // Commit only confirmed deletions; keep failed entries and the existing scan scope.
                HashSet<String> gone=new HashSet<>();for(int i=0;i<removed.length();i++){String id=removed.optString(i);gone.add(id);files.remove(id);thumbnailCache.remove(id);}
                JSONArray remaining=new JSONArray();for(int i=0;i<indexed.length();i++){JSONObject item=indexed.optJSONObject(i);if(item!=null&&!gone.contains(item.optString("id")))remaining.put(item);}indexed=remaining;
                try{JSONObject data=new JSONObject(snapshot);data.put("files",remaining);JSONObject delta=new JSONObject();delta.put("removed",removed);
                    try{StatFs stat=new StatFs(Environment.getDataDirectory().getAbsolutePath());JSONObject storage=new JSONObject();storage.put("total",stat.getTotalBytes());storage.put("available",stat.getAvailableBytes());data.put("storage",storage);delta.put("storage",storage);}catch(Exception ignored){}
                    snapshot=data.toString();js("window.receiveDeletion("+delta.toString()+")");
                }catch(Exception e){toast("清單更新失敗，請手動重新掃描。");}
                scanning.set(false);toast("已刪除 "+deleted+" 個；略過或失敗 "+skipped+" 個。"+(skipped>0?"可能是權限不足或檔案已變更，未刪除項目已保留。":""));
            });
        }).show();
    }
    private void remove(int index){
        if(scanning.get())return;JSONArray rs=roots();JSONObject root=rs.optJSONObject(index);if(root==null)return;
        new AlertDialog.Builder(this).setTitle("解除資料夾關聯？").setMessage(root.optString("name")+"\n原始檔案會保留。").setNegativeButton("取消",null).setPositiveButton("解除",(d,w)->{
            JSONArray kept=new JSONArray();for(int i=0;i<rs.length();i++)if(i!=index)kept.put(rs.opt(i));saveRoots(kept);
            try{getContentResolver().releasePersistableUriPermission(Uri.parse(root.optString("uri")),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}startScan();
        }).show();
    }

    private byte[] thumbnail(String id){
        JSONObject f=files.get(id);if(f==null||destroyed)return null;String mime=f.optString("mime");if(!mime.startsWith("image/")&&!mime.startsWith("video/"))mime=mime(f.optString("name"));boolean video=mime.startsWith("video/");if(!video&&!mime.startsWith("image/"))return null;
        File raw=f.has("raw")?new File(f.optString("raw")):null;Uri uri=raw==null?Uri.parse(f.optString("uri")):null;
        if(raw!=null&&(!Environment.isExternalStorageManager()||!isAllowed(raw)||!raw.isFile()))return null;
        if(uri!=null&&checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),Intent.FLAG_GRANT_READ_URI_PERMISSION)!=PackageManager.PERMISSION_GRANTED)return null;
        byte[] cached=thumbnailCache.get(id);if(cached!=null)return cached;
        boolean acquired=false;android.graphics.Bitmap bitmap=null;
        try{thumbnailSlots.acquire();acquired=true;if(destroyed)return null;android.util.Size size=new android.util.Size(320,320);
            if(raw!=null)bitmap=video?android.media.ThumbnailUtils.createVideoThumbnail(raw,size,thumbnailCancel):android.media.ThumbnailUtils.createImageThumbnail(raw,size,thumbnailCancel);
            else{try{bitmap=getContentResolver().loadThumbnail(uri,size,thumbnailCancel);}catch(Exception ignored){
                if(!video)bitmap=android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(getContentResolver(),uri),(decoder,info,source)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();double scale=Math.min(1.0,320.0/Math.max(w,h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);});
                else{android.media.MediaMetadataRetriever retriever=new android.media.MediaMetadataRetriever();try{retriever.setDataSource(this,uri);bitmap=retriever.getScaledFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,320,320);}finally{retriever.release();}}
            }}
            if(bitmap==null)return null;ByteArrayOutputStream output=new ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,80,output);byte[] data=output.toByteArray();if(!destroyed)thumbnailCache.put(id,data);return data;
        }catch(Exception|OutOfMemoryError e){return null;}finally{if(bitmap!=null)bitmap.recycle();if(acquired)thumbnailSlots.release();}
    }
    private boolean usageAccess(){AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;}
    private boolean systemDark(){return (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
    private void applyNativeTheme(){String mode=prefs.getString("theme","system");boolean dark=mode.equals("dark")||(mode.equals("system")&&systemDark());setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);int color=dark?0xff111916:0xfff6f7f9;frame.setBackgroundColor(color);web.setBackgroundColor(color);getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);applyNativeTheme();js("window.themeChanged("+JSONObject.quote(prefs.getString("theme","system"))+","+systemDark()+")");}
    private void launchSettings(Intent intent){try{returning=true;startActivity(intent);}catch(Exception e){returning=false;toast("系統不支援此入口，請在手機設定內搜尋對應項目。");}}
    @Override protected void onResume(){super.onResume();if(ready&&returning){returning=false;startScan();js("window.refreshAppDetails()");}}
    private synchronized byte[] applicationIcon(String pkg){
        if(pkg==null||destroyed||!appNames.containsKey(pkg))return null;
        byte[] cached=appIconCache.get(pkg);if(cached!=null)return cached;
        android.graphics.Bitmap bitmap=null;
        try{
            android.graphics.drawable.Drawable icon=getPackageManager().getApplicationIcon(pkg);
            bitmap=android.graphics.Bitmap.createBitmap(128,128,android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
            int w=icon.getIntrinsicWidth(),h=icon.getIntrinsicHeight();float scale=128f/Math.max(1,Math.max(w,h));
            int width=w>0?Math.max(1,Math.round(w*scale)):128,height=h>0?Math.max(1,Math.round(h*scale)):128;
            icon.setBounds((128-width)/2,(128-height)/2,(128+width)/2,(128+height)/2);icon.draw(canvas);
            ByteArrayOutputStream output=new ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
            byte[] bytes=output.toByteArray();if(!destroyed)appIconCache.put(pkg,bytes);return bytes;
        }catch(Exception e){return null;}finally{if(bitmap!=null)bitmap.recycle();}
    }
    private void loadInstalled(){
        appIconCache.evictAll();
        JSONArray result=new JSONArray();appNames.clear();try{List<PackageInfo> list=getPackageManager().getInstalledPackages(0);for(PackageInfo p:list){if(p.applicationInfo==null)continue;ApplicationInfo a=p.applicationInfo;String name=getPackageManager().getApplicationLabel(a).toString();appNames.put(p.packageName,name);JSONObject item=new JSONObject();item.put("package",p.packageName);item.put("name",name);item.put("version",p.versionName==null?"未知":p.versionName);item.put("system",(a.flags&ApplicationInfo.FLAG_SYSTEM)!=0);long bytes=new File(a.sourceDir).length();if(a.splitSourceDirs!=null)for(String path:a.splitSourceDirs)bytes+=new File(path).length();item.put("apkBytes",bytes>0?bytes:-1);result.put(item);}}catch(Exception e){toast("部分應用資訊讀取失敗。");}installed=result;
    }
    public static String mime(String name){int dot=name.lastIndexOf('.');String ext=dot>=0?name.substring(dot+1).toLowerCase(java.util.Locale.ROOT):"";String m=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);return m==null?"application/octet-stream":m;}
    private File sharedRoot(){return Environment.getExternalStorageDirectory();}
    private boolean isAllowed(File f){return SafePaths.allowed(sharedRoot(),f);}
    private void loadMediaOwners(){
        mediaOwners.clear();sourceLookup="系統未提供來源資訊，使用目錄辨識";
        try(Cursor cursor=getContentResolver().query(android.provider.MediaStore.Files.getContentUri("external"),new String[]{android.provider.MediaStore.MediaColumns.DATA,android.provider.MediaStore.MediaColumns.OWNER_PACKAGE_NAME},android.provider.MediaStore.MediaColumns.IS_TRASHED+"=0 AND "+android.provider.MediaStore.MediaColumns.IS_PENDING+"=0",null,null)){
            if(cursor==null)return;int count=0;while(cursor.moveToNext()&&!cancelScan&&!destroyed){if(++count>150000){sourceLookup="系統來源索引達到上限，部分檔案使用目錄辨識";return;}String path=cursor.getString(0),owner=cursor.getString(1);if(path!=null&&!SourceRules.genericOwner(owner)&&appNames.containsKey(owner)){File f=new File(path);if(isAllowed(f))mediaOwners.put(f.getAbsolutePath(),owner);}}
            sourceLookup="已讀取系統來源索引，搭配目錄辨識";
        }catch(Exception e){sourceLookup="系統來源索引不可讀，改用目錄辨識";}
    }
    private void assignApp(JSONObject f,String relative)throws Exception{
        SourceRules.Match match=SourceRules.resolve(relative,mediaOwners.get(f.optString("raw")),appNames);
        f.put("app",match.label);f.put("sourceHint",match.evidence);
        if(!match.pkg.isEmpty()){f.put("packageName",match.pkg);f.put("packageState",appNames.containsKey(match.pkg)?"installed":"unknown");}
    }
    private boolean scanShared(JSONArray result,JSONArray errors){
        PriorityQueue<File> queue=new PriorityQueue<>((a,b)->{int order=Integer.compare(SourceRules.priority(a.getAbsolutePath()),SourceRules.priority(b.getAbsolutePath()));return order!=0?order:a.getAbsolutePath().compareTo(b.getAbsolutePath());});queue.add(sharedRoot());int visited=0,denied=0;long last=0;boolean limited=false;
        while(!queue.isEmpty()&&!cancelScan&&!destroyed&&!Thread.currentThread().isInterrupted()){
            File dir=queue.remove();File[] children=dir.listFiles();if(children==null){denied++;continue;}
            for(File file:children){if(cancelScan||destroyed)break;if(++visited>100000||result.length()>=50000){limited=true;break;}if(!isAllowed(file))continue;if(file.isDirectory()){queue.add(file);continue;}if(!file.isFile())continue;
                try{JSONObject f=new JSONObject();String id=UUID.randomUUID().toString(),relative=sharedRoot().toPath().relativize(file.getParentFile().toPath()).toString();f.put("id",id);f.put("raw",file.getAbsolutePath());f.put("name",file.getName());f.put("path",relative.isEmpty()?"共用儲存空間":relative);f.put("mime",mime(file.getName()));f.put("size",file.length());f.put("date",file.lastModified());f.put("deletable",file.canWrite());assignApp(f,relative);result.put(f);files.put(id,f);}catch(Exception ignored){}
            }
            if(limited)break;
            if(System.currentTimeMillis()-last>1500){last=System.currentTimeMillis();send(result,new JSONArray(),true,false);}
        }
        if(denied>0)errors.put(denied+" 個目錄無法讀取，結果不包含這些內容。");if(limited)errors.put("已達 50,000 個檔案或 100,000 個項目上限，目前為部分結果。");return limited;
    }
    private String permissionGroup(String permission){
        if(permission.endsWith("CAMERA"))return "相機";if(permission.endsWith("RECORD_AUDIO"))return "麥克風";if(permission.contains("LOCATION"))return "位置";if(permission.contains("READ_MEDIA")||permission.contains("EXTERNAL_STORAGE"))return "照片、影片與儲存";if(permission.contains("CONTACTS"))return "聯絡人";if(permission.contains("CALENDAR"))return "日曆";if(permission.contains("SMS"))return "簡訊";if(permission.contains("PHONE")||permission.contains("CALL_LOG")||permission.endsWith("CALL_PHONE"))return "電話";if(permission.contains("BLUETOOTH")||permission.contains("NEARBY"))return "附近裝置";if(permission.contains("NOTIFICATIONS"))return "通知";if(permission.contains("BODY_SENSORS")||permission.contains("ACTIVITY_RECOGNITION"))return "感應器與活動";return "其他權限";
    }
    private void loadAppDetails(String pkg){
        try{PackageInfo info=getPackageManager().getPackageInfo(pkg,PackageManager.GET_PERMISSIONS);JSONObject d=new JSONObject();d.put("package",pkg);d.put("name",getPackageManager().getApplicationLabel(info.applicationInfo).toString());d.put("version",info.versionName);d.put("versionCode",info.getLongVersionCode());d.put("usageAccess",usageAccess());d.put("system",(info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0);
            if(usageAccess()){try{StorageStats stats=((StorageStatsManager)getSystemService(STORAGE_STATS_SERVICE)).queryStatsForPackage(info.applicationInfo.storageUuid==null?StorageManager.UUID_DEFAULT:info.applicationInfo.storageUuid,pkg,android.os.Process.myUserHandle());d.put("appBytes",stats.getAppBytes());d.put("cacheBytes",stats.getCacheBytes());d.put("dataBytes",Math.max(0,stats.getDataBytes()-stats.getCacheBytes()));d.put("totalBytes",stats.getAppBytes()+stats.getDataBytes());}catch(Exception e){d.put("statsError","系統未提供此 App 的容量資料");}}
            Map<String,int[]> groups=new TreeMap<>();if(info.requestedPermissions!=null)for(int i=0;i<info.requestedPermissions.length;i++){String permission=info.requestedPermissions[i];try{PermissionInfo pi=getPackageManager().getPermissionInfo(permission,0);if((pi.protectionLevel&PermissionInfo.PROTECTION_MASK_BASE)!=PermissionInfo.PROTECTION_DANGEROUS)continue;}catch(Exception e){continue;}String group=permissionGroup(permission);int[] counts=groups.computeIfAbsent(group,k->new int[2]);counts[1]++;if(info.requestedPermissionsFlags!=null&&(info.requestedPermissionsFlags[i]&PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0)counts[0]++;}
            JSONArray permissions=new JSONArray();for(Map.Entry<String,int[]> e:groups.entrySet()){JSONObject item=new JSONObject();item.put("name",e.getKey());int[] n=e.getValue();item.put("status",n[0]==0?"未授權":n[0]==n[1]?"已授權":"部分已授權");permissions.put(item);}d.put("permissions",permissions);d.put("battery",((PowerManager)getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(pkg)?"已豁免電池最佳化":"未豁免電池最佳化");js("window.receiveAppDetails(JSON.parse("+JSONObject.quote(d.toString())+"))");
        }catch(Exception e){js("window.appDetailsError("+JSONObject.quote(pkg)+")");}
    }

    @Override public void onBackPressed(){js("(()=>{const d=document.querySelector('dialog[open]');if(d)d.close();else if(page==='apps'&&activePackage){activePackage=null;appDetail=null;renderInstalled();}else if(page==='browse'&&browseLevel==='files'){leaveFiles();}else if(page!=='clean'){navigate('clean');}else Android.exit();})()");}
    @Override protected void onDestroy(){destroyed=true;appIconCache.evictAll();thumbnailCancel.cancel();thumbnailCache.evictAll();worker.shutdownNow();detailsWorker.shutdownNow();web.removeJavascriptInterface("Android");web.destroy();super.onDestroy();}
}
