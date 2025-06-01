package servent.handler.follow;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;
import servent.message.follow.FollowAcceptMessage;
import servent.message.util.MessageUtil;

public class FollowRequestHandler implements MessageHandler {

    private Message clientMessage;

    public FollowRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.FOLLOW_REQ) {
            AppConfig.timestampedErrorPrint("Follow Accept handler got message of type: " + clientMessage.getMessageType());
            return;
        }

        if(!AppConfig.chordState.followers.containsKey(clientMessage.getSenderPort())){
            AppConfig.chordState.pendingFollows.put(clientMessage.getSenderPort(), new Object());
        }
        else{
            AppConfig.timestampedStandardPrint("Port " + clientMessage.getSenderPort() + " is already following you.");
        }
    }

}