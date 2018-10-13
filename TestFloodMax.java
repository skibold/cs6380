import java.lang.String;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.util.ArrayList;

public class TestFloodMax {
	private static String classname = TestFloodMax.class.getName();

	public static void main(String[] args) {
		String method = "main";

		String logfile = args[0];
		Logger.init(logfile, true);

		// ids
		int[] ids = {1,2,3,4,5,6};
		
		// topology
		ArrayList<Edge> edges = new ArrayList<Edge>();
		edges.add(new Edge(6,4));
		edges.add(new Edge(6,3));
		edges.add(new Edge(5,1));
		edges.add(new Edge(4,3));
		edges.add(new Edge(4,2));
		edges.add(new Edge(3,1));
		edges.add(new Edge(3,2));
		edges.add(new Edge(2,1));

		// create procs with all incident edges
		ArrayList<Process> procs = new ArrayList<Process>();
		for(int i=0; i<ids.length; i++) {
			ArrayList<Edge> e4i = new ArrayList<Edge>();
			for(Edge e : edges) {
				if(e.v1() == ids[i] || e.v2() == ids[i])
					e4i.add(e);
			}
			procs.add(new Process(ids[i], e4i));
		}

		// run
		boolean terminate = false;
		int r = 0;
		try {
			while(!terminate) {
				Logger.toScreen(classname, method, "round " + (r++));
				// start this round for each proc
				for(Process p : procs)
					p.start();
				// wait for each proc to finish this round
				for(Process p : procs) {
					Logger.debug(classname, method, "try to join " + p.getId());
					p.join();
				}
				// see if all procs are terminated
				terminate = true; // if even 1 proc is not terminated this will flip to false, which we want
				for(Process p : procs)
					terminate = terminate && p.isTerminated();
			}
		} catch(InterruptedException ex) {
			Logger.error(classname, method, "Massive exception");
		}

		for(Process p : procs)
			Logger.toScreen(classname, method, p.getId() + " parent: " + p.parent() + 
					", children: " + p.children() + ", is leader? " + p.isLeader());

	}

}
