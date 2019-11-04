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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import static java.nio.file.StandardWatchEventKinds.*;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Menu;
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
	public static ResourceEnumGenerator generator = null;
	public static TrayIcon g_trayIcon = null;

	private ArrayList<ArrayList<String>> paths;
	public final HashMap<WatchKey, Path> registeredKeys;
	public volatile boolean isProcessing = true;
	private EnumWriter writer;

	private static boolean ignoreNext = false;

	public static void showTrayMessage(String title, String message, boolean ignorable, boolean willIgnoreNext)
	{
		if(ResourceEnumGenerator.g_trayIcon != null && (!ignoreNext || !ignorable))
			ResourceEnumGenerator.g_trayIcon.displayMessage("ResourceEnumGenerator: " + title, message, TrayIcon.MessageType.INFO);
		
		ignoreNext = willIgnoreNext;
	}

	public void registerPath(String dir, boolean checkModify) throws IOException {
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
		if(!Files.exists(Paths.get(dir)))
			return;
		Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes atr) throws IOException {
				registerPath(dir.toString(), false);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public List<Path> getPathsListening() 
	{
		Path p;
		List<Path> dirs = new ArrayList<Path>();
		for (Map.Entry<WatchKey, Path> entries : registeredKeys.entrySet()) {
			p = entries.getValue();
			String str = p.toString();
			if (!str.equals("./") && !str.equals(".\\") && !str.equals("."))
				dirs.add(p);
		}
		return dirs;
	}

	public void pathsRestart(String initialDir) throws IOException 
	{
		Path p;
		for (Map.Entry<WatchKey, Path> entries : registeredKeys.entrySet()) {
			p = entries.getValue();
			entries.getKey().cancel();
			registeredKeys.remove(entries.getKey());
		}
		registerRecursive(initialDir, false);
		registerPath("./", true);
	}

	public void processEvents() 
	{
		while (isProcessing) {
			WatchKey key = null;
			try {
				key = service.take();
			} catch (Exception e) {
			}

			Path dir = registeredKeys.get(key);
			if (dir == null) {
				System.err.println("Key wasn't recognized");
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) 
			{
				WatchEvent.Kind<?> kind = event.kind();
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path name = ev.context();
				Path child = dir.resolve(name);

				if (kind == OVERFLOW)
					return;
				if (child.toString().contains(Paths.get(writer.path).toAbsolutePath().normalize().toString()))
					continue;
				if (kind == ENTRY_CREATE) 
				{
					if(!name.toString().contains("settings.config") && !name.toString().contains(".TMP") && !name.toString().contains("~"))
					{
						try 
						{
							if (Files.isDirectory(child, NOFOLLOW_LINKS))
								registerRecursive(child.toString(), false);
						}
						catch (IOException e) {InnerClassWriter.showError(e);}
						System.out.println("Created " + name.toString());
					}
				}
				if (kind == ENTRY_DELETE || kind == ENTRY_CREATE && !name.toString().contains("settings.config") && !name.toString().contains(".TMP") && !name.toString().contains("~"))
				{
					try {writer.scheduleUpdate(this.getPathsListening(), false);}
					catch (IOException e) {InnerClassWriter.showError(e);}
				} 
				else if (kind == ENTRY_MODIFY && name.toString().equals("settings.config")) 
				{
					try 
					{
						//if (!writer.isReadingConfig && !writer.isConfigUpdateScheduled)
						writer.scheduleUpdate(this.getPathsListening(), true);
						System.out.println("Read config scheduled");
					}
					catch (IOException e) {InnerClassWriter.showError(e);}
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
		return new Thread(new Runnable() {
			@Override
			public void run() {
				generator.processEvents();
			}
		});
	}

	public static Image createImage(String path, String description) {
		URL imageURL = ResourceEnumGenerator.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else
			return (new ImageIcon(imageURL, description)).getImage();
	}

	public static void addTray(EnumWriter writer) 
	{
		if (!SystemTray.isSupported()) 
		{
			JOptionPane.showMessageDialog(null, "SystemTray is not supported on your OS", "SystemTray not supported", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		final PopupMenu popup = new PopupMenu();
		final TrayIcon trayIcon = new TrayIcon(createImage("icon.png", "tray icon"));
		g_trayIcon = trayIcon;
		final SystemTray tray = SystemTray.getSystemTray();

		
		Menu presets = new Menu("Presets");
		MenuItem CSharpPreset = new MenuItem("C#");
		CSharpPreset.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				writer.setAsCSharp();
				try {writer.generateDefaultFormatFile();}
				catch (IOException e1) {e1.printStackTrace();}
			}
		});

		MenuItem JavaPreset = new MenuItem("Java");
		JavaPreset.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				writer.setAsJava();
				try {writer.generateDefaultFormatFile();}
				catch (IOException e1) {e1.printStackTrace();}
			}
		});


		MenuItem JSONPreset = new MenuItem("JSON");
		JSONPreset.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				writer.setAsJSON();
				try {writer.generateDefaultFormatFile();}
				catch (IOException e1) {e1.printStackTrace();}
			}
		});
		MenuItem JavascriptPreset = new MenuItem("Javascript");
		JavascriptPreset.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				writer.setAsJavascript();
				try {writer.generateDefaultFormatFile();}
				catch (IOException e1) {e1.printStackTrace();}
			}
		});

		presets.add(CSharpPreset);
		presets.add(JavaPreset);
		presets.add(JavascriptPreset);
		presets.add(JSONPreset);

		MenuItem input = new MenuItem("Set Input Path");
		MenuItem output = new MenuItem("Set Output Path");
		MenuItem relative = new MenuItem("Set Relative Path");


		MenuItem showInput = new MenuItem("Show Input Path");
		MenuItem show = new MenuItem("Show Output File");
		MenuItem openConfig = new MenuItem("Open Config File");


		MenuItem exit = new MenuItem("Exit");


		
		popup.add(presets);
		presets.add(CSharpPreset);
		presets.add(JSONPreset);
		
		popup.add(input);
		popup.add(output);
		popup.add(relative);
		popup.addSeparator();
		popup.add(showInput);
		popup.add(show);
		popup.add(openConfig);
		popup.addSeparator();
		popup.add(exit);

		input.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newPath = CrossPlatformFunctions.crossPlatformGetDir("Set input path for the program observe",
						"");
				if (newPath != null && !newPath.equals("")) 
				{
					if (writer.pathRelativeTo != null && !writer.pathRelativeTo.equals(""))
						if (!InnerClassWriter.isRootEqual(newPath, writer.pathRelativeTo)) 
						{
							int res = JOptionPane.showConfirmDialog(null, "The path's root selected ("
									+ InnerClassWriter.getRoot(newPath) + ") differs from the relative path ("
									+ InnerClassWriter.getRoot(writer.pathRelativeTo) + ")\nClear the relative path?",
									"Different Roots", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (res == JOptionPane.YES_OPTION)
								writer.pathRelativeTo = "";
							else
								return;
						}
					writer.pathToWatch = newPath;
					try {
						writer.readConfig(true);
						writer.updatePathToWatch();
					} catch (IOException e1) {
						InnerClassWriter.showError(e1);
					}
				}
			}
		});

		output.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newPath = CrossPlatformFunctions.crossPlatformSave("Set output path and filename", "");
				if (newPath != null && !newPath.equals("")) 
				{
					writer.path = EnumWriter.updateToRelative(newPath);
					try {
						writer.readConfig(true);
					} catch (IOException e1) {
						InnerClassWriter.showError(e1);
					}
				}
			}
		});

		relative.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String newPath = CrossPlatformFunctions.crossPlatformGetDir("Set relative path", "");
				if (newPath != null && !newPath.equals("")) {
					if (!InnerClassWriter.isRootEqual(newPath, writer.pathToWatch)) {
						JOptionPane.showMessageDialog(null, "The path's root selected ("
								+ InnerClassWriter.getRoot(newPath) + ") differs from the watching path ("
								+ InnerClassWriter.getRoot(writer.pathToWatch) + ")\nRelative path will be ignored",
								"Different Roots", JOptionPane.ERROR_MESSAGE);
						return;
					}
					writer.pathRelativeTo = newPath;
					try {
						writer.readConfig(true);
					} catch (IOException e1) {
						InnerClassWriter.showError(e1);
					}
				}
			}
		});

		showInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CrossPlatformFunctions.openCurrentSystemExplorer(
						Paths.get(writer.pathToWatch).toAbsolutePath().toString(), false, false);
			}
		});
		show.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CrossPlatformFunctions.openCurrentSystemExplorer(Paths.get(writer.path).toAbsolutePath().toString(),
						false, true);
			}
		});

		openConfig.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CrossPlatformFunctions.openCurrentSystemExplorer("settings.config", false, false);
			}
		});

		trayIcon.setPopupMenu(popup);
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent a) {
				System.exit(0);
			}
		});

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			InnerClassWriter.showError(e);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		EnumWriter writer = new EnumWriter();
		writer.scheduleUpdate(null, true);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				addTray(writer);
			}
		});
		generator = new ResourceEnumGenerator(writer);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (listeningToNewPaths) {
					try {
						runningThread = createThread(generator);
					} catch (IOException e) {
						InnerClassWriter.showError(e);
					}
					if (runningThread == null) {
						System.err.println("Could not start program: Invalid PATH_TO_WATCH: " + writer.pathToWatch);
						return;
					}
					runningThread.start();
					try {
						runningThread.join();
					} catch (InterruptedException e) {
						InnerClassWriter.showError(e);
					}
					if (listeningToNewPaths) {
						try {
							generator = new ResourceEnumGenerator(writer);
						} catch (IOException e) {
							InnerClassWriter.showError(e);
						}
					}
				}
			}
		});
		t.start();

		Scanner commands = new Scanner(System.in);

		String command = commands.next();
		while (!command.equals("quit")) {
			command = commands.next();
		}
		listeningToNewPaths = false;
		commands.close();
		generator.isProcessing = false;
		generator.service.close();
		while (writer.isWriting) {
			Thread.sleep(10);
		}
		writer.isRunning = false;
		t.join();

	}

}
