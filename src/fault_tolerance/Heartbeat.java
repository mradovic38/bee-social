package fault_tolerance;

import app.*;
import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.fault_tolerance.AskHasTokenMessage;
import servent.message.fault_tolerance.PingMessage;
import servent.message.fault_tolerance.SusAskMessage;
import servent.message.fault_tolerance.UpdateAfterDeathMessage;
import servent.message.util.MessageUtil;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// sluzi da pinguje da vidi da li su zivi sledbenik i prethodnik
public class Heartbeat implements Runnable, Cancellable{
    private volatile boolean working = true;

    private static final int PING_INTERVAL_MS = 1000;
    private static final int SOMEONE_HAS_TOKEN_WAIT = 7000;

    private final NodeHealthInfo predecessorNodeHealthInfo;
    private final NodeHealthInfo successorNodeHealthInfo;

    public AtomicBoolean someoneHasToken = new AtomicBoolean(false);

    public Heartbeat() {
        this.predecessorNodeHealthInfo = new NodeHealthInfo();
        this.successorNodeHealthInfo = new NodeHealthInfo();
    }

    @Override
    public void stop() {
        working = false;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Heartbeat started <3");
        while (working) {
            try {

                ServentInfo predecessor = AppConfig.chordState.getPredecessor();
                ServentInfo successor = AppConfig.chordState.getSuccessorTable()[0];

                // proveri successor i predecessor-a
                checkBuddy(predecessor, successor,  predecessorNodeHealthInfo, AppConfig.chordState.predecessorBackup);
                checkBuddy(successor, predecessor, successorNodeHealthInfo, AppConfig.chordState.successorBackup);

                Thread.sleep(PING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    private void checkBuddy(ServentInfo checkInfo, ServentInfo buddyInfo, NodeHealthInfo nodeHealthInfo,
                            Map<Integer, Map<String, ImageEntry>> backup) {

        if(checkInfo == null){
            // mrtav :(
            nodeHealthInfo.setNodeStatus(NodeStatus.DEAD);
            nodeHealthInfo.setTimestamp(System.currentTimeMillis());
            nodeHealthInfo.setFinishedBroadcasting(false);
            return;
        }

        // pinguj
        Message pingMessage = new PingMessage(AppConfig.myServentInfo.getListenerPort(), checkInfo.getListenerPort());
        MessageUtil.sendMessage(pingMessage);

        // ako je ako je proslo vise od WEAK_LIMIT vremena, stavi na sus i broadcast na true, znaci saljemo buddyju da ga pinguje
        if (System.currentTimeMillis() - nodeHealthInfo.getTimestamp() > AppConfig.WEAK_LIMIT
                && !nodeHealthInfo.isFinishedBroadcasting()) {
            nodeHealthInfo.setNodeStatus(NodeStatus.SUS);
            nodeHealthInfo.setFinishedBroadcasting(true);

            // reci buddy-ju da proveri
            Message saMsg = new SusAskMessage(AppConfig.myServentInfo.getListenerPort(),
                    buddyInfo.getListenerPort(), checkInfo.getListenerPort());

            MessageUtil.sendMessage(saMsg);
        }

        // proslo je vise od STRONG_LIMIT vremena i SUS je => MRTAV - salji na reorganizaciju
        if (System.currentTimeMillis() - nodeHealthInfo.getTimestamp() > AppConfig.STRONG_LIMIT
                && nodeHealthInfo.getNodeStatus() == NodeStatus.SUS) {

            AppConfig.timestampedStandardPrint(buddyInfo.getListenerPort() + " might be dead !?");

            // ako mi nemamo lock
            if(!AppConfig.chordState.mutex.hasToken()) {

                // da li je node koji je nestao imao lock. ako zauvek cekamo moramo da napravimo novi token
                someoneHasToken = new AtomicBoolean(false);

                BasicMessage askMsg = new AskHasTokenMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getListenerPort());
                Broadcast.broadcastMessage(askMsg);

                // ako se desi neki edge case da se ne ceka zauvek
                int wait = SOMEONE_HAS_TOKEN_WAIT;
                // sacekaj da dobijes odg od svih
                AppConfig.timestampedStandardPrint("Waiting to get if the token is in the system");
                while (wait >= 0 && !someoneHasToken.get() && AppConfig.isAlive.get()) {
                    try {
                        wait -= 30;
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (!AppConfig.isAlive.get())
                    return;

                // ako je imao (niko nije vratio da je imao) => napravi novi token
                if (!someoneHasToken.get()) {
                    AppConfig.timestampedStandardPrint("Token dissapeared. Creating new...");
                    AppConfig.chordState.mutex.setToken(new SuzukiKasamiToken());
                }
            }

            //  uzmi lock

            AppConfig.chordState.mutex.urgentLock();
            if(!AppConfig.isAlive.get())
                return;

            // SALJI BOOTSTRAPU DA OBRISE
            informBootstrap(checkInfo.getListenerPort());

            AppConfig.timestampedStandardPrint("Node on port: " + checkInfo.getListenerPort() + " has died :(");

            nodeHealthInfo.setNodeStatus(NodeStatus.DEAD);

            // BRISI
            AppConfig.chordState.removeNode(checkInfo.getChordId());
            // BRISI IZ QUEUE AKO JE BILO
            AppConfig.chordState.mutex.getToken().Q.remove(checkInfo.getListenerPort());


            // salji kao put value
            for(Integer key: backup.keySet()){
                for (Map.Entry<String, ImageEntry> entry: backup.get(key).entrySet()){
                    AppConfig.chordState.putValue(key, entry.getKey(), AppConfig.myServentInfo.getListenerPort());
                }
            }
            // clear-uj
            backup.clear();

            // Broadcastuj drugima da urade update
            BasicMessage uadMsg = new UpdateAfterDeathMessage(AppConfig.myServentInfo.getListenerPort(), AppConfig.myServentInfo.getListenerPort(), checkInfo);
            Broadcast.broadcastMessage(uadMsg);

            // stavi alive jer smo ga sredili
            nodeHealthInfo.setNodeStatus(NodeStatus.ALIVE);
            nodeHealthInfo.setFinishedBroadcasting(false);

            // otkljucaj
            AppConfig.chordState.mutex.unlock();

        }
    }

    private void informBootstrap(int port){
        try {
            Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Quit\n" + port + "\n");
            bsWriter.flush();
            bsSocket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public NodeHealthInfo getPredecessorNodeHealthInfo() {
        return predecessorNodeHealthInfo;
    }


    public NodeHealthInfo getSuccessorNodeHealthInfo() {
        return successorNodeHealthInfo;
    }
}
