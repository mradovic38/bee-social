package cli.command;


import app.AppConfig;

public class FollowCommand implements CLICommand {

    @Override
    public String commandName() {
        return "follow";
    }

    @Override
    public void execute(String args) {
        if(args.isBlank()){
            try{
                String[] data = args.split(":");

                int port = Integer.parseInt(data[1]);
                // TODO: posalji follow request poruku


            }
            catch(NumberFormatException e){
                AppConfig.timestampedErrorPrint("Could not parse int port arg of follow command");
            }

        }
        AppConfig.timestampedStandardPrint("Sent follow request to: " +  AppConfig.chordState.pendingFollows.toString());

    }

}
