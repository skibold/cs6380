import java.lang.String;
import java.lang.Integer;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class GHS {
	private static String classname = GHS.class.getName();

	public static void main(String[] args) throws IOException {
		String method = "main";

		// check for input file
		if(args.length == 0) {
			System.err.println("You must provide an input text file");
			System.exit(1);
		}
		String input = args[0];

		// setup logger
		boolean debug = false;
		if(args.length > 1) {
			debug = (args[1].equalsIgnoreCase("d"));
		}
		Logger.init(input+".log", debug);

		String line;
		BufferedReader br = new BufferedReader(new FileReader(input));

		// a priori how many procs
		int num_procs = 0;
		if((line = br.readLine()) != null)
			num_procs = Integer.parseInt(line);

		// process uids, the number better match num procs
		int[] ids = new int[num_procs];
		int i = 0;
		if((line = br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			if(st.countTokens() != num_procs) {
				Logger.error(classname, method, "Number of UIDs given(" + st.countTokens() + 
					") does not match number of processes(" + num_procs + ")");
				System.exit(1);
			}
			while(st.hasMoreTokens())
				ids[i++] = Integer.parseInt(st.nextToken());
		}

		// topology
		Clock clock = new Clock();
		ArrayList<Edge> edges = new ArrayList<Edge>();
		i = 0;
		while((line = br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			if(st.countTokens() != num_procs) {
				Logger.error(classname, method, "Connectivity matrix for process(" + ids[i] + 
					") gives edges for " + st.countTokens() + " processes instead of " + num_procs);
				System.exit(1);
			}

			int j = 0;
			while(st.hasMoreTokens()) {
				double w = Double.parseDouble(st.nextToken());
				// symmetric matrix so skip the first i, and only use non-negative weights
				if(j > i && w >= 0) 
					edges.add(new Edge(ids[i], ids[j], w, clock));
				j++;
			}
			i++;
		}
		if(i != num_procs) {
			Logger.error(classname, method, "Connectivity matrix gave edges for " + i + " processes instead of " + num_procs);
			System.exit(1);
		}
			

		// create procs with all incident edges
		ArrayList<Process> procs = new ArrayList<Process>();
		for(i=0; i<num_procs; i++) {
			ArrayList<Edge> e4i = new ArrayList<Edge>();
			for(Edge e : edges) {
				if(e.v1() == ids[i] || e.v2() == ids[i])
					e4i.add(e);
			}
			procs.add(new Process(ids[i], e4i));
		}

		// run
		boolean terminate = false;
		boolean exception = false;
		try {
			while(!terminate) {
				//Logger.toScreen(classname, method, "Step " + clock.read());
				// start this round for each proc
				for(Process p : procs)
					p.start();
				// wait for each proc to finish this round
				for(Process p : procs) {
					Logger.debug(classname, method, "try to join process " + p.getId());
					p.join();
				}
				// see if all procs are terminated and check for exceptions
				terminate = true; // if even 1 proc is not terminated this will flip to false, which we want
				for(Process p : procs) {
					terminate = terminate && p.isTerminated();
					if(p.hasException()) {
						Logger.error(classname, method, "Process " + p.getId() + 
							" threw exception at step " + clock.read() + ", terminating algorithm");
						throw new InterruptedException();
					}
				}
				clock.tick();
			}
		} catch(InterruptedException ex) {
			Logger.error(classname, method, "Massive exception");
			System.exit(1);
		}

		Logger.toScreen(classname, method, "Algorithm finished in " + clock.read() + " time steps");
		for(Process p : procs)
			Logger.toScreen(classname, method, "Process(" + p.getId() + "), leader: " + 
							p.isLeader() + ", mst edges: {" + p.mstEdges() + "}");

	}

}
