package servent.handler.follow;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;
import servent.message.util.MessageUtil;

public class FollowAcceptHandler implements MessageHandler {

    private Message clientMessage;

    public FollowAcceptHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.FOLLOW_ACC) {
            AppConfig.timestampedErrorPrint("Follow Accept handler got message of type: " + clientMessage.getMessageType());
            return;
        }



    }

}