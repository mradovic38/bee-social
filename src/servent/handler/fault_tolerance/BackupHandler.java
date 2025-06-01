package servent.handler.fault_tolerance;

import app.AppConfig;
import app.ChordState;
import app.ImageEntry;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.BackupMessage;
import servent.message.fault_tolerance.RemoveFileFromBackupMessage;

public class BackupHandler implements MessageHandler {

    private Message clientMessage;

    public BackupHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.BACKUP) {
            AppConfig.timestampedStandardPrint("Backup handler got: " + clientMessage.getMessageType());
            return;
        }

        BackupMessage buMsg =  (BackupMessage) clientMessage;
        ImageEntry imageEntry = buMsg.getImageEntry();


        AppConfig.chordState.putIntoBuddyMap(imageEntry, buMsg.getSenderPort());
    }

}
