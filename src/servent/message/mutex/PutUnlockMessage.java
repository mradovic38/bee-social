package servent.message.mutex;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class PutUnlockMessage extends BasicMessage {

    // storer Id je u tekstu, to je kao finalni receiver
    public PutUnlockMessage(int senderPort, int receiverPort) {
        super(MessageType.PUT_UNLOCK, senderPort, receiverPort);
    }
}
