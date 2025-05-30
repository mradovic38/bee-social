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

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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
            int senderRNVal;
            Integer bsPort = null;
            if(msgText[1].contains("#")){
                senderRNVal = Integer.parseInt(msgText[1].split("#")[0]);
                bsPort = Integer.parseInt(msgText[1].split("#")[1]);
            }
            else{
                senderRNVal = Integer.parseInt(msgText[1]);
            }


            // azuriraj sender RN
            /* When a site Sj receives the request message REQUEST(i, sn) from site Si, it sets RNj[i] to maximum of
               RNj[i] and sn i.e RNj[i] = max(RNj[i], sn).
             */
            int oldVal = AppConfig.chordState.mutex.RN.get(ogSender);
            AppConfig.chordState.mutex.RN.set(ogSender, Math.max(oldVal, senderRNVal));

            if(AppConfig.chordState.getSuccessorTable()[0] == null && !AppConfig.chordState.mutex.hasToken()){
                System.out.println("Token request waiting...");
                System.out.println(reqMessage);
                while(AppConfig.chordState.getSuccessorTable()[0] == null &&  !AppConfig.chordState.mutex.hasToken()){
                    try{
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                System.out.println("Token request finished waiting!");

            }

            // After updating RNj[i], Site Sj sends the token to site Si if it has token and RNj[i] = LN[i] + 1
            if (AppConfig.chordState.mutex.hasToken() &&
                    (AppConfig.chordState.mutex.RN.get(ogSender) == AppConfig.chordState.mutex.getToken().LN.get(ogSender) + 1 || bsPort != null)) {

                // Ako nije u kriticnoj sekciji => Posalji mu token
                if (!AppConfig.chordState.mutex.inCritialSection()){


                    if (bsPort == null) {
                        Message tokenMessage = new SuzukiKasamiSendTokenMessage(
                                AppConfig.myServentInfo.getListenerPort(),
                                AppConfig.chordState.getNextNodeForKey(ogSender).getListenerPort(),
                                String.valueOf(ogSender),
                                AppConfig.chordState.mutex.getToken());

                        MessageUtil.sendMessage(tokenMessage);
                    }
                    else{
                        Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
                        PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
                        String lnStr = String.join(",", AppConfig.chordState.mutex.getToken().LN.stream().map(String::valueOf).toList());
                        String qStr = String.join(",", AppConfig.chordState.mutex.getToken().Q.stream().map(String::valueOf).toList());
                        bsWriter.write("Token\n" + msgText[0] + "\n" + lnStr + "\n" + qStr + "\n");
                        bsWriter.flush();
                    }

                    AppConfig.chordState.mutex.setToken(null);
                }
                // Ako jeste => dodaj na queue
                else{
                    if (bsPort == null) {
                        AppConfig.chordState.mutex.getToken().Q.add(ogSender);
                    }
                }
            }

            else{

                Set<Integer> newVisitedIds = new HashSet<>(reqMessage.getVisitedIds());
                newVisitedIds.add(AppConfig.myServentInfo.getChordId());

                Set<ServentInfo> sendTo = new HashSet<>();

                for(ServentInfo nb: AppConfig.chordState.getSuccessorTable()){
                    if(!newVisitedIds.contains(nb.getChordId())){
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