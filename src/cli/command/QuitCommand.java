package cli.command;

import app.AppConfig;
import app.ServentInfo;
import cli.CLIParser;
import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.SimpleServentListener;
import servent.message.QuitMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;

public class QuitCommand implements CLICommand{

    // ova dva thread-a gasiti kad cvor izlazi iz sistema
    private CLIParser cliParser;
    private SimpleServentListener simpleServentListener;

    public QuitCommand(CLIParser cliParser, SimpleServentListener simpleServentListener) {
        this.cliParser = cliParser;
        this.simpleServentListener = simpleServentListener;
    }


    @Override
    public String commandName() {
        return "quit";
    }

    @Override
    public void execute(String args) {
        // recurrency
        if(AppConfig.didQuit.get())
            return;

        // flag za simple servent listener
        AppConfig.didQuit.set(true);

        cliParser.stop();

        // ako je node sam onda nema kome da se salju poruke -> sam je ako nema nikog ni pre ni posle
        if (AppConfig.chordState.getPredecessor() == null && AppConfig.chordState.getSuccessorTable()[0] == null){
            AppConfig.timestampedStandardPrint("Quitting the system...");
            simpleServentListener.stop();
            AppConfig.chordState.heartbeat.stop();
            return;
        }


        // udji u kriticnu sekciju
        AppConfig.chordState.mutex.lock();
        if(!AppConfig.isAlive.get())
            return;

        // Sad je token kod nas, treba da ga se otarasimo tako sto cemo ga poslati kroz poruku
        SuzukiKasamiToken token = AppConfig.chordState.mutex.getToken();
        AppConfig.chordState.mutex.setInCriticalSection(false);
        AppConfig.chordState.mutex.setToken(null);

        if(AppConfig.chordState.getSuccessorTable()[0] == null){
            try {
                confirmQuitFirstNode();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Salji sledecem da si quitovao, mora da se radi reorganizacija
        QuitMessage quitMsg = new QuitMessage(
                AppConfig.myServentInfo.getListenerPort(),
                AppConfig.chordState.getNextNodePort(),
                AppConfig.myServentInfo.getChordId() + ":" + AppConfig.chordState.getPredecessor().getChordId(),
                token);
        MessageUtil.sendMessage(quitMsg);
    }

    private void confirmQuitFirstNode() throws IOException {
        Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);

        PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
        bsWriter.write("Quit\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
        bsWriter.flush();
        bsSocket.close();
        AppConfig.timestampedStandardPrint("Quit finalized!");
    }
}
