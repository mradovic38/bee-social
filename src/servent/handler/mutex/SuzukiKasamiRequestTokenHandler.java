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

import java.util.Collections;
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
            boolean isNew = false;
            String ogText = reqMessage.getMessageText();
            if(ogText.contains("#")){
                isNew = true;
                ogText = ogText.replace("#", "");
            }
            // uzmi originalno sendera i njegov RN iz teksta poruke
            String[] msgText = ogText.split(":");
            int ogSender = Integer.parseInt(msgText[0]);
            int senderRNVal = Integer.parseInt(msgText[1]);




            // azuriraj sender RN
            /* When a site Sj receives the request message REQUEST(i, sn) from site Si, it sets RNj[i] to maximum of
               RNj[i] and sn i.e RNj[i] = max(RNj[i], sn).
             */
            int oldVal = AppConfig.chordState.mutex.RN.get(ogSender);
            AppConfig.chordState.mutex.RN.set(ogSender, Math.max(oldVal, senderRNVal));

            // After updating RNj[i], Site Sj sends the token to site Si if it has token and RNj[i] = LN[i] + 1
            if (AppConfig.chordState.mutex.hasToken() && AppConfig.chordState.mutex.RN.get(ogSender) == AppConfig.chordState.mutex.getToken().LN.get(ogSender) + 1) {
                // dodaj na queue -> ako je novi dodaj port, ako je vec tu dodaj id
                if(!isNew)
                    AppConfig.chordState.mutex.getToken().Q.add(ogSender);
                else
                    AppConfig.chordState.mutex.getToken().Q.add(reqMessage.getSenderPort());

                // Ako nije u kriticnoj => salji sledecem na queue
                if (!AppConfig.chordState.mutex.inCritialSection()){
                    AppConfig.chordState.mutex.checkQueue();
                }
            }
            // prosledi poruku dalje
            else if (!isNew && !AppConfig.chordState.mutex.hasToken()){


                Set<Integer> newVisitedIds = new HashSet<>(reqMessage.getVisitedIds());
                newVisitedIds.add(AppConfig.myServentInfo.getChordId());

                Set<ServentInfo> sendTo = new HashSet<>();

                for(ServentInfo nb: AppConfig.chordState.getSuccessorTable()){
                    if(nb != null && !newVisitedIds.contains(nb.getChordId())){
                        sendTo.add(nb);
                        newVisitedIds.add(nb.getChordId());
                    }
                }

                AppConfig.timestampedStandardPrint("Dont have token, Forwarding to: " + sendTo + " new visited: " + newVisitedIds);

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