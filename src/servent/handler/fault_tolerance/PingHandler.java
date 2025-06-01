package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.PongMessage;
import servent.message.util.MessageUtil;

public class PingHandler implements MessageHandler {

    private final Message clientMessage;

    public PingHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {
            if(clientMessage.getMessageType() != MessageType.PING)
            {
                AppConfig.timestampedErrorPrint("Ping handler got: " + clientMessage.getMessageType());
                return;
            }

            // posalji pong
            Message pongMessage = new PongMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort());
            MessageUtil.sendMessage(pongMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
