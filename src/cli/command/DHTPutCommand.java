package cli.command;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;

import java.io.File;
import java.util.HashSet;

public class DHTPutCommand implements CLICommand {

	@Override
	public String commandName() {
		return "dht_put";
	}

	@Override
	public void execute(String args) {
		System.out.println("Sada cu da odradim dht_put");
		String[] splitArgs = args.split(" ");
		
		if (splitArgs.length == 2) {
			int key = 0;
			int value = 0;
			try {
				key = Integer.parseInt(splitArgs[0]);
				value = Integer.parseInt(splitArgs[1]);
				
				if (key < 0 || key >= ChordState.CHORD_SIZE) {
					throw new NumberFormatException();
				}
				if (value < 0) {
					throw new NumberFormatException();
				}

				// LOCK
				AppConfig.chordState.mutex.lock(new HashSet<>(AppConfig.chordState.getAllNodeInfo().stream().map(ServentInfo::getListenerPort).toList()));
				
				AppConfig.chordState.putValue(key, value, AppConfig.myServentInfo.getChordId());
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Invalid key and value pair. Both should be ints. 0 <= key <= " + ChordState.CHORD_SIZE
						+ ". 0 <= value.");
			}
		} else {
			AppConfig.timestampedErrorPrint("Invalid arguments for put");
		}

	}

	public static boolean checkFile(String fileName) {
		try {
			File file = new File(AppConfig.rootDir + "/" + fileName);
			return file.exists() && !file.isDirectory();
		} catch (Exception e) {
			AppConfig.timestampedErrorPrint(fileName + " could not be added. Check if file is valid.");
			return false;
		}
	}

}
