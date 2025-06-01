package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class PongMessage extends BasicMessage {

    private static final long serialVersionUID = 8558091122220315233L;

    private final int markThisPort;

    public PongMessage(int senderPort, int receiverPort) {
        super(MessageType.PONG, senderPort, receiverPort);
        this.markThisPort = senderPort;
    }

    public PongMessage(int senderPort, int receiverPort, int markThisPort) {
        super(MessageType.PONG, senderPort, receiverPort);
        this.markThisPort = markThisPort;
    }

    public int getMarkThisPort() {
        return markThisPort;
    }
}
