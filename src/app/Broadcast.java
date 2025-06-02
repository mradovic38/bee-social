package app;

import servent.message.BasicMessage;
import servent.message.fault_tolerance.AskHasTokenMessage;
import servent.message.fault_tolerance.UpdateAfterDeathMessage;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Broadcast {

    private static final Map<String, Object> map = new HashMap<>();
    private static final Object lock = new Object();

    public static void broadcastMessage(BasicMessage message) {
        synchronized (lock) {
            boolean didPut = map.putIfAbsent(message.getSenderPort() + ":" + message.getMessageId(), new Object()) == null;

            // rebroadcast
            List<Integer> alreadySentTo = new ArrayList<>();
            if (didPut) {
                for (ServentInfo i : AppConfig.chordState.getSuccessorTable()) {
                    if(i != null && i.getListenerPort() != message.getSenderPort() && !alreadySentTo.contains(i.getListenerPort())
                        && i != message.getNextReceiver()) {

                        BasicMessage newMessage;

                        switch(message.getMessageType()) {
                            case TOKEN_REQUEST:
                                newMessage = ((SuzukiKasamiRequestTokenMessage)message).deepCopy();
                                break;
                            case ASK_HAS_TOKEN:
                                newMessage = ((AskHasTokenMessage)message).deepCopy();
                                break;
                            case UPDATE_AFTER_DEATH:
                                newMessage = ((UpdateAfterDeathMessage)message).deepCopy();
                                break;
                            default:
                                AppConfig.timestampedStandardPrint("Did not find anything to cast this message type in broadcast: "
                                        + message.getMessageType());
                                continue;
                        }

                        AppConfig.timestampedStandardPrint("Broadcast " + newMessage.getMessageId() + " " +
                                newMessage.getMessageType() + " to: " + i.getListenerPort());
                        newMessage.setNextReceiver(i);
                        MessageUtil.sendMessage(newMessage);
                        alreadySentTo.add(i.getListenerPort());
                    }
                }

            }
        }
    }
}
