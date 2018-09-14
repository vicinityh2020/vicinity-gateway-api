package eu.bavenir.ogwapi.restapi.services;

import java.util.Map;
import java.util.logging.Logger;

import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.commons.messages.StatusMessage;
import eu.bavenir.ogwapi.restapi.Api;
import eu.bavenir.ogwapi.commons.CommunicationManager;


/*
 * STRUCTURE
 * - constants
 * - public methods overriding HTTP methods 
 * - private methods
 */


/**
 * This class implements a {@link org.restlet.resource.ServerResource ServerResource} interface for following
 * Gateway API calls:
 * 
 *   URL: 				[server]:[port]/api/objects/{oid}/events/{eid}
 *   METHODS: 			GET, POST, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   					eid - Event identifier (as in object description) (e.g. switch).
 *   
 * @author sulfo
 *
 */
public class ObjectsOidEventsEid extends ServerResource {
	
	// === CONSTANTS ===
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	/**
	 * Name of the Event ID attribute.
	 */
	private static final String ATTR_EID = "eid";
	
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	
	/**
	 * Retrieves status of a remote event channel.
	 * 
	 * @return Latest property value.
	 */
	@Get
	public Representation represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrEid == null){
			logger.info("OID: " + attrOid + " EID: " + attrEid + " Invalid identifier.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid identifier.");
		}
		
		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		
		return new JsonRepresentation(communicationManager.getEventChannelStatus(callerOid, attrOid, attrEid, queryParams)); 
		
	}
	
	
	/**
	 * Subscribes to the channel.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model (from request).
	 * @return A task to perform the action was submitted.
	 */
	@Post("json")
	public Representation accept() {
		String attrOid = getAttribute(ATTR_OID);
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrEid == null){
			logger.info("OID: " + attrOid + " EID: " + attrEid + " Invalid identifier.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid identifier.");
		}

		CommunicationManager communicationManager 
					= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

	
		return new JsonRepresentation(communicationManager.subscribeToEventChannel(callerOid, attrOid, attrEid, queryParams));
	}
	
	
	
	/**
	 * Cancel subscription to the channel.
	 * 
	 * @return statusMessage {@link StatusMessage StatusMessage} with the result of the operation.
	 */
	@Delete
	public Representation remove() {
		String attrOid = getAttribute(ATTR_OID);
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrEid == null){
			logger.info("EID: " + attrEid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid identifier.");
		}
		
		CommunicationManager communicationManager 
						= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		return new JsonRepresentation(communicationManager.unsubscribeFromEventChannel(callerOid, attrOid, attrEid, queryParams));
	}
	
	
	
	// === PRIVATE METHODS ===
	

}
