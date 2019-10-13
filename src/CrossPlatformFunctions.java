import java.awt.Component;
import javax.swing.JOptionPane;
import java.awt.FileDialog;
import java.io.FilenameFilter;
import java.io.File;
import java.awt.Frame;

public class CrossPlatformFunctions 
{
   public static enum OS 
   {
      WINDOWS, MACOSX, LINUX, UNKNOWN
   }

   public static OS getOS() 
   {
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("mac") || os.contains("darwin"))
         return OS.MACOSX;
      else if (os.contains("win"))
         return OS.WINDOWS;
      else if (os.contains("nux") || os.contains("nix"))
         return OS.LINUX;
      return OS.UNKNOWN;
   }

   public static String convertDirToOS(String dir) 
   {
      OS os = getOS();
      String resultDir = "";
      switch (os) {
      case WINDOWS:
         resultDir = dir.replace('/', '\\');
         break;
      default:
         resultDir = dir.replace('\\', '/');
         break;
      }
      return resultDir;

   }

   public static boolean checkFileExistence(String filename) 
   {
      String nFileName = convertDirToOS(filename);
      File f = new File(nFileName);
      return f.exists();
   }

   public static String crossPlatformSelect(String description, String filter) 
   {
      FileDialog f = new FileDialog((Frame) null, description, FileDialog.LOAD);

      if(filter != null)
      {
    	  if (!filter.contains("*."))
    		  filter = "*." + filter;
    	  else if (!filter.contains("*"))
    		  filter = "*" + filter;
    	  final String finalFilter = filter;
    	  OS compareOs = getOS();
    	  if (compareOs == OS.WINDOWS)
    		  f.setFile(finalFilter);
    	  else 
    	  {
    		  f.setFilenameFilter(new FilenameFilter() 
    		  {
    			  @Override
    			  public boolean accept(File dir, String name) {
    				  return name.endsWith(finalFilter.substring(1));
    			  }
    		  });
    	  }
      }
      f.setVisible(true);
      String fileSelected = f.getFile();
      if (fileSelected == null || fileSelected == "")
         return "";
      fileSelected = f.getDirectory() + fileSelected;
      return fileSelected;
   }
   
   public static String crossPlatformGetDir(String description, String filter)
   {
	   FileDialog f = new FileDialog((Frame) null, description, FileDialog.LOAD);

	      if(filter != null)
	      {
	    	  if (!filter.contains("*."))
	    		  filter = "*." + filter;
	    	  else if (!filter.contains("*"))
	    		  filter = "*" + filter;
	    	  final String finalFilter = filter;
	    	  OS compareOs = getOS();
	    	  if (compareOs == OS.WINDOWS)
	    		  f.setFile(finalFilter);
	    	  else 
	    	  {
	    		  f.setFilenameFilter(new FilenameFilter() 
	    		  {
	    			  @Override
	    			  public boolean accept(File dir, String name) {
	    				  return name.endsWith(finalFilter.substring(1));
	    			  }
	    		  });
	    	  }
	      }
	      f.setFile("Select your directory");
	      f.setVisible(true);
	      String fileSelected = f.getFile();
	      if (fileSelected == null || fileSelected == "")
	         return "";
	      fileSelected = f.getDirectory();
	      return fileSelected;
   }

   public static File[] crossPlatformSelectMulti(String description, String filter)
   {
      FileDialog f = new FileDialog((Frame) null, description, FileDialog.LOAD);

      if (!filter.contains("*."))
         filter = "*." + filter;
      else if (!filter.contains("*"))
         filter = "*" + filter;
      final String finalFilter = filter;

      OS compareOs = getOS();
      if (compareOs == OS.WINDOWS)
         f.setFile(finalFilter);
      else 
      {
         f.setFilenameFilter(new FilenameFilter() 
         {
            @Override
            public boolean accept(File dir, String name) {
               return name.endsWith(finalFilter.substring(1));
            }
         });
      }
      f.setMultipleMode(true);
      f.setVisible(true);
      File[] filesSelected = f.getFiles();
      if(filesSelected.length > 0)
         return filesSelected;
      return null;
   }

   public static String getFileName(String fileDir) 
   {
      int index = 0;
      
      for (int i = 0, len = fileDir.length(); i < len; i++) {
         if (fileDir.charAt(i) == '\\' || fileDir.charAt(i) == '/')
            index = i;
      }
      return fileDir.substring(index + 1);
   }

   public static String crossPlatformSave(String description, String file) 
   {
      FileDialog f = new FileDialog((Frame) null, description, FileDialog.SAVE);
      f.setFile(file);
      f.setVisible(true);
      String dir = f.getDirectory();
      String name = f.getFile();
      if (name == null)
         return "";
      return dir + name;
   }

   public static void openCurrentSystemExplorer(String path, boolean willShowAlert, boolean willSelectFile) 
   {
      if(willShowAlert)
         if ((JOptionPane.showConfirmDialog(null, "Do you want to open system file explorer?", "Open system file explorer",
               JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
            return;
      String command = "";

      OS operatingSystem = getOS();
      switch (operatingSystem) {
      case WINDOWS:
         command = "Explorer.exe " + ((willSelectFile) ? "/select, " : "") + path;
         break;
      case MACOSX:
         command = "open " + path;
         break;
      case LINUX:
         command = "xdg-open " + path;
         break;
      default:
         System.out.println("Opening file explorer not supported on your system");
         return;
      }

      try {
         Runtime.getRuntime().exec(command);
      } catch (Exception e) {
         JOptionPane.showConfirmDialog((Component) null, "The command " + command + " is not supported on your system",
               "Command not supported", 0, 0);
         e.printStackTrace();
      }
   }
   
   public static String getFileDirectory(String directory)
   {
	   String s = "/";
	   if(directory.contains("/"))
		   return directory.substring(0, directory.lastIndexOf(s) + 1);
	   else if(directory.contains("\\"))
	   {
		   s = "\\";
		   return directory.substring(0, directory.lastIndexOf(s) + 1);
	   }
	   return directory;
   }
}