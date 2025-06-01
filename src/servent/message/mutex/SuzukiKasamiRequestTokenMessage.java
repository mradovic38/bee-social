package servent.message.mutex;

import app.AppConfig;
import app.ChordState;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class SuzukiKasamiRequestTokenMessage extends BasicMessage {

    private static final long serialVersionUID = -8558031124520235033L;

    private final int rnVal;


    public SuzukiKasamiRequestTokenMessage(int senderPort, int receiverPort, int rnVal) {
        super(MessageType.TOKEN_REQUEST, senderPort, receiverPort);
        this.rnVal = rnVal;

    }

    public SuzukiKasamiRequestTokenMessage(int senderPort, int receiverPort, int rnVal, boolean isNew) {
        super(MessageType.TOKEN_REQUEST, senderPort, receiverPort);
        this.rnVal = rnVal;
        setNextReceiver(AppConfig.chordState.getNextNodeForKey(ChordState.chordHash(getReceiverPort())));
    }

    public int getRnVal() {
        return rnVal;
    }
}
