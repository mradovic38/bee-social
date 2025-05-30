package servent.message.mutex;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.List;
import java.util.Set;

public class SuzukiKasamiRequestTokenMessage extends BasicMessage {

    private static final long serialVersionUID = -8558031124520235033L;

    private final Set<Integer> visitedIds;

    public SuzukiKasamiRequestTokenMessage(int senderPort, int receiverPort, String text, Set<Integer> visitedIds) {
        super(MessageType.TOKEN_REQUEST, senderPort, receiverPort, text);
        this.visitedIds = visitedIds;
    }

    public Set<Integer> getVisitedIds() {
        return visitedIds;
    }
}
