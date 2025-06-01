package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class SusPingMessage extends BasicMessage {

    private static final long serialVersionUID = 3558022122220315233L;

    private final int initiatorPort;

    public SusPingMessage(int senderPort, int receiverPort, int initiatorPort) {
        super(MessageType.SUS_PING, senderPort, receiverPort);
        this.initiatorPort = initiatorPort;
    }

    public int getInitiatorPort() {
        return initiatorPort;
    }
}
