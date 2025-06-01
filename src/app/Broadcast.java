package app;

import servent.message.BasicMessage;
import servent.message.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public class Broadcast {

    private static final Map<String, Object> map = new HashMap<>();
    private static final Object lock = new Object();

    public static void broadcastMessage(BasicMessage message) {
        synchronized (lock) {
            boolean didPut = map.putIfAbsent(message.getSenderPort() + ":" + message.getMessageId(), new Object()) == null;

            // rebroadcast
            if (didPut) {
                for (ServentInfo i : AppConfig.chordState.getSuccessorTable()) {
                    if(i != null) {
                        AppConfig.timestampedStandardPrint("Broadcast to: " + i.getListenerPort());
                        message.setNextReceiver(i);
                        MessageUtil.sendMessage(message);
                    }
                }

            }
        }
    }
}
