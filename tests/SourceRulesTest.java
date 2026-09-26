import local.appatlas.SourceRules;import java.util.*;
public class SourceRulesTest {
 static void check(boolean value,String msg){if(!value)throw new AssertionError(msg);}
 public static void main(String[] args){Map<String,String> apps=new HashMap<>();apps.put("com.quark.browser","夸克");apps.put("org.telegram.messenger","Telegram");apps.put("com.test.app","測試");
  for(String path:new String[]{"Download/QuarkDownloads/CloudDrive","Quark/Download","QuarkCloudDrive","夸克/下載","Android/media/com.quark.browser/files"})check(SourceRules.resolve(path,null,apps).pkg.equals("com.quark.browser"),"Quark path: "+path);
  check(SourceRules.resolve("Download",null,apps).pkg.isEmpty(),"Generic Download must not be attributed to Quark");
  check(SourceRules.resolve("Download",null,apps).label.equals("來源未確認的下載"),"Generic Download labeled unknown");
  check(SourceRules.resolve("Download","com.quark.browser",apps).pkg.equals("com.quark.browser"),"Media owner attribution");
  check(SourceRules.resolve("Download","com.android.providers.downloads",apps).pkg.isEmpty(),"System provider is not downloader");
  check(SourceRules.resolve("Documents/MyQuarkResearch",null,apps).pkg.isEmpty(),"No substring overmatch");
  check(SourceRules.resolve("測試/Documents",null,apps).pkg.equals("com.test.app"),"Two-character Chinese display name");
  check(SourceRules.resolve("Android/media/org.telegram.messenger","com.quark.browser",apps).pkg.equals("org.telegram.messenger"),"Explicit package folder takes precedence");
  check(SourceRules.priority("Download/QuarkDownloads")<SourceRules.priority("DCIM/Camera"),"Download priority");
  String[][] cases={{"微信","MicroMsg"},{"QQ","QQfile_recv"},{"UC浏览器","UCDownloads"},{"Chrome","ChromeDownloads"},{"百度网盘","BaiduNetdisk"},{"阿里云盘","AliyunDrive"},{"迅雷","ThunderDownload"},{"抖音","Douyin"},{"快手","Kuaishou"},{"QQ音乐","QQMusic"},{"网易云音乐","CloudMusic"},{"小红书","XiaoHongShu"},{"钉钉","DingTalk"},{"飞书","Feishu"}};
  for(int i=0;i<cases.length;i++){String pkg="example.app"+i;apps.put(pkg,cases[i][0]);check(SourceRules.resolve("Download/"+cases[i][1],null,apps).pkg.equals(pkg),"Common alias "+cases[i][0]);check(SourceRules.resolve("Download/My"+cases[i][1]+"Research",null,apps).pkg.isEmpty(),"No partial alias "+cases[i][0]);}
  check(SourceRules.resolve("Tencent/Download",null,apps).pkg.isEmpty(),"Tencent folder is ambiguous");
  apps.put("example.clone","微信");check(SourceRules.resolve("MicroMsg",null,apps).pkg.isEmpty(),"Duplicate installed labels stay unassigned");
  check(SourceRules.resolve("TelegramVideoResearch",null,apps).pkg.isEmpty(),"Telegram prefix is insufficient");
  System.out.println("PASS: 44 attribution checks, including common apps and false positives");
 }
}
