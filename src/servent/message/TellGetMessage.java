package servent.message;

import app.ImageEntry;

import java.util.Map;

public class TellGetMessage extends BasicMessage {

	private static final long serialVersionUID = -6213394344524749872L;

	private final Map<Integer, Map<String, ImageEntry>> values;

	private final boolean isPrivate;

	public TellGetMessage(int senderPort, int receiverPort) {
		super(MessageType.TELL_GET, senderPort, receiverPort);
		this.isPrivate = true;
		this.values = null;
	}

	public TellGetMessage(int senderPort, int receiverPort, Map<Integer, Map<String, ImageEntry>> values) {
		super(MessageType.TELL_GET, senderPort, receiverPort);
		this.isPrivate = false;
		this.values = values;
	}

	public Map<Integer, Map<String, ImageEntry>> getValues() {
		return this.values;
	}

	public boolean isPrivate() {
		return isPrivate;
	}
}
