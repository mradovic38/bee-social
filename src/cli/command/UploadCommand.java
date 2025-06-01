package cli.command;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;

import java.io.File;
import java.util.HashSet;

public class UploadCommand implements CLICommand {

	@Override
	public String commandName() {
		return "upload";
	}

	@Override
	public void execute(String path) {


		// uzmi hash path-a da bi znali na koji key da ga stavimo
		
		if (!path.isBlank() && checkFile(path)) {
			int key = Math.abs(path.hashCode() % ChordState.CHORD_SIZE);

			// LOCK
			AppConfig.chordState.mutex.lock(new HashSet<>(AppConfig.chordState.getAllNodeInfo().stream().map(ServentInfo::getListenerPort).toList()));

			AppConfig.timestampedStandardPrint("Upload acquired lock");
			AppConfig.chordState.putValue(key, path, AppConfig.myServentInfo.getListenerPort());

		} else {
			AppConfig.timestampedErrorPrint("Invalid arguments for put");
		}

	}

	public static boolean checkFile(String fileName) {
		try {
			File file = new File(AppConfig.ROOT_DIR + "/" + fileName);
			return file.exists() && !file.isDirectory();
		} catch (Exception e) {
			AppConfig.timestampedErrorPrint(fileName + " could not be added. Check if file is valid.");
			return false;
		}
	}

}
