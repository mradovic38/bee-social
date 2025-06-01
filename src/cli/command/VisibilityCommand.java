package cli.command;


import app.AppConfig;

public class VisibilityCommand implements CLICommand {

    @Override
    public String commandName() {
        return "visibility";
    }

    @Override
    public void execute(String args) {
        if(args.trim().equalsIgnoreCase("public")){

            AppConfig.chordState.setPublic(true);
        }
        else if(args.trim().equalsIgnoreCase("private")){
            AppConfig.chordState.setPublic(false);
        }
        else{
            AppConfig.timestampedStandardPrint("Visibility can only be set to private or public.");
        }


    }

}
