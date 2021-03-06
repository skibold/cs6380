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
	private int uid, largestId, round;
	private Edge link2parent;
	private Message bestExplore;
	private ArrayList<Integer> links2children;
	private HashMap<Integer,Edge> edgeMap; // map of recipient process id : edge between me and recipient
	private HashMap<Integer, Message> awaitingResponse; // I am waiting for ack/nack
	private HashMap<Integer, Message> pendingResponse; // I must respond to the senders with ack/nack

	public Process(int uid, ArrayList<Edge> edges) {
		this.uid = uid;
		this.round = 0;
		instancename = classname + "(" + uid + ")";
		this.largestId = uid;
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
		Logger.normal(instancename, method, "round " + (round++));
		t = new Thread(this, String.valueOf(uid));
		t.start();
	}

	public void join() throws InterruptedException {
		if(t != null) t.join();
		Logger.normal(instancename, "finish", "round " + (round-1));
	}

	//override
	public void run() {
		final String method = "run";
		if(init) {
			Logger.normal(instancename, method, "First round, send explore on all edges: " + logEdges);
			init = false;
			for(Integer recipient : edgeMap.keySet()) {
				Message explore = Message.explore(uid, recipient);
				awaitingResponse.put(recipient, explore);
				edgeMap.get(recipient).send(uid, explore);
			}
			return;
		}

		// receive all messages sent to me last round
		Message explore2forward = null;
		Message leader2forward = null;
		for(Edge e : edgeMap.values()) {
			int sender = e.otherSide(uid);
			Message m = e.poll(uid); // receive a message from process at other end of Edge e
			if(m == null) {
				Logger.normal(instancename, method, "No message from " + sender);
				continue;
			}
			Logger.normal(instancename, method, "Received " + m + " from " + sender);			

			if(term || leader2forward != null) continue; // do nothing, but drain queues on all other edges
			
			if(m.isLeader()) {
				leader2forward = m;
				Logger.normal(instancename, method, "Just found out who the leader is");
				continue; // drain queues on all other edges
			}
			else if(m.isExplore()) {
				// add m to list of messages i have received which i must respond to
				pendingResponse.put(sender, m);
				Logger.debug(instancename, method, "I must respond to " + sender);
				// remember the largest
				if(m.originator() > largestId) {
					largestId = m.originator();
					link2parent = e;
					bestExplore = m;
					explore2forward = m;
				}
				if(m.originator() >= largestId) {
					// I might've been waiting for a response from this sender, 
					// but he just sent me an even better leader candidate, 	
					// so delete the awaitingResponse flag if there was one.  
					// I might get a nack later from this sender, but it won't matter.
					responseFrom(sender);
				}
			}
			else {
				if(m.isAck() && !links2children.contains(e.otherSide(uid))) {
					// if this is an ack, then make the sender my child
					links2children.add(e.otherSide(uid));
					Logger.debug(instancename, method, sender + " sent me an ACK, he is my newest child.");
				}
				responseFrom(sender);
			}
		}

		if(term) { Logger.normal(instancename, method, "already terminated"); return; }

		// send out all messages
		// forward the leader message if it exists and terminate
		if(leader2forward != null) {
			largestId = leader2forward.originator();
			for(Integer i : links2children) {
				Edge e = edgeMap.get(i);
				Message leaderClone = Message.cloneForNewRecipient(leader2forward, e.otherSide(uid));
				Logger.normal(instancename, method, "Forward " + leaderClone);
				e.send(uid, leaderClone);
			}
			term = true;
			return;
		}

		// forward the explore message if I have one
		if(explore2forward != null) {
			Logger.normal(instancename, method, 
				largestId + " is the largest I've seen. Forward to everyone. Set new parent = " + link2parent.otherSide(uid));
			for(Integer recipient : edgeMap.keySet()) {
				Message m = pendingResponse.get(recipient); // might be a message I receive this round, but what I have to forward is better
				if(m == null || m.originator() != explore2forward.originator()) {
					Edge e = edgeMap.get(recipient);
					Message exploreClone = Message.cloneForNewRecipient(explore2forward, recipient);
					Logger.normal(instancename, method, "Forward " + exploreClone);
					Logger.debug(instancename, method, "This counts as a response TO (" + recipient + "," + 
						pendingResponse.get(recipient) + "). Now I need a response FROM " + recipient);
					awaitingResponse.put(recipient, exploreClone);
					pendingResponse.remove(recipient);
					e.send(uid, exploreClone);
					
				}
			}			
		}

		// send nacks next
		ArrayList<Integer> toDelete = new ArrayList<Integer>(); // avoid concurrent modification
		for(Integer sender : pendingResponse.keySet()) {
			Message m = pendingResponse.get(sender);
			if(m != bestExplore) {
				// this message was useless to me, send nack, delete
				Message nack = Message.nack(m);
				Logger.normal(instancename, method, "Send " + nack + " to " + sender);
				edgeMap.get(sender).send(uid, nack);
				toDelete.add(sender);
			}
		}
		for(Integer sender : toDelete) {
			pendingResponse.remove(sender);
		}
		
		// send back acks if I'm not waiting for responses from anyone
		if(awaitingResponse.isEmpty()) {
			Logger.normal(instancename, method, "I have all my responses.");
			if(link2parent != null && pendingResponse.get(link2parent.otherSide(uid)) != null) {
				Message ack = Message.ack(bestExplore);
				Logger.normal(instancename, method, "Send " + ack + " to parent(" + link2parent.otherSide(uid) + ")");
				link2parent.send(uid, ack);	
				pendingResponse.remove(link2parent.otherSide(uid));
			} else if(largestId == uid) {
				for(Integer i : links2children) {
					Edge e = edgeMap.get(i);
					Message imTheLeader = Message.lead(uid, e.otherSide(uid));
					Logger.normal(instancename, method, "I am the leader. Send " + imTheLeader);
					e.send(uid, imTheLeader);
				}
				term = true;
			}
		}
	}

	//override
	public long getId() {
		return uid;
	}

	public boolean isTerminated() {
		return term;
	}

	public boolean isLeader() {
		return term && largestId == uid;
	}

	public void reset() {
		this.round = 0;
		this.largestId = uid;
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
