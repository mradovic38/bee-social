package servent.handler.mutex;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.mutex.PutUnlockMessage;
import servent.message.util.MessageUtil;

public class PutUnlockHandler implements MessageHandler {

    private Message clientMessage;

    public PutUnlockHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() != MessageType.PUT_UNLOCK){
                AppConfig.timestampedStandardPrint("PutUnlockHandler got: " + clientMessage.getMessageType());
                return;
            }

            AppConfig.chordState.mutex.unlock();
            AppConfig.timestampedStandardPrint("Put unlock released lock");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}