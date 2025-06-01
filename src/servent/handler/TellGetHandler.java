package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;

public class TellGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public TellGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TELL_GET) {

			TellGetMessage tellGetMessage =  (TellGetMessage) clientMessage;

			// unlock
			AppConfig.chordState.mutex.unlock();

			if(tellGetMessage.isPrivate()){
				AppConfig.timestampedStandardPrint("You do not have access to this site's images </3");
			}
			else {
				AppConfig.saveImages(tellGetMessage.getValues());
			}

		} else {
			AppConfig.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
