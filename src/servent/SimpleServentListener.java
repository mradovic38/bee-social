package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import servent.handler.*;
import servent.handler.fault_tolerance.*;
import servent.handler.follow.FollowAcceptHandler;
import servent.handler.follow.FollowRequestHandler;
import servent.handler.mutex.PutUnlockHandler;
import servent.handler.mutex.SuzukiKasamiRequestTokenHandler;
import servent.handler.mutex.SuzukiKasamiSendTokenHandler;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	public SimpleServentListener() {
		
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				Socket clientSocket = listenerSocket.accept();
				
				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */



				switch (clientMessage.getMessageType()) {
					case TOKEN_REQUEST:
						messageHandler = new SuzukiKasamiRequestTokenHandler(clientMessage);
						break;
					case TOKEN_SEND:
						messageHandler = new SuzukiKasamiSendTokenHandler(clientMessage);
						break;
					case QUIT:
						messageHandler = new QuitHandler(clientMessage);
						break;
					case PUT_UNLOCK:
						messageHandler = new PutUnlockHandler(clientMessage);
						break;
					case CONFIRM_QUIT:
						messageHandler = new ConfirmQuitHandler(clientMessage);
						break;

					case PING:
						messageHandler = new PingHandler(clientMessage);
						break;
					case PONG:
						messageHandler = new PongHandler(clientMessage);
						break;
					case UPDATE_AFTER_DEATH:
						messageHandler = new UpdateAfterDeathHandler(clientMessage);
						break;
					case SUS_ASK:
						messageHandler = new SusAskHandler(clientMessage);
						break;
					case SUS_PING:
						messageHandler = new SusPingHandler(clientMessage);
						break;
					case SUS_PONG:
						messageHandler = new SusPongHandler(clientMessage);
						break;
					case ASK_HAS_TOKEN:
						messageHandler = new AskHasTokenHandler(clientMessage);
						break;
					case TELL_HAS_TOKEN:
						messageHandler = new TellHasTokenHandler(clientMessage);
						break;

					case FOLLOW_REQ:
						messageHandler = new FollowRequestHandler(clientMessage);
						break;
					case FOLLOW_ACC:
						messageHandler = new FollowAcceptHandler(clientMessage);
						break;
					case REMOVE_FILE:
						messageHandler = new RemoveFileHandler(clientMessage);
						break;
					case REMOVE_FILE_UNLOCK:
						messageHandler = new RemoveFileUnlockHandler(clientMessage);
						break;
					case REMOVE_FROM_BACKUP:
						messageHandler = new RemoveFileFromBackupHandler(clientMessage);
						break;
					case BACKUP:
						messageHandler = new BackupHandler(clientMessage);
						break;

					case NEW_NODE:
						messageHandler = new NewNodeHandler(clientMessage);
						break;
					case WELCOME:
						messageHandler = new WelcomeHandler(clientMessage);
						break;
					case SORRY:
						messageHandler = new SorryHandler(clientMessage);
						break;
					case UPDATE:
						messageHandler = new UpdateHandler(clientMessage);
						break;
					case PUT:
						messageHandler = new PutHandler(clientMessage);
						break;
					case ASK_GET:
						messageHandler = new AskGetHandler(clientMessage);
						break;
					case TELL_GET:
						messageHandler = new TellGetHandler(clientMessage);
						break;
					case POISON:
						break;

				}




				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
