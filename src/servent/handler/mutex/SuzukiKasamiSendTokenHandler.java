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
            if (clientMessage.getMessageType() == MessageType.TOKEN_SEND) {
                AppConfig.timestampedErrorPrint("Token request handler received: " + clientMessage.getMessageType());
                return;
            }

            SuzukiKasamiSendTokenMessage sendMsg = (SuzukiKasamiSendTokenMessage) clientMessage;
            int finalReceiverId = Integer.parseInt(sendMsg.getMessageText());
            SuzukiKasamiToken token =  new SuzukiKasamiToken();

            // ako sam ja trazio token => ulazim u kriticnu sekciju
            if(AppConfig.chordState.isKeyMine(finalReceiverId)){
                AppConfig.chordState.mutex.setToken(token);
            }

            // nisam ja trazio token => nadji jos jednog blizeg i prosledi mu
            else{
                int nextNodePort = AppConfig.chordState.getNextNodeForKey(finalReceiverId).getListenerPort();

                SuzukiKasamiSendTokenMessage newMsg = new SuzukiKasamiSendTokenMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        nextNodePort,
                        sendMsg.getMessageText(),
                        token
                );
                MessageUtil.sendMessage(newMsg);

            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}