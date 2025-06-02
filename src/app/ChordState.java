package app;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import fault_tolerance.Heartbeat;
import mutex.suzuki_kasami.SuzukiKasamiMutex;
import servent.message.*;
import servent.message.fault_tolerance.BackupMessage;
import servent.message.fault_tolerance.RemoveFileFromBackupMessage;
import servent.message.mutex.PutUnlockMessage;
import servent.message.util.MessageUtil;

import javax.imageio.ImageIO;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * 
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public class ChordState {

	public static int CHORD_SIZE;
	public static int chordHash(int value) {
		return 61 * value % CHORD_SIZE;
	}
	
	private int chordLevel; //log_2(CHORD_SIZE)
	
	private ServentInfo[] successorTable; // samo oni nodeovi do kojih mozemo da dodjemo
	private ServentInfo predecessorInfo; // odakle smo dosli
	
	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;
	
	private Map<Integer, Map<String, ImageEntry>> valueMap;

	public SuzukiKasamiMutex mutex = new SuzukiKasamiMutex();

	public Heartbeat heartbeat = new Heartbeat();

	public Map<Integer, Object> pendingFollows = new ConcurrentHashMap<>();

	public Map<Integer, Object> followers = new ConcurrentHashMap<>();

	private AtomicBoolean isPublic = new AtomicBoolean(true);

	public Map<Integer, Map<String, ImageEntry>> predecessorBackup = new ConcurrentHashMap<>();
	public Map<Integer, Map<String, ImageEntry>> successorBackup = new ConcurrentHashMap<>();



	public ChordState() {
		this.chordLevel = 1;
		int tmp = CHORD_SIZE;
		while (tmp != 2) {
			if (tmp % 2 != 0) { //not a power of 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			this.chordLevel++;
		}
		
		successorTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}
		
		predecessorInfo = null;
		valueMap = new HashMap<>();
		allNodeInfo = new ArrayList<>();
	}
	
	/**
	 * This should be called once after we get <code>WELCOME</code> message.
	 * It sets up our initial value map and our first successor so we can send <code>UPDATE</code>.
	 * It also lets bootstrap know that we did not collide.
	 */
	public void init(WelcomeMessage welcomeMsg) {
		//set a temporary pointer to next node, for sending of update message
		successorTable[0] = new ServentInfo("localhost", welcomeMsg.getSenderPort());

		this.valueMap = welcomeMsg.getValues();
		
		//tell bootstrap this node is not a collider
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			
			bsWriter.flush();
			bsSocket.close();



		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getChordLevel() {
		return chordLevel;
	}
	
	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}
	
	public int getNextNodePort() {
		return successorTable[0].getListenerPort();
	}
	
	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}
	
	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public Map<Integer, Map<String, ImageEntry>> getValueMap() {
		return valueMap;
	}

	public void setValueMap(Map<Integer, Map<String, ImageEntry>> valueMap) {
		this.valueMap = valueMap;
	}

	public boolean isCollision(int chordId) {
		if (chordId == AppConfig.myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if we are the owner of the specified key.
	 */
	public boolean isKeyMine(int key) {
		if (predecessorInfo == null) {
			return true;
		}
		
		int predecessorChordId = predecessorInfo.getChordId();
		int myChordId = AppConfig.myServentInfo.getChordId();
		
		if (predecessorChordId < myChordId) { //no overflow
			if (key <= myChordId && key > predecessorChordId) {
				return true;
			}
		} else { //overflow
			if (key <= myChordId || key > predecessorChordId) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Main chord operation - find the nearest node to hop to to find a specific key.
	 * We have to take a value that is smaller than required to make sure we don't overshoot.
	 * We can only be certain we have found the required 1 when it is our first next node.
	 */
	public ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return AppConfig.myServentInfo;
		}
		
		//normally we start the search from our first successor
		int startInd = 0;
		
		//if the key is smaller than us, and we are not the owner,
		//then all nodes up to CHORD_SIZE will never be the owner,
		//so we start the search from the first item in our table after CHORD_SIZE
		//we know that such a node must exist, because otherwise we would own this key
		if (key < AppConfig.myServentInfo.getChordId()) {
			int skip = 1;
			while (successorTable[skip].getChordId() > successorTable[startInd].getChordId()) {
				startInd++;
				skip++;
			}
		}
		
		int previousId = successorTable[startInd].getChordId();
		
		for (int i = startInd + 1; i < successorTable.length; i++) {
			if (successorTable[i] == null) {
				AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
				break;
			}
			
			int successorId = successorTable[i].getChordId();
			
			if (successorId >= key) {
				return successorTable[i-1];
			}
			if (key > previousId && successorId < previousId) { //overflow
				return successorTable[i-1];
			}
			previousId = successorId;
		}
		//if we have only one node in all slots in the table, we might get here
		//then we can return any item
		return successorTable[0];
	}

	private void updateSuccessorTable() {
		//first node after me has to be successorTable[0]
		
		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTable[0] = currentNode;
		
		int currentIncrement = 2;
		
		ServentInfo previousNode = AppConfig.myServentInfo;
		
		//i is successorTable index
		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
			//we are looking for the node that has larger chordId than this
			int currentValue = (AppConfig.myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;
			
			int currentId = currentNode.getChordId();
			int previousId = previousNode.getChordId();
			
			//this loop needs to skip all nodes that have smaller chordId than currentValue
			while (true) {
				if (currentValue > currentId) {
					//before skipping, check for overflow
					if (currentId > previousId || currentValue < previousId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				} else { //node id is larger
					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
					int nextNodeId = nextNode.getChordId();
					//check for overflow
					if (nextNodeId < currentId && currentValue <= nextNodeId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				}
			}
		}

		AppConfig.timestampedStandardPrint("Predecessor: " + predecessorInfo);
		AppConfig.timestampedStandardPrint("Successor: " + successorTable[0]);
		
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 * 
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);
		
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}
			
		});
		
		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();
		
		int myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}
		
		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.getLast();
		} else {
			predecessorInfo = newList.getLast();
		}
		
		updateSuccessorTable();
	}

	public void removeNode(int nodeToRemove) {
		// ukloni iz allnodeinfo
//		allNodeInfo.removeIf(node -> node.getListenerPort() == nodeToRemove.getListenerPort());
		allNodeInfo.removeIf(node -> node.getChordId() == nodeToRemove);

		//region kopirano iz addNodes
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}
		});

		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();

		int myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}

		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);



		// namestanje predecessor info-a
		if (!newList2.isEmpty()) {
			predecessorInfo = newList2.getLast();
		} else if(!newList.isEmpty()) {
			predecessorInfo = newList.getLast();
		}
		//endregion

		// edge case kad smo jedini node u sistemu => gasi prede
		else {
			predecessorInfo = null;
			Arrays.fill(successorTable, 0, chordLevel, null);
			return;
		}

		// zbog reorganizacije dobicemo novog predecessora i successore pa treba resetovati vreme
		if(predecessorInfo.getChordId() == nodeToRemove){
			heartbeat.getPredecessorNodeHealthInfo().setTimestamp(System.currentTimeMillis());

		}
		else if (successorTable[0] == null || successorTable[0].getChordId() == nodeToRemove){
			heartbeat.getSuccessorNodeHealthInfo().setTimestamp(System.currentTimeMillis());
		}

		updateSuccessorTable();
	}

	/**
	 * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
	 */
	public void putValue(int key, String path, int storerPort) {

		if (isKeyMine(key)) {
			putIntoMap(key, path, storerPort);
			AppConfig.timestampedStandardPrint("Added: " + path + " at key " + key );

			// nasa slika => unlock
			if(storerPort == AppConfig.myServentInfo.getListenerPort()) {
				mutex.unlock();
			}
			// nije nasa slika => put unlock i to ga propagiraj da dodje najbrzim putem
			else{
				AppConfig.timestampedStandardPrint("Sent put unlock to " + storerPort);
				Message puMsg = new PutUnlockMessage(AppConfig.myServentInfo.getListenerPort(),
						storerPort);
				MessageUtil.sendMessage(puMsg);
			}
		} else {
			AppConfig.timestampedStandardPrint("Sent put to " + getNextNodeForKey(key) + " for " + path);
			ServentInfo nextNode = getNextNodeForKey(key);
			PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, path, storerPort);
			MessageUtil.sendMessage(pm);
		}
	}

	public boolean putIntoBuddyMap(ImageEntry imageEntry, int buddyPort) {
		int key = Math.abs(imageEntry.getPath().hashCode() % CHORD_SIZE);

        if(buddyPort == predecessorInfo.getListenerPort()){
            Map<String, ImageEntry> map = predecessorBackup.computeIfAbsent(key, k -> new HashMap<>());
            map.putIfAbsent(imageEntry.getPath(), imageEntry);
			AppConfig.timestampedStandardPrint("Added: " + imageEntry.getPath() + " to predecessorBackup");
            return true;
        }
        else if(successorTable[0] != null && buddyPort == successorTable[0].getListenerPort()){
            Map<String, ImageEntry> map = successorBackup.computeIfAbsent(key, k -> new HashMap<>());
            map.putIfAbsent(imageEntry.getPath(), imageEntry);
			AppConfig.timestampedStandardPrint("Added: " + imageEntry.getPath() + " to successorBackup");

            return true;
        }

        return false;
    }

	public ImageEntry putIntoMap(int key, String path, int storerId) {
		try{
			File imgFile = new File(AppConfig.ROOT_DIR + "/" + path);
			Image img = ImageIO.read(imgFile);

			Map<String, ImageEntry> map = valueMap.computeIfAbsent(key, k -> new HashMap<>());
			ImageEntry entry = new ImageEntry(path, storerId, img);
			map.putIfAbsent(path, entry);

			// posalji komsijama za backup
			sendPutToBackup(entry);

			return entry;

		} catch (IOException e) {
			e.printStackTrace();
            throw new RuntimeException(e);
        }

	}

	private void sendPutToBackup(ImageEntry imageEntry) {
		if(predecessorInfo != null) {
			Message buMsg = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
					predecessorInfo.getListenerPort(), imageEntry);
			MessageUtil.sendMessage(buMsg);
		}
		if (successorTable[0] != null) {
			BackupMessage buMsg = new BackupMessage(AppConfig.myServentInfo.getListenerPort(),
					successorTable[0].getListenerPort(), imageEntry);
			MessageUtil.sendMessage(buMsg);
		}
	}




	public void deleteValue(String path, int requestorPort){
		// uzmi key, path je value
		int key = Math.abs(path.hashCode() % CHORD_SIZE);

		// pokusaj da izbrises
		Map<String, ImageEntry> mapEntry = valueMap.get(key);
		if(mapEntry == null){
//			AppConfig.timestampedStandardPrint("Map Entry is not present! Don't have anything to delete");
			// posalji poruku da nisi uspeo da izbrises
			Message duMsg = new RemoveFileUnlockMessage(AppConfig.myServentInfo.getListenerPort(), requestorPort,path, false);
			MessageUtil.sendMessage(duMsg);
			return;
		}

		// brisi
		if(isKeyMine(key)){
			if(mapEntry.get(path) == null){
//				AppConfig.timestampedStandardPrint("Path is not present! Don't have anything to delete");
				Message duMsg = new RemoveFileUnlockMessage(AppConfig.myServentInfo.getListenerPort(), requestorPort, path, false);
				MessageUtil.sendMessage(duMsg);
				return;
			}
			ImageEntry removedImg = valueMap.get(key).remove(path);
			// izbrisi backup
			if(removedImg != null){
				sendRemoveFromBackup(key, path);
			}

			// mi smo trazili => ne moras poruku
			if(requestorPort == AppConfig.myServentInfo.getListenerPort()) {
				AppConfig.timestampedStandardPrint("Deleted: " + path + " at key " + key);
				mutex.unlock();

			}
			// inace moras
			else{
				Message duMsg = new RemoveFileUnlockMessage(AppConfig.myServentInfo.getListenerPort(), requestorPort, path, true);
				MessageUtil.sendMessage(duMsg);
			}
		}
		// salji dalje
		else{
			ServentInfo nextNode = getNextNodeForKey(key);
			Message removeFileMessage = new RemoveFileMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(),
					path, requestorPort);
			MessageUtil.sendMessage(removeFileMessage);
		}
	}

	public void removeFromBackup(int key, String value, int buddyPort) {
		if(predecessorInfo.getListenerPort() == buddyPort){
			Map<String, ImageEntry> mapEntry = predecessorBackup.get(key);
			if(mapEntry != null && isKeyMine(key) && mapEntry.get(value) != null){
				valueMap.get(key).remove(value);
			}
			AppConfig.timestampedStandardPrint("Removed: " + value + " at key " + key + " from backup of predecessor " + buddyPort);
		}

		if(successorTable[0] != null && successorTable[0].getListenerPort() == buddyPort){
			Map<String, ImageEntry> mapEntry = predecessorBackup.get(key);
			if(mapEntry != null && isKeyMine(key) && mapEntry.get(value) != null){
				valueMap.get(key).remove(value);
			}
			AppConfig.timestampedStandardPrint("Removed: " + value + " at key " + key + " from backup of successor " + buddyPort);
		}
	}


	private void sendRemoveFromBackup(int key, String value) {
		if(predecessorInfo != null) {
			Message dbMsg = new RemoveFileFromBackupMessage(AppConfig.myServentInfo.getListenerPort(),
					predecessorInfo.getListenerPort(), key, value, AppConfig.myServentInfo.getListenerPort());
			MessageUtil.sendMessage(dbMsg);
		}
		if (successorTable[0] != null) {
			Message dbMsg = new RemoveFileFromBackupMessage(AppConfig.myServentInfo.getListenerPort(),
					successorTable[0].getListenerPort(), key, value, AppConfig.myServentInfo.getListenerPort());
			MessageUtil.sendMessage(dbMsg);
		}
	}


	public Integer getPortOfNode(int nodeId){
		for (ServentInfo info : allNodeInfo) {
			if (info.getChordId() == nodeId)
				return info.getListenerPort();
		}
		return null;
	}

	public List<ServentInfo> getAllNodeInfo() {
		return allNodeInfo;
	}

	public boolean isPublic(){
		return isPublic.get();
	}

	public void setPublic(boolean isPublic){
		AppConfig.timestampedStandardPrint("Public set to: " + isPublic);
		this.isPublic.set(isPublic);
	}



}
