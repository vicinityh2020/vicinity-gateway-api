package eu.bavenir.ogwapi;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import eu.bavenir.ogwapi.restapi.RestletThread;



/**
 * Main class of the Gateway API program. Loads configuration, initialises logger and runs the threads. It also waits
 * for signal from the OS to run its shutdown hook and perform a cleanup.
 * 
 * @author sulfo
 *
 */
public class App {

	/* === CONSTANTS === */
	
	/**
	 * Path to configuration file.
	 */
	private static final String CONFIG_PATH = "config/GatewayConfig.xml";
	
	/**
	 * Name of configuration parameter for switching the additional logging of events to console (aside from logging
	 * them into the log file) on or off.  
	 */
	private static final String CONFIG_PARAM_LOGGINGCONSOLEOUTPUT = "logging.consoleOutput";
	
	/**
	 * Name of the configuration parameter for path to log file.
	 */
	private static final String CONFIG_PARAM_LOGGINGFILE = "logging.file";
	
	/**
	 * Name of the configuration parameter for logging level.
	 */
	private static final String CONFIG_PARAM_LOGGINGLEVEL = "logging.level";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_LOGGINGCONSOLEOUTPUT CONFIG_PARAM_LOGGINGCONSOLEOUTPUT} configuration 
	 * parameter. This value is taken into account when no suitable value is found in the configuration file. 
	 */
	private static final Boolean CONFIG_DEF_LOGGINGCONSOLEOUTPUT = false;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_LOGGINGLEVEL CONFIG_PARAM_LOGGINGLEVEL} configuration parameter. This 
	 * value is taken into account when no suitable value is found in the configuration file. 
	 */
	private static final String CONFIG_DEF_LOGGINGLEVEL = "INFO";
	
	/**
	 * Error message for configuration loading failure. 
	 */
	private static final String ERR_CONF = "Failure during loading the configuration. Does the config file exist?";
	
	/**
	 * Error message for failure during the logging file creation.
	 */
	private static final String ERR_LOGGING = "The log file could not be created. Please check the write permissions "
			+ "for given directory.";
	
	/**
	 * Error message for initialisation failure.
	 */
	private static final String ERR_INIT = "Initialization failed. See standard error stream.";
	
	/**
	 * Thread sleep time in milliseconds, used while waiting for signal.
	 */
	private static final long THREAD_SLEEP = 100;
	
	
	/* === OTHER FIELDS === */
	
	// logging
	private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static FileHandler logfileTxt;
	private static SimpleFormatter formatterTxt;

	
	// Apache commons configuration
	private static Configurations configurations;
	private static XMLConfiguration config;
	
	
	// executing threads
	private static RestletThread restletThread;
	

	/* === METHODS === */
	
	/**
	 * Initialisation method called during application startup. Loads program configuration and sets logging facilities.
	 * 
	 * @return True if initialisation is successful. False otherwise.
	 */
	private static boolean initialize(){
		
		// === load the configuration file ===
		
		configurations = new Configurations();
		try {
			config = configurations.xml(CONFIG_PATH);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.err.println(ERR_CONF);
			return false;
		}
		
		
		// === set up the logger ===
		
		// set LOGGER logging level
		String confLoggingLevel = config.getString(CONFIG_PARAM_LOGGINGLEVEL, CONFIG_DEF_LOGGINGLEVEL);
		Level logLevel = translateLoggingLevel(confLoggingLevel);
		logger.setLevel(logLevel);
		
		// get whether the logger should log to console
		Boolean confLoggingConsoleOutput = config.getBoolean(CONFIG_PARAM_LOGGINGCONSOLEOUTPUT, 
				CONFIG_DEF_LOGGINGCONSOLEOUTPUT);
		
		Logger rootLogger = Logger.getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		if (handlers[0] instanceof ConsoleHandler) {
			if (confLoggingConsoleOutput == false){
				// suppress the logging output to the console if set so
				rootLogger.removeHandler(handlers[0]);
			} else {
				// otherwise set the log level
				handlers[0].setLevel(logLevel);
			}
		}
		
		
		// log file - if set
		String confLoggingFile = config.getString(CONFIG_PARAM_LOGGINGFILE);
		if (confLoggingFile != null && !confLoggingFile.isEmpty()){
			
			// create the filename string (essentially adding the time stamp to the string)
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd'T'HH_mm_ss.SSS");
			String logFileName = String.format(confLoggingFile, LocalDateTime.now().format(formatter));
			
			// this is for the application itself - the RESTLET needs to be taken care of later
			try {
				logfileTxt = new FileHandler(logFileName);
			} catch (SecurityException | IOException e) {
				e.printStackTrace();
				System.err.println(ERR_LOGGING);
				return false;
			}
			formatterTxt = new SimpleFormatter();
			
			// put it together
			logfileTxt.setFormatter(formatterTxt);
			logfileTxt.setLevel(logLevel);
			logger.addHandler(logfileTxt);
			
			// now set the same log file for RESTLET - no kidding, this is from their site
			System.setProperty("java.util.logging.config.file", logFileName);
			
			// log message
			logger.config("Log file: " + logFileName);
			
		}
		
		// === set up the API thread ===
		restletThread = new RestletThread(config, logger);
		
		return true;
	}
	
	
	
	/**
	 * Translates the string value of logging level configuration parameter to {@link java.util.logging.Level Level}
	 * object, that can be fed to {@link java.util.logging.logger Logger}. If the stringValue is null, it will return
	 * the default logging level set by {@link #CONFIG_DEF_LOGGINGLEVEL CONFIG_DEF_LOGGINGLEVEL} constant. If the 
	 * string contains other unexpected value (worst case) returns {@link java.util.logging.level#INFO INFO}.  
	 * 
	 * @param stringValue String value of the configuration parameter.
	 * @return String translated into {@link java.util.logging.level Level} object.
	 */
	private static Level translateLoggingLevel(String stringValue){
		
		if (stringValue == null){
			stringValue = CONFIG_DEF_LOGGINGLEVEL;
		}
		
		switch (stringValue){
		
		case "OFF":
			return Level.OFF;
		
		case "SEVERE":
			return Level.SEVERE;
			
		case "WARNING":
			return Level.WARNING;
			
		case "INFO":
			return Level.INFO;
			
		case "CONFIG":
			return Level.CONFIG;
			
		case "FINE":
			return Level.FINE;
			
		case "FINER":
			return Level.FINER;
			
		case "FINEST":
			return Level.FINEST;
			
		default: 
			return Level.INFO;
		}
	}
	
	
	
	/**
	 * Main method of the Vicinity Gateway application. In starts the thread and registers a shutdown hook that waits
	 * for OS signal to terminate. 
	 * 
	 * @param args
	 */
	public static void main( String[] args ){
		
		// attempt to initialise
		if (!initialize()){
			System.out.println(ERR_INIT);
			System.exit(1);
		}
		
		// log message
		logger.fine("Vicinity Gateway API initialized.");
		
		// start threads
		restletThread.start();
		
		// log message
		logger.fine("API thread started.");
		
		// register a shutdown hook - this will be executed after catching a signal to terminate
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				
				try {
					
					
					// only system.out style logging can be executed at this phase
					// (but there's nothing too much interesting anyway...)
					System.out.println("Vicinity Gateway API: Shutdown hook run, terminating threads.");
					
					restletThread.terminateThread();				
					restletThread.join();
					
				} catch (InterruptedException e) {
					// nothing else to do
					e.printStackTrace();
				}
			}
		});
		
		
		while(true){
			try {
				Thread.sleep(THREAD_SLEEP);
			} catch (InterruptedException e) {
				// nothing else to do
				e.printStackTrace();
			}
		}
		
	}
}