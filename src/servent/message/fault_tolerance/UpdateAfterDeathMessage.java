package servent.message.fault_tolerance;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class UpdateAfterDeathMessage extends BasicMessage {

    private static final long serialVersionUID = 8558091122220235233L;

    private final ServentInfo deadServentInfo;

    public UpdateAfterDeathMessage(int senderPort, int receiverPort, ServentInfo deadServentInfo) {
        super(MessageType.UPDATE_AFTER_DEATH, senderPort, receiverPort);
        this.deadServentInfo = deadServentInfo;
    }

    public ServentInfo getDeadServentInfo() {
        return deadServentInfo;
    }
}
