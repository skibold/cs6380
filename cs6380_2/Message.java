import java.lang.String;

public class Message {
	private static String classname = Message.class.getName();

	// message types
	private static String INIT = "init";
	private static String ACCEPT = "accept";
	private static String REJECT = "reject";
	private static String TEST = "test";
	private static String REPORT = "report";
	private static String CONNECT = "connect"; // message across mwoe to neighbor cluster
	private static String CHROOT = "chroot"; // message from cluster leader to node with outgoing mwoe

	private String type; // must be one of the above enums
	private int originator; // the process id which sent this message, set this for explore messages only
	private int recipient; // the process this message is sent to
	private String cid; // component id of sender
	private int level; // level of sending component
	private Edge mwoe;
	private int delay;
	private String tostr;

	// private constructor, clients should use public static "factory" methods
	private Message(String t, int o, int r, int l, String cid, Edge mwoe) {
		this.type = t;
		this.originator = o;
		this.recipient = r;
		this.level = l;
		this.cid = cid;
		this.mwoe = mwoe;
		this.delay = 0;
		this.tostr = "{type: " + type + 
				", originator: " + originator + 
				", recipient: " + recipient +
				", level: " + level +
				", cid: " + cid +
				", mwoe: " + (mwoe==null?"null":mwoe.toString());
	}

	public Message(Message m) {
		this.type = m.type();
		this.originator = m.originator();
		this.recipient = m.recipient();
		this.level = m.level();
		this.cid = m.cid();
		this.mwoe = m.mwoe();
		this.delay = m.getDelay();
		this.tostr = "{type: " + type + 
				", originator: " + originator + 
				", recipient: " + recipient +
				", level: " + level +
				", cid: " + cid +
				", mwoe: " + (mwoe==null?"null":mwoe.toString());
	}

	// factory methods to make construction easy
	public static Message init(int o, int r, int l, String cid) {
		return new Message(INIT, o, r, l, cid, null);
	}
	public static Message test(int o, int r, int l, String cid) {
		return new Message(TEST, o, r, l, cid, null);
	}
	public static Message accept(Message m) {
		return new Message(ACCEPT, m.recipient(), m.originator(), -1, null, null);
	}
	public static Message reject(Message m) {
		return new Message(REJECT, m.recipient(), m.originator(), -1, null, null);
	}
	public static Message report(int o, int r, Edge mwoe) {
		return new Message(REPORT, o, r, -1, null, mwoe);
	}
	public static Message connect(int o, int r, int l, String cid) {
		return new Message(CONNECT, o, r, l, cid, null);
	}
	public static Message chroot(int o, int l, String cid, Edge mwoe) {
		return new Message(CHROOT, o, -1, l, cid, mwoe);
	}

	// public getters
	public boolean isInit() {
		return INIT.equals(type);
	}
	public boolean isTest() {
		return TEST.equals(type);
	}
	public boolean isAccept() {
		return ACCEPT.equals(type);
	}
	public boolean isReject() {
		return REJECT.equals(type);
	}
	public boolean isReport() {
		return REPORT.equals(type);
	}
	public boolean isConnect() {
		return CONNECT.equals(type);
	}
	public boolean isChroot() {
		return CHROOT.equals(type);
	}

	public int originator() {
		return originator;
	}
	public int recipient() {
		return recipient;
	}
	public String type() {
		return type;
	}
	public int level() {
		return level;
	}
	public String cid() {
		return cid;
	}
	public Edge mwoe() {
		return mwoe;
	}
	public void setDelay(int d) {
		this.delay = d;
	}
	public int getDelay() {
		return this.delay;
	}

	public static Message cloneForNewRecipient(Message m, int r) {
		return new Message(m.type(), m.originator(), r, m.level(), m.cid(), m.mwoe());
	}
	public String toString() {
		return tostr + ", delay: " + delay + "}";
	}
}
