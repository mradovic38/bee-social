package servent.message.fault_tolerance;

import servent.message.BasicMessage;
import servent.message.MessageType;

public class TellHasTokenMessage extends BasicMessage {

    private static final long serialVersionUID = 1239931124440315233L;

    private final boolean hasToken;
    public TellHasTokenMessage(int senderPort, int receiverPort, boolean hasToken) {
        super(MessageType.TELL_HAS_TOKEN, senderPort, receiverPort);
        this.hasToken = hasToken;
    }

    public boolean isHasToken() {
        return hasToken;
    }
}
