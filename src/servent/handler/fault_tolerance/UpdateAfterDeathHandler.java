package servent.handler.fault_tolerance;

import app.AppConfig;
import app.ChordState;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.PongMessage;
import servent.message.fault_tolerance.UpdateAfterDeathMessage;
import servent.message.util.MessageUtil;

public class UpdateAfterDeathHandler implements MessageHandler {

    private final Message clientMessage;

    public UpdateAfterDeathHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {
            if(clientMessage.getMessageType() != MessageType.UPDATE_AFTER_DEATH)
            {
                AppConfig.timestampedErrorPrint("Update after death handler got: " + clientMessage.getMessageType());
                return;
            }

            UpdateAfterDeathMessage updateAfterDeathMessage = (UpdateAfterDeathMessage) clientMessage;

            AppConfig.chordState.removeNode(updateAfterDeathMessage.getDeadServentInfo().getChordId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
