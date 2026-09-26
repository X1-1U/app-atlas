package local.appatlas;
import java.io.File;
public final class SafePaths {
 public static boolean allowed(File root,File file){
  try{
   java.nio.file.Path base=root.getAbsoluteFile().toPath().normalize(),input=file.getAbsoluteFile().toPath().normalize();
   if(!input.startsWith(base)||input.equals(base))return false;
   String relative=base.relativize(input).toString().replace(File.separatorChar,'/');
   if(relative.equals("Android/data")||relative.startsWith("Android/data/")||relative.equals("Android/obb")||relative.startsWith("Android/obb/"))return false;
   File expected=new File(root.getCanonicalFile(),relative);
   return file.getCanonicalPath().equals(expected.getAbsolutePath());
  }catch(Exception e){return false;}
 }
}
