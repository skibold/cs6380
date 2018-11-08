import java.lang.String;
import java.lang.Thread;
import java.lang.Runnable;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Process implements Runnable { //extends Thread {
	private static final String classname = Process.class.getName();

	private Thread t;
	private String instancename;
	private String logEdges;
	private String cid;
	private boolean init, term;
	private int uid, round, level;
	private Edge link2parent;
	private Edge mwoe;
	private Integer connectTrail;
	private HashMap<Integer, Edge> edgeMap; // hold all edges
	private ArrayList<Edge> links2children; // might be the same as component edges
	private ArrayList<Edge> componentEdges; // edges to other processes in my component; these edges are part of the MST
	private ArrayList<Edge> outsideEdges; // edges which might be outside my component, initially all incident edges
	private ArrayList<Edge> rejectedEdges; // edges which lead to process in my component, but are not part of the MST
	private ArrayList<Message> cCastReports; // report messages from children
	private Integer awaitingResponse; // I am waiting for accept/reject from process with this uid
	private HashMap<Integer, Message> pendingResponse; // I must respond to the senders with ack/nack
	
	public Process(int uid, ArrayList<Edge> edges) {
		this.uid = uid;
		this.cid = null;
		this.level = 0;
		this.round = 0;
		instancename = classname + "(" + uid + ")";
		this.cCastReports = new ArrayList<Message>();
		this.outsideEdges = new ArrayList<Edge>();
		this.componentEdges = new ArrayList<Edge>();
		this.links2children = new ArrayList<Edge>(); // maybe the same as component edges
		this.rejectedEdges = new ArrayList<Edge>();
		this.edgeMap = new HashMap<Integer, Edge>();
		this.logEdges = "{";
		for(Edge e : edges) {
			logEdges += e.toString() + ", ";
			insertionSort(outsideEdges, e);
			edgeMap.put(e.otherSide(uid), e);
		}
		this.logEdges += "}";
		this.awaitingResponse = null;
		this.link2parent = null;
		this.mwoe = null;
		this.connectTrail = null;
		this.term = false;

		Logger.normal(classname, "Process", "created process " + uid + " with edges " + logEdges);
	}

	public void start() {
		final String method = "start";
		Logger.normal(instancename, method, "step " + (round++));
		t = new Thread(this, String.valueOf(uid));
		t.start();
	}

	public void join() throws InterruptedException {
		if(t != null) t.join();
		Logger.normal(instancename, "finish", "step " + (round-1));
	}

	//override
	public void run() {
		final String method = "run";
		Logger.entering(instancename, method);
		if(level == 0) { // begin by sending init to myself
			Message init = Message.init(uid, uid, level, cid, null);
			this.receiveInitMsg(init);
			return;
		}

		// receive all messages on my incident links
		for(Edge e : edgeMap.values()) {
			int sender = e.otherSide(uid);
			Message m = e.poll(uid); // receive a message from process at other end of Edge e
			if(m == null) {
				Logger.normal(instancename, method, "No message from " + sender + " at step  " + (round-1));
				continue;
			}
			Logger.normal(instancename, method, "Received " + m + " from " + sender + " at step " + (round-1));

			if(m.isInit()) {
				link2parent = e;
				receiveInitMsg(m);
			} else if(m.isReport()) {
				receiveReportMsg(m);
			} else if(m.isTest()) {
				receiveTestMsg(m);
			} else if(m.isAccept()) {
				receiveAcceptMsg(m);
			} else if(m.isReject()) {
				receiveRejectMsg(m);
			} else if(m.isConnect()) {
				receiveConnectMsg(m);
			}
		}
	}

/*** begin functions to handle receiving different message types ***/

	// INIT message
	private boolean sendInitMsg() {
		final String method = "sendInitMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveInitMsg(Message m) {
		final String method = "receiveInitMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
	}

	// REPORT message
	private boolean sendReportMsg() {
		final String method = "sendReportMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveReportMsg(Message m) {
		final String method = "receiveReportMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
	}

	// TEST message
	private boolean sendTestMsg(int i) {
		final String method = "sendTestMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveTestMsg(Message m) {
		final String method = "receiveTestMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
	}

	// ACCEPT message
	private boolean sendAcceptMsg() {
		final String method = "sendAcceptMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveAcceptMsg(Message m) {
		final String method = "receiveAcceptMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
	}

	// REJECT message
	private boolean sendRejectMsg() {
		final String method = "sendRejectMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveRejectMsg(Message m) {
		final String method = "receiveRejectMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);		
	}

	// CONNECT message
	private boolean sendConnectMsg() {
		final String method = "sendConnectMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveConnectMsg(Message m) {
		final String method = "receiveConnectMsg";
		Logger.entering(instancename, method);

		Logger.exiting(instancename, method);
	}
/*** end functions to handle messages ***/

	private void insertionSort(ArrayList<Edge> collection, Edge e) {
		if(e == null) return;
		int i;
		for(i=0; i<collection.size(); i++) {
			if(collection.get(i).compare(e) == 1) { // e is smaller than element at i
				break;
			}
		}
		if(i < collection.size()) {
			collection.add(i,e);
		} else {
			collection.add(e);
		}
	}

	//override
	public long getId() {
		return uid;
	}

	public boolean isTerminated() {
		return term;
	}
}
