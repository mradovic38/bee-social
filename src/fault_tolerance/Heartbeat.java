package fault_tolerance;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import servent.message.fault_tolerance.PingMessage;
import servent.message.util.MessageUtil;

// sluzi da pinguje da vidi da li su zivi sledbenik i prethodnik
public class Heartbeat implements Runnable, Cancellable{
    private volatile boolean working = true;

    private static final int PING_INTERVAL_MS = 2000;

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

                if (predecessor == null || successor == null) continue;

                // Check both buddies (can separate these in real-world)
                checkBuddy(predecessor, successor);
                checkBuddy(successor, predecessor);

                Thread.sleep(PING_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    private void checkBuddy(ServentInfo target, ServentInfo helper) {
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

}
