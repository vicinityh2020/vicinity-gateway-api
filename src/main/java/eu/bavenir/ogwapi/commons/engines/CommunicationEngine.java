package eu.bavenir.ogwapi.commons.engines;

import java.util.Collection;
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
public abstract class CommunicationEngine {

	/* === CONSTANTS === */
	
	
	/* === FIELDS === */

	// logger and configuration
	protected XMLConfiguration config;
	protected Logger logger;
	
	// credentials
	protected String objectID;
	protected String password;
	
	// connection descriptor for call backs
	protected ConnectionDescriptor connectionDescriptor;
	
	
	/* === PUBLIC METHODS === */
	
	
	public CommunicationEngine(String objectID, String password, XMLConfiguration config, Logger logger, 
																	ConnectionDescriptor connectionDescriptor) {
		
		this.config = config;
		this.logger = logger;
		
		this.objectID = objectID;
		this.password = password;
		
		this.connectionDescriptor = connectionDescriptor;
		
	}
	
	/**
	 * 
	 * @param objectID
	 * @param password
	 * @return
	 */
	public abstract boolean connect(String objectID, String password);
	

	public abstract void disconnect();
	
	public abstract boolean isConnected();
	
	public abstract Collection<String> getRoster();

	/**
	 * 
	 * @param destinationObjectID
	 * @param message
	 * @return
	 */
	public abstract boolean sendMessage(String destinationObjectID, String message);
	
		
	/* === PRIVATE METHODS === */
}
