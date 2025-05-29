package servent.message;


public class ConfirmQuitMessage extends BasicMessage {
    private static final long serialVersionUID = 5263032219888734276L;

    public ConfirmQuitMessage(int senderPort, int receiverPort) {
        super(MessageType.CONFIRM_QUIT, senderPort, receiverPort);
    }
}
