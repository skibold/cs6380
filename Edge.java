import java.util.concurrent.ArrayBlockingQueue;
import java.lang.InterruptedException;
import java.lang.String;

public class Edge {
	private static String classname = Edge.class.getName();

	private static final int Q_CAP = 2; // at most two messages at a time on a queue
	private int v1, v2; // the ids of the two processes on either end of this edge
	private ArrayBlockingQueue<Message> v1producer; // process v1 writes to this, v2 reads
	private ArrayBlockingQueue<Message> v2producer; // process v2 writes to this, v1 reads

	public Edge(int v1, int v2) {
		this.v1 = v1;
		this.v2 = v2;
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
			} else if(producerId == v2) {
				v2producer.put(m); // blocks until queue is below capacity
				success = true;
			} 
		} catch(InterruptedException e) {
			Logger.error(classname, "send", "Error sending message from " + producerId);
			success = false;
		}
		return success;
	}

	public Message poll(int consumerId) {
		Message m = null;
		if(consumerId == v1) {
			m = v2producer.poll();
		} else if(consumerId == v2) {
			m = v1producer.poll();
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
}
