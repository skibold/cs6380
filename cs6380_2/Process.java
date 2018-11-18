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
	private boolean begin, exc;
	private int uid, round, level;
	private Edge link2parent;
	private Edge mwoe;
	private HashMap<Integer, Edge> edgeMap; // hold all edges incident on this process
	private ArrayList<Edge> componentEdges; // edges to other processes in my component; these edges are part of the MST
	private ArrayList<Edge> outsideEdges; // edges which might be outside my component, initially all incident edges
	private ArrayList<Edge> rejectedEdges; // edges which lead to processes in my component, but are not part of the MST
	
	
	public Process(int uid, ArrayList<Edge> edges) {
		this.uid = uid;
		this.cid = Integer.toString(uid);
		this.level = 0;
		this.round = 0;
		instancename = classname + "(" + uid + ")";
		this.outsideEdges = new ArrayList<Edge>();
		this.componentEdges = new ArrayList<Edge>();
		this.rejectedEdges = new ArrayList<Edge>();
		this.edgeMap = new HashMap<Integer, Edge>();
		this.logEdges = "{";
		for(Edge e : edges) {
			logEdges += e.toString() + ", ";
			// add the edge to outsideEdges and keep it sorted
			edgeMap.put(e.otherSide(uid), e);
		}
		this.logEdges += "}";
		
		this.link2parent = null;
		this.mwoe = null;
		this.begin = true;
		this.exc = false;

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
		edgeCount();
		try {
			if(begin) { // begin by sending init to myself
				begin = false;
				//Message init = Message.init(uid, uid, level, cid);
				//this.receiveInitMsg(init);
				Logger.exiting(instancename, method);
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
				} else if(m.isChroot()) {
					receiveChrootMsg(m);
				}
			}
		} catch(Exception ex) {
			Logger.error(instancename, method, ex.toString());
			ex.printStackTrace();
			this.exc = true;
		}
		edgeCount();
		Logger.exiting(instancename, method);
	}

/*** begin functions to handle receiving different message types ***/

	// INIT message
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
	private boolean sendTestMsg() {
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
	private void receiveAcceptMsg(Message m) {
		final String method = "receiveAcceptMsg";
		Logger.entering(instancename, method);
		

		Logger.exiting(instancename, method);
	}

	// REJECT message
	private void receiveRejectMsg(Message m) {
		final String method = "receiveRejectMsg";
		Logger.entering(instancename, method);
		

		Logger.exiting(instancename, method);		
	}

	// CONNECT message
	private void receiveConnectMsg(Message m) {
		final String method = "receiveConnectMsg";
		Logger.entering(instancename, method);
		
		
		Logger.exiting(instancename, method);
	}

	// CHROOT message
	private boolean sendChrootMsg(Message m) {
		final String method = "sendChrootMsg";
		Logger.entering(instancename, method);

		
		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveChrootMsg(Message m) {
		final String method = "receiveChrootMsg";
		Logger.entering(instancename, method);

		

		Logger.exiting(instancename, method);
	}

/*** end functions to handle messages ***/


	//override
	public long getId() {
		return uid;
	}

	public boolean isTerminated() {
		return this.outsideEdges.isEmpty();
	}

	public boolean hasException() {
		return this.exc;
	}

	public String mstEdges() {
                String mst = new String();
                if(componentEdges.isEmpty()) return mst;
                for(Edge e : componentEdges) {
                        mst += e.toString() + ", ";
                }
                return mst.substring(0,mst.length()-2);
        }

	private void edgeCount() {
		Logger.debug(instancename, "edgeCount", "step: " + (round-1) + ", outsideEdges: " + outsideEdges.size() + ", rejectedEdges: " + rejectedEdges.size() + ", componentEdges: " + componentEdges.size());
	}
}
