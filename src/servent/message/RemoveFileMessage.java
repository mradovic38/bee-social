package servent.message;

public class RemoveFileMessage extends BasicMessage {

    private static final long serialVersionUID = 2266336627766084210L;

    private final String pathToRemove;

    private final int requestorPort;

    public RemoveFileMessage(int senderPort, int receiverPort, String pathToRemove, int requestorPort) {
        super(MessageType.REMOVE_FILE, senderPort, receiverPort);
        this.pathToRemove = pathToRemove;
        this.requestorPort = requestorPort;
    }

    public String getPathToRemove() {
        return pathToRemove;
    }

    public int getRequestorPort() {
        return requestorPort;
    }
}
