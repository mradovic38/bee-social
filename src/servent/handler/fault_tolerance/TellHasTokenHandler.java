package servent.handler.fault_tolerance;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.AskHasTokenMessage;
import servent.message.fault_tolerance.PongMessage;
import servent.message.fault_tolerance.TellHasTokenMessage;
import servent.message.util.MessageUtil;

public class TellHasTokenHandler implements MessageHandler {

    private final Message clientMessage;

    public TellHasTokenHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {
            if(clientMessage.getMessageType() != MessageType.TELL_HAS_TOKEN)
            {
                AppConfig.timestampedErrorPrint("Tell has token handler got: " + clientMessage.getMessageType());
                return;
            }

            TellHasTokenMessage tellMsg = (TellHasTokenMessage) clientMessage;
            // inkrementiraj i setuj na true ako je true

            if(tellMsg.isHasToken()){
                AppConfig.chordState.heartbeat.someoneHasToken.set(true);
            }

            AppConfig.chordState.heartbeat.noTokenCount.incrementAndGet();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
