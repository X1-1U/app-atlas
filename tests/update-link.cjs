const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),root=__dirname+'/../';
const read=p=>fs.readFileSync(root+p,'utf8');
const manifest=read('android/AndroidManifest.xml'),js=read('android/assets/app.js'),java=read('android/src/local/appatlas/MainActivity.java'),html=read('android/assets/index.html');
const url='https://github.com/X1-1U/app-atlas/releases/latest';
assert.equal(js.match(/const UPDATE_URL='([^']+)'/)[1],url);assert.equal(java.match(/String UPDATE_URL="([^"]+)"/)[1],url);
// Network is limited to the native, user-started version lookup against one fixed URL.
assert.ok(/android\.permission\.INTERNET/.test(manifest));assert.ok(!/ACCESSIBILITY/.test(manifest));
assert.match(manifest,/usesCleartextTraffic="false"/);
assert.match(java,/setBlockNetworkLoads\(true\)/);assert.match(html,/connect-src 'none'/);
assert.equal(java.match(/String UPDATE_API="([^"]+)"/)[1],'https://api.github.com/repos/X1-1U/app-atlas/releases/latest');
assert.equal((java.match(/openConnection\(/g)||[]).length,1);assert.match(java,/new java\.net\.URL\(UPDATE_API\)/);
assert.match(java,/public void checkUpdate\(\)\{/);assert.match(java,/public void openUpdate\(\)\{/);
assert.ok(!/fetch\(|XMLHttpRequest|WebSocket|sendBeacon/.test(js));
const version=manifest.match(/android:versionName="([^"]+)"/)[1];
assert.equal(js.match(/let appVersion='([^']+)'/)[1],version);
assert.ok(read('README.md').includes('**'+version+'**'));
assert.ok(!/app-atlas-v\d/.test(read('build.sh')));
// Version comparison and the result handler.
let rendered=0;const scope={window:{},appVersion:'0.11.0',page:'settings',renderSettings:()=>rendered++};vm.createContext(scope);
vm.runInContext(js.slice(js.indexOf('let updateState='),js.indexOf('let motionEnabled=true;')).replace('let updateState','var updateState'),scope);
const c=scope.compareVersions;
assert.equal(c('0.11.0','0.10.9'),1);assert.equal(c('v0.9.0','0.10.0'),-1);assert.equal(c('1.0','1.0.0'),0);assert.equal(c('0.10.0','0.9.9'),1);
scope.window.receiveUpdate('v0.12.0');assert.equal(scope.updateState.status,'newer');assert.equal(scope.updateState.latest,'0.12.0');
scope.window.receiveUpdate('v0.11.0');assert.equal(scope.updateState.status,'current');
scope.window.receiveUpdate('v0.9.0');assert.equal(scope.updateState.status,'current');
scope.window.receiveUpdate('');assert.equal(scope.updateState.status,'error');assert.equal(rendered,4);
console.log('PASS: update check uses one fixed native URL, WebView stays blocked, versions compare correctly, version '+version+' is consistent');
