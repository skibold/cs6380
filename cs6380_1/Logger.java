import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.String;
import java.lang.Exception;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Logger {
	// log levels
	private static String ENTERING = "ENTERING";
	private static String EXITING = "EXITING";
	private static String NORMAL = "NORMAL";
	private static String DEBUG = "DEBUG";
	private static String ERROR = "ERROR";
	private static String WARNING = "WARNING";
	private static String STDOUT = "STDOUT";

	private static DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	private FileWriter fw = null;
	private String lf;
	private boolean debug;
	private static Logger instance = null;

	private Logger(String logfile, boolean debug) throws IOException {
		this.debug = debug;
		lf = logfile;
		fw = new FileWriter(logfile,true); // append = true
	}

	public void finalize() {
		try {
			fw.flush();
			fw.close();
		} catch(IOException e) {}
	}

	public static void init(String logfile, boolean debug) {
		if(instance == null) {
			try {
				instance = new Logger(logfile, debug);
				System.out.println("Initialized with log file " + logfile);
			} catch(IOException e) {
				System.out.println("Failed to initialize with log file " + logfile);
			}
		} else {
			System.out.println("Logger already initialized with log file " + instance.filename());
		}
	}

	public static void init(String logfile) {
		init(logfile, false);
	}

	public static void toScreen(String classname, String methodname, String msg) {
		if(instance != null)
			instance.write(format(STDOUT, classname, methodname, msg));
		System.out.print(format(STDOUT, classname, methodname, msg));
	}

	public static void entering(String classname, String methodname) {
		if(instance != null)
			instance.write(format(ENTERING, classname, methodname));
		else
			System.out.print(format(ENTERING, classname, methodname));
	}

	public static void exiting(String classname, String methodname) {
		if(instance != null)
			instance.write(format(EXITING, classname, methodname));
		else
			System.out.print(format(EXITING, classname, methodname));
	}

	public static void normal(String classname, String methodname, String msg) {
		if(instance != null)
			instance.write(format(NORMAL, classname, methodname, msg));
		else
			System.out.print(format(NORMAL, classname, methodname, msg));
	}

	public static void debug(String classname, String methodname, String msg) {
		if(instance == null || !instance.debug()) return;
		if(instance != null && instance.debug())
			instance.write(format(DEBUG, classname, methodname, msg));
		else
			System.out.print(format(DEBUG, classname, methodname, msg));
	}

	public static void error(String classname, String methodname, String msg) {
		if(instance != null)
			instance.write(format(ERROR, classname, methodname, msg));
		System.err.print(format(ERROR, classname, methodname, msg));
	}

	public static void error(String classname, String methodname, Exception e) {
		if(instance != null)
			instance.write(format(ERROR, classname, methodname, e.getLocalizedMessage()));
		System.err.print(format(ERROR, classname, methodname, e.getLocalizedMessage()));
	}

	public static void warning(String classname, String methodname, String msg) {
		if(instance != null)
			instance.write(format(WARNING, classname, methodname, msg));
		else
			System.out.print(format(WARNING, classname, methodname, msg));
	}

	private static String format(String type, String classname, String methodname, String msg) {
		String fmt = time() + " " + type + " " + classname + "." + methodname + ": " + msg + "\n";
		return fmt;
	}

	private static String format(String type, String classname, String methodname) {
		String fmt = time() + " " + type + " " + classname + "." + methodname + "\n";
		return fmt;
	}

	private static String time() {
		Date d = new Date();
		return dateTimeFormat.format(d);
	} 

	private void write(String s) {
		try {
//System.out.println(s);
			fw.write(s);
			fw.flush();
		} catch(IOException e) {
			System.out.print("FAILED to log:\n  " + s);
		}
	}

	private String filename() {
		return lf;
	}

	private boolean debug() {
		return debug;
	}
}
