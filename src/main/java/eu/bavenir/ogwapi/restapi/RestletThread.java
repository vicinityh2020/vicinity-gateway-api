package eu.bavenir.ogwapi.restapi;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.Component;
import org.restlet.Server;
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
	 * Name of the configuration parameter for path to keystore used when HTTPS connections are enabled.
	 */
	private static final String CONF_PARAM_KEYSTOREFILE = "api.keystoreFile";
	
	/**
	 * Name of the configuration parameter for keystore password.
	 */
	private static final String CONF_PARAM_KEYSTOREPASSWORD = "api.keystorePassword";
	
	/**
	 * Name of the configuration parameter for key password.
	 */
	private static final String CONF_PARAM_KEYPASSWORD = "api.keyPassword";
	
	/**
	 * Name of the configuration parameter for keystore type.
	 */
	private static final String CONF_PARAM_KEYSTORETYPE = "api.keystoreType";
	
	/**
	 * Default value for {@link CONF_PARAM_KEYSTOREFILE CONF_PARAM_KEYSTOREFILE} parameter
	 */
	private static final String CONF_DEF_KEYSTOREFILE = "keystore/ogwapi.keystore";
	
	/**
	 * Default value for {@link CONF_PARAM_KEYSTOREPASSWORD CONF_PARAM_KEYSTOREPASSWORD} parameter
	 */
	private static final String CONF_DEF_KEYSTOREPASSWORD = "";
	
	/**
	 * Default value for {@link CONF_PARAM_KEYPASSWORD CONF_PARAM_KEYPASSWORD} parameter
	 */
	private static final String CONF_DEF_KEYPASSWORD = "";
	
	/**
	 * Default value for {@link CONF_PARAM_KEYSTORETYPE CONF_PARAM_KEYSTORETYPE} parameter
	 */
	private static final String CONF_DEF_KEYSTORETYPE = "PKCS12";
	
	/**
	 * Name of the configuration parameter for port, at which the API will be served.
	 */
	private static final String CONF_PARAM_APIPORT = "api.port";
	
	/**
	 * Name of the configuration parameter for enabling HTTPS protocol while serving the Gateway API. 
	 */
	private static final String CONF_PARAM_APIENABLEHTTPS = "api.enableHttps";
	
	/**
	 * Default value for {@link #CONF_PARAM_APIPORT CONF_PARAM_APIPORT} parameter.
	 */
	private static final int CONF_DEF_APIPORT = 8181;
	
	/**
	 * Default value for {@link #CONF_PARAM_APIENABLEHTTPS CONF_PARAM_APIENABLEHTTPS} parameter.
	 */
	private static final Boolean CONF_DEF_APIENABLEHTTPS = false;
	
	/**
	 * The URL path to the Gateway API. It is the part of URL after host. 
	 */
	private static final String API_URL_PATH = "/api";
	
	/**
	 * Thread sleep time in milliseconds, used while waiting for interrupt. 
	 */
	private static final long THREAD_SLEEP = 100; 
	
	
	/**
	 * Maximum number of threads the RESTLET will be allowed to spawn.
	 */
	private static final String MAX_THREADS = "1000";
	
	
	/* === FIELDS === */
	/**
	 * Indicates whether the OGWAPI should be stopped or not. 
	 */
	private volatile boolean threadRunning; 
	
	/**
	 * OGWAPI configuration.
	 */
	private XMLConfiguration config;
	
	/**
	 * OGWAPI Logger.
	 */
	private Logger logger;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initialises the configuration and logger instances.
	 * 
	 * @param config OGWAPI configuration.
	 * @param logger OGWAPI Logger.
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
		
		// create the main component
		Component component = new Component();
		
		// the RESTLET server
		Server server;
		
		// this is just to make the logs look smarter
		String serverType;
		
		// add a new HTTP/HTTPS server listening on set port
		if (config.getBoolean(CONF_PARAM_APIENABLEHTTPS, CONF_DEF_APIENABLEHTTPS) == true){
			server = component.getServers().add(Protocol.HTTPS, config.getInt(CONF_PARAM_APIPORT, CONF_DEF_APIPORT));
			
			server.getContext().getParameters().add("sslContextFactory", "org.restlet.engine.ssl.DefaultSslContextFactory");
			
			server.getContext().getParameters().add("keyStorePath", 
						config.getString(CONF_PARAM_KEYSTOREFILE, CONF_DEF_KEYSTOREFILE));
			
			server.getContext().getParameters().add("keyStorePassword", 
						config.getString(CONF_PARAM_KEYSTOREPASSWORD, CONF_DEF_KEYSTOREPASSWORD));
			
			server.getContext().getParameters().add("keyPassword", 
						config.getString(CONF_PARAM_KEYPASSWORD, CONF_DEF_KEYPASSWORD));
			
			server.getContext().getParameters().add("keyStoreType", 
						config.getString(CONF_PARAM_KEYSTORETYPE, CONF_DEF_KEYSTORETYPE));
			
			serverType = "HTTPS";
		} else {
			server = component.getServers().add(Protocol.HTTP, config.getInt(CONF_PARAM_APIPORT, CONF_DEF_APIPORT));
			serverType = "HTTP";
		}
		
		server.getContext().getParameters().add("maxThreads", MAX_THREADS); 
		server.getContext().getParameters().add("threadPool.maxThreads", MAX_THREADS); 
		
		// attach the API application  
		component.getDefaultHost().attach(API_URL_PATH, new Api(config, logger));  

		// log message
		logger.config(serverType + " server configured.");
		
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
			logger.warning("RESTLET thread interrupted.");
			// in this stage, it is unwise to only use logger
			e.printStackTrace();
		}
	}
	
	
	/* === PRIVATE METHODS === */

}