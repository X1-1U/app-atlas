package local.appatlas;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;import java.util.concurrent.*;
/** Only opaque, explicitly granted, read-only file tokens are served. */
public final class LocalFiles extends ContentProvider {
 static final ConcurrentHashMap<String,File> grants=new ConcurrentHashMap<>();
 public boolean onCreate(){return true;}
 static Uri share(File file){String key=java.util.UUID.randomUUID().toString();grants.put(key,file);return Uri.parse("content://local.appatlas.files/"+key);}
 private File file(Uri uri)throws FileNotFoundException{File f=grants.get(uri.getLastPathSegment());if(f==null||!Environment.isExternalStorageManager()||!SafePaths.allowed(Environment.getExternalStorageDirectory(),f)||!f.isFile())throw new FileNotFoundException();return f;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri uri){try{return MainActivity.mime(file(uri).getName());}catch(Exception e){return "application/octet-stream";}}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{File f=file(uri);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] values=new Object[cols.length];for(int i=0;i<cols.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?f.getName():OpenableColumns.SIZE.equals(cols[i])?f.length():null;c.addRow(values);return c;}catch(Exception e){return null;}}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
