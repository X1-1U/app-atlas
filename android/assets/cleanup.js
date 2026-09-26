/* Explainable local rules. Candidates require review; no auto-deletion. */
(function(root){
'use strict';
const DAY=86400000;
function ageDays(file,now){return file.date>0&&file.date<=now?Math.floor((now-file.date)/DAY):-1}
function classify(file,now=Date.now()){
 const path=(file.path||'').toLowerCase().split(/[/:\\]+/).map(p=>p.trim()).filter(Boolean);
 const name=(file.name||'').toLowerCase(), age=ageDays(file,now), found=[];
 if(age>=7&&(/\.(tmp|temp|cache|part|crdownload)$/i.test(name)||path.some(p=>['cache','.cache','caches','tmp','temp','.temp'].includes(p))))
  found.push({key:'temp',reason:'位於暫存目錄或使用暫存副檔名，且至少 7 天未修改。可能仍被 App 使用，請確認後刪除。'});
 if(file.packageState==='unknown'&&file.packageName)
  found.push({key:'residual',reason:'路徑包含套件名稱 '+file.packageName+'，但目前無法確認安裝狀態；也可能是可見性限制、其他使用者或保留的資料。尚不能判定已卸載。'});
 if(age>=14&&path.some(p=>['clipboard','.clipboard','clipboards'].includes(p)))
  found.push({key:'clipboard',reason:'位於名為 clipboard 的目錄，至少 14 天未修改。僅為檔案候選，不代表三星鍵盤的剪貼簿歷史。'});
 if(age>=30&&/\.(apk|apks|xapk)$/i.test(name))
  found.push({key:'installer',reason:'Android 安裝包至少 30 天未修改。刪除不會卸載 App，但會失去這份安裝或備份檔案。'});
 if(age>=7&&file.size===0)
  found.push({key:'empty',reason:'大小為 0 B，至少 7 天未修改。空檔案可能是設定標記，並不一定無用。'});
 if(file.size>=100*1048576)
  found.push({key:'large',reason:'檔案至少 100 MB。只依大小列出，不表示垃圾檔案。'});
 return found;
}
function summary(files,now=Date.now()){
 const categories={temp:[],residual:[],clipboard:[],installer:[],empty:[],large:[]};
 const candidates=[];
 files.forEach(f=>{const reasons=classify(f,now);reasons.forEach(r=>categories[r.key].push(f));if(reasons.some(r=>r.key!=='large'))candidates.push(f)});
 return {categories,candidates,bytes:candidates.reduce((n,f)=>n+Math.max(0,f.size),0)};
}
const api={classify,summary,ageDays};if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.CleanupRules=api;
})(typeof window!=='undefined'?window:this);
