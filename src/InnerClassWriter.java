import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InnerClassWriter
{
    public static String header;
    public static String content;
    public static String footer;


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
        //System.out.println(dir);
        if(!dir.contains("/"))
            s = "\\";

        //System.out.println(dir.substring(dir.indexOf(s) + 1));
        return dir.substring(dir.indexOf(s) + 1);
    }

    public static String getRootOfDir(String dir)
    {
        String str = dir;
        String s = "/";

        if(!dir.contains("/"))
            s = "\\";
        if(dir.contains("." + s))
        {
            str = dir.substring(dir.indexOf("." + s) + 2);
            return str.substring(0, str.indexOf(s));
        }
        else
            return dir.substring(0, str.indexOf(s));
    }

    
	public static List<Path> removeCommonPaths(List<Path> paths)
	{
        List<Path> common = new ArrayList<Path>();
		common.addAll(paths);

		String checking = "";
		String current = "";

        //System.out.println(common.toString());
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

    public static String generateInner(Path p, int count)
    {
        String str = p.toFile().getPath();
        return null;
    }
}