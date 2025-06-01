package servent.message;

public class RemoveFileUnlockMessage extends BasicMessage {

    private static final long serialVersionUID = 2266333621366084210L;

    private final String pathToRemove;

    private final boolean didRemove;

    public RemoveFileUnlockMessage(int senderPort, int receiverPort, String pathToRemove, boolean didRemove) {
        super(MessageType.REMOVE_FILE_UNLOCK, senderPort, receiverPort);
        this.pathToRemove = pathToRemove;
        this.didRemove = didRemove;
    }

    public String getPathToRemove() {
        return pathToRemove;
    }

    public boolean isDidRemove() {
        return didRemove;
    }
}
