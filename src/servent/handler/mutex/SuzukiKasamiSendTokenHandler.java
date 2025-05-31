package servent.handler.mutex;

import app.AppConfig;
import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.mutex.SuzukiKasamiSendTokenMessage;
import servent.message.util.MessageUtil;

public class SuzukiKasamiSendTokenHandler implements MessageHandler {


    private Message clientMessage;

    public SuzukiKasamiSendTokenHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    /**
     * <ul>
     * <li> Site Si executes the critical section if it has acquired the token.
     * </ul>
     */

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() != MessageType.TOKEN_SEND) {
                AppConfig.timestampedErrorPrint("Token send handler received: " + clientMessage.getMessageType());
                return;
            }

            SuzukiKasamiSendTokenMessage sendMsg = (SuzukiKasamiSendTokenMessage) clientMessage;
            SuzukiKasamiToken token =  sendMsg.getToken();

            // setuj token
            AppConfig.chordState.mutex.setToken(token);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}