package servent.handler.mutex;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.mutex.SuzukiKasamiSendTokenMessage;
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

            // uzmi originalno sendera i njegov RN iz teksta poruke
            String[] msgText = reqMessage.getMessageText().split(":");
            int ogSender = ChordState.chordHash(Integer.parseInt(msgText[0]));
            int senderRNVal = Integer.parseInt(msgText[1]);


            // azuriraj sender RN
            /* When a site Sj receives the request message REQUEST(i, sn) from site Si, it sets RNj[i] to maximum of
               RNj[i] and sn i.e RNj[i] = max(RNj[i], sn).
             */
            int oldVal = AppConfig.chordState.mutex.RN.get(ogSender);
            AppConfig.chordState.mutex.RN.set(ogSender, Math.max(oldVal, senderRNVal));

            // After updating RNj[i], Site Sj sends the token to site Si if it has token and RNj[i] = LN[i] + 1
            if (AppConfig.chordState.mutex.hasToken() && AppConfig.chordState.mutex.RN.get(ogSender) == AppConfig.chordState.mutex.getToken().LN.get(ogSender) + 1) {

                // Ako nije u kriticnoj sekciji => Posalji mu token
                if (!AppConfig.chordState.mutex.inCritialSection()){

                    Message tokenMessage = new SuzukiKasamiSendTokenMessage(
                            AppConfig.myServentInfo.getListenerPort(),
                            AppConfig.chordState.getNextNodeForKey(ogSender).getListenerPort(),
                            String.valueOf(ogSender),
                            AppConfig.chordState.mutex.getToken());

                    MessageUtil.sendMessage(tokenMessage);

                    AppConfig.chordState.mutex.setToken(null);
                }
                // Ako jeste => dodaj na queue
                else{
                    AppConfig.chordState.mutex.getToken().Q.add(ogSender);
                }
            }
            // prosledi poruku dalje
            else{

                Set<Integer> newVisitedIds = new HashSet<>(reqMessage.getVisitedIds());
                newVisitedIds.add(AppConfig.myServentInfo.getChordId());

                Set<ServentInfo> sendTo = new HashSet<>();

                for(ServentInfo nb: AppConfig.chordState.getSuccessorTable()){
                    if(nb != null && !newVisitedIds.contains(nb.getChordId())){
                        sendTo.add(nb);
                        newVisitedIds.add(nb.getChordId());
                    }
                }

                for(ServentInfo nb: sendTo){
                    Message newMsg = new SuzukiKasamiRequestTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                            nb.getListenerPort(),
                            reqMessage.getMessageText(),
                            newVisitedIds);

                    // prosledi poruku dalje jer nemamo token
                    MessageUtil.sendMessage(newMsg);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}