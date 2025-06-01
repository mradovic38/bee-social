package servent.message;

import app.ImageEntry;

import java.util.Map;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private Map<Integer, Map<String, ImageEntry>> values;
	
	public WelcomeMessage(int senderPort, int receiverPort, Map<Integer, Map<String, ImageEntry>> values) {
		super(MessageType.WELCOME, senderPort, receiverPort);
		
		this.values = values;
	}
	
	public Map<Integer, Map<String, ImageEntry>> getValues() {
		return values;
	}
}
