package eu.bavenir.ogwapi.commons.connectors;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

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
	/**
	 * Reserved word for source OID parameter. explain more...
	 */
	public static final String PARAM_SOURCEOID = "sourceOid";
	
	/* === FIELDS === */
	
	// logger and configuration
	protected XMLConfiguration config;
	protected Logger logger;

	
	
	/* === PUBLIC METHODS === */

	public AgentConnector(XMLConfiguration config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}

	public abstract NetworkMessageResponse forwardEventToObject(String sourceOid, String destinationOid, String eventId, 
			String body, Map<String, String> parameters);
	
	public abstract NetworkMessageResponse getObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters);
	
	public abstract NetworkMessageResponse setObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters);
	
	public abstract NetworkMessageResponse startObjectAction(String sourceOid, String destinationOid, String actionId, 
			String body, Map<String, String> parameters);
	
	public abstract NetworkMessageResponse stopObjectAction(String sourceOid, String destinationOid, String actionId, 
			String body, Map<String, String> parameters);
	
		
	
	/* === PRIVATE METHODS === */
		

}
