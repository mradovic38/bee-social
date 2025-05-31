package servent.handler;

import java.util.ArrayList;
import java.util.List;

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
			// dodavanje novog cvora
			if (clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
				ServentInfo newNodInfo = new ServentInfo("localhost", clientMessage.getSenderPort());

				List<ServentInfo> newNodes = new ArrayList<>();
				newNodes.add(newNodInfo);
				AppConfig.chordState.addNodes(newNodes);

				// TODO: AZURIRATI U PORUCI RN I TO - RN KOJI SE SALJE = MAX(RN, RN KOJI JE PRIMLJEN)
				// TODO: UMESTO OVOG BESKORISNOG SRANJA AZURIRATI FAJLOVE I PROSLEDITI KROZ UPDATEMESSAGE
				String newMessageText = "";
				if (clientMessage.getMessageText().equals("")) {
					newMessageText = String.valueOf(AppConfig.myServentInfo.getListenerPort());
				} else {
					newMessageText = clientMessage.getMessageText() + "," + AppConfig.myServentInfo.getListenerPort();
				}
				Message nextUpdate = new UpdateMessage(clientMessage.getSenderPort(), AppConfig.chordState.getNextNodePort(),
						newMessageText);
				MessageUtil.sendMessage(nextUpdate);
			}
			// do mene stigla poruka, znaci sve smo ih obavestili da dolazimo => unlock
			else {
				// TODO: SACUVAJ SLIKE KOJE SU MOJE
				// TODO: Splitovati poruku na listu portova i rn-ove
				// TODO: Azurirati jebene rn-ove
				String messageText = clientMessage.getMessageText();
				String[] ports = messageText.split(",");
				
				List<ServentInfo> allNodes = new ArrayList<>();
				for (String port : ports) {
					allNodes.add(new ServentInfo("localhost", Integer.parseInt(port)));
				}
				AppConfig.chordState.addNodes(allNodes);
				AppConfig.chordState.mutex.unlock();
			}
		} else {
			AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
		}
	}

}
