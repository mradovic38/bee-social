package servent.message.follow;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class FollowRequestMessage extends BasicMessage {

    private static final long serialVersionUID = 698711124210015233L;

    public FollowRequestMessage(int senderPort, int receiverPort) {
        super(MessageType.FOLLOW_REQ, senderPort, receiverPort);
    }
}
