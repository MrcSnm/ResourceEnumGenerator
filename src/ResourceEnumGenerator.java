import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.attribute.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class ResourceEnumGenerator 
{
	private final WatchService service;
	private ArrayList<ArrayList<String>> paths;
	public final HashMap<WatchKey, Path> registeredKeys;
	public volatile boolean isProcessing = true;
	private EnumWriter writer;
	
	public void registerPath(String dir) throws IOException
	{
		Path directory = Paths.get(dir);
		WatchKey key = directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		registeredKeys.put(key, directory);
	}
	
	public void registerRecursive(String dir) throws IOException
	{
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes atr) throws IOException
			{
				registerPath(dir.toString());
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	public List<Path> getPathsListening()
	{
		Path p;
		List<Path> dirs = new ArrayList<Path>();
		for(Map.Entry<WatchKey, Path> entries: registeredKeys.entrySet())
		{
			p = entries.getValue();
			dirs.add(p);
		}
		return dirs;
	}
	
	public void processEvents()
	{
		while(isProcessing)
		{
			WatchKey key;
			try{key = service.take();}
			catch(Exception e){return;}
			
			Path dir = registeredKeys.get(key);
			if(dir == null)
			{
				System.err.println("Key wasn't recognized");
				return;
			}
			
			for(WatchEvent<?> event : key.pollEvents())
			{
				WatchEvent.Kind<?> kind = event.kind();
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>)event;
				Path name = ev.context();
				Path child = dir.resolve(name);
				
				System.out.println(kind.toString() + ": " + name + " " + child);
				
				if(kind == OVERFLOW)
					return;
				
				
				if(kind == ENTRY_CREATE)
				{
					try
					{
						if(Files.isDirectory(child, NOFOLLOW_LINKS))
							registerRecursive(child.toString());
					}
					catch(IOException e){}
				}
				if(kind == ENTRY_DELETE || kind == ENTRY_CREATE)
					writer.scheduleUpdate(this.getPathsListening());
			}
			if(!key.reset())
			{
				System.out.println("The key " + dir + " became invalid.");
				registeredKeys.remove(key);
				if(registeredKeys.isEmpty())
					return;
			}
		}
	}
	
	ResourceEnumGenerator(EnumWriter writer) throws IOException
	{
		this.writer = writer;
		String path = writer.pathToWatch;
		service = FileSystems.getDefault().newWatchService();
		paths = new ArrayList<ArrayList<String>>();
		registeredKeys = new HashMap<WatchKey, Path>();
		System.out.println("Directory " + path + " is still being scanned");
		registerRecursive(path);
		System.out.println("Listening for changes on the directory " + path);
		
		System.out.println("On start updating");
		writer.scheduleUpdate(this.getPathsListening());
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		
		EnumWriter writer = new EnumWriter();
		ResourceEnumGenerator generator = new ResourceEnumGenerator(writer);
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				generator.processEvents();
			}
		});
		t.start();
		
		Scanner commands= new Scanner(System.in);
		
		String command = commands.next();
		while(!command.contains("quit"))
		{
			command = commands.next();
		}
		
		generator.service.close();
		generator.isProcessing = false;
		while(writer.isWriting)
		{
			Thread.sleep(10);
		}
		writer.isRunning = false;
		t.join();
		
		
		

	}

}
