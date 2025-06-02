package servent.handler.fault_tolerance;

import app.AppConfig;
import app.Broadcast;
import servent.handler.MessageHandler;
import servent.message.BasicMessage;
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

            boolean hasToken = AppConfig.chordState.mutex.hasToken();

            // posalji tell
            Message tellMsg = new TellHasTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                    clientMessage.getSenderPort(), hasToken);
            MessageUtil.sendMessage(tellMsg);

            // rebroadcast
            if(!hasToken) {
                Broadcast.broadcastMessage((BasicMessage) clientMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
