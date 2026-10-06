# 開發約定

- 使用聊天內 App 預覽台技能 /Users/onsunday/.codex/skills/app-preview/SKILL.md；每個專案用自己的介面。
- 不啟動或重建 ANDROID模擬.app、獨立懸浮視窗或 LaunchAgent。先預覽，使用者確認後才能建置 APK。
- 網路只用於使用者手動按下的「檢查更新」（原生端向固定的 GitHub API 網址查版本號）。WebView 保持封鎖網路，不自動連線、不上傳檔案或個人資料，不新增其他連線用途；不新增無障礙權限。
- 未知用途不推薦刪除。每次刪除保留使用者確認。
- 不提交簽章私鑰或個人檔案；保留舊 APK。預覽不等於 Android 實機驗證。
