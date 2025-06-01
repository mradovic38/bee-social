package servent.message.fault_tolerance;

import app.ImageEntry;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class BackupMessage extends BasicMessage {

    private static final long serialVersionUID = 2266330027126084210L;

    private final ImageEntry imageEntry;


    public BackupMessage(int senderPort, int receiverPort, ImageEntry imageEntry) {
        super(MessageType.BACKUP, senderPort, receiverPort);
        this.imageEntry = imageEntry;
    }

    public ImageEntry getImageEntry() {
        return imageEntry;
    }
}
