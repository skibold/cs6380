import java.util.*;

public class Main{
  private static int n;
  private static String classname = Main.class.getName();
  // private static ArrayList<Integer> adjacencyList;
  public static void main(String[] args){
    ArrayList<Integer> adjacencyList=new ArrayList<Integer>();
    ArrayList<Edge> edgeList=new ArrayList<Edge>();
    ArrayList<ArrayList<Integer>> adjacencyMatrix=new ArrayList<ArrayList<Integer>> ();
    Scanner sc = new Scanner(System.in);
    int n = sc.nextInt();
    int[] ids= new int[n];
    System.out.println(n);
    String method = "main";

		String logfile = args[0];
		Logger.init(logfile, true);

    while(sc.hasNextInt()){

      for (int m=0;m<n;m++)
      {
      	ids[m]=sc.nextInt();
      }
      for(int i=0;i<n;i++)
      
      {
        ids[i]=i+1;
        adjacencyList.clear();
        for(int j=0;j<n;j++)
        {
          int num = sc.nextInt();
          //System.out.println(num);
          if(num==1){
              edgeList.add(new Edge(i+1,j+1));
          }
          adjacencyList.add(num);
        }
        adjacencyMatrix.add(adjacencyList);
      }
    }

    /*for(int k=0;k<n;k++)
    {
      System.out.println(Arrays.toString(adjacencyMatrix.get(k).toArray()));
    }*/

    ArrayList<Process> procs = new ArrayList<Process>();
		for(int i=0; i<ids.length; i++) {
			ArrayList<Edge> e4i = new ArrayList<Edge>();
			for(Edge e : edgeList) {
				if(e.v1() == ids[i] || e.v2() == ids[i])
					e4i.add(e);
			}
			procs.add(new Process(ids[i], e4i));
		}
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
