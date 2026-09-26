import Cocoa
import WebKit

final class Preview: NSObject, NSApplicationDelegate, WKNavigationDelegate {
    var panel: NSPanel!
    var web: WKWebView!
    func applicationDidFinishLaunching(_ notification: Notification) {
        let screen = NSScreen.main!.visibleFrame
        let height = min(CGFloat(893), screen.height - 64)
        let width = height * 1440 / 3120
        panel = NSPanel(contentRect: NSRect(x:screen.maxX-width-18,y:screen.maxY-height-40,width:width,height:height),styleMask:[.titled,.closable,.miniaturizable],backing:.buffered,defer:false)
        panel.title = "ANDROID模擬"
        panel.level = .floating
        panel.hidesOnDeactivate = false
        panel.collectionBehavior = [.canJoinAllSpaces,.fullScreenAuxiliary]
        panel.isReleasedWhenClosed = false
        web = WKWebView(frame:NSRect(x:0,y:0,width:width,height:height))
        web.navigationDelegate = self
        web.autoresizingMask = [.width,.height]
        panel.contentView = web
        let menu=NSMenu(),appItem=NSMenuItem(),appMenu=NSMenu()
        menu.addItem(appItem); appItem.submenu=appMenu
        let refresh=NSMenuItem(title:"重新載入介面",action:#selector(reload),keyEquivalent:"r");refresh.target=self;appMenu.addItem(refresh)
        let float=NSMenuItem(title:"切換置頂",action:#selector(toggleFloat),keyEquivalent:"t");float.target=self;appMenu.addItem(float)
        appMenu.addItem(NSMenuItem(title:"結束預覽",action:#selector(NSApplication.terminate(_:)),keyEquivalent:"q"))
        let hide=NSMenuItem(title:"隱藏 ANDROID模擬",action:#selector(NSApplication.hide(_:)),keyEquivalent:"h");appMenu.addItem(hide)
        NSApp.applicationIconImage=NSImage(contentsOf:Bundle.main.resourceURL!.appendingPathComponent("AndroidWave.icns"))
        NSApp.mainMenu=menu
        reload();panel.makeKeyAndOrderFront(nil);NSApp.activate(ignoringOtherApps:true)
    }
    func applicationShouldHandleReopen(_ sender:NSApplication,hasVisibleWindows flag:Bool)->Bool{panel.makeKeyAndOrderFront(nil);return true}
    @objc func toggleFloat(){panel.level = panel.level == .floating ? .normal : .floating}
    @objc func reload(){
        // Prefer the working UI for development; the bundle remains usable on its own.
        let configured=ProcessInfo.processInfo.environment["APP_ATLAS_ASSETS"] ?? Bundle.main.object(forInfoDictionaryKey:"AtlasAssetsPath") as? String ?? ""
        let working=URL(fileURLWithPath:configured).appendingPathComponent("floating.html")
        let url=FileManager.default.fileExists(atPath:working.path) ? working : Bundle.main.resourceURL!.appendingPathComponent("assets/floating.html")
        web.loadFileURL(url,allowingReadAccessTo:url.deletingLastPathComponent())
    }
    func webView(_ webView:WKWebView,decidePolicyFor action:WKNavigationAction,decisionHandler:@escaping(WKNavigationActionPolicy)->Void){decisionHandler(action.request.url?.isFileURL == true || action.request.url?.absoluteString == "about:blank" ? .allow : .cancel)}
    func applicationShouldTerminateAfterLastWindowClosed(_ sender:NSApplication)->Bool{return true}
}
let app=NSApplication.shared
let delegate=Preview()
app.delegate=delegate
app.setActivationPolicy(.regular)
app.run()
