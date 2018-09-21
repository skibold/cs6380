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
	private HashMap<int,Edge> edgeMap;
	private ArrayList<Message> messagesAwaitingResponse;

	public Process(int uid, ArrayList<Edge> e) {
		this.uid = uid;
		this.largestId = -1; // no real uid will be negative
		this.edgeMap = new HashMap<int, Edge>();
		for(Edge e : edges) {
			edgeMap.put(e.otherSide(uid), e);
		}
		//this.messagesAwaitingResponse = new ArrayList<Message>();
		this.link2parent = null;
		this.init = true;
		this.term = false;
	}

	//override
	public void run() {
		// reset some state variables at the beginning of each round
		Message explore2forward = null;
		if(term) return;
		if(init) {
			init = false;
			for(int k : edgeMap.keySet()) {
				explore2forward = Message.explore(uid, k);
				edgeMap.get(k).send(explore2forward);
			}
		}

		// receive all messages sent to me last round
		for(Edge e : edgeMap.values()) {
			Message m = e.poll(uid);
			if(m == null) continue;
			if(m.isExplore()) {
				if(m.payload() > largestId) {
					largestId = m.payload;
					link2parent = e;
					explore2forward = m;
				} else {
					Message nack = Message.nack(uid, m);
					e.send(nack);
				}
			} else if(m.isAck()) {

			} else if(m.isNack()) {

			}
		}

		// send out all messages
		//if(explore2forward != null)

	}

	//override
	public long getId() {
		return uid;
	}
}
