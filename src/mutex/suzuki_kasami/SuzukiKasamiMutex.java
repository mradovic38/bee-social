package mutex.suzuki_kasami;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.mutex.SuzukiKasamiSendTokenMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static app.ChordState.CHORD_SIZE;

public class SuzukiKasamiMutex implements Mutex {

    // RN[i] je najveci broj poruka koje je cvor i primio do sad preko request poruke od nas
    public List<Integer> RN = new CopyOnWriteArrayList<>();

    private SuzukiKasamiToken token = null; // null ako nema token
    private final AtomicBoolean hasToken = new AtomicBoolean(false); // true ako token nije null, da bi bilo lakse za get i te gluposti
    private final AtomicBoolean inCriticalSection = new AtomicBoolean(false); // da li smo trenutno u kriticnoj sekciji

    private final Object recurrencyLock = new Object(); // da se ne udje vise puta odjednom u lock() i slicne metode

    public SuzukiKasamiMutex() {
        for (int i = 0; i < CHORD_SIZE; i++) {
            RN.add(0);
        }
    }

    public void lock(){
        lock(new HashSet<>());
    }



    /**
     * <ul>
     * <li> When a site Si wants to enter the critical section and it does not have the token then it increments its sequence number RNi[i] and sends a request message REQUEST(i, sn) to all other sites in order to request the token.
     * Here sn is update value of RNi[i]
     * </ul>
     */
    public void lock(Set<Integer> broadcastToPorts){
        synchronized (recurrencyLock) {
            // ako nema token, a zeli da udje u kriticku sekciju
            if (token == null) {

                // inkrementiraj broj requestova
                int myId = AppConfig.myServentInfo.getChordId();
                RN.set(myId, RN.get(myId) + 1);

                int newVal = RN.get(myId);

                String messageText = AppConfig.myServentInfo.getChordId() + ":" + newVal; // da bi se sacuvalo to ko je zatrazio


                Set<Integer> newVisitedIds = new HashSet<>();
                newVisitedIds.add(AppConfig.myServentInfo.getChordId());
                Set<Integer> sendTo = new HashSet<>();

                if(broadcastToPorts.isEmpty()) {
                    for (ServentInfo nb : AppConfig.chordState.getSuccessorTable()) {
                        if (nb != null) {
                            sendTo.add(nb.getListenerPort());
                            newVisitedIds.add(nb.getChordId());
                        }
                    }
                }
                else{
                    sendTo = broadcastToPorts;
                }

                for(Integer nb: sendTo){
                    Message newMsg = new SuzukiKasamiRequestTokenMessage(AppConfig.myServentInfo.getListenerPort(),
                            nb,
                            messageText,
                            newVisitedIds);

                    // prosledi poruku dalje jer nemamo token
                    MessageUtil.sendMessage(newMsg);
                }
            }


            // cekaj dok ne dobijes
            while (!hasToken.get()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // udji u kriticnu sekciju
            inCriticalSection.set(true);

        }

    }



    /**
     * <ul>
     * <li> sets LN[i] = RNi[i] to indicate that its critical section request RNi[i] has been executed
     * <li> For every site Sj, whose ID is not present in the token queue Q, it appends its ID to Q if RNi[j] = LN[j] + 1
     *     to indicate that site Sj has an outstanding request
     * <li> After above updation, if the Queue Q is non-empty, it pops a site ID from the Q and sends the
     *    token to site indicated by popped ID.
     * <li> If the queue Q is empty, it keeps the token
     * </ul>
     */
    public void unlock(){
        synchronized (recurrencyLock) {
            if(token == null){
                inCriticalSection.set(false);
                AppConfig.chordState.mutex.setToken(null);
                return;
            }


            // azuriraj LN[i]
            int myId = AppConfig.myServentInfo.getChordId();
            token.LN.set(myId, RN.get(myId));

            // za svaki node Sj ciji id nije prisutan u Q appenduj njegov id na Q ako RNi[j] = LN[j] + 1
            for (int i = 0; i < CHORD_SIZE; i++) {
                if (!token.Q.contains(i) && RN.get(i) == token.LN.get(i) + 1) {
                    token.Q.add(i);
                }
            }

            // posalji token onom koji je cekao na token
            if (!token.Q.isEmpty()) {

                Integer finalReceiverId = token.Q.poll();
                AppConfig.timestampedStandardPrint("==============FORWARDUJEM TOKEN CVORU: " + finalReceiverId + "===============");

                Message tokenMessage = new SuzukiKasamiSendTokenMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(finalReceiverId).getListenerPort(),
                        String.valueOf(finalReceiverId),
                        AppConfig.chordState.mutex.getToken());
                MessageUtil.sendMessage(tokenMessage);

                AppConfig.chordState.mutex.setToken(null);
            }
            inCriticalSection.set(false);
        }
        AppConfig.timestampedStandardPrint("Unlocked");
    }


    // region GETTERS AND SETTERS

    public boolean hasToken(){
        return hasToken.get();
    }
    public boolean inCritialSection(){
        return inCriticalSection.get();
    }
    public void setInCriticalSection(boolean value){
        this.inCriticalSection.set(value);
    }

    public void setToken(SuzukiKasamiToken token){
        if (token != null){
            AppConfig.timestampedStandardPrint("=================JA IMAM TOKEN SADA==================");
            this.token = token;
            this.hasToken.set(true);
        }
        else{
            AppConfig.timestampedStandardPrint("=================NEMAM VISE TOKEN==================");
            this.token = null;
            this.hasToken.set(false);
            this.inCriticalSection.set(false);
        }
    }

    public SuzukiKasamiToken getToken() {
        return token;
    }

    //endregion
}
