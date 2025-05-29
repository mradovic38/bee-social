package servent.message;

public class QuitMessage extends BasicMessage{
    private static final long serialVersionUID = 5163032219888734276L;


    public QuitMessage(int senderPort, int receiverPort) {
        super(MessageType.QUIT, senderPort, receiverPort);
    }
}
