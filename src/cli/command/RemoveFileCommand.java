package cli.command;


import app.AppConfig;
import app.ServentInfo;

import java.io.File;
import java.util.stream.Collectors;

public class RemoveFileCommand implements CLICommand {

    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String fileName) {
        // lock
        AppConfig.chordState.mutex.lock(AppConfig.chordState.getAllNodeInfo().stream().map(ServentInfo::getListenerPort).collect(Collectors.toSet()));

        // delete
        AppConfig.chordState.deleteValue(fileName, AppConfig.myServentInfo.getListenerPort());

    }


}
