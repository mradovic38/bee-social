package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.PongMessage;
import servent.message.fault_tolerance.TellHasTokenMessage;
import servent.message.util.MessageUtil;

public class AskHasTokenHandler implements MessageHandler {

    private final Message clientMessage;

    public AskHasTokenHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {
            if(clientMessage.getMessageType() != MessageType.ASK_HAS_TOKEN)
            {
                AppConfig.timestampedErrorPrint("Ask has token handler got: " + clientMessage.getMessageType());
                return;
            }

            // posalji tell
            Message tellMsg = new TellHasTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                    clientMessage.getSenderPort(), AppConfig.chordState.mutex.hasToken());
            MessageUtil.sendMessage(tellMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
