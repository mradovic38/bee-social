package app;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class contains all the global application configuration stuff.
 * @author bmilojkovic
 *
 */
public class AppConfig {

	/**
	 * Convenience access for this servent's information
	 */
	public static ServentInfo myServentInfo;
	
	/**
	 * Print a message to stdout with a timestamp
	 * @param message message to print
	 */
	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.out.println(timeFormat.format(now) + " - " + message);
	}
	
	/**
	 * Print a message to stderr with a timestamp
	 * @param message message to print
	 */
	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		
		System.err.println(timeFormat.format(now) + " - " + message);
	}

	public static AtomicBoolean didQuit = new AtomicBoolean(false);
	
	public static boolean INITIALIZED = false;
	public static int BOOTSTRAP_PORT;
	public static int SERVENT_COUNT;

	public static String ROOT_DIR;
	public static String CACHE_DIR;

	public static int STRONG_LIMIT;
	public static int WEAK_LIMIT;
	
	public static ChordState chordState;

	/**
	 * Reads a config file. Should be called once at start of app.
	 * The config file should be of the following format:
	 * <br/>
	 * <code><br/>
	 * servent_count=3 			- number of servents in the system <br/>
	 * chord_size=64			- maximum value for Chord keys <br/>
	 * bs.port=2000				- bootstrap server listener port <br/>
	 * servent0.port=1100 		- listener ports for each servent <br/>
	 * servent1.port=1200 <br/>
	 * servent2.port=1300 <br/>
	 * 
	 * </code>
	 * <br/>
	 * So in this case, we would have three servents, listening on ports:
	 * 1100, 1200, and 1300. A bootstrap server listening on port 2000, and Chord system with
	 * max 64 keys and 64 nodes.<br/>
	 * 
	 * @param configName name of configuration file
	 * @param serventId id of the servent, as used in the configuration file
	 */
	public static void readConfig(String configName, int serventId){
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
			
		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}
		
		try {
			BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bs.port"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading bootstrap_port. Exiting...");
			System.exit(0);
		}
		
		try {
			SERVENT_COUNT = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}
		
		try {
			int chordSize = Integer.parseInt(properties.getProperty("chord_size"));
			
			ChordState.CHORD_SIZE = chordSize;
			chordState = new ChordState();
			
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading chord_size. Must be a number that is a power of 2. Exiting...");
			System.exit(0);
		}

		try {
			ROOT_DIR = properties.getProperty("root");
		} catch (NullPointerException e) {
			timestampedErrorPrint("Problem reading root dir. Exiting...");
			System.exit(0);
		}
		try{
			CACHE_DIR = properties.getProperty("cache");
			clearDirectory(CACHE_DIR);
		}
		catch (NullPointerException e){
			CACHE_DIR = null;
		}

		try {
			STRONG_LIMIT = Integer.parseInt(properties.getProperty("strong_limit"));
			WEAK_LIMIT = Integer.parseInt(properties.getProperty("weak_limit"));
		} catch (NullPointerException e) {
			timestampedErrorPrint("Problem reading strong_limit and hard_limit. Exiting...");
			System.exit(0);
		}


		String portProperty = "servent"+serventId+".port";
		
		int serventPort = -1;
		
		try {
			serventPort = Integer.parseInt(properties.getProperty(portProperty));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
			System.exit(0);
		}
		
		myServentInfo = new ServentInfo("localhost", serventPort);






	}

	public static List<String> saveImages(Map<Integer, Map<String, ImageEntry>> images){
		List<String> collectedPaths = new ArrayList<>();
		try {

			for (Map.Entry<Integer, Map<String, ImageEntry>> entry : images.entrySet()) {
				for (Map.Entry<String, ImageEntry> imageEntryEntry : entry.getValue().entrySet()) {
					ImageEntry imageEntry = imageEntryEntry.getValue();

					if(CACHE_DIR != null) {
						File outputFile = new File(CACHE_DIR + "/" + imageEntry.getPath() + "-" + imageEntry.getStorerId() + ".jpg");

						boolean success = ImageIO.write(imageEntry.getBufferedImage(), "jpg", outputFile);

						if (!success) {
							AppConfig.timestampedErrorPrint("Error writing image to: " + outputFile);
						}
					}

					collectedPaths.add(imageEntryEntry.getKey());
				}
			}
			AppConfig.timestampedStandardPrint("Retrieved the following images: " + collectedPaths);
		}
		catch (IOException e) {
			timestampedErrorPrint("Problem saving images");
		}

		return collectedPaths;

	}


	private static boolean clearDirectory(String path) {

		File directory = new File(path);

		if (!directory.exists() || !directory.isDirectory()) {
			System.err.println("Invalid directory: " + directory.getAbsolutePath());
			return false;
		}

		File[] files = directory.listFiles();
		if (files == null) {
			System.err.println("Failed to list files in: " + directory.getAbsolutePath());
			return false;
		}

		boolean success = true;
		for (File file : files) {
			if (file.isDirectory()) {
				success &= clearDirectory(file.getPath()); // recursively clear subdirectory
			}
			success &= file.delete(); // delete file or now-empty directory
		}

		return success;
	}
	
}
