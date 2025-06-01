package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class PingMessage extends BasicMessage {

    private static final long serialVersionUID = 8558031124440315233L;

    public PingMessage(int senderPort, int receiverPort) {
        super(MessageType.PING, senderPort, receiverPort);
    }
}
