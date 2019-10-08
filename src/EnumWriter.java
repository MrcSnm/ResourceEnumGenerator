import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class EnumWriter 
{
	public String path = "./enumwriter.cs";
	
	private String classDeclarator = "public class ";
	private String className = "class1";
	public String pathToWatch = "./";

	private String surroundEnumConstsWith = "";
	private String enumDeclarator = "public enum ";
	private String postEnumDeclaration = "";
	private String afterEnumLastBracket = "";
	private boolean enumConstToUppercase = false;
	private boolean enumToUppercase = false;

	private String assignSymbol = "=";

	private ArrayList<String> toIgnore = new ArrayList<String>();
	private ArrayList<String> pathsToIgnore = new ArrayList<String>();
	private String ignoredExtensions = ".config, .java, .git, .classpath, .project";
	private String ignoredPaths = ".git, .vscode";

	private Thread t;
	private boolean willUseAssign = false;
	public boolean isWriting = false;
	
	public boolean isRunning = true;
	public boolean isUpdateScheduled = false;
	
	
	private List<Path> scheduledPath;
	
	
	public EnumWriter() throws IOException
	{
		readConfig();
		EnumWriter ref = this;
		t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				int up = 0;
				while(ref.isRunning)
				{
					try {Thread.sleep(30);} 
					catch (InterruptedException e1) {e1.printStackTrace();}
					if(ref.isUpdateScheduled)
					{
						
						try {write(scheduledPath);} 
						catch (IOException e) {	e.printStackTrace();}
						isUpdateScheduled = false;
					}
				}
				
			}
		});
		t.start();
	}
	
	
	private void generateDefaultFormatFile() throws IOException
	{
		PrintWriter pw = new PrintWriter("./settings.config");
		String config = "";
		
		config+= "PATH_TO_WATCH= " + pathToWatch+"\n";
		config+= "PATH_TO_CREATE_FILE= " + path+"\n";
		config+= "CLASS_NAME= " + className+"\n";
		config+= "CLASS_DECLARATOR= " + classDeclarator+"\n";
		config+= "ENUM_DECLARATOR= " + enumDeclarator+"\n";
		config+= "POST_ENUM_DECLARATION= " + postEnumDeclaration+"\n";
		config+= "AFTER_ENUM_LAST_BRACKET= " + afterEnumLastBracket+"\n";
		config+= "ENUM_CONST_SURROUND_WITH= " + surroundEnumConstsWith+"\n";
		config+= "ENUM_TO_UPPERCASE= " + enumToUppercase+"\n";
		config+= "ENUM_CONST_TO_UPPERCASE= " + enumConstToUppercase+"\n";
		config+= "ASSIGN_SYMBOL= " + assignSymbol+"\n";
		config+= "WILL_USE_ASSIGN= " + willUseAssign+"\n";
		config+= "IGNORE_EXTENSIONS= " + ignoredExtensions+"\n";
		config+= "IGNORE_PATHS= " + ignoredPaths+"\n";
		
		
		pw.write(config);
		pw.close();
	}
	
	private void readConfig() throws IOException
	{
		File f = new File("./settings.config");
		if(f.exists())
		{
			List<String> list =  Files.readAllLines(f.toPath());
			
			path = getContent(list, "PATH_TO_CREATE_FILE= ");
			className = getContent(list, "CLASS_NAME= ");
			classDeclarator = getContent(list, "CLASS_DECLARATOR= ");
			enumDeclarator = getContent(list, "ENUM_DECLARATOR= ");
			postEnumDeclaration = getContent(list, "POST_ENUM_DECLARATION= ");
			afterEnumLastBracket = getContent(list, "AFTER_ENUM_LAST_BRACKET= ");
			surroundEnumConstsWith = getContent(list, "ENUM_CONST_SURROUND_WITH= ");
			enumToUppercase = getBool(list, "ENUM_TO_UPPERCASE= ");
			enumConstToUppercase = getBool(list, "ENUM_CONST_TO_UPPERCASE= ");
			assignSymbol = getContent(list, "ASSIGN_SYMBOL= ");
			willUseAssign = getBool(list, "WILL_USE_ASSIGN= ");
			toIgnore = getStrings(list, "IGNORE_EXTENSIONS= ");
			ignoredExtensions = getContent(list, "IGNORE_EXTENSIONS= ");
			pathsToIgnore = getStrings(list, "IGNORE_PATHS= ");
			ignoredPaths = getContent(list, "IGNORE_PATHS= ");
		}
		else
			generateDefaultFormatFile();
	}
	
	public void scheduleUpdate(List<Path> pathsToSchedule)
	{
		scheduledPath = pathsToSchedule;
		isUpdateScheduled = true;
	}

	private boolean checkIgnored(String str, ArrayList<String> listToIgnore)
	{
		for(int i = 0, len = listToIgnore.size(); i < len; i++)
		{
			if(str.contains(listToIgnore.get(i)))
			{
				System.out.println(str + " was ignored because of the " + listToIgnore.get(i) + " ignore");
				return true;
			}
		}
		return false;

	}
	
	
	
	public void write(List<Path> paths) throws IOException
	{
		if(!isWriting)
		{
			isWriting = true;
			System.out.println("Starting to write file");
			String code = "";
			PrintWriter pw = new PrintWriter(path);
			if(!classDeclarator.equals("") && !className.equals(""))
				code+= classDeclarator + className + "\n{\n";
			else
				code+="{\n";

			for(int i = 0, len = paths.size(); i < len; i++)
			{
				if(checkIgnored(paths.get(i).toString(), pathsToIgnore))
				{
					paths.remove(i);
					i--;
					len--;
				}
			}

			for(Path path : paths)
			{
				File[] files = path.toFile().listFiles(File::isFile);
				if(enumToUppercase)
					code+= "\t" + enumDeclarator + path.toFile().getName().toUpperCase() + postEnumDeclaration + "\n\t{\n";
				else
					code+= "\t" + enumDeclarator + path.toFile().getName() + postEnumDeclaration + "\n\t{\n";
				if(files != null)
				{
					for(int i = 0, len = files.length; i < len; i++)
					{
						String enumConstant = files[i].getName();
						if(checkIgnored(enumConstant, toIgnore))
							continue;
						enumConstant = enumConstant.replaceAll("[\\s|\\-]", "_");
						if(enumConstToUppercase)
							code+= "\t\t" + surroundEnumConstsWith + enumConstant.toUpperCase() + surroundEnumConstsWith;
						else
							code+= "\t\t" + surroundEnumConstsWith + enumConstant + surroundEnumConstsWith;
						if(willUseAssign)
							code+= " " + assignSymbol + " " + enumConstant;
						if(i != len -1)
							code+= ",";
						code+= "\n";
					}
				}
				code+= "\t}" + afterEnumLastBracket + "\n";
			}
			code+= "}";
			pw.write(code);
			pw.close();
			System.out.println("File write finished");
			isWriting = false;
		}
	}
	
	private String getContent(List<String> lines, String place)
	{
		for(String line : lines)
		{
			if(line.contains(place))
				return line.substring(place.length(), line.length());
		}
		return null;
	}

	private ArrayList<String> getStrings(List<String> lines, String place)
	{
		String str = getContent(lines, place);
		if(str == null)
			return null;
		String strBuffer = "";

		System.out.println(str);
		ArrayList<String> res = new ArrayList<String>();
		for(int i = 0, len = str.length(); i < len; i++)
		{
			if(str.charAt(i) == ' ')
				continue;
			else if(str.charAt(i) != ',')
				strBuffer+= str.charAt(i);
			else 
			{
				res.add(strBuffer);
				strBuffer = "";
			}
		}
		if(!strBuffer.equals(""))
			res.add(strBuffer);
		return res;

	}
	
	private boolean getBool(List<String> lines, String place)
	{
		String str = getContent(lines, place);
		if(str != null)
			return Boolean.parseBoolean(str);
		return false;
	}
	
	private int getInt(List<String> lines, String place)
	{
		String str = getContent(lines, place);
		if(str != null)
			return Integer.parseInt(str);
		return 0;
	}
}
