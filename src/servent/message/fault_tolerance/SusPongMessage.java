package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class SusPongMessage extends BasicMessage {

    private static final long serialVersionUID = 2558022122220315233L;

    private final int initiatorPort;

    public SusPongMessage(int senderPort, int receiverPort, int checkThisPort) {
        super(MessageType.SUS_PONG, senderPort, receiverPort);
        this.initiatorPort = checkThisPort;
    }

    public int getInitiatorPort() {
        return initiatorPort;
    }
}
