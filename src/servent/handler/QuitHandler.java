package servent.handler;

import app.AppConfig;
import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.message.*;
import servent.message.util.MessageUtil;

public class QuitHandler implements MessageHandler{

    private Message clientMessage;

    public QuitHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() != MessageType.QUIT) {
                AppConfig.timestampedErrorPrint("Quit handler got: " + clientMessage.getMessageType());
            }

            QuitMessage quitMsg = (QuitMessage) clientMessage;

            // uzmi podatke iz poruke
            String[] msgTextParts = quitMsg.getMessageText().split(":");
            int quitterId = Integer.parseInt(msgTextParts[0]);
            int predecessorId = Integer.parseInt(msgTextParts[1]);
            SuzukiKasamiToken token = quitMsg.getToken();

            AppConfig.chordState.removeNode(quitterId);

            Message newMsg;

            // ako sam predecessor => pusti lock i posalji poruku cvoru koji zeli da quituje da moze da se ugasi
            if(AppConfig.myServentInfo.getChordId() == predecessorId){
                // prvo setuj lock da je tvoj
                AppConfig.chordState.mutex.setToken(token);
                // onda izadji iz kriticne sekcije ako si u njoj
                if (!AppConfig.chordState.mutex.inCritialSection()){
                    AppConfig.chordState.mutex.unlock();
                }

                // salji cvoru da moze da quituje (ovo sme jer je on nas sledbenik pa smo povezani sa njim)
                newMsg = new ConfirmQuitMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodePort());


            }
            // nije predecessor => salji dalje
            else {
                newMsg = new QuitMessage(
                        AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodePort(),
                        quitMsg.getMessageText(),
                        quitMsg.getToken());

                MessageUtil.sendMessage(newMsg);
            }

            MessageUtil.sendMessage(newMsg);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
