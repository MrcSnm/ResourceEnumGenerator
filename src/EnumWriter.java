import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JOptionPane;

public class EnumWriter 
{ 
	public String path = "./enumwriter.cs";
	private String classDeclarator = "public class ";
	private String postClassDeclarator = "";
	private boolean willUseClassName = true;
	private String customClassName = "";
	private boolean classNameStartWithCapital = true;
	public String pathToWatch = "./";
	public String pathRelativeTo = "";
	public boolean relativizePathNamesToGeneratorDir = true;

	private boolean isEnumMode = true;
	private String surroundEnumConstsWith = "";
	private String enumDeclarator = "public enum ";
	private String postEnumDeclaration = "";
	private String afterEnumLastBracket = "";

	private boolean enumStartWithCapital = true;
	private boolean enumConstToUppercase = false;
	private boolean enumToUppercase = false;
	
	

	private String enumStartBlockSymbol = "{";
	private String enumEndBlockSymbol = "}";
	private String lastEnumEndBlockSymbol = "}";

	
	
	
	private String innerClassDeclarator = "public static class ";
	private String postInnerClassDeclarator = "";
	private String innerClassStartBlockSymbol = "{";
	private String innerClassEndBlockSymbol = "}";
	private String lastInnerClassEndBlockSymbol = "}";
	
	private String stringArrayDeclarator = "public static string[] ";
	private String stringArrayPrefix = "get";
	private String stringArraySufix = "";
	private boolean willStartStringArrayWithCapital = true;

	private String postStringArrayDeclarator = " = new string[]";
	private String stringArrayStartBlockSymbol = "{";
	private String stringArrayEndBlockSymbol = "};";
	private String lastStringArrayEndBlockSymbol = "};";

	private String postStringDefinition = ",";

	private String assignSymbol = "=";

	private ArrayList<String> toIgnore = new ArrayList<String>();
	private ArrayList<String> pathsToIgnore = new ArrayList<String>();
	private String ignoredExtensions = ".config, .java, .git, .classpath, .project, .meta";
	private String ignoredPaths = ".git, .vscode, node_modules";
	
	private String packageDeclarator = "package ";
	private String packageName = "";
	private String importDeclarator = "import ";
	
	private ArrayList<String> imports = new ArrayList<String>();
	private String importString = "";
	

	private Thread t;
	private boolean willUseAssign = false;
	private boolean willRemoveExtension = false;
	private boolean willRemoveExtensionFromString = false;

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
				while(ref.isRunning)
				{
					try {Thread.sleep(30);} 
					catch (InterruptedException e1) {e1.printStackTrace();}
					if(isConfigUpdateScheduled && !isReadingConfig && !isWriting)
					{
						try {readConfig();} 
						catch (IOException e) {	InnerClassWriter.showError(e);}
						isConfigUpdateScheduled = false;
						continue;
					}
					if(isUpdateScheduled && !isReadingConfig && !isWriting)
					{
						isUpdateScheduled = false;
						try {write(scheduledPath);} 
						catch (IOException e) {	InnerClassWriter.showError(e);}
						continue;
					}
				}
				
			}
		});
		t.start();
	}
	
	public void setAsCSharp()
	{
		if(path.contains("."))
			path = path.substring(0, path.lastIndexOf(".")) + ".cs";
		classDeclarator = "public class ";
		postClassDeclarator = "";
		willUseClassName = true;
		customClassName = "";
		classNameStartWithCapital = true;

		isEnumMode = true;
		surroundEnumConstsWith = "";
		enumDeclarator = "public enum ";
		postEnumDeclaration = "";
		afterEnumLastBracket = "";

		enumStartWithCapital = true;
		enumConstToUppercase = false;
		enumToUppercase = false;
		
		

		enumStartBlockSymbol = "{";
		enumEndBlockSymbol = "}";
		lastEnumEndBlockSymbol = "}";

		
		
		
		innerClassDeclarator = "public static class ";
		postInnerClassDeclarator = "";
		innerClassStartBlockSymbol = "{";
		innerClassEndBlockSymbol = "}";
		lastInnerClassEndBlockSymbol = "}";
		
		stringArrayDeclarator = "public static string[] ";
		willStartStringArrayWithCapital = true;

		postStringArrayDeclarator = " = new string[]";
		stringArrayStartBlockSymbol = "{";
		stringArrayEndBlockSymbol = "};";
		lastStringArrayEndBlockSymbol = "};";

		postStringDefinition = ",";

		assignSymbol = "=";
		willUseAssign = false;

		packageDeclarator = "package ";
		importDeclarator = "using ";
		ResourceEnumGenerator.showTrayMessage("Config", "C# config was generated", false, true);
	}

	public void setAsJava()
	{
		if(path.contains("."))
			path = path.substring(0, path.lastIndexOf(".")) + ".java";
		classDeclarator = "public class ";
		postClassDeclarator = "";
		willUseClassName = true;
		customClassName = "";
		classNameStartWithCapital = true;

		isEnumMode = true;
		surroundEnumConstsWith = "";
		enumDeclarator = "public enum ";
		postEnumDeclaration = "";
		afterEnumLastBracket = "";

		enumStartWithCapital = true;
		enumConstToUppercase = false;
		enumToUppercase = false;
		
		

		enumStartBlockSymbol = "{";
		enumEndBlockSymbol = "}";
		lastEnumEndBlockSymbol = "}";

		
		
		
		innerClassDeclarator = "public static class ";
		postInnerClassDeclarator = "";
		innerClassStartBlockSymbol = "{";
		innerClassEndBlockSymbol = "}";
		lastInnerClassEndBlockSymbol = "}";
		
		stringArrayDeclarator = "public static String[] ";
		willStartStringArrayWithCapital = true;

		postStringArrayDeclarator = " = new String[]";
		stringArrayStartBlockSymbol = "{";
		stringArrayEndBlockSymbol = "};";
		lastStringArrayEndBlockSymbol = "};";

		postStringDefinition = ",";

		assignSymbol = "=";
		willUseAssign = false;

		packageDeclarator = "package ";
		importDeclarator = "import ";
		ResourceEnumGenerator.showTrayMessage("Config", "Java config was generated", false, true);
	}

	public void setAsJavascript()
	{
		if(path.contains("."))
			path = path.substring(0, path.lastIndexOf(".")) + ".js";
		classDeclarator = "const ";
		postClassDeclarator = " = ";
		willUseClassName = true;
		customClassName = "";
		classNameStartWithCapital = true;

		isEnumMode = true;
		surroundEnumConstsWith = "";
		enumDeclarator = "";
		postEnumDeclaration = " : ";
		afterEnumLastBracket = "";

		enumStartWithCapital = true;
		enumConstToUppercase = false;
		enumToUppercase = false;
		
		

		enumStartBlockSymbol = "{";
		enumEndBlockSymbol = "},";
		lastEnumEndBlockSymbol = "}";

		
		
		
		innerClassDeclarator = "";
		postInnerClassDeclarator = " : ";
		innerClassStartBlockSymbol = "{";
		innerClassEndBlockSymbol = "},";
		lastInnerClassEndBlockSymbol = "}";
		
		stringArrayDeclarator = "const ";
		willStartStringArrayWithCapital = true;

		postStringArrayDeclarator = " = ";
		stringArrayStartBlockSymbol = "[";
		stringArrayEndBlockSymbol = "],";
		lastStringArrayEndBlockSymbol = "]";

		postStringDefinition = ",";

		assignSymbol = ":";
		willUseAssign = true;

		packageDeclarator = "package ";
		importDeclarator = "import ";
		ResourceEnumGenerator.showTrayMessage("Config", "Javascript config was generated", false, true);
	}

	public void setAsJSON()
	{
		if(path.contains("."))
			path = path.substring(0, path.lastIndexOf(".")) + ".json";
		classDeclarator = "\"";
		postClassDeclarator = "";

		willUseClassName = false;
		customClassName = "";

		isEnumMode = true;
		surroundEnumConstsWith = "\"";
		enumDeclarator = "\"";
		postEnumDeclaration = "\" :";
	
		enumStartBlockSymbol = "{";
		enumEndBlockSymbol = "},";
		lastEnumEndBlockSymbol = "}";

		
		
		
		innerClassDeclarator = "\"";
		postInnerClassDeclarator = "\" :";
		innerClassStartBlockSymbol = "{";
		innerClassEndBlockSymbol = "},";
		lastInnerClassEndBlockSymbol = "}";
		
		stringArrayDeclarator = "\"";
		postStringArrayDeclarator = "\" :";

		stringArrayStartBlockSymbol = "{";
		stringArrayEndBlockSymbol = "},";
		lastStringArrayEndBlockSymbol = "}";

		postStringDefinition = ",";

		assignSymbol = ":";
		willUseAssign = true;
		
		packageDeclarator = "package ";
		importDeclarator = "import ";

		ResourceEnumGenerator.showTrayMessage("Config", "JSON config was generated", false, true);
	}

	public static String updateToRelative(String target)
	{
		String currentDir = Paths.get("./").toAbsolutePath().normalize().toString();
		if(!InnerClassWriter.isRootEqual(target, currentDir))
			return target;
		else if(target.equals(currentDir))
			return "./";
		else
			return Paths.get("./").toAbsolutePath().normalize().relativize(Paths.get(target).toAbsolutePath().normalize()).toString();
	}
	
	
	public synchronized void generateDefaultFormatFile() throws IOException
	{
		if(relativizePathNamesToGeneratorDir)
		{
			String toWatch = null, toRelate = null, toOut = null;
			toWatch = updateToRelative(pathToWatch);
			toRelate = updateToRelative(pathRelativeTo);			
			toOut = updateToRelative(path);
			if(toWatch != null  && !toWatch.equals(""))
				pathToWatch = toWatch;
			if(toRelate != null && !toRelate.equals(""))
				pathRelativeTo = toRelate;
			if(toOut != null && !toOut.equals(""))
				path = toOut;
		}
		else
		{	
			if(pathRelativeTo != null && !pathRelativeTo.equals(""))
				pathRelativeTo = Paths.get(pathRelativeTo).toAbsolutePath().normalize().toString();
			pathToWatch = Paths.get(pathToWatch).toAbsolutePath().normalize().toString();
			path = Paths.get(path).toAbsolutePath().normalize().toString();
		}
		
		
		boolean fileExists = Files.exists(Paths.get("./settings.config"));
		
		//if(!fileExists)
		//else
			//pw = new PrintWriter("./settings.config_TEMP");
		File f = new File("./settings.config");
		String config = "";
		
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		raf.setLength(0);
		
		
		config+= "PATH_TO_WATCH= " + pathToWatch+"\n";
		config+= "PATH_RELATIVE_TO= " + pathRelativeTo+"\n";
		config+= "PATH_TO_CREATE_FILE= " + path+"\n";
		config+= "RELATIVIZE_PATH_NAMES_TO_GENERATOR_DIRECTORY= " + relativizePathNamesToGeneratorDir  + "\n";
		config+="\n\n";
		
		
		config+= "PACKAGE_DECLARATOR= " + packageDeclarator + "\n";
		config+= "PACKAGE_NAME= " + packageName + "\n";
		config+= "IMPORT_DECLARATOR= " + importDeclarator + "\n";
		config+= "IMPORT_LIST= " + importString + "\n";
		config+= "\n\n";


		config+= "WILL_USE_CLASS_NAME= " + willUseClassName+"\n";
		config+= "CUSTOM_CLASS_NAME= " + customClassName+"\n";
		config+= "CLASS_NAME_START_WITH_CAPITAL= " + classNameStartWithCapital+"\n";
		config+= "CLASS_DECLARATOR= " + classDeclarator+"\n";
		config+= "POST_CLASS_DECLARATOR= " + postClassDeclarator+"\n";
		config+= "\n\n";

		config+= "IS_ENUM_MODE= " + isEnumMode+"\n";
		config+= "ENUM_DECLARATOR= " + enumDeclarator+"\n";
		config+= "POST_ENUM_DECLARATION= " + postEnumDeclaration+"\n";
		config+= "\n";

		config+= "ENUM_START_BLOCK_SYMBOL= " + enumStartBlockSymbol+"\n";
		config+= "ENUM_END_BLOCK_SYMBOL= " + enumEndBlockSymbol+"\n";
		config+= "LAST_ENUM_END_BLOCK_SYMBOL= " + lastEnumEndBlockSymbol +"\n";
		config+= "\n";

		config+= "ENUM_CONST_SURROUND_WITH= " + surroundEnumConstsWith+"\n";
		config+= "ENUM_START_WITH_CAPITAL= " + enumStartWithCapital+"\n";
		config+= "ENUM_TO_UPPERCASE= " + enumToUppercase+"\n";
		config+= "ENUM_CONST_TO_UPPERCASE= " + enumConstToUppercase+"\n";
		config+="\n\n";


		config+= "INNER_CLASS_DECLARATOR= " + innerClassDeclarator+"\n";
		config+= "POST_INNER_CLASS_DECLARATOR= "+ postInnerClassDeclarator+"\n";
		config+= "INNER_CLASS_START_BLOCK_SYMBOL= " + innerClassStartBlockSymbol+"\n";
		config+= "INNER_CLASS_END_BLOCK_SYMBOL= " + innerClassEndBlockSymbol+"\n";
		config+= "LAST_INNER_CLASS_END_BLOCK_SYMBOL= " + lastInnerClassEndBlockSymbol+"\n";
		config+= "\n\n";


		config+= "STRING_ARRAY_DECLARATOR= " + stringArrayDeclarator+"\n";
		config+= "POST_STRING_ARRAY_DECLARATOR= " + postStringArrayDeclarator+"\n";
		config+= "STRING_ARRAY_PREFIX= " + stringArrayPrefix+"\n";
		config+= "WILL_START_STRING_ARRAY_WITH_CAPITAL= " + willStartStringArrayWithCapital+"\n";
		config+= "STRING_ARRAY_SUFIX= " + stringArraySufix+"\n";
		config+="\n";

		config+= "STRING_ARRAY_START_BLOCK_SYMBOL= " + stringArrayStartBlockSymbol+"\n";
		config+= "STRING_ARRAY_END_BLOCK_SYMBOL= " + stringArrayEndBlockSymbol+"\n";
		config+= "LAST_STRING_ARRAY_END_BLOCK_SYMBOL= " + lastStringArrayEndBlockSymbol+"\n";

		config+= "POST_STRING_DEFINITION= " + postStringDefinition+"\n";



		config+= "ASSIGN_SYMBOL= " + assignSymbol+"\n";
		config+= "WILL_USE_ASSIGN= " + willUseAssign+"\n";
		config+= "WILL_REMOVE_EXTENSION= " + willRemoveExtension+"\n";
		config+= "WILL_REMOVE_EXTENSION_FROM_STRING= " + willRemoveExtensionFromString +"\n";


		config+= "IGNORE_EXTENSIONS= " + ignoredExtensions+"\n";
		config+= "IGNORE_PATHS= " + ignoredPaths+"\n";
		
		//PrintWriter pw;
		//pw = new PrintWriter("./settings.config");
		//pw.write(config);
		//pw.close();
		raf.writeBytes(config);
		raf.close();
		ResourceEnumGenerator.showTrayMessage("Config", "A new config was generated", true, false);
		
	}

	public void updatePathToWatch() throws IOException
	{
		if(ResourceEnumGenerator.generator != null)
		{
			ResourceEnumGenerator.generator.service.close();
			ResourceEnumGenerator.generator.isProcessing = false;
		}
		//ResourceEnumGenerator.generator.pathsRestart(pathToWatch);
	}

	private String getAbsolutePath(String path)
	{
		return Paths.get(path).toAbsolutePath().normalize().toString();
	}
	
	public boolean isRelativeAndWatchingEqual()
	{
		if(pathRelativeTo != null && !pathRelativeTo.equals(""))
		{
			if(!InnerClassWriter.isRootEqual(getAbsolutePath(pathToWatch), getAbsolutePath(pathRelativeTo)))
			{
				JOptionPane.showMessageDialog(null, "The watchings path's root (" + InnerClassWriter.getRoot(getAbsolutePath(pathToWatch)) + ") differs from the relative path (" + InnerClassWriter.getRoot(pathRelativeTo) + ")\nRelative path will be cleared", "Different Roots", JOptionPane.WARNING_MESSAGE);
				pathRelativeTo = "";
				return false;
			}	
		}
		return true;
	}
	
	public synchronized void readConfig(boolean updateDefault) throws IOException
	{
		if(isReadingConfig)
			return;
		
		
		isReadingConfig = true;
		File f = Paths.get("./settings.config").toFile();
		boolean scheduledToGenerate = false;
		if(f.exists() && !updateDefault)
		{
			InnerClassWriter.waitToFinishWithError(f);
			System.out.println("____Has Started Reading Config_____");
			List<String> list = Files.readAllLines(f.toPath());
			
			String watching = pathToWatch;
			System.out.println("@UPDATES@: " + ++updates);
			
			pathToWatch = getContent(list, "PATH_TO_WATCH= ");
			if(pathToWatch == null || pathToWatch.equals(""))
			{
				JOptionPane.showMessageDialog(null, "Your 'PATH_TO_WATCH= ' is empty, it is scheduled to be default on ./", "No PATH_TO_WATCH=  defined", JOptionPane.WARNING_MESSAGE);
				pathToWatch = "./";
				scheduledToGenerate = true;
			}
			else if(!Files.exists(Paths.get(pathToWatch)))
			{
				JOptionPane.showMessageDialog(null, "Your 'PATH_TO_WATCH= ' does not exists. It is scheduled to be default on ./", "PATH_TO_WATCH= not found", JOptionPane.WARNING_MESSAGE);
				pathToWatch = "./";
				scheduledToGenerate = true;
			}

			path = getContent(list, "PATH_TO_CREATE_FILE= ");
			if(path == null || path.equals(""))
			{
				JOptionPane.showMessageDialog(null, "Your 'PATH_TO_CREATE_FILE= ' is empty, it is scheduled to be default on ./enumwriter.cs", "No PATH_TO_WATCH=  defined", JOptionPane.WARNING_MESSAGE);
				path = "./enumwriter.cs";
				scheduledToGenerate = true;
			}
			else if(!Files.exists(Paths.get(InnerClassWriter.getRoot(path))))
			{
				JOptionPane.showMessageDialog(null, "Your 'PATH_TO_WATCH= ' root does not exists. It is scheduled to be default on ./enumwriter.cs", "PATH_TO_WATCH= not found", JOptionPane.WARNING_MESSAGE);
				path = "./enumwriter.cs";
				scheduledToGenerate = true;
			}
			
			pathRelativeTo = getContent(list, "PATH_RELATIVE_TO= ");
			if(!isRelativeAndWatchingEqual())
				scheduledToGenerate = true;

			
			boolean isRelativizing = relativizePathNamesToGeneratorDir;
			relativizePathNamesToGeneratorDir = getBool(list, "RELATIVIZE_PATH_NAMES_TO_GENERATOR_DIRECTORY= ");
			if(isRelativizing != relativizePathNamesToGeneratorDir || (relativizePathNamesToGeneratorDir && pathToWatch.equals(getAbsolutePath(pathToWatch))))
				scheduledToGenerate = true;
			
			
			
 			packageDeclarator = getContent(list, "PACKAGE_DECLARATOR= ");
			packageName = getContent(list, "PACKAGE_NAME= ");
			importDeclarator = getContent(list, "IMPORT_DECLARATOR= ");
			
			imports = getStrings(list, "IMPORT_LIST= ");
			importString = getContent(list, "IMPORT_LIST= ");
			

			willUseClassName = getBool(list, "WILL_USE_CLASS_NAME= ");
			if(willUseClassName)
				customClassName = getContent(list, "CUSTOM_CLASS_NAME= ");

			classNameStartWithCapital = getBool(list, "CLASS_NAME_START_WITH_CAPITAL= ");

			classDeclarator = getContent(list, "CLASS_DECLARATOR= ");
			postClassDeclarator = getContent(list, "POST_CLASS_DECLARATOR= ");

			isEnumMode = getBool(list, "IS_ENUM_MODE= ");
			enumDeclarator = getContent(list, "ENUM_DECLARATOR= ");
			postEnumDeclaration = getContent(list, "POST_ENUM_DECLARATION= ");
			enumStartBlockSymbol = getContent(list, "ENUM_START_BLOCK_SYMBOL= ");
			enumEndBlockSymbol = getContent(list, "ENUM_END_BLOCK_SYMBOL= ");
			lastEnumEndBlockSymbol = getContent(list, "LAST_ENUM_END_BLOCK_SYMBOL= ");

			//afterEnumLastBracket = getContent(list, "AFTER_ENUM_LAST_BRACKET= ");
			surroundEnumConstsWith = getContent(list, "ENUM_CONST_SURROUND_WITH= ");
			enumStartWithCapital = getBool(list, "ENUM_START_WITH_CAPITAL= ");

			enumToUppercase = getBool(list, "ENUM_TO_UPPERCASE= ");
			enumConstToUppercase = getBool(list, "ENUM_CONST_TO_UPPERCASE= ");

			



			innerClassDeclarator = getContent(list, "INNER_CLASS_DECLARATOR= ");
			postInnerClassDeclarator = getContent(list, "POST_INNER_CLASS_DECLARATOR= ");
			innerClassStartBlockSymbol = getContent(list, "INNER_CLASS_START_BLOCK_SYMBOL= ");
			innerClassEndBlockSymbol = getContent(list, "INNER_CLASS_END_BLOCK_SYMBOL= ");
			lastInnerClassEndBlockSymbol = getContent(list, "LAST_INNER_CLASS_END_BLOCK_SYMBOL= ");


			stringArrayDeclarator = getContent(list, "STRING_ARRAY_DECLARATOR= ");

			
			stringArrayPrefix = getContent(list, "STRING_ARRAY_PREFIX= ");
			willStartStringArrayWithCapital = getBool(list, "WILL_START_STRING_ARRAY_WITH_CAPITAL= ");
			stringArraySufix = getContent(list, "STRING_ARRAY_SUFIX= ");

			postStringArrayDeclarator = getContent(list, "POST_STRING_ARRAY_DECLARATOR= ");


			postStringDefinition = getContent(list, "POST_STRING_DEFINITION= ");

			stringArrayStartBlockSymbol = getContent(list, "STRING_ARRAY_START_BLOCK_SYMBOL= ");
			stringArrayEndBlockSymbol = getContent(list, "STRING_ARRAY_END_BLOCK_SYMBOL= ");
			lastStringArrayEndBlockSymbol = getContent(list, "LAST_STRING_ARRAY_END_BLOCK_SYMBOL= ");

			assignSymbol = getContent(list, "ASSIGN_SYMBOL= ");
			willUseAssign = getBool(list, "WILL_USE_ASSIGN= ");
			willRemoveExtension = getBool(list, "WILL_REMOVE_EXTENSION= ");
			willRemoveExtensionFromString = getBool(list, "WILL_REMOVE_EXTENSION_FROM_STRING= ");

			toIgnore = getStrings(list, "IGNORE_EXTENSIONS= ");
			ignoredExtensions = getContent(list, "IGNORE_EXTENSIONS= ");
			pathsToIgnore = getStrings(list, "IGNORE_PATHS= ");
			ignoredPaths = getContent(list, "IGNORE_PATHS= ");

			
			
			if(!watching.equals(pathToWatch))
				updatePathToWatch();
			
			if(scheduledToGenerate)
				generateDefaultFormatFile();
			System.out.println("____Has Finished Reading Config_____");
		}
		else
			generateDefaultFormatFile();

		isReadingConfig = false;
	}
	
	public void readConfig() throws IOException
	{
		readConfig(false);
	}
	
	public void scheduleUpdate(List<Path> pathsToSchedule, boolean updateConfig) throws IOException
	{
		/*if(isWriting || isReadingConfig)
			return;*/
		if(updateConfig)
			isConfigUpdateScheduled = true;
		
		if(pathsToSchedule != null)
		{
			scheduledPath = pathsToSchedule;
			isUpdateScheduled = true;	
		}
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

	private synchronized String pathWrite(Path path, int branchCount, boolean isLast)
	{
		int extraTab = (isEnumMode) ? 1 : 0;
		String code = "";
		int count = branchCount;

		File[] files = path.toFile().listFiles(File::isFile);
		if(isEnumMode)
		{
			String str = path.toFile().getName().replaceAll("[\\s|\\.|\\-]", "_");
			if(enumStartWithCapital)
				code+= InnerClassWriter.multiplyString("\t", count) + enumDeclarator + String.valueOf(str.charAt(0)).toUpperCase() + str.substring(1) + postEnumDeclaration + "\n" + InnerClassWriter.multiplyString("\t", count) + enumStartBlockSymbol + "\n";
			else if(enumToUppercase)
				code+= InnerClassWriter.multiplyString("\t", count) + enumDeclarator + str.toUpperCase() + postEnumDeclaration + "\n" + InnerClassWriter.multiplyString("\t", count) + postEnumDeclaration + "\n";
			else
				code+= InnerClassWriter.multiplyString("\t", count) + enumDeclarator + str + postEnumDeclaration + "\n" + InnerClassWriter.multiplyString("\t", count) + postEnumDeclaration + "\n";
		}
		
		String arrayName = path.toFile().getName().replaceAll("[\\s|\\.|\\-]", "_");
		if(willStartStringArrayWithCapital)
			arrayName = String.valueOf(arrayName.charAt(0)).toUpperCase() + arrayName.substring(1);
		String strArray = InnerClassWriter.multiplyString("\t", count) + stringArrayDeclarator + stringArrayPrefix + arrayName + stringArraySufix + postStringArrayDeclarator + "\n";
		strArray+= InnerClassWriter.multiplyString("\t", count) + stringArrayStartBlockSymbol + "\n";
		if(files != null)
		{
			ArrayList<File> buffer = new ArrayList<File>();
			for(int i = 0, len = files.length; i < len; i++)
			{
				String enumConstant = files[i].getName();
				enumConstant = enumConstant.replaceAll("[\\$|\\(|\\)]", "");
				if(checkIgnored(enumConstant, toIgnore))
					continue;
				buffer.add(files[i]);
			}
			files = buffer.toArray(new File[0]);
			for(int i = 0, len = files.length; i < len; i++)
			{
				String enumConstant = files[i].getName();
				enumConstant = enumConstant.replaceAll("[\\$|\\(|\\)]", "");
				if(!willRemoveExtension)
					enumConstant = enumConstant.replaceAll("\\.", "_");
				else
				{
					int index = enumConstant.lastIndexOf(".");
					if(index != -1)
						enumConstant = enumConstant.substring(0, index);
				}
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
				{
					if(pathRelativeTo.equals(""))
						code+= " " + assignSymbol + " \"" + files[i].getPath().replaceAll("\\\\", "\\\\\\\\") + "\"";
					else
					{
						Path checking = null;
						try{checking = Paths.get(getAbsolutePath(files[i].getCanonicalPath()).toString());}
						catch(IOException e){InnerClassWriter.showError(e);}
						Path other = Paths.get(getAbsolutePath(pathRelativeTo));
						code+= " " + assignSymbol + " \"" + other.relativize(checking).toString().toString().replaceAll("\\\\", "\\\\\\\\") + "\"";
					}
				}
				else
				{
					if(pathRelativeTo.equals(""))
					{
						if(!willRemoveExtensionFromString)
							strArray+= InnerClassWriter.multiplyString("\t", count + 1) + "\"" + files[i].getPath().replaceAll("\\\\", "\\\\\\\\") + "\"" + postStringDefinition + "\n";
						else
						{
							String pathNoExtension = files[i].getPath().replaceAll("\\\\", "\\\\\\\\");
							int index = pathNoExtension.lastIndexOf(".");
							if(index != -1)
								pathNoExtension = pathNoExtension.substring(0, index);
							strArray+= InnerClassWriter.multiplyString("\t", count + 1) + "\"" + pathNoExtension + "\"" + postStringDefinition + "\n";
						}
					}
					else
					{
						Path checking = null;
						try
						{checking = Paths.get(files[i].getCanonicalPath());}
						catch(IOException e){InnerClassWriter.showError(e);}
						Path other = Paths.get(pathRelativeTo).toAbsolutePath().normalize();
						if(!willRemoveExtensionFromString)
							strArray+= InnerClassWriter.multiplyString("\t", count + 1) + "\"" + other.relativize(checking).toString().replaceAll("\\\\", "\\\\\\\\") + "\"" + postStringDefinition + "\n";
						else
						{
							String pathNoExtension = other.relativize(checking).toString().replaceAll("\\\\", "\\\\\\\\");
							int index = pathNoExtension.lastIndexOf(".");
							if(index != -1)
								pathNoExtension = pathNoExtension.substring(0, index);
							strArray+= InnerClassWriter.multiplyString("\t", count + 1) + "\"" + pathNoExtension + "\"" + postStringDefinition + "\n";
						}
					}
				}
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
			code+= InnerClassWriter.multiplyString("\t", count) + ((isLast) ? lastEnumEndBlockSymbol : enumEndBlockSymbol) + "\n";
		if(!willUseAssign && isEnumMode)
		{
			strArray+= InnerClassWriter.multiplyString("\t", count) + ((isLast) ? lastStringArrayEndBlockSymbol : stringArrayEndBlockSymbol) + "\n";
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
			//PrintWriter pw = new PrintWriter(path);
			File f = new File(path);
			
			
			
			if(packageName != null && !packageName.equals(""))
				code+= packageDeclarator + packageName + ";\n";
			if(imports != null && imports.size() != 0)
			{
				for(String currentImport : imports)
					code+= importDeclarator + currentImport + ";\n";
			}
			
			
			if(willUseClassName)
			{
				if(!customClassName.equals(""))
					code+= classDeclarator + customClassName + postClassDeclarator + "\n{\n";
				else
				{
					String s = InnerClassWriter.getCurrentDirName(pathToWatch);
					if(s.equals(""))
						s = InnerClassWriter.getCurrentDirName(getAbsolutePath(pathToWatch));
					if(classNameStartWithCapital)
						code+= classDeclarator + String.valueOf(s.charAt(0)).toUpperCase() + s.substring(1) + postClassDeclarator + "\n{\n";
					else
						code+= classDeclarator + s + postClassDeclarator + "\n{\n";
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
			
			int count = 1;
			while(paths.size() != 0)
			{
				path = paths.get(0);
				boolean hasWriteAlready = false;
				
				String currentDir = getAbsolutePath(path.toString());
				String staticCurrentDir = getAbsolutePath(Paths.get(currentDir).toString());
				currentDir = InnerClassWriter.enterNextDir(currentDir);
				
				//This part handles not creating inner classes for the path that doesn't matter
				currentDir = InnerClassWriter.subtractPath(currentDir, getAbsolutePath(pathToWatch));
				String traveled = getAbsolutePath(pathToWatch);
				if(traveled.charAt(traveled.length() - 1) != '/' && traveled.charAt(traveled.length() - 1) != '\\')
				{
					traveled+= ((traveled.contains("/") ? "/" : "\\"));
				}
				
				//Handles skip movement when there is same directory
				int bufferCount = 0;
				if(count != 1)
				{
					while(count - bufferCount != 1)
				  	{
				  		bufferCount++;
				  		currentDir = InnerClassWriter.enterNextDir(currentDir);
				  		traveled+= InnerClassWriter.getRootOfDir(currentDir) + "/";
				  	}
				}
				
				if(InnerClassWriter.countDir(currentDir) - bufferCount == 1 && Paths.get(traveled + currentDir).toString().equals(staticCurrentDir))
				{
					if(currentDir.indexOf("/") == 0 || currentDir.indexOf("\\") == 0)
						currentDir = InnerClassWriter.enterNextDir(currentDir);
				}
				
				while((InnerClassWriter.countDir(currentDir) - bufferCount) > 0 && !hasWriteAlready)
				{
					currentDir = InnerClassWriter.enterNextDir(currentDir);
					String toWrite = InnerClassWriter.getRootOfDir(currentDir);
					traveled+= toWrite +"/";
					toWrite = String.valueOf(toWrite.charAt(0)).toUpperCase() + toWrite.substring(1, toWrite.length());
					code+= InnerClassWriter.multiplyString("\t", count) + innerClassDeclarator + toWrite.replaceAll("[\\s|\\.|\\-]", "_") +  postInnerClassDeclarator + "\n" + InnerClassWriter.multiplyString("\t", count) + innerClassStartBlockSymbol + "\n";
					//System.out.println(currentDir);
					count++;
					
					List<Path> commonPaths = new ArrayList<Path>();
					String nTravel = Paths.get(traveled).toString();
					for(Path nPath : paths)
					{
						String pathChecking = getAbsolutePath(nPath.toString());
						if(pathChecking.indexOf(nTravel) != -1)
							if(pathChecking.indexOf(nTravel) == staticCurrentDir.indexOf(nTravel))
								commonPaths.add(nPath);
					}
					int lastPathChecker = 0;
					for(Path nPath : commonPaths)
					{
						String pathChecking = getAbsolutePath(nPath.toString()).substring(nTravel.length());
						if(InnerClassWriter.countDir(pathChecking) == 1)
						{
							if(getAbsolutePath(nPath.toString()).equals(staticCurrentDir))
								hasWriteAlready = true;
							paths.remove(nPath);

							//code+= pathWrite(nPath, count, (paths.size() == 0 || commonPaths.indexOf(nPath) + 1 == commonPaths.size()));
							lastPathChecker++;
							code+= pathWrite(nPath, count, (lastPathChecker == commonPaths.size()));
						}
					}
				}

				if(!hasWriteAlready)
				{
					code+= pathWrite(path, count, paths.size() == 1);
					paths.remove(path);
				}
				while(count != 1)
				{
					String pathToCheckOthers = Paths.get(InnerClassWriter.getPathUntilCount(traveled, count - 1)).toString();
					boolean willQuit = false;
					if(paths.size() != 0)
					{
						Path buffer = paths.get(0);
						Path toSwap = null;
						for(Path nPath : paths)
						{
							if(nPath.toString().indexOf(pathToCheckOthers) != -1)
							{
								toSwap = nPath;
								willQuit = true;
								break;
							}
							
						}
						if(willQuit)
						{
							//SWAP OPERATION
							int index = paths.indexOf(toSwap);
							paths.set(0, toSwap);
							paths.set(index, buffer);
							break;
						}
					}
					code+= InnerClassWriter.multiplyString("\t", count - ((isEnumMode) ? 1 : 0));
					if(count > 2)
						code+= lastInnerClassEndBlockSymbol;
					else if(paths.size() == 0)
						code+= lastInnerClassEndBlockSymbol;
					else
						code+= innerClassEndBlockSymbol;
					//code+= ((paths.size() == 0 && count == 2) ? lastInnerClassEndBlockSymbol : innerClassEndBlockSymbol) + "\n";
					code+= "\n";
					count--;
				}
				//code+= ((paths.size() > 0) ? innerClassEndBlockSymbol : lastInnerClassEndBlockSymbol) + "\n";
			}
			code+= "}";
			//pw.write(code);
			RandomAccessFile raf = new RandomAccessFile(f, "rwd");
			raf.setLength(0);
			try{Thread.sleep(1000);}
			catch(Exception e){e.printStackTrace();}
			raf.writeBytes(code);
			raf.close();
			//pw.close();
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
