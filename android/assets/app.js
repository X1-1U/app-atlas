'use strict';
const $=id=>document.getElementById(id),native=typeof Android!=='undefined';
const demo=[
{id:'1',app:'Telegram',name:'旅行記錄_海邊日落.mp4',size:248*1048576,mime:'video/mp4',date:1789992000000,path:'Telegram / Telegram Video'},
{id:'2',app:'Telegram',name:'週末行程.pdf',size:2.4*1048576,mime:'application/pdf',date:1789905600000,path:'Telegram / Telegram Documents'},
{id:'3',app:'Telegram',name:'IMG_20260920_1530.jpg',size:4.8*1048576,mime:'image/jpeg',date:1789819200000,path:'Telegram / Telegram Images'},
{id:'4',app:'Bilibili',name:'已匯出_城市散步.mp4',size:536*1048576,mime:'video/mp4',date:1789732800000,path:'Movies / Bilibili'},
{id:'5',app:'WhatsApp',name:'語音備忘錄.opus',size:1.2*1048576,mime:'audio/ogg',date:1789646400000,path:'WhatsApp / Media / Audio'},
{id:'6',app:'Telegram',name:'設計參考.zip',size:128*1048576,mime:'application/zip',date:1789560000000,path:'Telegram / Telegram Documents'},
{id:'7',app:'WhatsApp',name:'朋友聚會.jpg',size:3.6*1048576,mime:'image/jpeg',date:1789473600000,path:'WhatsApp / Media / Images'}];
const DAY=86400000, now=Date.now();
if(!native) demo.push(
{id:'8',app:'Telegram',name:'preview_014.cache',size:46*1048576,mime:'application/octet-stream',date:now-21*DAY,path:'Telegram / cache'},
{id:'9',app:'待確認來源',name:'session.dat',size:78*1048576,mime:'application/octet-stream',date:now-60*DAY,path:'Documents / com.example.oldapp',packageName:'com.example.oldapp',packageState:'unknown'},
{id:'10',app:'剪貼簿',name:'clip_20260812.png',size:3.4*1048576,mime:'image/png',date:now-45*DAY,path:'Pictures / clipboard'},
{id:'11',app:'下載',name:'old_installer.apk',size:92*1048576,mime:'application/vnd.android.package-archive',date:now-40*DAY,path:'Download'},
{id:'12',app:'下載',name:'download_018.part',size:34*1048576,mime:'application/octet-stream',date:now-12*DAY,path:'Download'},
{id:'13',app:'下載',name:'transfer.tmp',size:0,mime:'application/octet-stream',date:now-16*DAY,path:'Download / temp'});
if(!native){demo.push({id:'q1',app:'夸克',packageName:'com.quark.browser',name:'閱讀筆記.txt',mime:'text/plain',path:'Download/QuarkDownloads',size:15000,date:now,sourceHint:'依夸克／Quark 系列目錄名稱推測'},{id:'q2',app:'夸克',packageName:'com.quark.browser',name:'旅途影片.mp4',mime:'video/mp4',path:'Quark/Download',size:150*1048576,date:now,sourceHint:'依夸克／Quark 系列目錄名稱推測'},{id:'q3',app:'夸克',packageName:'com.quark.browser',name:'下載的圖片.jpg',mime:'image/jpeg',path:'Download',size:2*1048576,date:now,sourceHint:'Android 媒體索引登記的建立 App'},{id:'u1',app:'來源未確認的下載',name:'來源不明的文件.txt',mime:'text/plain',path:'Download',size:5000,date:now,sourceHint:'未找到可確認的 App 來源'})}
let theme=native?Android.theme():'system';try{if(!native)theme=localStorage.getItem('atlas-theme')||'system'}catch(e){}
let state={files:native?[]:demo,roots:[],busy:false,errors:[],limited:false,theme,osDark:native?Android.systemDark():false,storage:native?null:{total:256*1073741824,available:87.6*1073741824}},app='全部',type='全部',page=native?Android.page():'clean',selected=null;
state.installed=native?[]:[{package:'org.telegram.messenger',name:'Telegram',version:'示範版本',apkBytes:85*1048576,system:false},{package:'tv.danmaku.bili',name:'Bilibili',version:'示範版本',apkBytes:170*1048576,system:false},{package:'com.whatsapp',name:'WhatsApp',version:'示範版本',apkBytes:95*1048576,system:false}];state.allFiles=!native;state.usageAccess=!native;state.scanDone=!native;
let activePackage=null,appDetail=null,appQuery='',showSystem=false;
function hasScanAccess(){return !native||state.allFiles||state.roots.length>0}
function grantFiles(){if(native)Android.grantFiles();else toast('手機版會開啟「所有檔案存取權」設定，授權後自動掃描。')}
function grantUsage(){if(native)Android.grantUsage();else toast('手機版會開啟「使用情況存取權」，用來讀取各 App 的容量統計。')}
let showUnassignedDownloads=false;
let browseLevel='sources',sourceLayout='grid',sourceScroll=0;
try{sourceLayout=native?Android.sourceLayout():localStorage.getItem('atlas-source-layout')||'grid'}catch(e){}
function enterFiles(name){sourceScroll=window.scrollY;app=name;browseLevel='files';showUnassignedDownloads=false;$('source-notes').open=false;$('search').value='';type='全部';render();window.scrollTo(0,0)}
function leaveFiles(){browseLevel='sources';render();requestAnimationFrame(()=>window.scrollTo(0,sourceScroll))}
function changeLayout(mode){sourceLayout=mode;if(native)Android.setSourceLayout(mode);else try{localStorage.setItem('atlas-source-layout',mode)}catch(e){}render()}

let cleanCategory='temp' ,picked=new Set();
const categories=[['temp','暫存與未完成下載','至少 7 天未修改'],['residual','疑似 App 殘留','安裝狀態未確認'],['clipboard','剪貼簿相關檔案','僅限可讀取的目錄'],['installer','舊安裝包','至少 30 天未修改'],['empty','零位元組檔案','可能仍有用途'],['large','大型檔案','100 MB 以上，非垃圾判定']];
function applyTheme(){const mode=state.theme||'system';const dark=mode==='dark'||(mode==='system'&&(native?!!state.osDark:matchMedia('(prefers-color-scheme: dark)').matches));document.documentElement.dataset.theme=dark?'dark':'light'}
matchMedia('(prefers-color-scheme: dark)').addEventListener('change',()=>{if(!native)applyTheme()});
function setTheme(mode){state.theme=mode;if(native)Android.setTheme(mode);else{try{localStorage.setItem('atlas-theme',mode)}catch(e){}}applyTheme();renderSettings()}

const types=['全部','影片','圖片','文件','音訊','其他'];
function kind(f){return FileIntelligence.kind(f)}
function bytes(n){if(n===undefined||n===null||!Number.isFinite(n)||n<0)return '未取得';if(n>=1073741824)return (n/1073741824).toFixed(2)+' GB';if(n>=1048576)return (n/1048576).toFixed(1)+' MB';if(n>=1024)return (n/1024).toFixed(1)+' KB';return n+' B'}
function el(tag,cls,text){let n=document.createElement(tag);if(cls)n.className=cls;if(text!==undefined)n.textContent=text;return n}
function toast(s){$('toast').textContent=s;$('toast').hidden=false;clearTimeout(toast.timer);toast.timer=setTimeout(()=>$('toast').hidden=true,3500)}
function render(){
 applyTheme();
 $('mode').textContent=native?'離線 · 本機檔案':'離線 · 介面示範';
 $('notice').textContent=state.busy?'正在掃描 · 已找到 '+state.files.length+' 個檔案…':!native?'示範資料，不是你的手機檔案。':state.errors.length?state.errors.join('；'):!hasScanAccess()?'尚未取得檔案管理授權，沒有掃描你的檔案。':state.limited?'目前為部分掃描結果。':state.allFiles?'已掃描可讀取的共用儲存空間；不含其他 App 私有資料。':'目前僅掃描你曾手動授權的資料夾。';
 $('refresh').disabled=state.busy;$('add').disabled=state.busy;
 $('browser').hidden=page!=='browse';$('settings').hidden=page!=='settings';$('cleanup').hidden=page!=='clean';$('installedpage').hidden=page!=='apps';$('authorization').hidden=!['clean','browse'].includes(page)||!!state.allFiles;
 $('heading').textContent=page==='apps'?'每個 App，一目了然。':page==='settings'?'你的資料，你做主。':page==='clean'?'空間，留給重要的事。':'檔案，跟著 App 找。';
 $('subtitle').textContent=page==='apps'?'查看版本、儲存用量與權限。':page==='settings'?'管理分類來源與本機存取權限。':page==='clean'?'先檢查，再清理。每一項都有判斷依據。':'不用記住路徑，從熟悉的 App 開始。';
 document.querySelectorAll('[data-page]').forEach(b=>b.classList.toggle('active',b.dataset.page===page));
 const names=[...new Set(['全部',...state.files.map(f=>f.app),...state.roots.map(r=>r.app),...(state.installed||[]).filter(a=>!a.system).map(a=>a.name)])];
 $('browse-sources').hidden=browseLevel!=='sources';$('browse-files').hidden=browseLevel!=='files';
 const inFiles=page==='browse'&&browseLevel==='files';$('back-sources').hidden=!inFiles;$('subtitle').hidden=inFiles;
 $('notice').hidden=!native||(inFiles&&!state.busy&&!state.errors.length&&hasScanAccess()&&!state.limited&&state.allFiles);
 if(inFiles){$('heading').textContent=app==='全部'?'全部檔案':app+' 的檔案'}
 $('apps').classList.toggle('list-view',sourceLayout==='list');['grid','list'].forEach(v=>{$('view-'+v).classList.toggle('active',sourceLayout===v);$('view-'+v).setAttribute('aria-pressed',String(sourceLayout===v))});
 const counts=new Map();state.files.forEach(f=>counts.set(f.app,(counts.get(f.app)||0)+1));
 $('apps').replaceChildren();names.filter(n=>n.toLowerCase().includes($('source-search').value.trim().toLowerCase())).forEach(name=>{let b=el('button','app'+(app===name?' active':''));b.dataset.app=name;b.setAttribute('aria-pressed',String(app===name));b.append(appIcon(name),el('strong','',name));let count=name==='全部'?state.files.length:counts.get(name)||0;b.append(el('small','',count?count+' 個檔案':!hasScanAccess()?'待授權':state.busy?'掃描中':'尚未辨識到來源'));b.onclick=()=>enterFiles(name);$('apps').append(b)});
 const af=state.files.filter(f=>showUnassignedDownloads?f.app==='來源未確認的下載':app==='全部'||f.app===app);renderSourceHelp(af);$('summary-label').textContent=showUnassignedDownloads?'來源未確認的下載（不代表來自 '+app+'）':app==='全部'?'已索引檔案總量':app+' · 已索引檔案';$('total').textContent=!hasScanAccess()?'待授權':!state.scanDone&&!af.length?'掃描中':!af.length?'未找到':bytes(af.reduce((n,f)=>n+Math.max(0,f.size),0));$('total-count').textContent=' / '+af.length+' 個';
 $('filters').replaceChildren();types.forEach(t=>{let b=el('button','filter'+(type===t?' active':''),t);b.setAttribute('aria-pressed',String(type===t));b.onclick=()=>{type=t;render()};$('filters').append(b)});
 let query=$('search').value.trim();let found=FileIntelligence.search(af,query);let fs=found.files.filter(f=>type==='全部'||kind(f)===type);const sort=$('sort').value;if(sort!=='relevance')fs.sort((a,b)=>sort==='size'?b.size-a.size:sort==='name'?a.name.localeCompare(b.name):b.date-a.date);
 $('search-help').textContent=query?'搜尋目前分類 · '+(found.parsed.labels.join(' · ')||'多關鍵字、檔名與來源比對')+' · 時間依最後修改日期':'';
 $('result-count').textContent=fs.length+' 個結果'+(fs.length>300?' · 顯示前 300 個，請搜尋縮小範圍':'');$('files').replaceChildren();
 if(!fs.length){let e=el('div','empty');e.append(el('strong','',state.busy?'正在整理…':'這裡還沒有符合的檔案'),el('p','',query||type!=='全部'?'試試其他關鍵字或類型。':!hasScanAccess()?'請先授予檔案管理權限，返回後會自動掃描。':'可讀取範圍未找到相關檔案；私有快取無法在此列出。'));$('files').append(e)}
 fs.slice(0,300).forEach(f=>{let b=el('button','file');b.append(filePreview(f));let t=el('div','file-text');t.append(el('div','filename',f.name),el('div','meta',f.app+' · '+(f.sourceHint?f.sourceHint+' · ':'')+(f.date?new Date(f.date).toLocaleDateString('zh-TW'):'日期未知')));b.append(t,el('span','size',bytes(f.size)),el('span','meta','›'));b.onclick=()=>detail(f);$('files').append(b)});
 renderAuthorization();if(page==='clean')renderCleanup();if(page==='settings')renderSettings();if(page==='apps')renderInstalled();
}
function renderSourceHelp(related){const panel=$('source-help');panel.replaceChildren();panel.hidden=app==='全部';$('source-notes').hidden=panel.hidden;if(panel.hidden)return;
 panel.append(el('strong','',showUnassignedDownloads?'以下下載尚未確認來源，不算作 '+app+' 的檔案':app+' · App 來源辨識'));
 panel.append(el('p','subtle',related.length?'依系統建立者資訊或目錄名稱歸類，點檔案可查看依據。':'目前未辨識到相關檔案，不代表這個 App 沒有下載內容。檔案可能使用其他目錄名稱，或保存在系統限制讀取的區域。'));
 if(state.sourceLookup)panel.append(el('p','subtle',state.sourceLookup));
 if(/夸克|quark/i.test(app))panel.append(el('p','subtle','會比對 Quark、QuarkDownloads、QuarkCloudDrive、夸克等目錄及系統來源。若檔案在 Android/data，需先從夸克匯出到共用儲存空間；無法直接繞過限制。'));
 const unknown=state.files.filter(f=>f.app==='來源未確認的下載').length;let b=el('button','btn',showUnassignedDownloads?'返回 '+app+' 分類':'查看來源未確認的下載（'+unknown+'）');b.onclick=()=>{showUnassignedDownloads=!showUnassignedDownloads;$('search').value='';type='全部';render()};panel.append(b);
}
function iconPackage(name,explicit){
 const installed=state.installed||[];
 if(explicit)return installed.some(a=>a.package===explicit)?explicit:null;
 const matches=installed.filter(a=>a.name===name);if(matches.length===1)return matches[0].package;if(matches.length>1)return null;
 const candidates=[...new Set(state.files.filter(f=>f.app===name&&f.packageName).map(f=>f.packageName))];
 return candidates.length===1&&installed.some(a=>a.package===candidates[0])?candidates[0]:null;
}
function appIcon(name,explicit){
 const symbols={'全部':'▦','下載':'↓','來源未確認的下載':'↓','剪貼簿':'▤','待確認來源':'?','其他檔案':'▱','相機':'◉','圖片':'▧'};
 const box=el('span','app-icon',symbols[name]||name.slice(0,1));box.setAttribute('aria-hidden','true');
 const pkg=iconPackage(name,explicit);if(!native||!pkg)return box;
 const img=el('img');img.alt='';img.loading='lazy';img.decoding='async';
 img.onload=()=>{box.classList.add('has-image');box.replaceChildren(img)};
 img.onerror=()=>{img.remove();box.classList.remove('has-image')};
 img.src='https://appassets.androidplatform.net/app-icon/'+encodeURIComponent(pkg);box.append(img);return box;
}
function filePreview(f,large=false){const k=kind(f);if(k!=='影片'&&k!=='圖片')return el('span','file-icon',{'文件':'DOC','音訊':'AUD','其他':'FILE'}[k]||'FILE');let t=el('span','thumb'+(large?' detail-thumb':''),native?'載入預覽…':'示範檔案');if(native){let img=el('img');img.alt=f.name+' 的預覽';img.loading=large?'eager':'lazy';img.decoding='async';img.src='https://appassets.androidplatform.net/thumb/'+encodeURIComponent(f.id);img.onerror=()=>{img.remove();t.replaceChildren(el('span','','無法預覽'))};t.append(img)}if(k==='影片')t.append(el('span','video-mark','▶'));return t}
function detail(f){selected=f;$('detail-title').textContent=f.name;let body=$('detail-body');body.replaceChildren();if(['圖片','影片'].includes(kind(f)))body.append(filePreview(f,true));['來源分類：'+f.app+(f.sourceHint?'（'+f.sourceHint+'）':''),'位置：'+f.path,'大小：'+bytes(f.size),'類型：'+f.mime,'開啟時會交給你選擇的其他 App；該 App 有自己的網路與隱私設定。'].forEach(s=>body.append(el('p','',s)));CleanupRules.classify(f).forEach(r=>body.append(el('p','detail-reason',r.reason)));
 let a=FileIntelligence.explain(f),section=el('section','explain-file'),dl=el('dl');section.append(el('h3','','檔案用途與刪除影響'));[['介紹與用途',a.purpose],['刪除後可能影響',a.effect],['風險判斷',a.risk],['刪除建議',a.recommendation]].forEach(([title,value])=>dl.append(el('dt','',title),el('dd',a.risk==='未知'&&title==='刪除建議'?'unknown-advice':'',value)));section.append(dl,el('p','evidence',a.evidence));body.append(section);$('delete-file').hidden=!native||!f.deletable;$('detail').showModal();$('detail').focus({preventScroll:true});$('detail').scrollTop=0}
function renderSettings(){
 let c=$('settings');c.className='settings';c.replaceChildren(el('h2','','外觀主題'));let ts=el('div','theme-options');[['system','跟隨系統'],['light','明亮'],['dark','深色']].forEach(([v,label])=>{let b=el('button','btn'+((state.theme||'system')===v?' active':''),label);b.setAttribute('aria-pressed',String((state.theme||'system')===v));b.onclick=()=>setTheme(v);ts.append(b)});c.append(ts,el('h2','','離線與權限'),el('p','','不申請網路或無障礙權限。檔案管理授權用來掃描共用儲存空間；使用情況授權用來查詢 App 容量。安裝清單只在本機使用。'),el('p','','清理候選不一定無用；不預選、不自動刪除。授權管理檔案後，可讀寫共用儲存空間中的大量檔案。'),el('h2','','剪貼簿紀錄'),el('p','','可清空目前的系統剪貼簿；無法讀取或保證刪除三星鍵盤、其他 App 的歷史紀錄。此操作不會刪掉原始圖片或文件。'));
 let clip=el('button','btn','清空目前剪貼簿…');clip.onclick=()=>native?Android.clearClipboard():toast('示範模式：手機版會再次確認後清空目前剪貼簿。');c.append(clip,accessPanel(),el('h2','','進階：手動資料夾（可選）'));if(!state.roots.length)c.append(el('p','',native?'尚未關聯資料夾。':'介面示範：手機版會在這裡列出授權來源。'));state.roots.forEach((r,i)=>{let d=el('div','rootrow');d.append(el('strong','',r.app),el('p','',r.name));let b=el('button','btn','解除關聯');b.disabled=state.busy;b.onclick=()=>Android.removeRoot(i);d.append(b);c.append(d)});let b=el('button','btn primary','＋ 關聯資料夾');b.disabled=state.busy;b.onclick=add;c.append(b,el('p','','拾檔 v0.8 · 檔案留在你的手機'));
}
function renderCleanup(){
 const c=$('cleanup'),report=CleanupRules.summary(state.files);c.replaceChildren();
 let hero=el('div','clean-hero'),row=el('div','hero-row'),text=el('div');text.append(el('p','','待檢查候選 · 不等於可安全釋放'),el('div','amount',!hasScanAccess()?'待授權':!state.scanDone?'檢查中':bytes(report.bytes)),el('p','',!hasScanAccess()?'尚未掃描你的檔案':report.candidates.length+' 個候選檔案 · 不重複計算'));
 row.append(text,el('div','hero-ring','◈'));hero.append(row);let scan=el('button','btn',state.busy?'正在掃描…':'重新檢查空間 ↻');scan.disabled=state.busy;scan.onclick=()=>!hasScanAccess()?grantFiles():native?Android.scan():toast('已依示範資料重新檢查；沒有讀取電腦檔案。');hero.append(scan);if(state.busy){let stop=el('button','btn','停止掃描');stop.onclick=()=>native&&Android.cancelScan();hero.append(stop)}c.append(hero);
 let storage=el('div','storage-box');storage.append(el('h2','','內部儲存空間'));if(state.storage){let used=Math.max(0,state.storage.total-state.storage.available),values=el('div','storage-values');values.append(el('span','','已用 '+bytes(used)),el('span','','可用 '+bytes(state.storage.available)));let track=el('div','storage-track'),bar=el('div');bar.style.width=Math.min(100,used/Math.max(1,state.storage.total)*100)+'%';track.append(bar);storage.append(track,values,el('p','subtle',native?'裝置資料分割區統計；不代表已逐檔掃描全部空間。':'示範容量，不是你的手機實際容量。'))}else storage.append(el('p','subtle','裝置容量會在手機版顯示。'));
 let link=el('div','system-link');link.append(el('span','','系統／其他 App 私有資料：不可逐檔讀取'));let sys=el('button','btn','系統儲存設定');sys.onclick=()=>native?Android.storageSettings():toast('手機版會開啟 Android 系統儲存設定。');link.append(sys);storage.append(link);c.append(storage);
 let h=el('div','section-head');h.append(el('h2','','清理檢查'),el('span','badge','僅限授權範圍'));c.append(h);let grid=el('div','clean-grid');categories.forEach(([key,label,note])=>{let fs=report.categories[key],b=el('button','clean-card'+(cleanCategory===key?' active':''));b.dataset.category=key;b.setAttribute('aria-pressed',String(cleanCategory===key));b.append(el('strong','',label),el('span','card-size',!hasScanAccess()?'待授權':!state.scanDone?'檢查中':bytes(fs.reduce((n,f)=>n+Math.max(0,f.size),0))),el('small','',fs.length+' 個 · '+note));b.onclick=()=>{cleanCategory=key;picked.clear();renderCleanup()};grid.append(b)});c.append(grid);
 let ch=el('div','clean-header');ch.append(el('h2','',categories.find(x=>x[0]===cleanCategory)[1]));let folder=el('button','textbtn','授權管理');folder.onclick=()=>navigate('settings');ch.append(folder);c.append(ch);
 const fs=report.categories[cleanCategory].slice().sort((a,b)=>b.size-a.size),visible=fs.slice(0,100);const valid=new Set(fs.map(f=>f.id));picked=new Set([...picked].filter(id=>valid.has(id)));
 const notes={temp:'依目錄、副檔名與修改時間辨識。仍在使用的暫存或未完成下載也可能符合，請先查看。',residual:'只列出帶套件名稱但無法確認安裝狀態的檔案。這不是已卸載的證明，請確認來源。',clipboard:'辨識名為 clipboard 的可讀取目錄，不等於三星鍵盤的歷史紀錄。',installer:'舊安裝包可能是你保留的版本或備份，請確認不需要後再刪除。',empty:'0 B 檔案不佔檔案內容空間，可能是必要的設定標記。',large:'大型檔案不代表無用。先開啟確認內容，再決定是否保留。'};
 c.append(el('p','rule-note',notes[cleanCategory]));if(cleanCategory==='clipboard'){let clip=el('button','btn','清空目前剪貼簿…');clip.onclick=()=>native?Android.clearClipboard():toast('手機版會確認後清空目前剪貼簿，無法代替清除鍵盤歷史。');c.append(clip)}
 let list=el('div','files');if(!visible.length){let e=el('div','empty');e.append(el('strong','',!hasScanAccess()?'尚未取得掃描授權':state.busy?'正在檢查…':'目前掃描範圍沒有符合項目'),el('p','',!hasScanAccess()?'先授予檔案管理權限，返回後會自動掃描。':'只檢查可讀取的檔案；未找到候選不代表其他 App 沒有私有快取。'));list.append(e)}
 visible.forEach(f=>{let line=el('div','clean-row'),check=el('input','pick');check.type='checkbox';check.checked=picked.has(f.id);check.disabled=state.busy||(native&&!f.deletable);check.setAttribute('aria-label','選取 '+f.name);check.onchange=()=>{check.checked?picked.add(f.id):picked.delete(f.id);renderBatch(fs)};let b=el('button','file'),t=el('div','file-text');t.append(el('div','filename',f.name),el('div','meta',f.app+' · '+f.path));b.append(filePreview(f),t,el('span','size',bytes(f.size)));b.onclick=()=>detail(f);line.append(check,b);list.append(line)});c.append(list);if(fs.length>100)c.append(el('p','subtle','先顯示最大的 100 個候選。其餘可在「找檔案」搜尋。'));
 let batch=el('div','batchbar');batch.id='batchbar';c.append(batch);renderBatch(fs);

}
function renderBatch(fs){const bar=$('batchbar');if(!bar)return;const chosen=fs.filter(f=>picked.has(f.id));bar.style.display=chosen.length?'flex':'none';bar.replaceChildren(el('span','','已選 '+chosen.length+' 個 · '+bytes(chosen.reduce((n,f)=>n+Math.max(0,f.size),0))));let b=el('button','btn danger','檢查並刪除…');b.disabled=!chosen.length||state.busy;b.onclick=()=>{if(native)Android.deleteFiles(JSON.stringify(chosen.map(f=>f.id)));else toast('示範模式不會刪除檔案；手機版會列出清單，要求再次確認。')};bar.append(b)}
function add(){$('app-name').value=app==='全部'?'':app;$('folder-dialog').showModal()}
$('add').onclick=()=>navigate('settings');$('choose-folder').onclick=()=>{let name=$('app-name').value.trim();if(!name){$('app-name').focus();return}if(native){Android.pickFolder(name);$('folder-dialog').close()}else{toast('此處是介面預覽；安裝 APK 後可選擇手機資料夾。')}};
$('open-file').onclick=()=>{if(native&&selected)Android.openFile(selected.id);else toast('示範檔案無實際內容；手機版會呼叫檔案開啟工具。')};
$('delete-file').onclick=()=>{if(native&&selected){Android.deleteFile(selected.id);$('detail').close()}};
$('refresh').onclick=()=>!hasScanAccess()?grantFiles():native?Android.scan():toast('示範模式；手機版會重新掃描已授權資料夾。');
$('back-sources').onclick=leaveFiles;$('source-search').oninput=render;$('view-grid').onclick=()=>changeLayout('grid');$('view-list').onclick=()=>changeLayout('list');
let searchTimer;$('search').oninput=()=>{clearTimeout(searchTimer);searchTimer=setTimeout(render,180)};['TG 影片','最近7天 圖片','大於100MB','截圖'].forEach(text=>{let b=el('button','btn',text);b.onclick=()=>{$('search').value=text;type='全部';render()};$('search-suggestions').append(b)});$('sort').onchange=render;document.querySelectorAll('[data-page]').forEach(b=>b.onclick=()=>navigate(b.dataset.page));
function navigate(next){page=next;if(next==='browse')browseLevel='sources';if(native)Android.rememberPage(page);render()}
function accessPanel(){const panel=el('div','access-card');panel.append(el('h2','','授權與自動掃描'));let f=el('div','access-row');f.append(el('strong','',state.allFiles?'✓ 檔案管理已授權':'1. 檔案管理'),el('p','','允許存取共用儲存空間，返回 App 後自動掃描、分類。Android/data 與其他 App 私有目錄仍受限制。'));let fb=el('button','btn primary',state.allFiles?'管理檔案授權':'授予檔案管理權限');fb.onclick=grantFiles;f.append(fb);let u=el('div','access-row');u.append(el('strong','',state.usageAccess?'✓ 使用情況已授權':'2. App 容量統計（可選）'),el('p','','系統稱為「使用情況存取權」。拾檔只用它查詢 App、資料與快取大小，不讀取使用時間紀錄。'));let ub=el('button','btn',state.usageAccess?'管理使用情況授權':'授予容量統計權限');ub.onclick=grantUsage;u.append(ub);panel.append(f,u,el('p','','已安裝應用清單由 Android 套件查詢提供，不另彈出授權視窗。分類為虛擬分類，不建立實體資料夾。'));return panel}
function renderAuthorization(){const c=$('authorization');c.replaceChildren();if(!state.allFiles)c.append(accessPanel())}
function renderInstalled(){const c=$('installedpage');if(activePackage){renderAppDetail();return}if($('installed-list')&&c.contains($('installed-list'))){renderInstalledList();return}c.replaceChildren();let search=el('input');search.type='search';search.value=appQuery;search.placeholder='搜尋應用名稱或套件名稱';search.setAttribute('aria-label','搜尋已安裝應用');let wrap=el('div','search');wrap.append(search);let label=el('label','show-system'),check=el('input');check.type='checkbox';check.checked=showSystem;label.append(check,el('span','','顯示系統應用'));c.append(wrap,label,el('p','subtle','列表大小為安裝檔案大小；完整 App／資料／快取用量請點入查看。'));let list=el('div');list.id='installed-list';c.append(list);search.oninput=()=>{appQuery=search.value;renderInstalledList()};check.onchange=()=>{showSystem=check.checked;renderInstalledList()};renderInstalledList()}
function renderInstalledList(){const list=$('installed-list');if(!list)return;list.replaceChildren();const q=appQuery.trim().toLowerCase(),items=(state.installed||[]).filter(a=>(showSystem||!a.system)&&(a.name+' '+a.package).toLowerCase().includes(q)).sort((a,b)=>a.name.localeCompare(b.name));list.append(el('p','subtle',items.length+' 個應用'));items.forEach(a=>{let b=el('button','installed-row'),t=el('div','file-text');t.append(el('strong','',a.name),el('span','app-meta','版本 '+a.version),el('span','app-meta',a.package));b.append(appIcon(a.name,a.package),t,el('span','size',bytes(a.apkBytes)));b.onclick=()=>showApp(a.package);list.append(b)});if(!items.length)list.append(el('p','subtle',state.busy?'正在讀取應用清單…':'沒有符合的應用。'))}
function showApp(pkg){activePackage=pkg;appDetail=null;renderAppDetail();if(native)Android.appDetails(pkg);else{const a=state.installed.find(a=>a.package===pkg);appDetail={...a,appBytes:a.apkBytes,dataBytes:380*1048576,cacheBytes:64*1048576,totalBytes:a.apkBytes+444*1048576,permissions:[{name:'相機',status:'未授權'},{name:'位置',status:'部分已授權'},{name:'照片、影片與儲存',status:'已授權'}],battery:'未豁免電池最佳化',usageAccess:true};renderAppDetail()}}
function appSettings(){if(native)Android.appSettings(activePackage);else toast('手機版會開啟此 App 的系統「應用程式資訊」，由你修改權限或卸載。')}
function renderAppDetail(){let c=$('installedpage');c.replaceChildren();let back=el('button','btn','‹ 返回應用列表');back.onclick=()=>{activePackage=null;appDetail=null;renderInstalled()};c.append(back);if(!appDetail){c.append(el('p','subtle','正在讀取 App 詳情…'));return}const a=appDetail;let head=el('div','app-detail-header');head.append(appIcon(a.name||a.package,a.package),el('h2','',a.name||a.package),el('p','','版本 '+(a.version||'未知')+(a.versionCode?'（'+a.versionCode+'）':'')),el('p','',a.package));c.append(head);if(a.error){c.append(el('p','subtle','讀取失敗，App 可能已卸載或系統限制存取。'));return}let grid=el('div','metric-grid');[['總計',a.totalBytes],['App 本體',a.appBytes],['資料（不含快取）',a.dataBytes],['快取',a.cacheBytes]].forEach(([label,value])=>{let d=el('div','metric');d.append(el('span','',label),el('strong','',bytes(value)));grid.append(d)});c.append(grid);if(!a.usageAccess){c.append(el('p','subtle','尚未授予容量統計權限，因此顯示「未取得」，不是 0 B。'));let b=el('button','btn primary','授予容量統計權限');b.onclick=grantUsage;c.append(b)}else if(a.statsError)c.append(el('p','subtle',a.statsError));else c.append(el('p','subtle','系統統計可能有更新延遲或共用 UID 計量差異；不含無法歸屬的共用檔案。'));
 c.append(el('h2','','權限與電池'),el('p','subtle','點選後進入系統 App 資訊，再選「權限」或「電池」。前景、單次、精確位置等細節以系統頁為準。'));
 (a.permissions||[]).forEach(p=>{let b=el('button','permission-row'),left=el('div');left.append(el('strong','',p.name),el('small','','前往系統權限設定'));b.append(left,el('span','',p.status+' ›'));b.onclick=appSettings;c.append(b)});if(!(a.permissions||[]).length)c.append(el('p','subtle','未列出需要使用者授權的執行階段權限；特殊存取權請到系統 App 資訊確認。'));let battery=el('button','permission-row');battery.append(el('strong','','電池'),el('span','',(a.battery||'以系統設定為準')+' ›'));battery.onclick=appSettings;c.append(battery,el('p','subtle','電池豁免狀態不等同三星所有後台限制設定。'));
 let actions=el('div','app-actions'),related=el('button','btn','查看相關共用檔案');related.onclick=()=>{navigate('browse');enterFiles(a.name)};let settings=el('button','btn','系統應用資訊');settings.onclick=appSettings;let uninstall=el('button','btn danger',a.system?'到系統管理／停用':'到系統卸載…');uninstall.onclick=appSettings;actions.append(related,settings,uninstall);c.append(actions,el('p','subtle','不會自動卸載；請在系統頁面自行確認。'));
}
window.receiveAppDetails=data=>{if(data.package===activePackage){appDetail=data;renderAppDetail()}};
window.appDetailsError=pkg=>{if(pkg===activePackage){appDetail={package:pkg,error:true};renderAppDetail()}};
window.refreshAppDetails=()=>{if(activePackage&&native)Android.appDetails(activePackage)};
window.themeChanged=(mode,osDark)=>{state.theme=mode;state.osDark=osDark;applyTheme();renderSettings()};
window.receiveDeletion=delta=>{const y=window.scrollY,removed=new Set(delta.removed||[]);state.files=state.files.filter(f=>!removed.has(f.id));for(const id of removed)picked.delete(id);if(selected&&removed.has(selected.id)){selected=null;if($('detail').open)$('detail').close()}if(delta.storage)state.storage=delta.storage;render();requestAnimationFrame(()=>window.scrollTo(0,y))};
window.receiveState=data=>{const savedTheme=state.theme;state=data;state.theme=savedTheme;if(data.busy)picked.clear();render()};window.nativeToast=toast;render();if(native)Android.ready();
