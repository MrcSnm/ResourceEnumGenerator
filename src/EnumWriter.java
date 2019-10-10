import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EnumWriter 
{
	public String path = "./enumwriter.cs";
	
	private String classDeclarator = "public class ";
	private boolean willUseClassName = true;
	private String customClassName = "";
	private boolean classNameStartWithCapital = true;
	public String pathToWatch = "./";

	private boolean isEnumMode = true;
	private String surroundEnumConstsWith = "";
	private String enumDeclarator = "public enum ";
	private String postEnumDeclaration = "";
	private String afterEnumLastBracket = "";
	private boolean enumConstToUppercase = false;
	private boolean enumToUppercase = false;




	private String innerClassDeclarator = "public static class ";
	private String postInnerClassDeclarator = "";
	
	private String stringArrayDeclarator = "public static string[] ";
	private String stringArrayPrefix = "get";
	private String stringArraySufix = "";
	private boolean willStartStringArrayWithCapital = true;
	private String postStringArrayDeclarator = " = new String[]";
	private String stringArrayStartBlockSymbol = "{";
	private String stringArrayEndBlockSymbol = "};";
	private String postStringDefinition = ",";

	private String assignSymbol = "=";

	private ArrayList<String> toIgnore = new ArrayList<String>();
	private ArrayList<String> pathsToIgnore = new ArrayList<String>();
	private String ignoredExtensions = ".config, .java, .git, .classpath, .project";
	private String ignoredPaths = ".git, .vscode";

	private Thread t;
	private boolean willUseAssign = false;
	private boolean willRemoveExtension = false;

	public boolean isRunning = true;

	public volatile boolean isWriting = false;
	public boolean isUpdateScheduled = false;
	public boolean isConfigUpdateScheduled = false;
	public volatile boolean isReadingConfig = false;

	private List<Path> scheduledPath;

	public int updates = 0;
	
	
	public EnumWriter() throws IOException
	{
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
					if(isConfigUpdateScheduled && !isReadingConfig && !isWriting)
					{
						try {readConfig();} 
						catch (IOException e) {	e.printStackTrace();}
						isConfigUpdateScheduled = false;
						continue;
					}
					if(isUpdateScheduled && !isReadingConfig && !isWriting)
					{
						try {write(scheduledPath);} 
						catch (IOException e) {	e.printStackTrace();}
						isUpdateScheduled = false;
						continue;
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

		config+= "WILL_USE_CLASS_NAME= " + willUseClassName+"\n";
		config+= "CUSTOM_CLASS_NAME= " + customClassName+"\n";
		config+= "CLASS_NAME_START_WITH_CAPITAL= " + classNameStartWithCapital+"\n";

		config+= "CLASS_DECLARATOR= " + classDeclarator+"\n";

		config+= "IS_ENUM_MODE= " + isEnumMode+"\n";
		config+= "ENUM_DECLARATOR= " + enumDeclarator+"\n";
		config+= "POST_ENUM_DECLARATION= " + postEnumDeclaration+"\n";
		config+= "AFTER_ENUM_LAST_BRACKET= " + afterEnumLastBracket+"\n";
		config+= "ENUM_CONST_SURROUND_WITH= " + surroundEnumConstsWith+"\n";
		config+= "ENUM_TO_UPPERCASE= " + enumToUppercase+"\n";
		config+= "ENUM_CONST_TO_UPPERCASE= " + enumConstToUppercase+"\n";

		config+= "INNER_CLASS_DECLARATOR= " + innerClassDeclarator+"\n";
		config+= "POST_INNER_CLASS_DECLARATOR= "+ postInnerClassDeclarator+"\n";

		config+= "STRING_ARRAY_DECLARATOR= " + stringArrayDeclarator+"\n";
		config+= "POST_STRING_ARRAY_DECLARATOR= " + postStringArrayDeclarator+"\n";

		config+= "STRING_ARRAY_PREFIX= " + stringArrayPrefix+"\n";
		config+= "WILL_START_STRING_ARRAY_WITH_CAPITAL= " + willStartStringArrayWithCapital+"\n";
		config+= "STRING_ARRAY_SUFIX= " + stringArraySufix+"\n";
		config+= "STRING_ARRAY_START_BLOCK_SYMBOL= " + stringArrayStartBlockSymbol+"\n";
		config+= "STRING_ARRAY_END_BLOCK_SYMBOL= " + stringArrayEndBlockSymbol+"\n";

		config+= "POST_STRING_DEFINITION= " + postStringDefinition+"\n";



		config+= "ASSIGN_SYMBOL= " + assignSymbol+"\n";
		config+= "WILL_USE_ASSIGN= " + willUseAssign+"\n";
		config+= "WILL_REMOVE_EXTENSION= " + willRemoveExtension+"\n";

		config+= "IGNORE_EXTENSIONS= " + ignoredExtensions+"\n";
		config+= "IGNORE_PATHS= " + ignoredPaths+"\n";
		
		
		pw.write(config);
		pw.close();
	}
	
	public synchronized void readConfig() throws IOException
	{
		if(isReadingConfig)
			return;
		isReadingConfig = true;
		File f = new File("./settings.config");
		if(f.exists())
		{
			List<String> list =  Files.readAllLines(f.toPath());
			
			path = getContent(list, "PATH_TO_CREATE_FILE= ");

			String watching = pathToWatch;
			System.out.println("@UPDATES@: " + ++updates);

			pathToWatch = getContent(list, "PATH_TO_WATCH= ");
			if(!watching.equals(pathToWatch))
			{
				ResourceEnumGenerator.generator.service.close();
				ResourceEnumGenerator.generator.isProcessing = false;
			}

			willUseClassName = getBool(list, "WILL_USE_CLASS_NAME= ");
			if(willUseClassName)
				customClassName = getContent(list, "CUSTOM_CLASS_NAME= ");

			classNameStartWithCapital = getBool(list, "CLASS_NAME_START_WITH_CAPITAL= ");

			classDeclarator = getContent(list, "CLASS_DECLARATOR= ");

			isEnumMode = getBool(list, "IS_ENUM_MODE= ");
			enumDeclarator = getContent(list, "ENUM_DECLARATOR= ");
			postEnumDeclaration = getContent(list, "POST_ENUM_DECLARATION= ");
			afterEnumLastBracket = getContent(list, "AFTER_ENUM_LAST_BRACKET= ");
			surroundEnumConstsWith = getContent(list, "ENUM_CONST_SURROUND_WITH= ");
			enumToUppercase = getBool(list, "ENUM_TO_UPPERCASE= ");
			enumConstToUppercase = getBool(list, "ENUM_CONST_TO_UPPERCASE= ");

			innerClassDeclarator = getContent(list, "INNER_CLASS_DECLARATOR= ");
			postInnerClassDeclarator = getContent(list, "POST_INNER_CLASS_DECLARATOR= ");

			stringArrayDeclarator = getContent(list, "STRING_ARRAY_DECLARATOR= ");

			
			stringArrayPrefix = getContent(list, "STRING_ARRAY_PREFIX= ");
			willStartStringArrayWithCapital = getBool(list, "WILL_START_STRING_ARRAY_WITH_CAPITAL= ");
			stringArraySufix = getContent(list, "STRING_ARRAY_SUFIX= ");

			postStringArrayDeclarator = getContent(list, "POST_STRING_ARRAY_DECLARATOR= ");


			postStringDefinition = getContent(list, "POST_STRING_DEFINITION= ");

			stringArrayStartBlockSymbol = getContent(list, "STRING_ARRAY_START_BLOCK_SYMBOL= ");
			stringArrayEndBlockSymbol = getContent(list, "STRING_ARRAY_END_BLOCK_SYMBOL= ");

			assignSymbol = getContent(list, "ASSIGN_SYMBOL= ");
			willUseAssign = getBool(list, "WILL_USE_ASSIGN= ");
			willRemoveExtension = getBool(list, "WILL_REMOVE_EXTENSION= ");

			toIgnore = getStrings(list, "IGNORE_EXTENSIONS= ");
			ignoredExtensions = getContent(list, "IGNORE_EXTENSIONS= ");
			pathsToIgnore = getStrings(list, "IGNORE_PATHS= ");
			ignoredPaths = getContent(list, "IGNORE_PATHS= ");
		}
		else
			generateDefaultFormatFile();

		isReadingConfig = false;
	}
	
	public void scheduleUpdate(List<Path> pathsToSchedule, boolean updateConfig) throws IOException
	{
		if(isWriting || isReadingConfig)
			return;
		if(updateConfig)
			isConfigUpdateScheduled = true;
		scheduledPath = pathsToSchedule;
		isUpdateScheduled = true;
	}

	private boolean checkIgnored(String str, ArrayList<String> listToIgnore)
	{
		for(int i = 0, len = listToIgnore.size(); i < len; i++)
		{
			if(str.contains(listToIgnore.get(i)))
			{
				//System.out.println(str + " was ignored because of the " + listToIgnore.get(i) + " ignore");
				return true;
			}
		}
		return false;

	}

	private synchronized String pathWrite(Path path, int branchCount)
	{
		int extraTab = (isEnumMode) ? 1 : 0;
		String code = "";
		int count = branchCount;

		File[] files = path.toFile().listFiles(File::isFile);
		if(isEnumMode)
		{
			if(enumToUppercase)
				code+= InnerClassWriter.multiplyString("\t", count) + enumDeclarator + path.toFile().getName().toUpperCase().replaceAll("\\s", "_") + postEnumDeclaration + "\n" + InnerClassWriter.multiplyString("\t", count) + "{\n";
			else
				code+= InnerClassWriter.multiplyString("\t", count) + enumDeclarator + path.toFile().getName().replaceAll("\\s", "_") + postEnumDeclaration + "\n" + InnerClassWriter.multiplyString("\t", count) + "{\n";
		}

		String arrayName = path.toFile().getName().replaceAll("\\s", "_");
		if(willStartStringArrayWithCapital)
			arrayName = String.valueOf(arrayName.charAt(0)).toUpperCase() + arrayName.substring(1);
		String strArray = InnerClassWriter.multiplyString("\t", count) + stringArrayDeclarator + stringArrayPrefix + arrayName + stringArraySufix + postStringArrayDeclarator + "\n";
		strArray+= InnerClassWriter.multiplyString("\t", count) + stringArrayStartBlockSymbol + "\n";
		if(files != null)
		{
			for(int i = 0, len = files.length; i < len; i++)
			{
				String enumConstant = files[i].getName();
				if(!willRemoveExtension)
					enumConstant = enumConstant.replaceAll("\\.", "_");
				else
				{
					int index = enumConstant.lastIndexOf(".");
					if(index != -1)
						enumConstant = enumConstant.substring(0, index);
				}
				enumConstant = enumConstant.replaceAll("[\\$|\\(|\\)]", "");
				if(checkIgnored(enumConstant, toIgnore))
					continue;
				enumConstant = enumConstant.replaceAll("[\\s|\\-]", "_");
				code+= InnerClassWriter.multiplyString("\t", count + extraTab);
				if(!isEnumMode)
					code+= enumDeclarator;
				code+= surroundEnumConstsWith;
				if(enumConstToUppercase)
					code+= enumConstant.toUpperCase() + surroundEnumConstsWith;
				else
					code+= enumConstant + surroundEnumConstsWith;

				if(willUseAssign || !isEnumMode)
					code+= " " + assignSymbol + " \"" + files[i].getPath().replaceAll("\\\\", "\\\\\\\\") + "\"";
				else
					strArray+= InnerClassWriter.multiplyString("\t", count + 1) + "\"" + files[i].getPath().replaceAll("\\\\", "\\\\\\\\") + "\"" + postStringDefinition + "\n";
				if(isEnumMode)
				{
					if(i != len -1)
						code+= ",";
				}
				else
					code+= ";";
				code+= "\n";
			}
		}
		if(isEnumMode)
			code+= InnerClassWriter.multiplyString("\t", count) + "}" + afterEnumLastBracket + "\n";
		if(!willUseAssign && isEnumMode)
		{
			strArray+= InnerClassWriter.multiplyString("\t", count) + stringArrayEndBlockSymbol + "\n";
			code+= strArray;
		}
		return code;
	}
	
	
	public synchronized void write(List<Path> paths) throws IOException
	{
		if(!isWriting)
		{
			isWriting = true;
			System.out.println("Starting to write file");
			String code = "";
			PrintWriter pw = new PrintWriter(path);
			if(willUseClassName)
			{
				if(!customClassName.equals(""))
					code+= classDeclarator + customClassName + "\n{\n";
				else
				{
					String s = InnerClassWriter.getCurrentDirName(pathToWatch);
					if(s.equals(""))
						s = InnerClassWriter.getCurrentDirName(Paths.get(pathToWatch).toAbsolutePath().toString());
					if(classNameStartWithCapital)
						code+= classDeclarator + String.valueOf(s.charAt(0)).toUpperCase() + s.substring(1) + "\n{\n";
					else
						code+= classDeclarator + s + "\n{\n";
				}

			}
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
			
			paths = InnerClassWriter.removeCommonPaths(paths);
			Collections.sort(paths, new Comparator<Path>()
			{
				@Override
				public int compare(Path p1, Path p2) 
				{
					 return InnerClassWriter.countDir(p2.toString()) - InnerClassWriter.countDir(p1.toString());
				}
			});
			Path path;
			while(paths.size() != 0)
			{
				path = paths.get(0);
				boolean hasWriteAlready = false;
				int count = 1;
				String currentDir = path.toString();
				String staticCurrentDir = Paths.get(currentDir).toString();
				currentDir = InnerClassWriter.enterNextDir(currentDir);
				if(!pathToWatch.equals("./") && !pathToWatch.equals(".\\"))
				{
					if(!pathToWatch.contains(":\\"))
						currentDir = currentDir.substring(Paths.get(pathToWatch).toString().length() - 1);
					else
						currentDir = currentDir.substring(Paths.get(pathToWatch).toString().length() - 2);
				}
				String traveled = pathToWatch;
				if(traveled.charAt(traveled.length() - 1) != '/' && traveled.charAt(traveled.length() - 1) != '\\')
				{
					traveled+= ((traveled.contains("/") ? "/" : "\\"));
				}

				while(InnerClassWriter.countDir(currentDir) > 0)
				{
					String toWrite = InnerClassWriter.getRootOfDir(currentDir);
					traveled+= toWrite +"/";
					toWrite = String.valueOf(toWrite.charAt(0)).toUpperCase() + toWrite.substring(1, toWrite.length());
					code+= InnerClassWriter.multiplyString("\t", count) + innerClassDeclarator + toWrite +  postInnerClassDeclarator + "\n" + InnerClassWriter.multiplyString("\t", count) + "{\n";
					count++;
					currentDir = InnerClassWriter.enterNextDir(currentDir);
					
					List<Path> commonPaths = new ArrayList<Path>();
					String nTravel = Paths.get(traveled).toString();
					for(Path nPath : paths)
					{
						String pathChecking = nPath.toString();
						if(pathChecking.indexOf(nTravel) != -1)
							if(pathChecking.indexOf(nTravel) == staticCurrentDir.indexOf(nTravel))
								commonPaths.add(nPath);
					}
					for(Path nPath : commonPaths)
					{
						String pathChecking = nPath.toString().substring(nTravel.length());
						if(InnerClassWriter.countDir(pathChecking) == 1)
						{
							if(nPath.toString().equals(staticCurrentDir))
								hasWriteAlready = true;
							paths.remove(nPath);
							code+= pathWrite(nPath, count);
						}
					}
				}

				if(!hasWriteAlready)
				{
					code+= pathWrite(path, count);
					paths.remove(path);
				}
				while(count != 1)
				{
					code+= InnerClassWriter.multiplyString("\t", count - ((isEnumMode) ? 1 : 0)) + "}\n";
					count--;
				}
				
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
