const fs=require('fs'),assert=require('node:assert/strict'),root=__dirname+'/../';
const read=p=>fs.readFileSync(root+p,'utf8');
const manifest=read('android/AndroidManifest.xml'),js=read('android/assets/app.js'),java=read('android/src/local/appatlas/MainActivity.java');
const url='https://github.com/X1-1U/app-atlas/releases/latest';
const jsUrl=js.match(/const UPDATE_URL='([^']+)'/)[1],javaUrl=java.match(/String UPDATE_URL="([^"]+)"/)[1];
assert.equal(jsUrl,url);assert.equal(javaUrl,url);
// The link is handed to the browser; the app must stay offline.
assert.ok(!/android\.permission\.INTERNET/.test(manifest));assert.ok(!/ACCESSIBILITY/.test(manifest));
assert.match(java,/setBlockNetworkLoads\(true\)/);
// Native side opens only its own constant, never a URL passed from the page.
assert.match(java,/public void openUpdate\(\)\{/);
const version=manifest.match(/android:versionName="([^"]+)"/)[1];
assert.equal(js.match(/let appVersion='([^']+)'/)[1],version);
assert.ok(read('README.md').includes('**'+version+'**'));
assert.ok(!/app-atlas-v\d/.test(read('build.sh')));
console.log('PASS: update link matches in UI and native, app stays offline, version '+version+' is consistent (8 checks)');
