package util;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import arro.Constants;


/**
 */
public class Logger {
	
	private static Logger instance = null;
	public static DebugTrace out;
	MessageConsoleStream console;
	
	public static final String STD = "/debug/option1";
	public static final String BUILDER = "/debug/builder";
    public static final String EDITOR = "/debug/editor";
    public static final String MPE = "/debug/mpeditor";
    public static final String WS = "/debug/workspace";
	public static final String ERROR = "/debug/error";

	
	// Add to Debug Configurations -> Arguments:
	//  -debug /..../Arro/Arro/debug.txt
	//
	// File containing (example):
    // Arro/debug/option1=true
    // Arro/debug/option2=false
    // Arro/debug/option3=false
    // Arro/debug/error=false

	private Logger(BundleContext context) {
		ServiceTracker debugTracker = new ServiceTracker(context, DebugOptions.class.getName(), null);
		debugTracker.open(true);
		DebugOptions debugOptions = (DebugOptions) debugTracker.getService();
		debugOptions.setFile(null);
		debugOptions.setDebugEnabled(true);
		
		out = debugOptions.newDebugTrace("Arro");
		out.trace(STD, "Logger started");
		
		MessageConsole myConsole = findConsole(Constants.CONSOLE_NAME);
		console = myConsole.newMessageStream();
	}
	
	/**
	 * Get singleton object.
	 * 
	 * @return
	 */
	public static Logger getInstance(BundleContext context) {
		System.out.println("test");
		if(instance == null) {
			instance = new Logger(context);
		}
		return instance;
	}
	
	private MessageConsole findConsole(String name) {
	    ConsolePlugin plugin = ConsolePlugin.getDefault();
	    IConsoleManager conMan = plugin.getConsoleManager();
	    IConsole[] existing = conMan.getConsoles();
	    for (int i = 0; i < existing.length; i++)
	         if (name.equals(existing[i].getName()))
	            return (MessageConsole) existing[i];
		      //no console found, so create a new one
	    MessageConsole myConsole = new MessageConsole(name, null);
	    conMan.addConsoles(new IConsole[]{myConsole});
	    return myConsole;
	}
	
	public static void writeToConsole(String msg) {
		instance.console.println(msg);
	}
}
