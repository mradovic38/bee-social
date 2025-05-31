package servent.message.mutex;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class SuzukiKasamiRequestTokenMessage extends BasicMessage {

    private static final long serialVersionUID = -8558031124520235033L;

    private final int rnVal;

    public SuzukiKasamiRequestTokenMessage(int senderPort, int receiverPort, int rnVal) {
        super(MessageType.TOKEN_REQUEST, senderPort, receiverPort);
        this.rnVal = rnVal;
    }

    public int getRnVal() {
        return rnVal;
    }
}
