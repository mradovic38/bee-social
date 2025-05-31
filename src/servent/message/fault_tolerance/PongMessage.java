package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class PongMessage extends BasicMessage {

    private static final long serialVersionUID = 8558091124520315233L;

    public PongMessage(int senderPort, int receiverPort) {
        super(MessageType.PONG, senderPort, receiverPort);
    }
}
