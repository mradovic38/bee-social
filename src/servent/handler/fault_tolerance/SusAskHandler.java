package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.SusAskMessage;
import servent.message.fault_tolerance.SusPingMessage;
import servent.message.util.MessageUtil;

public class SusAskHandler implements MessageHandler {

    private final Message clientMessage;

    public SusAskHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {

            if (clientMessage.getMessageType() != MessageType.SUS_ASK) {
                AppConfig.timestampedErrorPrint("Sus ask handler got: " + clientMessage.getMessageType());
                return;
            }
            SusAskMessage saMsg = (SusAskMessage) clientMessage;

            // pinguj sus cvor
            SusPingMessage spMsg = new SusPingMessage(AppConfig.myServentInfo.getListenerPort(), saMsg.getCheckThisPort(), saMsg.getSenderPort());
            MessageUtil.sendMessage(spMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
