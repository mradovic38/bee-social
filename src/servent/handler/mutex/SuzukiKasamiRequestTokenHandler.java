package servent.handler.mutex;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.util.MessageUtil;

import java.util.HashSet;
import java.util.Set;

public class SuzukiKasamiRequestTokenHandler implements MessageHandler {


    private Message clientMessage;

    public SuzukiKasamiRequestTokenHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    /**
     * <ul>
     * <li> When a site Sj receives the request message REQUEST(i, sn) from site Si, it sets RNj[i] to maximum of RNj[i]
     * and sn i.e., RNj[i] = max(RNj[i], sn).
     * <li> After updating RNj[i], Site Sj sends the token to site Si if it has token and RNj[i] = LN[i] + 1
     * </ul>
     */

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() != MessageType.TOKEN_REQUEST) {
                AppConfig.timestampedErrorPrint("Token request handler received: " + clientMessage.getMessageType());
                return;
            }

            // dohvati podatke iz poruke
            SuzukiKasamiRequestTokenMessage reqMessage = (SuzukiKasamiRequestTokenMessage) clientMessage;

            int senderRNVal = reqMessage.getRnVal();
            int senderId = ChordState.chordHash(clientMessage.getSenderPort());

            // azuriraj sender RN
            /* When a site Sj receives the request message REQUEST(i, sn) from site Si, it sets RNj[i] to maximum of
               RNj[i] and sn i.e RNj[i] = max(RNj[i], sn).
             */
            int oldVal = AppConfig.chordState.mutex.RN.get(senderId);

            AppConfig.chordState.mutex.RN.set(senderId, Math.max(oldVal, senderRNVal));

            // After updating RNj[i], Site Sj sends the token to site Si if it has token and RNj[i] = LN[i] + 1
            if (AppConfig.chordState.mutex.hasToken() &&
                    AppConfig.chordState.mutex.RN.get(senderId) == AppConfig.chordState.mutex.getToken().LN.get(senderId) + 1) {

                // dodaj na queue
                AppConfig.chordState.mutex.getToken().Q.add(reqMessage.getSenderPort());

                // Ako nije u kriticnoj => salji sledecem na queue
                if (!AppConfig.chordState.mutex.inCritialSection()){
                    AppConfig.chordState.mutex.checkQueue();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}