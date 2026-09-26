package local.appatlas;
import java.util.*;
/** Attribution evidence, not a guarantee about the original downloader. */
public final class SourceRules {
 public static final class Match {public final String pkg,label,evidence;public Match(String p,String l,String e){pkg=p;label=l;evidence=e;}}
 public static boolean genericOwner(String pkg){return pkg==null||pkg.isEmpty()||pkg.equals("android")||pkg.equals("local.appatlas")||pkg.startsWith("com.android.providers.")||pkg.startsWith("com.google.android.providers.");}
 // Specific directory aliases only; never infer an app from a generic Tencent/Download folder.
 private static final String[][] ALIASES={
  {"微信|WeChat","wechat|weixin|micromsg"},
  {"QQ","qq|qqfile_recv|qqimages|qqvideo|qqfile"},
  {"UC浏览器|UC瀏覽器|UC Browser","ucdownloads|ucbrowser"},
  {"Chrome|Google Chrome","chrome|chromedownloads"},
  {"Edge|Microsoft Edge","edge|edgedownloads"},
  {"Samsung Internet|三星瀏覽器|三星浏览器|三星網際網路|三星浏览器国际版","samsunginternet"},
  {"百度网盘|百度網盤|Baidu Netdisk","baidunetdisk|baiduyundownload|百度网盘|百度網盤"},
  {"阿里云盘|阿里雲盤|阿里云盘TV|Aliyun Drive","aliyundrive|alipandownload|阿里云盘|阿里雲盤"},
  {"迅雷|Thunder","thunder|thunderdownload|迅雷"},
  {"抖音|Douyin","douyin|抖音"},
  {"快手|Kuaishou","kuaishou|快手"},
  {"QQ音乐|QQ音樂|QQ Music","qqmusic|qq音乐|qq音樂"},
  {"网易云音乐|網易雲音樂|NetEase Music","cloudmusic|网易云音乐|網易雲音樂"},
  {"小红书|小紅書|REDnote","xiaohongshu|rednote|小红书|小紅書"},
  {"钉钉|釘釘|DingTalk","dingtalk|钉钉|釘釘"},
  {"飞书|飛書|Lark","feishu|lark|飞书|飛書"},
  {"Discord","discord"}, {"Signal","signal"}, {"Spotify","spotify"}
 };
 private static String normalized(String value){return value.toLowerCase(Locale.ROOT).replaceAll("[ _-]", "");}
 private static boolean aliasEquals(String value,String choices){for(String choice:choices.split("\\|"))if(normalized(choice).equals(normalized(value)))return true;return false;}
 private static Match common(String part,Map<String,String> apps){
  for(String[] rule:ALIASES)if(aliasEquals(part,rule[1])){
   String match=null;for(Map.Entry<String,String> entry:apps.entrySet())if(aliasEquals(entry.getValue(),rule[0])){if(match!=null)return null;match=entry.getKey();}
   if(match!=null)return known(match,match,apps,"依常用 App 目錄別名推測；需確認來源");
  }return null;
 }
 public static int priority(String path){for(String part:path.replace('\\','/').split("/")){String p=normalized(part);if(p.contains("download")||p.startsWith("quark")||p.startsWith("夸克"))return 0;for(String[] rule:ALIASES)if(aliasEquals(part,rule[1]))return 0;}return path.toLowerCase(Locale.ROOT).contains("/android/media")?1:2;}
 private static Match known(String pkg,String fallback,Map<String,String> apps,String why){return new Match(pkg,apps.containsKey(pkg)?apps.get(pkg):fallback,why);}
 public static Match resolve(String path,String owner,Map<String,String> apps){
  String[] parts=path.replace('\\','/').split("/");
  for(String part:parts)if(apps.containsKey(part))return known(part,part,apps,"路徑含 App 套件名稱");
  if(!genericOwner(owner)&&apps.containsKey(owner))return known(owner,owner,apps,"Android 媒體索引登記的建立 App；不一定是最初下載者");
  for(String part:parts){Match common=common(part,apps);if(common!=null)return common;String p=part.toLowerCase(Locale.ROOT).replaceAll("[ _-]","");
   if(p.matches("quark(downloads?|clouddrive|browser)?")||p.matches("夸克(下载|下載|浏览器|瀏覽器|网盘|網盤)?")){String target="com.quark.browser";if(p.contains("clouddrive")||p.contains("网盘")||p.contains("網盤")){String cloud=null;for(Map.Entry<String,String> app:apps.entrySet())if(app.getKey().startsWith("com.quark.")&&(app.getValue().contains("网盘")||app.getValue().contains("網盤"))){if(cloud!=null){cloud=null;break;}cloud=app.getKey();}if(cloud!=null)target=cloud;}return known(target,"夸克",apps,"依夸克／Quark 系列目錄名稱推測");}
   if(p.equals("telegram")||p.matches("telegram(videos?|images?|documents?|audio)"))return known("org.telegram.messenger","Telegram",apps,"依 Telegram 目錄名稱推測");
   if(p.equals("bilibili"))return known("tv.danmaku.bili","Bilibili",apps,"依 Bilibili 目錄名稱推測");
   if(p.equals("whatsapp")||p.matches("whatsapp(videos?|images?|documents?|audio|voicenotes)"))return known("com.whatsapp","WhatsApp",apps,"依 WhatsApp 目錄名稱推測");
  }
  Set<String> generic=new HashSet<>(Arrays.asList("download","downloads","pictures","movies","music","documents","dcim","android","data","cache","media","files","下载","下載","图片","圖片","文件"));
  for(String part:parts){if(generic.contains(part.toLowerCase(Locale.ROOT)))continue;String pkg=null;for(Map.Entry<String,String> e:apps.entrySet()){String label=e.getValue();if(label.length()>=2&&part.equalsIgnoreCase(label)){if(pkg!=null){pkg=null;break;}pkg=e.getKey();}}if(pkg!=null)return known(pkg,pkg,apps,"目錄名稱吻合 App 顯示名稱，需確認");}
  for(String part:parts)if(part.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){2,}"))return new Match(part,part,"路徑含未確認安裝狀態的套件名稱");
  String label=path.matches("(?i).*(^|/)(download|downloads)(/|$).*" )?"來源未確認的下載":parts.length>0&&parts[0].equalsIgnoreCase("DCIM")?"相機":parts.length>0&&parts[0].equalsIgnoreCase("Pictures")?"圖片":"其他檔案";
  return new Match("",label,"未找到可確認的 App 來源");
 }
}
