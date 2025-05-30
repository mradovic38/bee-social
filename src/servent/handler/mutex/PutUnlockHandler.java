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

            int storerId = Integer.parseInt(clientMessage.getMessageText());


            // to smo mi -> unlock
            if(AppConfig.chordState.isKeyMine(storerId)){
                AppConfig.chordState.mutex.unlock();
            }
            // nismo mi -> propagiraj dalje
            else{
                Message newMsg = new PutUnlockMessage(clientMessage.getSenderPort(),
                        AppConfig.chordState.getNextNodeForKey(storerId).getListenerPort(), clientMessage.getMessageText());
                MessageUtil.sendMessage(newMsg);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}