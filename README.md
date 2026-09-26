# 拾檔 · App Atlas

離線 Android 儲存清理與檔案管理工具。目前版本 **0.8.0**，最低 Android 11，目標 SDK 35。

手機版透過 Android PackageManager 讀取已安裝 App 自帶圖示，使用本機 128px PNG 與有限記憶體快取。無新增權限；桌面預覽沒有手機安裝資源，保留示意文字。

## 功能

- 清理候選：暫存、未完成下載、疑似殘留、舊安裝包、剪貼簿相關檔案等；未知用途不推薦刪除。
- 按 App 來源整理可讀取檔案，支援格子／列表與兩層導覽、搜尋、媒體縮圖及刪除影響說明。
- 刪除成功後更新索引與容量，不全盤重掃；失敗保留，保留原搜尋與瀏覽位置。
- 已安裝 App 的版本、容量、權限資訊與系統設定入口。
- 明亮、深色、跟隨系統主題。

不申請網路或無障礙權限。需要的特殊存取為所有檔案管理、套件查詢及可選的使用情況存取（僅查詢容量）。Android 私有目錄仍受限制；來源辨識是啟發式推測，不能保證找出每個 App 的所有檔案。刪除需使用者確認，沒有回收桶。

## 建置 Android

安裝 JDK 17、Android SDK platform 35、build-tools 34.0.0：

```sh
ANDROID_HOME=/path/to/android-sdk JAVA_HOME=/path/to/jdk17 bash build.sh
```

產物為 app-atlas-v0.8.0.apk。BUILD_DIR、SIGNING_KEY 可設定；預設建立本機開發用測試簽章，不可當成正式發行金鑰。簽章金鑰不入庫；自行建置的不同簽章無法直接覆蓋其他簽章的安裝。

## 獨立手機預覽（macOS）

```sh
bash desktop-preview/build.sh
```

以 Finder 開啟產生的 work/ANDROID模擬.app。浮動視窗預設靠右置頂，可拖動；⌘R 刷新，⌘T 切換置頂。使用同一份 App 介面，優先載入工作目錄最新檔案；無須啟動網頁伺服器。

S26 Ultra 螢幕比例，412 × 893 邏輯尺寸為模擬預設，並非實機密度量測。預覽為示範資料，不能取代 Android 權限、儲存操作或三星相容性實測。

## 測試

```sh
node tests/cleanup.cjs
node tests/intelligence.cjs
node tests/deletion.cjs
node tests/app-icons.cjs
mkdir -p work/tests
javac -encoding UTF-8 -d work/tests android/src/local/appatlas/SafePaths.java android/src/local/appatlas/SourceRules.java tests/SafePathsTest.java tests/SourceRulesTest.java
java -cp work/tests SafePathsTest
java -cp work/tests SourceRulesTest
```

開發時已完成規則測試、預覽操作驗證、APK 建置與簽章驗證；尚未完成全面實機測試。歷史改動與限制見 [CHANGELOG.md](CHANGELOG.md)。

APK 開發時主動開啟 ANDROID模擬，預設靠右懸浮。可用 ⌘H 暫時隱藏。尚無可用的 Codex 對話焦點事件，未實作隨對話切換自動開關，不得宣稱已有此功能。
