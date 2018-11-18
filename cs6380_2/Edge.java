import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;
import java.lang.InterruptedException;
import java.lang.String;

public class Edge {
	private static String classname = Edge.class.getName();

	private int v1, v2; // the ids of the two processes on either end of this edge
	private double weight;
	private String instancename;
	private Random rand;
	private Clock clock;
	private static final float BOUND = 20.0f;
	private ConcurrentLinkedQueue<Message> v1producer; // process v1 writes to this, v2 reads
	private ConcurrentLinkedQueue<Message> v2producer; // process v2 writes to this, v1 reads

	public Edge(int v1, int v2, double weight, Clock clock /*reference to a timer*/) {
		this.v1 = v1;
		this.v2 = v2;
		this.weight = weight;
		this.instancename = classname + "(" + v1 + "<-->" + v2 + ", " + weight + ")";
		this.v1producer = new ConcurrentLinkedQueue<Message>();
		this.v2producer = new ConcurrentLinkedQueue<Message>();
		this.rand = new Random();
		this.clock = clock;
		Logger.normal(classname, "Edge", "Creating an edge from " + v1 + " to " + v2);
	}

	public boolean send(int producerId, Message m) {
		final String method = "send";
		Logger.entering(instancename, method);
		boolean success = false;
		int delay = clock.read() + (int)(rand.nextFloat() * BOUND);
		m.setDelay(delay);
		Logger.debug(instancename, method, m.toString());
		try {
			if(producerId == v1) {
				success = v1producer.add(m); 
			} else if(producerId == v2) {
				success = v2producer.add(m); 
			} 
		} catch(NullPointerException e) {
			Logger.error(instancename, method, "Error sending message from " + producerId);
			success = false;
		}
		return success;
	}

	public Message poll(int consumerId) {
		Message m = null;
		if(consumerId == v1) {
			if(testMsg(v2producer.peek()))
				m = v2producer.poll();
			Logger.debug(instancename, "poll", v1 + " polling " + v2 + " ... received " + m);
		} else if(consumerId == v2) {
			if(testMsg(v1producer.peek()))
				m = v1producer.poll();
			Logger.debug(instancename, "poll", v2 + " polling " + v1 + " ... received " + m);
		}
		return m;
	}

	private boolean testMsg(Message m) {
		final String method = "testMsg";
		if(m == null) {
			Logger.debug(instancename, method, "Front of queue is null");
			return false;
		}
		if(m.getDelay() > clock.read()) {
			Logger.debug(instancename, method, "Message still delayed: " + m.getDelay() + " > " + clock.read());
			return false;
		}
		return true;
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
	public double weight() {
		return weight;
	}
	// return 0 if they are equal edges
	// return 1 if this edge is "heavier" than e
	// return -1 if e is "heavier" than this edge
	public int compare(Edge e) {
		if(e == null) return 1;
		if(this == e) return 0;
		if(this.weight == e.weight())
			if(this.v1 == e.v1())
				if(this.v2 == e.v2())
					return 0;
				else
					return this.v2 > e.v2()?1:-1;
			else
				return this.v1 > e.v1()?1:-1;
		else
			return this.weight > e.weight()?1:-1;
	}
	public String toString() {
		return instancename;
	}
}
