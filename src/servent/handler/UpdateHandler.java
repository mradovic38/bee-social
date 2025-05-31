package servent.handler;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private Message clientMessage;
	
	public UpdateHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.UPDATE) {
			UpdateMessage updateMessage = (UpdateMessage) clientMessage;

			// dodavanje novog cvora
			if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				ServentInfo newNodInfo = new ServentInfo("localhost", clientMessage.getSenderPort());

				List<ServentInfo> newNodes = new ArrayList<>();
				newNodes.add(newNodInfo);
				AppConfig.chordState.addNodes(newNodes);

				// dodaj mene kao port
				List<Integer> newPorts = new ArrayList<>(updateMessage.getPorts());
				newPorts.add(AppConfig.myServentInfo.getListenerPort());

				// azuriraj rn-ove RN KOJI SE SALJE = MAX(RN, RN KOJI JE PRIMLJEN)
				List<Integer> rns = updateMessage.getRns();
				List<Integer> newRns = new ArrayList<>();
				for(int i = 0; i < rns.size(); i++) {
					int rn = AppConfig.chordState.mutex.RN.get(i);
					int rnReceived = rns.get(i);
					newRns.add(Math.max(rn, rnReceived));
				}

				// Azuriraj fajlove TODO: izmeniti svuda da valuemap ima image
				Map<Integer, Integer> updatedFiles =  AppConfig.chordState.getValueMap();

				for (Map.Entry<Integer, Integer> entry : updateMessage.getFiles().entrySet()){
					if (updatedFiles.containsKey(entry.getKey())) {
						updatedFiles.put(entry.getKey(), entry.getValue());
					} else {
						updatedFiles.put(entry.getKey(), entry.getValue());
					}
				}

				// prosledi update message
				Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(), AppConfig.chordState.getNextNodePort(),
						 updatedFiles, newPorts, newRns);

				MessageUtil.sendMessage(nextUpdate);
			}
			// do mene stigla poruka, znaci sve smo ih obavestili da dolazimo => unlock
			else {

				// napravi servent info-e iz portova i pozovi addNodes
				List<ServentInfo> allNodes = new ArrayList<>();
				for (Integer port : updateMessage.getPorts()) {
					allNodes.add(new ServentInfo("localhost", port));
				}
				AppConfig.chordState.addNodes(allNodes);

				// azuriraj rn-ove, opet ista formula kao gore
				List<Integer> rns = updateMessage.getRns();
				for (int i = 0; i < rns.size(); i++) {
					int rn = AppConfig.chordState.mutex.RN.get(i);
					int rnReceived = rns.get(i);
					AppConfig.chordState.mutex.RN.set(i, Math.max(rn, rnReceived));
				}

				// sacuvaj slike koje su moje, TODO: izmeniti svuda da valuemap ima image
				for (Map.Entry<Integer, Integer> entry : updateMessage.getFiles().entrySet()) {
					if(AppConfig.chordState.isKeyMine(entry.getKey()))
						AppConfig.chordState.getValueMap().put(entry.getKey(), entry.getValue());

				}

				AppConfig.chordState.mutex.unlock();
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
