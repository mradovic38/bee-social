package cli.command;


import app.AppConfig;

import java.io.File;

public class RemoveFileCommand implements CLICommand {

    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String fileName) {
        if(checkFile(fileName))
            AppConfig.chordState.deleteValue(fileName);

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
