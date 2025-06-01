package servent.message.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or somesuch.
 * 
 * @author bmilojkovic
 *
 */
public class DelayedMessageSender implements Runnable {

	private Message messageToSend;
	
	public DelayedMessageSender(Message messageToSend) {
		this.messageToSend = messageToSend;
	}
	
	public void run() {
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 50) + 50);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		if (MessageUtil.MESSAGE_UTIL_PRINTING && messageToSend != null && messageToSend.getMessageType() != MessageType.PING && messageToSend.getMessageType() != MessageType.PONG) {
			AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
		}
		
		try {
			Socket sendSocket;
			if(messageToSend instanceof BasicMessage && ((BasicMessage)messageToSend).getNextReceiver() != null){
				sendSocket = new Socket(messageToSend.getReceiverIpAddress(), ((BasicMessage) messageToSend).getNextReceiver().getListenerPort());
			}
			else {
				sendSocket = new Socket(messageToSend.getReceiverIpAddress(), messageToSend.getReceiverPort());
			}
			
			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			oos.writeObject(messageToSend);
			oos.flush();
			
			sendSocket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString());
			e.printStackTrace();
		}
	}
	
}
