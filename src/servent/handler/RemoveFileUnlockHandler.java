package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.RemoveFileMessage;
import servent.message.RemoveFileUnlockMessage;

public class RemoveFileUnlockHandler implements MessageHandler {

    private Message clientMessage;

    public RemoveFileUnlockHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.REMOVE_FILE) {
            AppConfig.timestampedStandardPrint("Remove file unlock handler got: " + clientMessage.getMessageType());
            return;
        }

        RemoveFileUnlockMessage rfuMsg =  (RemoveFileUnlockMessage) clientMessage;

        // uspesno uklonjen
        if(rfuMsg.isDidRemove()){
            AppConfig.timestampedStandardPrint("Image on path: " + rfuMsg.getPathToRemove() + " successfully removed!");
        }
        // nisi uspeo da ga izbrises - ne postoji
        else{
            AppConfig.timestampedStandardPrint("Image on path: " + rfuMsg.getPathToRemove() + " does not exist :O");
        }

        AppConfig.chordState.mutex.unlock();



    }

}
