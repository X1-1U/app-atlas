# 拾檔桌面手機預覽

獨立 macOS 浮動視窗，預設靠右、置頂，可拖動。Command-R 重新載入，Command-T 切換置頂，Command-Q 結束。

依 Samsung S26 Ultra 官方 1440 × 3120 面板比例呈現。使用 412 × 893 邏輯尺寸（含示意狀態列與手勢列）作為開發預設，這是模擬值，未量測使用者手機的螢幕縮放、WebView 密度與字體設定。螢幕不足高時僅縮小外觀比例，內部版面寬度仍為 412 CSS px。

官方規格：https://www.samsung.com/ca/business/smartphones/galaxy-s/galaxy-s26-ultra-sm-s948wlbexac/

使用 WKWebView 載入同一份本機 App 介面，不是 Android 模擬器或手機投影；無法測試原生權限或手機檔案。優先讀取工作目錄最新 assets，找不到時使用 bundle 內的副本。不需要 localhost 伺服器。

APK 開發時主動開啟 ANDROID模擬，預設靠右懸浮。可用 ⌘H 暫時隱藏。尚無可用的 Codex 對話焦點事件，未實作隨對話切換自動開關，不得宣稱已有此功能。
