package servent.message;

import app.ImageEntry;

import java.util.List;
import java.util.Map;

public class UpdateMessage extends BasicMessage {

	private static final long serialVersionUID = 3586102505319194978L;

	private final  Map<Integer, Map<String, ImageEntry>> files;
	private final List<Integer> ports;
	private final List<Integer> rns;

	public UpdateMessage(int senderPort, int receiverPort, Map<Integer, Map<String, ImageEntry>> files, List<Integer> ports, List<Integer> rns) {
		super(MessageType.UPDATE, senderPort, receiverPort);
		this.files = files;
		this.ports = ports;
		this.rns = rns;
	}

	public List<Integer> getPorts() {
		return ports;
	}

	public List<Integer> getRns() {
		return rns;
	}

	public  Map<Integer, Map<String, ImageEntry>> getFiles() {
		return files;


	}
}
