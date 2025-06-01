package servent.handler.fault_tolerance;

import app.AppConfig;
import fault_tolerance.NodeStatus;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.fault_tolerance.PongMessage;

public class PongHandler implements MessageHandler {

    private final Message clientMessage;

    public PongHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {

            if (clientMessage.getMessageType() != MessageType.PONG) {
                AppConfig.timestampedErrorPrint("Pong handler got: " + clientMessage.getMessageType());
                return;
            }

            PongMessage pongMsg = (PongMessage) clientMessage;

            // resetuj predecessora
            if(AppConfig.chordState.getPredecessor() != null && pongMsg.getMarkThisPort() == AppConfig.chordState.getPredecessor().getListenerPort()){
                AppConfig.chordState.heartbeat.getPredecessorNodeHealthInfo().setNodeStatus(NodeStatus.ALIVE);
                AppConfig.chordState.heartbeat.getPredecessorNodeHealthInfo().setFinishedBroadcasting(true);
                AppConfig.chordState.heartbeat.getPredecessorNodeHealthInfo().setTimestamp(System.currentTimeMillis());
            }
            // resetuj successora
            else if(AppConfig.chordState.getSuccessorTable()[0] != null && pongMsg.getMarkThisPort() == AppConfig.chordState.getSuccessorTable()[0].getListenerPort()) {
                AppConfig.chordState.heartbeat.getSuccessorNodeHealthInfo().setNodeStatus(NodeStatus.ALIVE);
                AppConfig.chordState.heartbeat.getSuccessorNodeHealthInfo().setFinishedBroadcasting(true);
                AppConfig.chordState.heartbeat.getSuccessorNodeHealthInfo().setTimestamp(System.currentTimeMillis());
            }
            else if(AppConfig.chordState.getSuccessorTable()[0] != null || AppConfig.chordState.getPredecessor() != null){
                AppConfig.timestampedStandardPrint("Pong got from node that is neither successor or predecessor: " + pongMsg.getMarkThisPort()
                + " (predecessor: " + AppConfig.chordState.getPredecessor()+ ", successor: " +
                        AppConfig.chordState.getSuccessorTable()[0] + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
