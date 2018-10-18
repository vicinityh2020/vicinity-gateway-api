package eu.bavenir.ogwapi.commons.engines;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.ConnectionDescriptor;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

// TODO documentation
// don't forget to mention the usage of configuration parameters that are (or could be) common to more 
// than one engine, like general.encryption or general.sessionRecovery
public abstract class CommunicationEngine {

	/* === CONSTANTS === */
	
	
	/* === FIELDS === */

	// logger and configuration
	protected XMLConfiguration config;
	protected Logger logger;
	
	// credentials
	protected String objectId;
	protected String password;
	
	// connection descriptor for call backs
	protected ConnectionDescriptor connectionDescriptor;
	
	
	/* === PUBLIC METHODS === */
	
	
	public CommunicationEngine(String objectId, String password, XMLConfiguration config, Logger logger, 
																	ConnectionDescriptor connectionDescriptor) {
		
		this.config = config;
		this.logger = logger;
		
		this.objectId = objectId;
		this.password = password;
		
		this.connectionDescriptor = connectionDescriptor;
		
	}
	
	
	
	/**
	 * 
	 * @param objectId
	 * @param password
	 * @return
	 */
	public abstract boolean connect(String objectId, String password);
	

	public abstract void disconnect();
	
	public abstract boolean isConnected();
	
	public abstract Set<String> getRoster();

	/**
	 * 
	 * @param destinationObjectId
	 * @param message
	 * @return
	 */
	public abstract boolean sendMessage(String destinationObjectId, String message);
	
		
	/* === PRIVATE METHODS === */
}
