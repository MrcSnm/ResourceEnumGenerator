import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

public class InnerClassWriter
{
    public static String header;
    public static String content;
    public static String footer;
    
    public static void showError(Exception e)
    {
    	JOptionPane.showMessageDialog(null, "Please, report this bug in the repository: https://github.com/MrcSnm/ResourceEnumGenerator\n" + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    	e.printStackTrace();
    }
    
    public static boolean waitToFinish(File f)
    {
    	int timeOut = 20 * 1000;
    	float counter = 0;
    	while(timeOut > 0)
    	{
    		if(f.canRead() && f.canWrite() && f.renameTo(f))
    			return true;
    		try {Thread.sleep(500);} 
    		catch (InterruptedException e) {e.printStackTrace();}
    		timeOut-= 500;
    		counter+= 500;
    		System.out.println("Waiting... " + counter / 1000);
    		
    	}
    	return false;
    }
    
    public static void waitToFinishWithError(File f)
    {
    	if(!waitToFinish(f))
    	{
    		JOptionPane.showMessageDialog(null, "Timeout when waiting to read-write for file '" + f.toString() + "'", "Timeout", JOptionPane.ERROR_MESSAGE);
    		System.exit(-1);
    	}
    }
    
    public static String getRoot(String path)
    {
    	String root = null;
		try{root = Paths.get(Paths.get(path).toFile().getCanonicalPath()).getRoot().toString();}
		catch(IOException e1){showError(e1);}
		if(root == null)
			JOptionPane.showMessageDialog(null, "Could not get root from " + path, "No root", JOptionPane.ERROR_MESSAGE);
		return root;
    }
    
    public static boolean isRootEqual(String path1, String path2)
    {
    	if(path1.equals("") || path2.equals("") || path1 == null || path2 == null)
			return false;
		return getRoot(path1).equals(getRoot(path2));
    }


    public static int countDir(String str)
    {
        int count = 0;
        for(int i = 0, len = str.length(); i < len; i++)
        {
            if(str.charAt(i) == '/' || str.charAt(i) == '\\')
                count++;
        }

        return count;
    }

    public static String enterNextDir(String dir)
    {
        String s = "/";
        if(!dir.contains("/"))
            s = "\\";
        return dir.substring(dir.indexOf(s) + 1);
    }
    
    public static String subtractPath(String input, String pathToSubtract)
    {
    	String str = input;
    	if(!pathToSubtract.equals("./") && !pathToSubtract.equals(".\\")) //Ignore this part if there is no inner class to skip
		{
			if(!pathToSubtract.contains(":\\")) //Untested 
				str = input.substring(Paths.get(pathToSubtract).toString().length() - 1);
			else if(pathToSubtract.matches(".\\:\\\\.*"))  //Matches Windows FileSystem
				str = input.substring(Paths.get(pathToSubtract).toString().length() - 3);
			else //For when having pathes with ./
				str = input.substring(Paths.get(pathToSubtract).toString().length() - 2);
		}
    	return str;
    }

    public static String getCurrentDirName(String dir)
    {
        String str = dir;
        String last = "";
        while(countDir(str) > 0)
        {
            last = str;
            str = enterNextDir(str);
        }
        if(str.equals("."))
        {
            if(last.contains("/"))
                return last.substring(0, last.indexOf("/"));
            return last.substring(0, last.indexOf("\\"));
        }
        return str;
    }

    public static String getRootOfDir(String dir)
    {
        String str = dir;
        String s = "/";

        if(!dir.contains("/") && !dir.contains("\\"))
        	return dir;
        if(!dir.contains("/"))
            s = "\\";
        
        if(dir.indexOf(s) == 0)
        {
        	if(dir.indexOf(s, 1) != -1)
        		return dir.substring(1, dir.indexOf(s, 1));
        	else
        		return dir.substring(1);
        }
        
        return dir.substring(0, str.indexOf(s));
    }

    
	public static List<Path> removeCommonPaths(List<Path> paths)
	{
        List<Path> common = new ArrayList<Path>();
		common.addAll(paths);

		String checking = "";
		String current = "";
		for(int i = 0, len = common.size(); i < len; i++)
		{
			checking = common.get(i).toString();
			for(int z = 0; z < len; z++)
			{
				current = common.get(z).toString();
				if(!checking.equals(current) && checking.contains(current))
				{
					common.remove(z);
					i--;
					z--;
                    len--;
                    break;
				}
			}
		}
		return common;
	}

    public static String multiplyString(String str, int multiple)
    {
        String nStr = "";
        for(int i = 0; i < multiple; i++)
            nStr+= str;
        return nStr;
    }

    public static String getStringBetween(String input, String separator, int count)
    {
        String[] separated;
        if(!input.contains(separator))
            return input;
        
        if(!separator.equals("\\"))
        	separated = input.split(separator);
        else
        	separated = input.split("\\\\");
        
        
        String res = "";
        for(int i = 0; i < count; i++)
            res+= separated[i] + separator;
        return res;
    }

    public static String getPathUntilCount(String path, int count)
    {
        String s = "/";
        if(!path.contains("/"))
            s = "\\";
        return getStringBetween(path, s, count);
    }
    

    public static List<String> listDir(File f)
    {
        String[] dirs = f.list(new FilenameFilter(){
        
            @Override
            public boolean accept(File dir, String name) 
            {
                return new File(dir, name).isDirectory();
            }
        });

        Arrays.asList(dirs).forEach(System.out::println);
        return Arrays.asList(dirs);
    }

    public static String getNewClass(String str, int intMaxDeep)
    {
        String currentDir = str;
        int currentCount = countDir(currentDir);

        if(currentCount > 1)
        {
            //WRITE PATH TO THE CLASS
            getRootOfDir(currentDir);
            multiplyString("\t", intMaxDeep - currentCount);
            enterNextDir(currentDir);
            //WRITE
            //CLOSE CURRENT CONTEXT
            //multiplyString("\t", intMaxDeep - currentCount) + "}";

        }
        return null;
    }
}