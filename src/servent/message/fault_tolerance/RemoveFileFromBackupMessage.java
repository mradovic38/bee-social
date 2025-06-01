package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class RemoveFileFromBackupMessage extends BasicMessage {

    private static final long serialVersionUID = 2266336627126084210L;

    private final String pathToRemove;

    private final int keyToRemove;

    private final int buddyPort;

    public RemoveFileFromBackupMessage(int senderPort, int receiverPort, int keyToRemove, String pathToRemove, int buddyPort) {
        super(MessageType.REMOVE_FILE, senderPort, receiverPort);
        this.pathToRemove = pathToRemove;
        this.keyToRemove = keyToRemove;
        this.buddyPort = buddyPort;
    }

    public String getPathToRemove() {
        return pathToRemove;
    }

    public int getKeyToRemove() {
        return keyToRemove;
    }

    public int getBuddyPort() {
        return buddyPort;
    }
}
