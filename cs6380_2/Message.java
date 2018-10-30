import java.lang.String;

public class Message {
	private static String classname = Message.class.getName();

	// message types
/* TODO replace these with new stuff applicable to GHS algorithm
	private static String EXP = "explore";
	private static String ACK = "ack";
	private static String NACK = "nack";
	private static String LEAD = "leader";
*/

	private String type; // must be one of TODO comment the new enums here
	private int originator; // the process id which sent this message, set this for explore messages only
	private int recipient; // the process this message is sent to
	private int delay;
	private String tostr;

	// private constructor, clients should use public static "factory" methods
	private Message(String t, int o, int r) {
		this.type = t;
		this.originator = o;
		this.recipient = r;
		this.delay = 0;
		this.tostr = "{type: " + type + 
				", originator: " + originator + 
				", recipient: " + recipient + 
				", delay: " + delay +"}";
	}
/* TODO replace all these with new stuff applicable to GHS algorithm
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
*/
	public int originator() {
		return originator;
	}
	public int recipient() {
		return recipient;
	}
	public String type() {
		return type;
	}

	public void setDelay(int d) {
		this.delay = d;
	}

	public int getDelay() {
		return this.delay;
	}

	// other utils
//	public boolean isResponseTo(Message m) {
//		// if originators and recipients match then m.isExplore and (this.isAck || this.isNack) to m
//		return m.isExplore() && (this.isAck() || this.isNack()) && 
//			m.originator() == this.originator && m.recipient() == this.recipient;
//	}
	public static Message cloneForNewRecipient(Message m, int r) {
		return new Message(m.type(), m.originator(), r);
	}
	public String toString() {
		return tostr;
	}
}
