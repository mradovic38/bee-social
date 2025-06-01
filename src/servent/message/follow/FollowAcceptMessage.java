package servent.message.follow;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class FollowAcceptMessage extends BasicMessage {

    private static final long serialVersionUID = 698711124440315233L;

    public FollowAcceptMessage(int senderPort, int receiverPort) {
        super(MessageType.FOLLOW_ACC, senderPort, receiverPort);
    }
}
