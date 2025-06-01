package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.RemoveFileFromBackupMessage;

public class RemoveFileFromBackupHandler implements MessageHandler {

    private Message clientMessage;

    public RemoveFileFromBackupHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.REMOVE_FROM_BACKUP) {
            AppConfig.timestampedStandardPrint("Remove file from backup handler got: " + clientMessage.getMessageType());
            return;
        }

        RemoveFileFromBackupMessage rffbuMsg =  (RemoveFileFromBackupMessage) clientMessage;

        String deletePath = rffbuMsg.getPathToRemove();
        int key = rffbuMsg.getKeyToRemove();
        int buddyPort =  rffbuMsg.getBuddyPort();

        AppConfig.chordState.removeFromBackup(key, deletePath, buddyPort);

    }

}
