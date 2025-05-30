package servent.handler;

import app.AppConfig;
import servent.message.ConfirmQuitMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.io.PrintWriter;
import java.net.Socket;

public class ConfirmQuitHandler implements MessageHandler{

    private Message clientMessage;

    public ConfirmQuitHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }


    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.CONFIRM_QUIT){
                AppConfig.timestampedErrorPrint("Confirm Quit Handler got: " + clientMessage.getMessageType());
                return;
            }

            Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);

            PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
            bsWriter.write("Quit\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
            bsWriter.flush();

            AppConfig.timestampedStandardPrint("Quit finalized!");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
