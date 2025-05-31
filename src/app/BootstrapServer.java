package app;

import mutex.suzuki_kasami.SuzukiKasamiToken;
import servent.message.Message;
import servent.message.mutex.SuzukiKasamiRequestTokenMessage;
import servent.message.mutex.SuzukiKasamiSendTokenMessage;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BootstrapServer {

	private volatile boolean working = true;
	private final List<Integer> activeServents;


	// Separate lock for CLI synchronization
	private final Object cliLock = new Object();
	private boolean cliReady = false;

	private final AtomicBoolean joinLock = new AtomicBoolean(false);

	private class CLIWorker implements Runnable {
		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);

			// Signal that CLI is ready
			synchronized (cliLock) {
				cliReady = true;
				cliLock.notifyAll();
			}

			try {
				while (working) {
					if (!sc.hasNextLine()) {
						System.out.println("No more input. Exiting CLIWorker.");
						break;
					}

					String line = sc.nextLine();
					System.out.println("LINE: " + line);

					if (line.equals("stop")) {
						working = false;
						break;
					}
				}
			} finally {
				sc.close();
			}
		}
	}

	public BootstrapServer() {
		// Use thread-safe list, but still need external synchronization for complex operations
		activeServents = new CopyOnWriteArrayList<>();
	}

	public void doBootstrap(int bsPort) {
		Thread cliThread = new Thread(new CLIWorker());
		cliThread.start();

		// Wait for CLI to be ready
		synchronized (cliLock) {
			while (!cliReady) {
				try {
					cliLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(bsPort);
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e1) {
			AppConfig.timestampedErrorPrint("Problem while opening listener socket.");
			System.exit(0);
		}

		Random rand = new Random(System.currentTimeMillis());

		while (working) {
			try {
				Socket newServentSocket = listenerSocket.accept();

				/*
				 * Handling these messages is intentionally sequential, to avoid problems with
				 * concurrent initial starts.
				 *
				 * In practice, we would have an always-active backbone of servents to avoid this problem.
				 */

				Scanner socketScanner = new Scanner(newServentSocket.getInputStream());
				String message = socketScanner.nextLine();

				/*
				 * New servent has hailed us. He is sending us his own listener port.
				 * He wants to get a listener port from a random active servent,
				 * or -1 if he is the first one.
				 */
				if (message.equals("Hail")) {
					int newServentPort = socketScanner.nextInt();
//					AppConfig.timestampedStandardPrint("got " + newServentPort);

					PrintWriter socketWriter = new PrintWriter(newServentSocket.getOutputStream());

					// za sinhronizaciju
					boolean canJoin = joinLock.compareAndSet(false, true);


					if(!canJoin) {
						// -3 da bi on procitao da treba da ceka
						StringBuilder msgBuilder = new StringBuilder().append("-3").append(":");
						for (Integer s : activeServents) {
							msgBuilder.append(s);
							msgBuilder.append("|");
						}
						// brise poslednji | i dodaje newline
						String msg = msgBuilder.substring(0, msgBuilder.length() - 1) + ("\n");
						socketWriter.write(msg);
					}
					else {

						// First node
						if (activeServents.isEmpty()) {
							socketWriter.write(String.valueOf(-1) + "\n");

							activeServents.add(newServentPort); // first one doesn't need to confirm
							AppConfig.timestampedStandardPrint("Added first servent: " + newServentPort);

						}
						// neki drugi node
						else {
							int randServent = activeServents.get(rand.nextInt(activeServents.size()));

							StringBuilder msgBuilder = new StringBuilder().append(randServent).append(":");
							for (Integer s : activeServents) {
								msgBuilder.append(s);
								msgBuilder.append("|");
							}
							// brise poslednji | i dodaje newline
							String msg = msgBuilder.substring(0, msgBuilder.length() - 1) + ("\n");
							socketWriter.write(msg);

							AppConfig.timestampedStandardPrint("join lock acquired for node on port: " + newServentPort);
						}


					}



					socketWriter.flush();
					newServentSocket.close();

				}
				else {
					if (message.equals("New")) {
						/**
						 * When a servent is confirmed not to be a collider, we add him to the list.
						 */
						int newServentPort = socketScanner.nextInt();

						if (!activeServents.contains(newServentPort)) {
							activeServents.add(newServentPort);
						}
						AppConfig.timestampedStandardPrint("Added " + newServentPort);

						newServentSocket.close();


					} else if (message.equals("Quit")) {
						/*
						 * Cvor izlazi iz sistema -
						 * Javiti bootstrap-u da je neki cvor napustio sistem, bootstrap ga ukloni iz spiska aktivnih
						 */
						int quitterPort = socketScanner.nextInt();
						AppConfig.timestampedStandardPrint("Quitting " + quitterPort);

						activeServents.remove(Integer.valueOf(quitterPort));


						newServentSocket.close();

					}
					AppConfig.timestampedStandardPrint("join lock released");


					joinLock.set(false);
				}

			} catch (SocketTimeoutException e) {
				// Expected timeout, continue loop
			} catch (IOException e) {
				if (working) { // Only print error if we're still supposed to be working
					e.printStackTrace();
				}
			}
		}

		// Clean shutdown
		try {
			if (listenerSocket != null && !listenerSocket.isClosed()) {
				listenerSocket.close();
			}
		} catch (IOException e) {
			System.err.println("Error closing listener socket: " + e.getMessage());
		}
	}

	/**
	 * Expects one command line argument - the port to listen on.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			AppConfig.timestampedErrorPrint("Bootstrap started without port argument.");
		}

		int bsPort = 0;
		try {
			bsPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Bootstrap port not valid: " + args[0]);
			System.exit(0);
		}

		AppConfig.timestampedStandardPrint("Bootstrap server started on port: " + bsPort);

		BootstrapServer bs = new BootstrapServer();
		bs.doBootstrap(bsPort);
	}
}