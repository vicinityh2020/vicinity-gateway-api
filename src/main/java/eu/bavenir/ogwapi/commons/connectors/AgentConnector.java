package eu.bavenir.ogwapi.commons.connectors;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

// TODO documentation
public abstract class AgentConnector {
	
	/* === CONSTANTS === */
	
	/* === FIELDS === */
	
	// logger and configuration
	protected XMLConfiguration config;
	protected Logger logger;

	
	
	/* === PUBLIC METHODS === */

	public AgentConnector(XMLConfiguration config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}

	
	// TODO make them return StatusMessage
	public abstract NetworkMessageResponse forwardEventToObject(String objectID, String eventID, String eventBody);
	
	public abstract NetworkMessageResponse getObjectProperty(NetworkMessageRequest requestMessage);
	
	public abstract NetworkMessageResponse setObjectProperty(NetworkMessageRequest requestMessage);
	
	public abstract NetworkMessageResponse startObjectAction(String objectID, String actionID, String requestBody);
	
	public abstract NetworkMessageResponse cancelTask(String objectID, String actionID);
	
		
	
	/* === PRIVATE METHODS === */
		

}
