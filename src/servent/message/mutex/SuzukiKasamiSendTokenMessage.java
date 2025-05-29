package servent.message.mutex;

import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class SuzukiKasamiSendTokenMessage extends BasicMessage {

    private static final long serialVersionUID = -2558031124520335033L;
    private final SuzukiKasamiToken token;

    public SuzukiKasamiSendTokenMessage(int senderPort, int receiverPort, String text, SuzukiKasamiToken token) {
        super(MessageType.TOKEN_SEND, senderPort, receiverPort, text);
        this.token = token;
    }

    public SuzukiKasamiToken getToken() {
        return token;
    }
}
