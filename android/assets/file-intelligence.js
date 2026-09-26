(function(root){'use strict';
const DAY=86400000;
function normal(s){const map={'視':'视','頻':'频','圖':'图','檔':'档','暫':'暂','緩':'缓','載':'载','電':'电','報':'报','於':'于','週':'周','個':'个','訊':'讯','聲':'声','壓':'压','縮':'缩','舊':'旧'};return String(s||'').normalize('NFKC').toLowerCase().replace(/[視頻圖檔暫緩載電報於週個訊聲壓縮舊]/g,c=>map[c]||c)}
function kind(f){const m=f.mime||'',n=normal(f.name);if(m.startsWith('video/')||/\.(mp4|mkv|webm|mov|avi|3gp)$/.test(n))return '影片';if(m.startsWith('image/')||/\.(jpg|jpeg|png|gif|webp|heic|heif|avif)$/.test(n))return '圖片';if(m.startsWith('audio/')||/\.(mp3|m4a|aac|wav|opus|ogg|flac)$/.test(n))return '音訊';if(/pdf|text|document|sheet|presentation/.test(m)||/\.(pdf|txt|docx?|xlsx?|pptx?)$/.test(n))return '文件';return '其他'}
function parse(query){let rest=normal(query),p={raw:query,terms:[],labels:[],kind:null,app:null,min:null,max:null,days:null,screenshot:false,cache:false,download:false,installer:false,extension:null};
 const aliases=[['Quark',/quark|夸克/g],['Telegram',/telegram|telegr[ae]m|trelefram|电报|\btg\b/g],['Bilibili',/bilibili|哔哩哔哩|嗶哩嗶哩|b站/g],['WhatsApp',/whatsapp|whats app/g]];
 aliases.forEach(([name,re])=>{if(re.test(rest)){p.app=name;p.labels.push('App：'+name);rest=rest.replace(re,' ')}});
 rest=rest.replace(/(大于|超过|至少|小于|少于|不超过|[<>]=?)\s*(\d+(?:\.\d+)?)\s*(gb|mb|kb|g|m|k)(?:以上|以下)?/g,(_,op,n,u)=>{const v=Number(n)*({g:1073741824,m:1048576,k:1024}[u[0]]);if(['小于','少于','不超过','<','<='].includes(op))p.max=v;else p.min=v;p.labels.push((p.max===v?'小於／等於 ':'大於／等於 ')+n+u.toUpperCase());return ' '});
 rest=rest.replace(/(?:最近|近|过去)\s*(\d+)\s*(天|周|个月|月)/g,(_,n,u)=>{p.days=Number(n)*(u==='周'?7:/月/.test(u)?30:1);p.labels.push('最近 '+p.days+' 天修改');return ' '});
 rest=rest.replace(/这周|本周|一周内/g,()=>{p.days=7;p.labels.push('最近 7 天修改');return ' '});
 const typeWords=[['影片',/影片|视频|\bvideos?\b/g],['圖片',/图片|照片|相片|\bimages?\b|\bphotos?\b/g],['音訊',/音讯|音频|音乐|语音|語音|\baudio\b/g],['文件',/文件|文档|\bdocuments?\b/g]];
 typeWords.forEach(([k,re])=>{if(re.test(rest)){p.kind=k;p.labels.push('類型：'+k);rest=rest.replace(re,' ')}});
 rest=rest.replace(/暂存|缓存/g,()=>{p.cache=true;p.labels.push('暫存名稱／目錄');return ' '});
 rest=rest.replace(/安装包|安裝包/g,()=>{p.installer=true;p.labels.push('Android 安裝包');return ' '});
 rest=rest.replace(/下载/g,()=>{p.download=true;p.labels.push('下載目錄');return ' '});
 rest=rest.replace(/螢幕截图|屏幕截图|截图|截屏|\bscreenshots?\b/g,()=>{p.screenshot=true;p.labels.push('截圖名稱／目錄');return ' '});
 rest=rest.replace(/\b(pdf|apk|zip|mp4|jpg|png|docx|xlsx)\b/g,(_,ext)=>{p.extension=ext;p.labels.push('格式：'+ext.toUpperCase());return ' '});
 rest=rest.replace(/幫我找|帮我找|找一下|找出|搜尋|搜索|的/g,' ');p.terms=rest.split(/[\s,，]+/).filter(Boolean);return p;
}
function near(a,b){if(!/^[a-z0-9]+$/.test(a)||a.length<5||Math.abs(a.length-b.length)>1)return false;let i=0,j=0,d=0;while(i<a.length&&j<b.length){if(a[i]===b[j]){i++;j++;continue}if(++d>1)return false;if(a.length>b.length)i++;else if(b.length>a.length)j++;else{i++;j++}}return d+(i<a.length||j<b.length?1:0)<=1}
function search(files,query,now=Date.now()){
 const p=parse(query),matches=[];
 files.forEach(f=>{if(p.kind&&kind(f)!==p.kind)return;if(p.min!==null&&(f.size<0||f.size<p.min))return;if(p.max!==null&&(f.size<0||f.size>p.max))return;if(p.days!==null&&(!f.date||f.date>now||now-f.date>p.days*DAY))return;
 const name=normal(f.name),app=normal(f.app+' '+(f.packageName||'')),path=normal(f.path),all=name+' '+app+' '+path;
 if(p.app){let a=p.app.toLowerCase();if(a==='quark'){if(!/quark|夸克/.test(all))return}else if(a==='bilibili'){if(!/bilibili|bili|danmaku|哔哩|嗶哩/.test(all))return}else if(!all.includes(a))return}
 if(p.cache&&!/(^|[\/ ])(cache|caches|\.cache|tmp|temp)([\/ ]|$)|\.(tmp|temp|cache)$/.test(path+' '+name))return;if(p.download&&!/download|下载/.test(path))return;if(p.installer&&!/\.(apk|apks|xapk)$/.test(name))return;
 if(p.extension&&!name.endsWith('.'+p.extension))return;if(p.screenshot&&!/screenshot|screen_shot|截[图屏]|螢幕擷取/.test(all))return;
 let score=0;for(const term of p.terms){if(name.includes(term))score+=10;else if(app.includes(term))score+=6;else if(path.includes(term))score+=3;else if(all.split(/[^a-z0-9]+/).some(word=>near(term,word)))score+=1;else return}matches.push({file:f,score});
 });matches.sort((a,b)=>b.score-a.score||b.file.date-a.file.date);return {files:matches.map(x=>x.file),parsed:p};
}
function explain(f){const name=normal(f.name),path=normal(f.path),k=kind(f),ext=name.includes('.')?name.split('.').pop():'無副檔名';
 let a={purpose:'用途未知，可能是 App 資料或使用者檔案。',effect:'無法判定；刪除可能讓相關 App 遺失資料或無法使用某些功能。',risk:'未知',recommendation:'不推薦刪除，建議保留；先確認來源或備份。',evidence:'副檔名：'+ext+'；依檔名、類型和所在目錄推測，未分析內容或驗證 App 依賴。'};
 if(/\.(db|sqlite|sqlite3|db-wal|db-shm|key|pem|keystore|json|xml|ini|cfg|conf)$/.test(name)||name==='.nomedia'||/\/(databases|shared_prefs)(\/|$)/.test('/'+path))return {...a,purpose:name==='.nomedia'?'媒體索引標記，通常用於隱藏該目錄的媒體。':'可能是資料庫、設定、帳號狀態或金鑰等 App 功能資料。',risk:'高',effect:name==='.nomedia'?'可能讓原本隱藏的圖片或影片出現在相簿。':'可能影響登入、設定、聊天紀錄、離線功能，或造成 App 異常。',recommendation:'建議保留；優先使用原 App 的儲存管理功能。'};
 if(/\.(bak|backup|crypt\d*|zip|7z|rar)$/.test(name)||/(^|\/)backups?(\/|$)/.test(path))return {...a,purpose:'可能是備份或壓縮封存，用於保存／還原內容。',risk:'中至高',effect:'可能失去還原資料或重新取得壓縮內容的能力；是否影響 App 取決於它是否依賴此檔。',recommendation:'確認另有可用備份且不再需要後才刪除。'};
 if(f.size===0)return {...a,purpose:'零位元組檔案，可能是狀態或設定標記。',risk:'未知',effect:'可能影響 App 的狀態判斷；沒有內容大小不等於沒有用途。',recommendation:'不推薦刪除，建議保留；刪除不能釋放檔案內容空間。'};
 if(/\.(part|crdownload|download)$/.test(name))return {...a,purpose:'可能是尚未完成的下載資料。',risk:'中',effect:'可能中斷下載或失去續傳進度，需要重新下載。',recommendation:'先在原 App 取消不需要的下載，再考慮清理。'};
 if(/(^|\/)(cache|caches|\.cache|tmp|temp|\.temp)(\/|$)/.test(path)||/\.(cache|tmp|temp)$/.test(name))return {...a,purpose:'可能是暫存、預覽或處理中的中間檔案。',risk:'中',effect:'可能失去離線內容、需要重新下載，或讓下次載入變慢；若仍在使用也可能出錯。',recommendation:'確認原 App 未在處理此檔案，且可以重新取得內容後再清理。'};
 if(/\.(apk|apks|xapk)$/.test(name))return {...a,purpose:'Android 安裝包，用於安裝或保留某個版本。',risk:'較低（須確認）',effect:'若確實是獨立下載的安裝包，刪除不會卸載已安裝 App；會失去這份安裝／回退副本。',recommendation:'確認已安裝且不需要保留安裝包時，可考慮刪除。'};
 if(k==='圖片'||k==='影片'||k==='音訊')return {...a,purpose:k+'媒體檔案，可能是個人內容、聊天附件或離線下載。',risk:'中',effect:'會失去這份本機媒體；聊天或離線播放可能無法開啟，且不保證能重新下載。通常不會卸載 App。',recommendation:'先查看預覽或開啟內容；確認不需要或已備份後再刪除。'};
 if(k==='文件')return {...a,purpose:'文件或文字資料，可能是個人文件、匯出結果或 App 使用的內容。',risk:'中',effect:'會失去本機文件；若其他 App 引用它，相關開啟或匯入功能可能失效。',recommendation:'開啟確認並備份重要內容後，再決定是否刪除。'};
 if(/\.log$/.test(name))return {...a,purpose:'可能是執行或診斷日誌。',risk:'較低（須確認）',effect:'會失去故障追查紀錄；仍在寫入的日誌不適合直接刪除。',recommendation:'確認不是正在使用或需要保留的診斷資料後，可考慮刪除。'};
 return a;
}
const api={normal,kind,parse,search,explain};if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.FileIntelligence=api;
})(typeof window!=='undefined'?window:this);
