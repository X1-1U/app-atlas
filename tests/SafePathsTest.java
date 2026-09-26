import java.nio.file.*;import java.io.*;import local.appatlas.SafePaths;
public class SafePathsTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Path t=Files.createTempDirectory("atlas-paths-");Path root=Files.createDirectory(t.resolve("storage"));Path external=Files.createDirectory(t.resolve("outside"));Path dir=Files.createDirectory(root.resolve("Download"));Path file=Files.write(dir.resolve("a.txt"),new byte[]{1});Path outside=Files.write(external.resolve("secret.txt"),new byte[]{2});
  check(SafePaths.allowed(root.toFile(),file.toFile()),"regular file");check(!SafePaths.allowed(root.toFile(),outside.toFile()),"outside rejected");check(!SafePaths.allowed(root.toFile(),root.toFile()),"root rejected");
  check(!SafePaths.allowed(root.toFile(),root.resolve("Android/data/com.test.app/a").toFile()),"private data blocked");check(!SafePaths.allowed(root.toFile(),root.resolve("Android/obb/com.test.app/a").toFile()),"obb blocked");check(SafePaths.allowed(root.toFile(),root.resolve("Android/media/com.test.app/a").toFile()),"shared media allowed");
  Path escape=Files.createSymbolicLink(root.resolve("escape"),external);check(!SafePaths.allowed(root.toFile(),escape.resolve("secret.txt").toFile()),"symlink escape rejected");
  Path alias=Files.createSymbolicLink(t.resolve("alias"),root);check(SafePaths.allowed(alias.toFile(),alias.resolve("Download/a.txt").toFile()),"storage alias allowed");
  System.out.println("PASS: 8 scanner path boundary, Android restrictions and storage alias checks");
 }
}
