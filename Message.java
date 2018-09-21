import java.lang.String;

public class Message {
	private static String classname = Message.class.getName();

	// message types
	private static String EXP = "explore";
	private static String ACK = "ack";
	private static String NACK = "nack";
	private static String LEAD = "leader";

	private String type; // must be one of EXP, ACK, NACK, LEAD
	private int originator; // the process id which sent this message, set this for explore messages only
	private int recipient; // the process this message is sent to

	// private constructor, clients should use public static "factory" methods
	private Message(String t, int o, int r) {
		this.type = t;
		this.originator = o;
		this.recipient = r;
	}
	
	// factory methods to make construction easy
	public static Message explore(int o, int r) {
		return new Message(EXP, o, r);
	}
	public static Message ack(Message m) {
		return new Message(ACK, m.originator(), m.recipient());
	}
	public static Message nack(Message m) {
		return new Message(NACK, m.originator(), m.recipient());
	}
	public static Message lead(int o, int r) {
		return new Message(LEAD, o, r);
	}

	// public getters
	public boolean isExplore() {
		return EXP.equals(type);
	}
	public boolean isAck() {
		return ACK.equals(type);
	}
	public boolean isNack() {
		return NACK.equals(type);
	}
	public boolean isLeader() {
		return LEAD.equals(type);
	}
	public int originator() {
		return originator();
	}
	public int recipient() {
		return recipient;
	}
	public String type() {
		return type;
	}

	// other utils
	public boolean compare(Message m) {
		// if originators and recipients match then this.isExplore and (m.isAck || m.isNack) to this
		return m.originator() == this.originator && m.recipient() == this.recipient;
	}
	public Message cloneForNewRecipient(Message m, int r) {
		return new Message(m.type(), m.originator(), r);
	}
}
