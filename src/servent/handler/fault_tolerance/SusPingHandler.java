package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.SusPingMessage;
import servent.message.fault_tolerance.SusPongMessage;
import servent.message.util.MessageUtil;

public class SusPingHandler implements MessageHandler {

    private final Message clientMessage;

    public SusPingHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {

            if (clientMessage.getMessageType() != MessageType.SUS_PING) {
                AppConfig.timestampedErrorPrint("Sus ping handler got: " + clientMessage.getMessageType());
                return;
            }
            SusPingMessage sPingMsg = (SusPingMessage) clientMessage;

            // vrati pong buddy-ju
            SusPongMessage sPongMsg = new SusPongMessage(AppConfig.myServentInfo.getListenerPort(),
                    sPingMsg.getSenderPort(), sPingMsg.getInitiatorPort());
            MessageUtil.sendMessage(sPongMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
