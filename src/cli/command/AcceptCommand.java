package cli.command;


import app.AppConfig;

public class AcceptCommand implements CLICommand {

    @Override
    public String commandName() {
        return "accept";
    }

    @Override
    public void execute(String args) {
        if(!args.isBlank()){

            String[] data = args.split(":");

            int port = Integer.parseInt(data[1]);


            if(AppConfig.chordState.pendingFollows.containsKey(port)){
                AppConfig.chordState.pendingFollows.remove(port);
                AppConfig.chordState.followers.put(port, new Object());
            }

            AppConfig.timestampedStandardPrint("Follow request accepted from: " + port);
        }
        else{
            AppConfig.timestampedErrorPrint("Wrong arguments");
        }


    }

}
