package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.message.NewNodeMessage;
import servent.message.util.MessageUtil;

public class ServentInitializer implements Runnable {
	private Set<Integer> broadcastPorts = new HashSet<>();

	private int getSomeServentPort() {
		int bsPort = AppConfig.BOOTSTRAP_PORT;
		
		int retVal = -2;
		
		try {
			Socket bsSocket = new Socket("localhost", bsPort);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("Hail\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			bsWriter.flush();

			Scanner bsScanner = new Scanner(bsSocket.getInputStream());
			String data = bsScanner.nextLine();

			if(!data.contains(":")){
				retVal = Integer.parseInt(data);
			}
			else {

				// split retval and list of ports
				String[] fistSplit = data.split(":");
				retVal = Integer.parseInt(fistSplit[0]);

				// node-ovi (portovi) su razdvojeni sa '|'
				String[] nodes = fistSplit[1].split("\\|");

				for (String nodeStr : nodes) {
					broadcastPorts.add(Integer.parseInt(nodeStr));
				}
			}


			
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	@Override
	public void run() {
		int someServentPort = getSomeServentPort();
		
		if (someServentPort == -2) {
			AppConfig.timestampedErrorPrint("Error in contacting bootstrap. Exiting...");
			System.exit(0);
		}
		if (someServentPort == -1) { //bootstrap gave us -1 -> we are first
			AppConfig.timestampedStandardPrint("First node in Chord system.");

			// Inicijalizuj suzuki kasami token
			AppConfig.chordState.mutex.setToken(new SuzukiKasamiToken());

		} else { //bootstrap gave us something else - let that node tell our successor that we are here

			// novi node -> lock
			AppConfig.timestampedStandardPrint("Waiting for token...");
			AppConfig.chordState.mutex.lock(broadcastPorts);
			AppConfig.timestampedStandardPrint("Got token");

			NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo.getListenerPort(), someServentPort);
			MessageUtil.sendMessage(nnm);
		}
	}

}
