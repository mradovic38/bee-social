package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;
import servent.message.util.MessageUtil;

public class AskGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public AskGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.ASK_GET) {

			Message tellGet;

			// privatan i nije ti follower, ne salji
			if(!AppConfig.chordState.isPublic() && AppConfig.chordState.followers.contains(clientMessage.getSenderPort())){
				tellGet = new TellGetMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort());
			}
			// public => salji slike
			else{
				tellGet = new TellGetMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(), AppConfig.chordState.getValueMap());
			}
			MessageUtil.sendMessage(tellGet);

		} else {
			AppConfig.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
		}

	}

}