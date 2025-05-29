package servent.message.mutex;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class SuzukiKasamiRequestTokenMessage extends BasicMessage {

    private static final long serialVersionUID = -8558031124520235033L;

    public SuzukiKasamiRequestTokenMessage(int senderPort, int receiverPort, String text) {
        super(MessageType.TOKEN_REQUEST, senderPort, receiverPort, text);
    }
}
