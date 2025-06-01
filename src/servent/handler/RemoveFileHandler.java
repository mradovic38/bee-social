package servent.handler;

import app.AppConfig;
import app.ChordState;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.RemoveFileMessage;

public class RemoveFileHandler implements MessageHandler {

    private Message clientMessage;

    public RemoveFileHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.REMOVE_FILE) {
            AppConfig.timestampedStandardPrint("Remove file handler got: " + clientMessage.getMessageType());
            return;
        }

        RemoveFileMessage rfMsg =  (RemoveFileMessage) clientMessage;

        String value = rfMsg.getPathToRemove();
        int requestorPort= rfMsg.getRequestorPort();

        AppConfig.chordState.deleteValue(value, requestorPort);



    }

}
