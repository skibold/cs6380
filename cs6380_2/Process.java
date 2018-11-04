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
	private boolean init, term;
	private int uid, round;
	private Edge link2parent;
	private ArrayList<Integer> links2children;
	private HashMap<Integer,Edge> edgeMap; // map of recipient process id : edge between me and recipient
	private HashMap<Integer, Message> awaitingResponse; // I am waiting for ack/nack
	private HashMap<Integer, Message> pendingResponse; // I must respond to the senders with ack/nack

	public Process(int uid, ArrayList<Edge> edges) {
		this.uid = uid;
		this.round = 0;
		instancename = classname + "(" + uid + ")";
		this.edgeMap = new HashMap<Integer, Edge>();
		this.logEdges = "{";
		for(Edge e : edges) {
			logEdges += e.toString() + ", ";
			edgeMap.put(e.otherSide(uid), e);
		}
		this.logEdges += "}";
		this.awaitingResponse = new HashMap<Integer, Message>();
		this.pendingResponse = new HashMap<Integer, Message>();
		this.links2children = new ArrayList<Integer>();
		this.link2parent = null;
		this.init = true;
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
		if(init) {
			Logger.normal(instancename, method, "Init step, send dummy on all edges: " + logEdges);
			init = false;
			for(Integer recipient : edgeMap.keySet()) {
				Message dummy = Message.dummy(uid, recipient);
				awaitingResponse.put(recipient, dummy);
				edgeMap.get(recipient).send(uid, dummy);
			}
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
			if(m.isDummy()) {
				pendingResponse.put(sender,m);
			} else {
				responseFrom(sender);
			}
		}

		// send acks back
		ArrayList<Integer> toDelete = new ArrayList<Integer>(); // avoid concurrent modification
		for(Integer sender : pendingResponse.keySet()) {
			Message m = pendingResponse.get(sender);
			Message ack = Message.ack(m);
			Logger.normal(instancename, method, "Send " + ack + " to " + sender);
			edgeMap.get(sender).send(uid, ack);
			toDelete.add(sender);
		}
		for(Integer sender : toDelete) {
			pendingResponse.remove(sender);
		}

		// terminate if i got all my acks
		if(awaitingResponse.isEmpty()) {
			Logger.normal(instancename, method, "I have all my responses.");
			term = true;
		}

		
	}

	//override
	public long getId() {
		return uid;
	}

	public boolean isTerminated() {
		return term;
	}

	/*public boolean isLeader() {
		return term && largestId == uid;
	}*/

	public void reset() {
		this.round = 0;
		this.awaitingResponse.clear();
		this.pendingResponse.clear();
		this.links2children.clear();
		this.link2parent = null;
		this.init = true;
		this.term = false;
	}

	public String children() {
		String children = new String();
		if(links2children.isEmpty()) return children;
		for(Integer i : links2children) {
			children += i.toString() + ", ";
		}
		return children.substring(0,children.length()-2);
	}

	public String parent() {
		if(link2parent == null) return "None";
		return Integer.toString(link2parent.otherSide(uid));
	}

	private void responseFrom(int sender) {
		// was I waiting for a response from this other process?
		// if so, now I have it, so delete the waiting flag
		if(awaitingResponse.containsKey(sender)) {
			Logger.debug(instancename, "responseFrom", "Delete awaitingResponse(" + sender + ", " + awaitingResponse.get(sender) + ")");
			awaitingResponse.remove(sender);
		}
	}
}
