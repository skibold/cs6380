import java.util.concurrent.ArrayBlockingQueue;
import java.lang.InterruptedException;
import java.lang.String;

public class Edge {
	private static String classname = Edge.class.getName();

	private static final int Q_CAP = 1; // at most one message at a time on a queue
	private int v1, v2; // the ids of the two processes on either end of this edge
	private String instancename;
	private ArrayBlockingQueue<Message> v1producer; // process v1 writes to this, v2 reads
	private ArrayBlockingQueue<Message> v2producer; // process v2 writes to this, v1 reads

	public Edge(int v1, int v2) {
		this.v1 = v1;
		this.v2 = v2;
		this.instancename = classname + "(" + v1 + "<-->" + v2 + ")";
		this.v1producer = new ArrayBlockingQueue<Message>(Q_CAP);
		this.v2producer = new ArrayBlockingQueue<Message>(Q_CAP);
		Logger.normal(classname, "Edge", "Creating an edge from " + v1 + " to " + v2);
	}

	public boolean send(int producerId, Message m) {
		boolean success = false;
		try {
			if(producerId == v1) {
				v1producer.put(m); // blocks until queue is below capacity
				success = true;
				Logger.debug(instancename, "send", "Message on queue from " + v1 + " to " + v2);
			} else if(producerId == v2) {
				v2producer.put(m); // blocks until queue is below capacity
				success = true;
				Logger.debug(instancename, "send", "Message on queue from " + v2 + " to " + v1);
			} 
		} catch(InterruptedException e) {
			Logger.error(instancename, "send", "Error sending message from " + producerId);
			success = false;
		}
		return success;
	}

	public Message poll(int consumerId) {
		Message m = null;
		if(consumerId == v1) {
			m = v2producer.poll();
			Logger.debug(instancename, "poll", v1 + " polling " + v2 + " ... received " + m);
		} else if(consumerId == v2) {
			m = v1producer.poll();
			Logger.debug(instancename, "poll", v2 + " polling " + v1 + " ... received " + m);
		}
		return m;
	}

	public int otherSide(int id) {
		return id==v1?v2:v1;
	}
	public int v1() {
		return v1;
	}
	public int v2() {
		return v2;
	}
	public String toString() {
		return instancename;
	}
}
