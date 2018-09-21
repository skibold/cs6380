import java.lang.String;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;

// this is still majorly in work

public class Process extends Thread {
	private static String classname = Process.class.getName();

	private boolean init, term;
	private int uid, largestId;
	private Edge link2parent;
	private Message bestExplore;
	private ArrayList<Edge> links2children;
	private HashMap<Integer,Edge> edgeMap; // map of recipient process id : edge between me and recipient
	private ArrayList<Message> awaitingResponse; // I am waiting for ack/nack
	private HashMap<Integer, ArrayList<Message>> pendingResponse; // I must respond to the senders with ack/nack

	public Process(int uid, ArrayList<Edge> edges) {
		this.uid = uid;
		this.largestId = uid;
		this.edgeMap = new HashMap<Integer, Edge>();
		for(Edge e : edges) {
			edgeMap.put(e.otherSide(uid), e);
		}
		this.awaitingResponse = new ArrayList<Message>();
		this.pendingResponse = new HashMap<Integer, ArrayList<Message>>();
		this.links2children = new ArrayList<Edge>();
		this.link2parent = null;
		this.init = true;
		this.term = false;
	}

	//override
	public void run() {
		if(term) return;
		if(init) {
			init = false;
			for(Integer recipient : edgeMap.keySet()) {
				Message explore = Message.explore(uid, recipient);
				awaitingResponse.add(explore);
				edgeMap.get(recipient).send(uid, explore);
			}
			return;
		}

		// receive all messages sent to me last round
		Message explore2forward = null;
		Message leader2forward = null;
		for(Edge e : edgeMap.values()) {
			Message m = e.poll(uid); //TODO either poll twice or figure out farther down how to combine nack to old explore with a new forwarded explore
			if(leader2forward != null) continue; // do nothing, but drain queues on all other edges
			if(m == null) continue;
			if(m.isLeader()) {
				leader2forward = m;
				continue; // drain queues on all other edges
			}
			else if(m.isExplore()) {
				// add m to list of messages i have received from the process on the other end of this edge
				addToPendingResponse(m, e.otherSide(uid));
				// remember the largest
				if(m.originator() > largestId) {
					largestId = m.originator();
					link2parent = e;
					bestExplore = m;
					explore2forward = m;
				}
			} else { // ack or nack
				// collection to delete from awaitingResponse
				// this avoids concurrent modification
				ArrayList<Message> toDelete = new ArrayList<Message>();
				for(Message a : awaitingResponse) {
					// if this is a response to an explore I either originated or forwarded
					if(m.isResponseTo(a)) {
						toDelete.add(a); // stop waiting for a response
						if(m.isAck()) {
							// if this is an ack, then make the sender my child
							links2children.add(e);
						}
					}
				}
				awaitingResponse.removeAll(toDelete);
			}
		}

		// send out all messages
		// forward the leader message if it exists and terminate
		if(leader2forward != null) {
			largestId = leader2forward.originator();
			for(Edge c : links2children) {
				Message m = Message.cloneForNewRecipient(leader2forward, c.otherSide(uid));
				c.send(uid, m);
			}
			term = true;
			return;
		}
		// send nacks next; they are easiest
		for(Integer sender : pendingResponse.keySet()) {
			ArrayList<Message> fromSender = pendingResponse.get(sender);
			ArrayList<Message> toDelete = new ArrayList<Message>(); // avoid concurrent modification
			for(Message m : fromSender) {
				if(m != bestExplore) {
					// this message was useless to me, send nack, delete
					Message nack = Message.nack(m);
					edgeMap.get(sender).send(uid, nack);
					toDelete.add(m);
				}
			}
			fromSender.removeAll(toDelete);
		}
		// forward the explore message if I have one
		if(explore2forward != null) {
			for(Integer recipient : edgeMap.keySet()) {
				Edge e = edgeMap.get(recipient);
				if(e != link2parent) { // don't forward back to sender
					Message exploreClone = Message.cloneForNewRecipient(explore2forward, recipient);
					awaitingResponse.add(exploreClone);
					e.send(uid, exploreClone);
				}
			}			
		}
		// send back acks if I'm not waiting for responses from anyone
		if(awaitingResponse.isEmpty()) {
			if(link2parent != null) {
				Message ack = Message.ack(bestExplore);
				link2parent.send(uid, ack);
				pendingResponse.get(link2parent.otherSide(uid)).remove(bestExplore);
			} else if(largestId == uid) {
				for(Edge e : links2children) {
					Message imTheLeader = Message.lead(uid, e.otherSide(uid));
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

	private void addToPendingResponse(Message m, int sender) {
		ArrayList<Message> producerList = pendingResponse.get(sender);
		if(producerList == null) {
			producerList = new ArrayList<Message>();
			pendingResponse.put(sender, producerList);
		}
		producerList.add(m);
	}
}
