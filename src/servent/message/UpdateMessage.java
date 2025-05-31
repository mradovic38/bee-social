package servent.message;

import java.util.List;
import java.util.Map;

public class UpdateMessage extends BasicMessage {

	private static final long serialVersionUID = 3586102505319194978L;

	private final Map<Integer, Integer> files;
	private final List<Integer> ports;
	private final List<Integer> rns;

	public UpdateMessage(int senderPort, int receiverPort, Map<Integer, Integer> files, List<Integer> ports, List<Integer> rns) {
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

	public Map<Integer, Integer> getFiles() {
		return files;


	}
}
