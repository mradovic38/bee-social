package servent.message.fault_tolerance;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class UpdateAfterDeathMessage extends BasicMessage {

    private static final long serialVersionUID = 8558091122220235233L;

    private final ServentInfo deadServentInfo;

    public UpdateAfterDeathMessage(int senderPort, int receiverPort, ServentInfo deadServentInfo) {
        super(MessageType.UPDATE_AFTER_DEATH, senderPort, receiverPort);
        this.deadServentInfo = deadServentInfo;
        setNextReceiver(AppConfig.chordState.getNextNodeForKey(ChordState.chordHash(receiverPort)));
    }
    public BasicMessage deepCopy() {
        BasicMessage newMsg = new UpdateAfterDeathMessage(getSenderPort(), getReceiverPort(), deadServentInfo);
        newMsg.setMessageId(this.getMessageId());
        newMsg.setNextReceiver(this.getNextReceiver());

        return newMsg;
    }


    public ServentInfo getDeadServentInfo() {
        return deadServentInfo;
    }
}
