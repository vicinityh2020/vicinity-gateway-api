package eu.bavenir.vicinity.gatewayapi.restapi;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.Component;
import org.restlet.data.Protocol;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * Thread that runs the RESTLET server for Gateway API.
 * 
 * @author sulfo
 *
 */
public class RestletThread extends Thread {

	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for port, at which the API will be served.
	 */
	private static final String CONF_PARAM_APIPORT = "api.port";
	
	/**
	 * Name of the configuration parameter for enabling HTTPS protocol while serving the Gateway API. 
	 */
	private static final String CONF_PARAM_APIENABLEHTTPS = "api.enableHttps";
	
	/**
	 * Default value for API port parameter.
	 */
	private static final int CONF_DEF_APIPORT = 8181;
	
	/**
	 * Default value for enabling HTTPS protocol while serving the Gateway API.
	 */
	private static final Boolean CONF_DEF_APIENABLEHTTPS = true;
	
	/**
	 * The URL path to the Gateway API. It is the part of URL after host. 
	 */
	private static final String API_URL_PATH = "/api";
	
	/**
	 * Thread sleep time in milliseconds, used while waiting for interrupt. 
	 */
	private static final long THREAD_SLEEP = 100; 
	
	
	/* === FIELDS === */
	
	private volatile boolean threadRunning; 
	
	private XMLConfiguration config;
	private Logger logger;
	
	private Component component;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initializes the configuration and logger instances.
	 * 
	 * @param config
	 * @param logger
	 */
	public RestletThread(XMLConfiguration config, Logger logger){
		
		this.config = config;
		this.logger = logger;
		
		threadRunning = true;
	}
	
	
	/**
	 * Sets the running flag to false, causing the thread to stop execution in the next cycle.
	 */
	public void terminateThread(){
		threadRunning = false;
	}
	
	
	/**
	 * Method that runs the RESTLET server and API. 
	 */
	public void run() {
		
		// create a new component
		component = new Component();  

		// this is just to make the logs look smarter
		String serverType;
		
		// add a new HTTP/HTTPS server listening on set port
		if (config.getBoolean(CONF_PARAM_APIENABLEHTTPS, CONF_DEF_APIENABLEHTTPS) == true){
			component.getServers().add(Protocol.HTTPS, config.getInt(CONF_PARAM_APIPORT, CONF_DEF_APIPORT));	
			serverType = "HTTPS";
		} else {
			component.getServers().add(Protocol.HTTP, config.getInt(CONF_PARAM_APIPORT, CONF_DEF_APIPORT));
			serverType = "HTTP";
		}
		
		// log message
		logger.config(serverType + " server configured.");
		  
		// attach the API application  
		component.getDefaultHost().attach(API_URL_PATH, new Api());  

		// start the component
		try {
			component.start();
			logger.fine(serverType + " server component started.");
		} catch (Exception e) {
			logger.severe("Can't start RESTLET component. Exited with exception:\n" + e.getMessage());
		}
		
		try {
			
			// wait for the change on running flag
			while (threadRunning){
				Thread.sleep(THREAD_SLEEP);
			}
			
			// exit
			try {
				component.stop();
				// this logger call will probably never execute on OpenJDK VM because of the shutdown hook...
				logger.fine("RESTLET thread stopping.");
				
				// ...but this should execute without too much of a fuss				
				System.out.println("Vicinity Gateway API: Stopping RESTLET component.");
				
			} catch (Exception e) {
				logger.warning("RESTLET thread threw an exception while stopping:\n" + e.getMessage());
				// in this stage, it is unwise to only use logger
				e.printStackTrace();
			}
			
		} catch (InterruptedException e) {
			threadRunning = false;
			logger.warning("RESTLET thread interrupted unexpectedly.");
			// in this stage, it is unwise to only use logger
			e.printStackTrace();
		}
	}
	
	
	/* === PRIVATE METHODS === */

}