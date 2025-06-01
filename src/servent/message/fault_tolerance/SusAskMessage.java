package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class SusAskMessage extends BasicMessage {

    private static final long serialVersionUID = 8558022122220315233L;

    private final int checkThisPort;

    public SusAskMessage(int senderPort, int receiverPort, int checkThisPort) {
        super(MessageType.SUS_ASK, senderPort, receiverPort);
        this.checkThisPort = checkThisPort;
    }

    public int getCheckThisPort() {
        return checkThisPort;
    }
}
