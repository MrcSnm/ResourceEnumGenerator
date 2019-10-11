import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.attribute.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;


import static java.nio.file.StandardWatchEventKinds.*;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static java.nio.file.LinkOption.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class ResourceEnumGenerator
{
	public final WatchService service;
	public static boolean scheduledToUpdatePath = false;
	public static boolean listeningToNewPaths = true;
	public static Thread runningThread = null;
	public static ResourceEnumGenerator generator= null;

	private ArrayList<ArrayList<String>> paths;
	public final HashMap<WatchKey, Path> registeredKeys;
	public volatile boolean isProcessing = true;
	private EnumWriter writer;

	public void registerPath(String dir, boolean checkModify) throws IOException 
	{
		Path directory = Paths.get(dir);
		WatchKey key;
		if (checkModify)
			key = directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		else
			key = directory.register(service, ENTRY_CREATE, ENTRY_DELETE);
		registeredKeys.put(key, directory);
	}

	public void registerRecursive(String dir, final boolean checkModify) throws IOException 
	{
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() 
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes atr) throws IOException 
			{
				registerPath(dir.toString(), false);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public List<Path> getPathsListening() 
	{
		Path p;
		List<Path> dirs = new ArrayList<Path>();
		for (Map.Entry<WatchKey, Path> entries : registeredKeys.entrySet()) 
		{
			p = entries.getValue();
			String str = p.toString();
			if (!str.equals("./") && !str.equals(".\\") && !str.equals("."))
				dirs.add(p);
		}
		return dirs;
	}

	public void processEvents() 
	{
		while (isProcessing) 
		{
			WatchKey key;
			try {key = service.take();}
			catch (Exception e) {return;}

			Path dir = registeredKeys.get(key);
			if (dir == null) 
			{
				System.err.println("Key wasn't recognized");
				return;
			}
			try {Thread.sleep(100);} catch (InterruptedException e) {}

			for (WatchEvent<?> event : key.pollEvents()) 
			{
				WatchEvent.Kind<?> kind = event.kind();
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path name = ev.context();
				Path child = dir.resolve(name);

				if (kind == OVERFLOW)
					return;

				if (kind == ENTRY_CREATE) 
				{
					try 
					{
						if (Files.isDirectory(child, NOFOLLOW_LINKS))
							registerRecursive(child.toString(), false);
					} 
					catch (IOException e) {}
				}
				if (kind == ENTRY_DELETE || kind == ENTRY_CREATE) 
				{
					try {writer.scheduleUpdate(this.getPathsListening(), false);}
					catch (IOException e) {}
				}
				else if (kind == ENTRY_MODIFY && name.toString().equals("settings.config")) 
				{
					try {writer.scheduleUpdate(this.getPathsListening(), true);}
					catch (IOException e) {}
				}
			}
			if (!key.reset()) {
				System.out.println("The key " + dir + " became invalid.");
				registeredKeys.remove(key);
				if (registeredKeys.isEmpty())
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
		registerRecursive(path, false);
		registerPath(Paths.get("./").toString(), true);
		System.out.println("Listening for changes on the directory " + path);

		System.out.println("On start updating");
		writer.scheduleUpdate(this.getPathsListening(), true);
	}

	public static Thread createThread(ResourceEnumGenerator generator) throws IOException 
	{
		return new Thread(new Runnable() 
		{
			@Override
			public void run() {
				generator.processEvents();
			}
		});
	}
	
	public static Image createImage(String path, String description) 
	{
        URL imageURL = ResourceEnumGenerator.class.getResource(path);
         
        if (imageURL == null) 
        {
            System.err.println("Resource not found: " + path);
            return null;
        }
        else 
            return (new ImageIcon(imageURL, description)).getImage();
    }

	public static void addTray(EnumWriter writer)
	{
		if (!SystemTray.isSupported()) 
		{
            System.out.println("SystemTray is not supported on your OS");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(createImage("icon.png", "tray icon"));
        final SystemTray tray = SystemTray.getSystemTray();
       
        MenuItem input  = new MenuItem("Set Input Path");
		MenuItem output = new MenuItem("Set Output Path");
		MenuItem show = new MenuItem("Show Output File");
		MenuItem exit   = new MenuItem("Exit");
		popup.add(input);
		popup.add(output);
		popup.add(show);
		popup.addSeparator();
		popup.add(exit);
		
		input.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String newPath = CrossPlatformFunctions.crossPlatformSave("Set input path for the program observe", "");
				if(newPath != null && !newPath.equals(""))
				{
					writer.pathToWatch = newPath;
					try {writer.updatePathToWatch();}
					catch (IOException e1) {e1.printStackTrace();}
				}
			}
		});

		output.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String newPath = CrossPlatformFunctions.crossPlatformSave("Set output path and filename", "");
				if(newPath != null && !newPath.equals(""))
				{
					writer.path = newPath;
					try {writer.readConfig(true);}
					catch (IOException e1) {e1.printStackTrace();}
				}
			}
		});

		show.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CrossPlatformFunctions.openCurrentSystemExplorer(Paths.get(writer.path).toAbsolutePath().toString(), false);
			}
		});
        trayIcon.setPopupMenu(popup);
        exit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent a) 
			{
				System.exit(0);
			}
		});
       
        try {tray.add(trayIcon);} 
        catch (AWTException e) {System.out.println("Could not add tray icon.");}
	}

	public static void main(String[] args) throws IOException, InterruptedException 
	{
		EnumWriter writer = new EnumWriter();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				addTray(writer);
			}
		});
		generator = new ResourceEnumGenerator(writer);
		Thread t = new Thread(new Runnable() 
		{
			@Override
			public void run() 
			{
				while (listeningToNewPaths) 
				{
					try 
					{
						writer.readConfig();
						runningThread = createThread(generator);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					if (runningThread == null) 
					{
						System.err.println("Could not start program: Invalid PATH_TO_WATCH: " + writer.pathToWatch);
						return;
					}
					runningThread.start();
					try {runningThread.join();} 
					catch (InterruptedException e) {e.printStackTrace();}
					if(listeningToNewPaths)
					{
						try {generator = new ResourceEnumGenerator(writer);} 
						catch (IOException e) {e.printStackTrace();}
					}
				}
			}
		});
		t.start();
		
		Scanner commands= new Scanner(System.in);
		
		String command = commands.next();
		while(!command.equals("quit"))
		{
			command = commands.next();
		}
		listeningToNewPaths = false;
		commands.close();
		generator.isProcessing = false;
		generator.service.close();
		while(writer.isWriting)
		{
			Thread.sleep(10);
		}
		writer.isRunning = false;
		t.join();

	}

}
