package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.PongMessage;
import servent.message.fault_tolerance.SusPingMessage;
import servent.message.fault_tolerance.SusPongMessage;
import servent.message.util.MessageUtil;

public class SusPongHandler implements MessageHandler {

    private final Message clientMessage;

    public SusPongHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {

            if (clientMessage.getMessageType() != MessageType.SUS_PONG) {
                AppConfig.timestampedErrorPrint("Sus pong handler got: " + clientMessage.getMessageType());
                return;
            }
            SusPongMessage sPongMsg = (SusPongMessage) clientMessage;

            // prosledi pong inicijatoru
            PongMessage pMsg = new PongMessage(AppConfig.myServentInfo.getListenerPort(),
                    sPongMsg.getInitiatorPort(), clientMessage.getSenderPort());
            MessageUtil.sendMessage(pMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
