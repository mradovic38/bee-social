package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class PingMessage extends BasicMessage {

    private static final long serialVersionUID = 8558031124520315233L;

    public PingMessage(int senderPort, int receiverPort) {
        super(MessageType.PING, senderPort, receiverPort);
    }
}
