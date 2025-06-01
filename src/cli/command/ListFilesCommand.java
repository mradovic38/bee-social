package cli.command;

import app.AppConfig;
import app.ServentInfo;
import servent.message.AskGetMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.util.stream.Collectors;

public class ListFilesCommand implements CLICommand {

	@Override
	public String commandName() {
		return "list_files";
	}

	@Override
	public void execute(String args) {
		try {
			String[] data = args.split(":");

			int port = Integer.parseInt(data[1]);

			AppConfig.chordState.mutex.lock(AppConfig.chordState.getAllNodeInfo().stream().map(ServentInfo::getListenerPort).collect(Collectors.toSet()));

			// trazili smo nase, vrati nase ne moras da saljes poruku
			if(port == AppConfig.myServentInfo.getListenerPort()){

				AppConfig.chordState.mutex.unlock();

				AppConfig.saveImages(AppConfig.chordState.getValueMap());

				return;
			}

			AppConfig.timestampedStandardPrint("Please wait...");

			// salji poruku cvoru cije slike trazis
			Message askGet = new AskGetMessage(AppConfig.myServentInfo.getListenerPort(), port);
			MessageUtil.sendMessage(askGet);



		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Invalid argument for upload: " + args + ". Should be port, which is an int.");
		}
	}

}
