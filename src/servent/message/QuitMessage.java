package servent.message;

import mutex.suzuki_kasami.SuzukiKasamiToken;

public class QuitMessage extends BasicMessage{
    private static final long serialVersionUID = 5163032219888734276L;

    private final SuzukiKasamiToken token;

    public QuitMessage(int senderPort, int receiverPort, String text, SuzukiKasamiToken token) {
        super(MessageType.QUIT, senderPort, receiverPort, text);
        this.token = token;
    }

    public SuzukiKasamiToken getToken() {
        return token;
    }
}
