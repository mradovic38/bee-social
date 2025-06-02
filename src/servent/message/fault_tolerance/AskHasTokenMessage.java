package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class AskHasTokenMessage extends BasicMessage {

    private static final long serialVersionUID = 1559931124440315233L;

    public AskHasTokenMessage(int senderPort, int receiverPort) {
        super(MessageType.ASK_HAS_TOKEN, senderPort, receiverPort);
    }

    public BasicMessage deepCopy() {
        BasicMessage newMsg = new AskHasTokenMessage(getSenderPort(), getReceiverPort());
        newMsg.setMessageId(this.getMessageId());
        newMsg.setNextReceiver(this.getNextReceiver());

        return newMsg;
    }
}
