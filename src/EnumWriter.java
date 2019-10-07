import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class EnumWriter 
{
	public String path = "./enumwriter.cs";
	
	private String classDeclarator = "public class";
	private String className = "class1";
	public String pathToWatch = "./";
	private String surroundEnumConstsWith = "";
	private String enumDeclarator = "public enum";
	private String assignSymbol = "=";
	private ArrayList<String> toIgnore = new ArrayList<String>();
	private String ignoredExtensions = ".config, .java";
	private Thread t;
	private boolean willJumpLine = true;
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
		config+= "ENUM_CONST_SURROUND_WITH= " + surroundEnumConstsWith+"\n";
		config+= "ASSIGN_SYMBOL= " + assignSymbol+"\n";
		config+= "WILL_JUMP_LINE= " + willJumpLine+"\n";
		config+= "IGNORE_EXTENSIONS= " + ignoredExtensions+"\n";
		
		
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
			surroundEnumConstsWith = getContent(list, "ENUM_CONST_SURROUND_WITH= ");
			assignSymbol = getContent(list, "ASSIGN_SYMBOL= ");
			willJumpLine = getBool(list, "WILL_JUMP_LINE= ");
			toIgnore = getStrings(list, "IGNORE_EXTENSIONS= ");
			ignoredExtensions = getContent(list, "IGNORE_EXTENSIONS= ");
			
		}
		else
			generateDefaultFormatFile();
	}
	
	public void scheduleUpdate(List<Path> pathsToSchedule)
	{
		scheduledPath = pathsToSchedule;
		isUpdateScheduled = true;
	}

	private boolean checkIgnored(String str)
	{
		for(int i = 0, len = toIgnore.size(); i < len; i++)
			if(str.contains(toIgnore.get(i)))
				return true;
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
			code+= classDeclarator + " " + className + "\n{\n";
			for(Path path : paths)
			{
				File[] files = path.toFile().listFiles(File::isFile);
				code+= "\t" + enumDeclarator + " " + path.toFile().getName() + "\n\t{\n";
				for(int i = 0, len = files.length; i < len; i++)
				{
					String enumConstant = files[i].getName();
					if(checkIgnored(enumConstant))
						continue;
					enumConstant = enumConstant.replaceAll("[\\s|\\-]", "_");
					code+= "\t\t" + surroundEnumConstsWith + enumConstant + surroundEnumConstsWith;
					if(i != len -1)
						code+= ",";
					code+= "\n";
				}
				code+= "\t}\n";
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
		ArrayList<String> res = new ArrayList<String >();
		for(int i = 0, len = str.length(); i < len; i++)
		{
			if(str.charAt(i) != ',')
				strBuffer+= str.charAt(i);
			else
			{
				res.add(strBuffer);
				strBuffer = "";
			}
		}
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
