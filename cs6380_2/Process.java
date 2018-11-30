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
	private Edge coreEdge;
	private Integer connectTrail; // uid of the child who reported the mwoe; where i will forward the chroot message
	private HashMap<Integer, Edge> edgeMap; // hold all edges incident on this process
	private ArrayList<Edge> componentEdges; // edges to other processes in my component; these edges are part of the MST
	private ArrayList<Edge> outsideEdges; // edges which might be outside my component, initially all incident edges
	private ArrayList<Edge> rejectedEdges; // edges which lead to processes in my component, but are not part of the MST

	private Integer awaitingResponseTest; // I am waiting for accept/reject from process with this uid
	private Integer awaitingResponseConn; // I sent connect to this uid, waiting for a response
	private ArrayList<Message> pendingResponseTest; // I must respond to the senders with accept/reject
	private ArrayList<Message> pendingResponseConn; // I must respond with a mutual connect
	private ArrayList<Message> pendingResponseReport;



	public Process(int uid, ArrayList<Edge> edges) {
		this.uid = uid;
		this.cid = Integer.toString(uid);
		this.level = 0;
		this.round = 0;
		instancename = classname + "(" + uid + ")";
		this.pendingResponseTest = new ArrayList<Message>();
		this.pendingResponseConn = new ArrayList<Message>();
		this.outsideEdges = new ArrayList<Edge>();
		this.componentEdges = new ArrayList<Edge>();
		this.rejectedEdges = new ArrayList<Edge>();
		this.edgeMap = new HashMap<Integer, Edge>();
		this.logEdges = "{";
		for(Edge e : edges) {
			logEdges += e.toString() + ", ";
			insertionSort(outsideEdges, e); // keep them sorted least to greatest by weight
			edgeMap.put(e.otherSide(uid), e);
		}
		this.logEdges += "}";
		this.awaitingResponseTest = null;
		this.awaitingResponseConn = null;
		this.link2parent = null;
		this.mwoe = null;
		this.connectTrail = null;
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
				Message init = Message.init(uid,uid,level,cid);
				this.receiveInitMsg(init);
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
	private void sendInitMsg() {
		final String method = "sendInitMsg";

		Logger.entering(instancename, method);
		//for level 0 , each process is itself a component
		for(Edge e:componentEdges) {
			if(link2parent != null && link2parent == e) continue; // don't send init back to parent
			int neighbor=e.otherSide(uid);
			Message init = Message.init(uid, neighbor, level, cid);
			e.send(uid,init);
		}

		Logger.exiting(instancename, method);
	}

	// INIT message
	private void receiveInitMsg(Message m) {
		final String method = "receiveInitMsg";
		Logger.entering(instancename, method);

		// record a few things
		this.cid = m.cid();
		this.level = m.level();
		if(link2parent != null) {
			outsideEdges.remove(link2parent);
			if(!componentEdges.contains(link2parent)) {
				Logger.debug(instancename, method, "Insert link2parent in componentEdges");
				insertionSort(componentEdges, link2parent);
			}
		}
		Logger.normal(instancename, method, "My new cid: " + this.cid + ", level: " +
							this.level + ", link2parent: " + this.link2parent);

		// check if I have pending test messages to which I must repond; reprocess them
		ArrayList<Message> pending = new ArrayList<Message>(pendingResponseTest); // avoid concurrent modification
		for(Message test : pending) {
			Logger.normal(instancename, method, "reprocess test message from " + test.originator());
			this.receiveTestMsg(test);
		}

		// similarly check if I have pending connect messages to which I must respond; reprocess them
		// pending.clear(); pending.addAll(pendingResponseConn);
		// for(Message conn : pending) { do stuff }
		pending.clear();
		pending.addAll(pendingResponseConn);
			for(Message conn:pending){
				Logger.normal(instancename, method, "reprocess connect message from " + conn.originator());
				this.receiveConnectMsg(conn);
			}


		this.awaitingResponseConn=null;
		this.awaitingResponseTest=null;
		this.connectTrail=null;
		this.mwoe=null;

		// forward the init message to all neighbors
		sendInitMsg();

		if(!sendTestMsg()) // no more edges to test, so send the report
			sendReportMsg();

		Logger.exiting(instancename, method);
	}

	// REPORT message
	private boolean sendReportMsg() {
		final String method = "sendReportMsg";
		Logger.entering(instancename, method);



		// if an mwoe is chosen (either from a child or one of my own outgoing egdes),
		// set this.connectTrail to the uid of the process who reported it
/* comment out to compile
		if(this.mwoe!=null){
			Message report = Message.report(uid,link2parent.otherSide(uid),this.mwoe);
			report.send();
		}
*/

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveReportMsg(Message m) {
		final String method = "receiveReportMsg";
		// TODO check if I have received reports from all my children
		// if link2parent == null then numchildren = componentEdges.size()
		// else numchildren = componentEdges.size() -1
		// pendingResponseReport.add(m);
		// if(pendingResponseReport)
		// collect all report messages in a global arraylist here
		// then if I have them all and if i have a response from my test message (awaitingResponseTest==null),
		// choose the mwoe from among this.mwoe and all those from child report messages and send the report message
		// for the chosen mwoe set this.connectTrail = uid of the guy who reported it
		//    this.connectTrail = this.uid if the chose mwoe is this.mwoe OR
		//    this.connectTrail = m.originator() for the Message m which contained the chosen mwoe
		// Then send a new report message containing the chosen mwoe to link2parent
		// Unless link2parent == null, then you have the component's mwoe, so create the chroot message and sent it to this.connectTrail - the uid of the guy who reported the chosen mwoe
		// this.connectTrail=this.mwoe.originator();
		Logger.entering(instancename, method);
		if(this.mwoe!=null && link2parent!=null){
			Message report = Message.report(uid,link2parent.otherSide(uid),this.mwoe);
			link2parent.send(uid,report);
		}
		//once parent node receives
		//if()
		Logger.exiting(instancename, method);
	}

	// TEST message
	private boolean sendTestMsg() {
		final String method = "sendTestMsg";
		Logger.entering(instancename, method);

		if(outsideEdges.isEmpty()) {
			this.mwoe = null;
			Logger.normal(instancename, method, "No more edges to test, no mwoe");
			Logger.exiting(instancename, method);
			return false;
		}
		if(this.awaitingResponseTest != null) {
			Logger.normal(instancename, method, "Already have a test message out to " +
								awaitingResponseTest + ", do nothing");
			Logger.exiting(instancename, method);
			return false;
		}

		this.mwoe = outsideEdges.get(0); // outside edges already sorted, pending mwoe assignment
		this.awaitingResponseTest = this.mwoe.otherSide(this.uid);
		Logger.normal(instancename, method, " to " + this.awaitingResponseTest);
		Message test = Message.test(this.uid, this.awaitingResponseTest, this.level, this.cid);
		this.mwoe.send(this.uid, test);

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveTestMsg(Message m) {
		final String method = "receiveTestMsg";
		Logger.entering(instancename, method);

		Logger.normal(instancename, method, "this.cid = " + this.cid + ", m.cid = " + m.cid() +
						", this.level = " + this.level + ", m.level = " + m.level());
		pendingResponseTest.remove(m); // in case this is a re-process of the message
		Edge back = edgeMap.get(m.originator());
		if(this.cid == m.cid()) {
			Logger.debug(instancename, method, "reply reject");
			back.send(this.uid, Message.reject(m));
		} else if(this.level >= m.level()) {
			Logger.debug(instancename, method, "reply accept");
			back.send(this.uid, Message.accept(m));
		} else {
			// defer the response
			Logger.debug(instancename, method, "defer response");
			this.pendingResponseTest.add(m);
		}

		Logger.exiting(instancename, method);
	}

	// ACCEPT message
	private void receiveAcceptMsg(Message m) {
		final String method = "receiveAcceptMsg";
		Logger.entering(instancename, method);
		Logger.debug(instancename, method, m.originator() + " " + awaitingResponseTest);

		if(awaitingResponseTest != null && m.originator() == awaitingResponseTest) {
			awaitingResponseTest = null;
			sendReportMsg();
		} else {
			Logger.warning(instancename, method, "Didn't expect this accept, do nothing");
		}

		Logger.exiting(instancename, method);
	}

	// REJECT message
	private void receiveRejectMsg(Message m) {
		final String method = "receiveRejectMsg";
		Logger.entering(instancename, method);
		Logger.debug(instancename, method, m.originator() + " " + awaitingResponseTest);

		if(awaitingResponseTest != null && m.originator() == awaitingResponseTest) {
			Edge reject = edgeMap.get(m.originator());
			Logger.normal(instancename, method, "remove outsideEgde" + reject.toString() +
								" and insert into rejectedEdges");
			insertionSort(rejectedEdges, reject);
			outsideEdges.remove(reject);

			this.awaitingResponseTest = null;
			if(!sendTestMsg())
				sendReportMsg();
		} else {
			Logger.warning(instancename, method, "Didn't expect this reject, do nothing");
		}

		Logger.exiting(instancename, method);
	}

	// CONNECT message
	private void receiveConnectMsg(Message m) {
		final String method = "receiveConnectMsg";

		Logger.entering(instancename, method);
		int senderLevel=m.level();
		if(senderLevel==level){
			//MERGE
			// check if I previously sent a connect to the guy who just sent this to me
			if (this.awaitingResponseConn == m.originator()){
				coreEdge=edgeMap.get(m.originator());
				if (this.uid>m.originator()){
					this.cid=coreEdge.toString();
					this.level++;
					this.link2parent=null;  //i don't understand this part
					sendInitMsg();
				}
				insertionSort(componentEdges, coreEdge);
				outsideEdges.remove(coreEdge);
			}
			else{
				this.pendingResponseConn.add(m);
			}
											// 	add this edge to componentEdges, remove from outsideEdges
											//	if this.uid > m.uid() then i am the new leader
											//		this.cid = coreEdge.toString()
											//		this.level++
											//		this.link2parent = null because i'm the new component leader
											//		send init to all neighbors; this will include the guy who just me this connect
											//	else do nothing, i will eventually receive an init from the other side
											// else
											//	this.pendingResponseConn.add(m)
											//	don't process this just yet, wait until/if I get a chroot message to connect over this edge

								/* commenting out to compile
											Message connect= new Message.connect()
											coreEdge=edgeMap.get(m.originator())
											this.cid=m.cid>this.cid?m.cid:this.cid;
											insertionSort(componentEdges, coreEdge);
											outsideEdges.remove(coreEdge);
											this.level+=1;
											sendInitMsg(this.cid);
								*/
											// componentEdges.addAll
		}
		else if (level>senderLevel){
			//ABSORB
			coreEdge=edgeMap.get(m.originator());
			insertionSort(componentEdges, coreEdge);
			outsideEdges.remove(coreEdge);
			Message init = Message.init(this.uid,m.originator(),this.level,this.cid);
			coreEdge.send(uid,init);// don't send this init to all neighbors, just directly to the guy you just absorbed
		}

		Logger.exiting(instancename, method);
	}

	// CHROOT message
	private boolean sendChrootMsg(Message m) {
		final String method = "sendChrootMsg";
		Logger.entering(instancename, method);

		boolean sent = false;
		if(this.connectTrail != null) {
			Edge forward = this.edgeMap.get(this.connectTrail);
			Message fwd = Message.cloneForNewRecipient(m, forward.otherSide(this.uid));
			Logger.normal(instancename, method, "Fwd chroot down the trail");
			forward.send(this.uid, fwd);
			sent = true;
		} else {
			Logger.warning(instancename, method, "No trail to forward on");
		}

		Logger.exiting(instancename, method);
		return true;
	}
	private void receiveChrootMsg(Message m) {
		final String method = "receiveChrootMsg";
		Logger.entering(instancename, method);

		if(this.connectTrail != null &&
		   this.connectTrail == this.uid) { // I'm the guy who reported this mwoe, so connect over it
			Logger.normal(instancename, method, "I reported this mwoe, send connect");
			Edge local_mwoe = m.mwoe();
			Message connect = Message.connect(this.uid, local_mwoe.otherSide(this.uid), m.level(), m.cid());
			local_mwoe.send(this.uid, connect);
			// record that I have sent a connect and wait for response
			this.awaitingResponseConn = local_mwoe.otherSide(this.uid);
			// check if i have received a connect from the other side of this mwoe; reprocess it
			for(Message conn : this.pendingResponseConn) {
				if(conn.originator() == this.awaitingResponseConn) {
					Logger.normal(instancename, method, "reprocess connect message from " + conn.originator());
					this.receiveConnectMsg(conn);
					break;
				}
			}
		} else { // forward down the connect trail
			sendChrootMsg(m);
		}

		Logger.exiting(instancename, method);
	}

/*** end functions to handle messages ***/

	private void insertionSort(ArrayList<Edge> collection, Edge e) {
		final String method = "insertionSort";
		if(e == null) {
			Logger.debug(instancename, method, "Not gonna insert null");
			return;
		}

		int i = collection.size();
		if(!collection.isEmpty()) {
			for(i=0; i<collection.size(); i++) {
				if(collection.get(i).compare(e) == 1) { // e is smaller than element at i
					break;
				}
			}
		}
		if(i < collection.size()) {
			Logger.debug(instancename, method, "Insert " + e.toString() + " at index " + i);
			collection.add(i,e);
		} else {
			Logger.debug(instancename, method, "Insert " + e.toString() + " at end");
			collection.add(e);
		}
	}


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

	public boolean isLeader() {
		return this.link2parent == null;
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
