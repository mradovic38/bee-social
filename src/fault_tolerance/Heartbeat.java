package fault_tolerance;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.message.Message;
import servent.message.fault_tolerance.AskHasTokenMessage;
import servent.message.fault_tolerance.PingMessage;
import servent.message.fault_tolerance.SusAskMessage;
import servent.message.fault_tolerance.UpdateAfterDeathMessage;
import servent.message.util.MessageUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// sluzi da pinguje da vidi da li su zivi sledbenik i prethodnik
public class Heartbeat implements Runnable, Cancellable{
    private volatile boolean working = true;

    private static final int PING_INTERVAL_MS = 1000;

    private NodeHealthInfo predecessorNodeHealthInfo;
    private NodeHealthInfo successorNodeHealthInfo;

    public AtomicInteger noTokenCount = new AtomicInteger(0);
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
        while (working) {
            try {
                ServentInfo predecessor = AppConfig.chordState.getPredecessor();
                ServentInfo successor = AppConfig.chordState.getSuccessorTable()[0];

                // proveri successor i predecessor-a
                checkBuddy(predecessor, successor,  predecessorNodeHealthInfo);
                checkBuddy(successor, successor, predecessorNodeHealthInfo);

                Thread.sleep(PING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    private void checkBuddy(ServentInfo checkInfo, ServentInfo buddyInfo, NodeHealthInfo nodeHealthInfo) {

        if(checkInfo == null || buddyInfo == null){
            // mrtav :(
            nodeHealthInfo.setNodeStatus(NodeStatus.DEAD);
            nodeHealthInfo.setTimestamp(System.currentTimeMillis());
            nodeHealthInfo.setFinishedBroadcasting(false);
            return;
        }

        // pinguj predesesora
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

            // da li je node koji je nestao imao lock. ako zauvek cekamo moramo da napravimo novi token
            // TODO: ispraviti broadcast da ne gadja direktno
            int totalCnt = 0;
            noTokenCount = new AtomicInteger(0);
            someoneHasToken = new AtomicBoolean(false);

            for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
                if(serventInfo.getListenerPort() != AppConfig.myServentInfo.getListenerPort()){
                    Message askMsg = new AskHasTokenMessage(AppConfig.myServentInfo.getListenerPort(), serventInfo.getListenerPort());
                    MessageUtil.sendMessage(askMsg);
                    totalCnt++;
                }
            }
            // ako se desi neki edge case da se ne ceka zauvek
            int wait = 10000;
            // sacekaj da dobijes odg od svih
            AppConfig.timestampedStandardPrint("Waiting to get if the token is in the system");
            while(noTokenCount.get() < totalCnt && wait >= 0){
                try {
                    wait -= 30;
                    Thread.sleep(30);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // ako je imao (niko nije vratio da je imao) => napravi novi token
            if(!someoneHasToken.get()){
                AppConfig.timestampedStandardPrint("Token dissapeared. Creating new...");
                AppConfig.chordState.mutex.setToken(new SuzukiKasamiToken());
            }

            //  uzmi lock
            AppConfig.chordState.mutex.lock(AppConfig.chordState.getAllNodeInfo().stream()
                    .map(ServentInfo::getListenerPort).collect(Collectors.toSet()));


            AppConfig.timestampedStandardPrint("Node on port: " + checkInfo.getListenerPort() + " has died :(");

            nodeHealthInfo.setNodeStatus(NodeStatus.DEAD);

            // BRISI
            AppConfig.chordState.removeNode(checkInfo.getChordId());
            // BRISI IZ QUEUE AKO JE BILO
            AppConfig.chordState.mutex.getToken().Q.remove(checkInfo.getListenerPort());

            // TODO: ispraviti broadcast da ne gadja direktno
            // Broadcastuj drugima da urade update
            for (ServentInfo serventInfo : AppConfig.chordState.getAllNodeInfo()) {
                if (serventInfo.getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
                    Message uadMsg = new UpdateAfterDeathMessage(AppConfig.myServentInfo.getListenerPort(), serventInfo.getListenerPort(), checkInfo);

                    MessageUtil.sendMessage(uadMsg);
                }
            }

            // stavi alive jer smo ga sredili
            nodeHealthInfo.setNodeStatus(NodeStatus.ALIVE);
            nodeHealthInfo.setFinishedBroadcasting(false);

            // otkljucaj
            AppConfig.chordState.mutex.unlock();

        }

//        long lastSeen = AppConfig.buddyStatus.getLastSeen(target);
//        long now = System.currentTimeMillis();
//
//        // Send PING
//        MessageUtil.sendMessage(new PingMessage(AppConfig.myServentInfo.getListenerPort(), target.getListenerPort()));
//
//        // If >4s since last pong, ask helper to ping
//        if (now - lastSeen > AppConfig.WEAK_LIMIT && now - lastSeen <= AppConfig.STRONG_LIMIT) {
//            AppConfig.timestampedStandardPrint("Suspicious node: " + target.getListenerPort() + ". Asking for help from buddy.");
//
//            MessageUtil.sendMessage(new ComradeAskMessage(
//                    AppConfig.myServentInfo.getListenerPort(),
//                    helper.getListenerPort(),
//                    target.getListenerPort(),
//                    AppConfig.myServentInfo.getListenerPort()
//            ));
//        }
//
//        // If >10s, remove node
//        if (now - lastSeen > AppConfig.STRONG_LIMIT) {
//            AppConfig.timestampedStandardPrint("Node " + target.getListenerPort() + " did not respond for 10s. Declaring as DEAD.");
//
//            AppConfig.chordState.removeNode(target);
//            AppConfig.chordState.getSuzukiKasamiUtils().getToken().removeNodeFromTokenQueue(target.getListenerPort());
//
//            // Inform others
//            for (ServentInfo si : AppConfig.chordState.getAllNodeInfo()) {
//                if (si.getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
//                    MessageUtil.sendMessage(new RecoveryMessage(
//                            AppConfig.myServentInfo.getListenerPort(),
//                            si.getListenerPort(),
//                            target
//                    ));
//                }
//            }
//
//            // Optionally handle token recreation if necessary
//            if (AppConfig.chordState.mutex.getToken() == null) {
//                AppConfig.chordState.getSuzukiKasamiUtils().getToken().tryRecreateToken();
//            }
//
//            // Reset buddy state
//            AppConfig.buddyStatus.markRemoved(target);
//        }
    }


    public NodeHealthInfo getPredecessorNodeHealthInfo() {
        return predecessorNodeHealthInfo;
    }

    public void setPredecessorNodeHealthInfo(NodeHealthInfo predecessorNodeHealthInfo) {
        this.predecessorNodeHealthInfo = predecessorNodeHealthInfo;
    }

    public NodeHealthInfo getSuccessorNodeHealthInfo() {
        return successorNodeHealthInfo;
    }

    public void setSuccessorNodeHealthInfo(NodeHealthInfo successorNodeHealthInfo) {
        this.successorNodeHealthInfo = successorNodeHealthInfo;
    }
}
