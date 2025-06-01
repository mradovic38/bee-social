package cli.command;


import app.AppConfig;
import servent.message.Message;
import servent.message.follow.FollowRequestMessage;
import servent.message.util.MessageUtil;

public class FollowCommand implements CLICommand {

    @Override
    public String commandName() {
        return "follow";
    }

    @Override
    public void execute(String args) {
        if(!args.isBlank()){
            try{
                String[] data = args.split(":");

                int port = Integer.parseInt(data[1]);
                Message followMsg = new FollowRequestMessage(AppConfig.myServentInfo.getListenerPort(), port);
                MessageUtil.sendMessage(followMsg);

                AppConfig.timestampedStandardPrint("Sent follow request to: " + port);

            }
            catch(NumberFormatException e){
                AppConfig.timestampedErrorPrint("Could not parse int port arg of follow command");
            }

        }


    }

}
