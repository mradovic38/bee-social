package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class PutHandler implements MessageHandler {

	private Message clientMessage;
	
	public PutHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.PUT) {
			String[] kvStorerSplit = clientMessage.getMessageText().split("#");
			int storerId = Integer.parseInt(kvStorerSplit[1]);

			String[] splitText = kvStorerSplit[0].split(":");

			if (splitText.length == 2) {
				int key = 0;
				String path = "";
				
				try {
					key = Integer.parseInt(splitText[0]);
					path = splitText[1];
					
					AppConfig.chordState.putValue(key, path, storerId);
				} catch (NumberFormatException e) {
					AppConfig.timestampedErrorPrint("Got put message with bad text: " + clientMessage.getMessageText());
				}
			} else {
				AppConfig.timestampedErrorPrint("Got put message with bad text: " + clientMessage.getMessageText());
			}
			
			
		} else {
			AppConfig.timestampedErrorPrint("Put handler got a message that is not PUT");
		}

	}

}
