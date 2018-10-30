import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;
import java.lang.InterruptedException;
import java.lang.String;

public class Edge {
	private static String classname = Edge.class.getName();

	private int v1, v2; // the ids of the two processes on either end of this edge
	private String instancename;
	private Random rand;
	private Integer clock;
	private static final float BOUND = 20.0f;
	private ConcurrentLinkedQueue<Message> v1producer; // process v1 writes to this, v2 reads
	private ConcurrentLinkedQueue<Message> v2producer; // process v2 writes to this, v1 reads

	public Edge(int v1, int v2, Integer clock /*reference to a timer*/) {
		this.v1 = v1;
		this.v2 = v2;
		this.instancename = classname + "(" + v1 + "<-->" + v2 + ")";
		this.v1producer = new ConcurrentLinkedQueue<Message>();
		this.v2producer = new ConcurrentLinkedQueue<Message>();
		this.rand = new Random();
		Logger.normal(classname, "Edge", "Creating an edge from " + v1 + " to " + v2);
	}

	public boolean send(int producerId, Message m) {
		boolean success = false;
		int delay = clock + (int)(rand.nextFloat() * BOUND);
		m.setDelay(delay);
		try {
			if(producerId == v1) {
				success = v1producer.add(m); 
				Logger.debug(instancename, "send", "Message on queue from " + v1 + " to " + v2);
			} else if(producerId == v2) {
				success = v2producer.add(m); 
				Logger.debug(instancename, "send", "Message on queue from " + v2 + " to " + v1);
			} 
		} catch(NullPointerException e) {
			Logger.error(instancename, "send", "Error sending message from " + producerId);
			success = false;
		}
		return success;
	}

	public Message poll(int consumerId) {
		Message m = null;
		if(consumerId == v1) {
			while(v2producer.peek().getDelay() < clock) continue;
			m = v2producer.poll();
			Logger.debug(instancename, "poll", v1 + " polling " + v2 + " ... received " + m);
		} else if(consumerId == v2) {
			while(v1producer.peek().getDelay() < clock) continue;
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
