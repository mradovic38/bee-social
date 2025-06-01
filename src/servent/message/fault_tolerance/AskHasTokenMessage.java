package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class AskHasTokenMessage extends BasicMessage {

    private static final long serialVersionUID = 8559931124440315233L;

    public AskHasTokenMessage(int senderPort, int receiverPort) {
        super(MessageType.PING, senderPort, receiverPort);
    }
}
