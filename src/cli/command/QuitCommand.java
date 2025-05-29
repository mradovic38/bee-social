package cli.command;

import cli.CLIParser;
import servent.SimpleServentListener;

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

    }
}
