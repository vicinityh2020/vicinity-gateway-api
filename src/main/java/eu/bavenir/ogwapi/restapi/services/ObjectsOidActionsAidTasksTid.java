package eu.bavenir.ogwapi.restapi.services;


import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.commons.CommunicationManager;
import eu.bavenir.ogwapi.restapi.Api;

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
 *   URL: 				[server]:[port]/api/objects/{oid}/actions/{aid}/tasks/{tid}
 *   METHODS: 			GET, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   					aid - Action identifier (as in object description) (e.g. switch).
 *   					tid - Task identifier (e.g. ca43b079-0818-4c39-b896-699c2d31f2db).
 *   
 * @author sulfo
 *
 */
public class ObjectsOidActionsAidTasksTid extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	/**
	 * Name of the Action ID attribute.
	 */
	private static final String ATTR_AID = "aid";
	
	/**
	 * Name of the Task ID attribute.
	 */
	private static final String ATTR_TID = "tid";
	

	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Gets a specific task status to perform an action of an available IoT object.
	 * 
	 * @return Task status.
	 */
	@Get
	public Representation represent(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String attrTid = getAttribute(ATTR_TID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null || attrTid == null){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " TID: " + attrTid 
					+ " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Given identifier does not exist.");
		}
		
		String body = getRequestBody(entity, logger);
		
		return getActionTaskStatus(callerOid, attrOid, attrAid, attrTid, queryParams, body);
	}
	
	
	/**
	 * Deletes the given task to perform an action.
	 */
	@Delete
	public Representation remove(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String attrTid = getAttribute(ATTR_TID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null || attrTid == null){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " TID: " + attrTid 
					+ " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Given identifier does not exist.");
		}
		
		String body = getRequestBody(entity, logger);
		
		return deleteActionTask(callerOid, attrOid, attrAid, attrTid, queryParams, body);
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Retrieves the Task status.
	 * 
	 * @param sourceOid OID of the caller. 
	 * @param destinationOid OID of the destination.
	 * @param Aid AID of the Action.
	 * @param attrTid TID of the Task.
	 * @param logger Logger taken previously from Context.
	 * 
	 * @return Response from the remote station. 
	 */
	private Representation getActionTaskStatus(String sourceOid, String destinationOid, String actionId, 
			String taskId, Map<String, String> queryParams, String body){
		
		CommunicationManager communicationManager 
			= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.retrieveTaskStatus(sourceOid, destinationOid, actionId,
				taskId, queryParams, body).buildMessage().toString());
		
	}
	
	
	// TODO documentation
	private Representation deleteActionTask(String sourceOid, String destinationOid, String actionId, 
			String taskId, Map<String, String> queryParams, String body){
		
		CommunicationManager communicationManager 
			= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.cancelRunningTask(sourceOid, destinationOid, actionId,
				taskId, queryParams, body).buildMessage().toString());
	}
	
	
	
	// === PRIVATE METHODS ===
	private String getRequestBody(Representation entity, Logger logger) {
		
		if (entity == null) {
			return null;
		}
		
		// check the body of the event to be sent
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.warning("Invalid request body - must be a valid JSON.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid request body - must be a valid JSON.");
		}
		
		// get the json
		String eventJsonString = null;
		try {
			eventJsonString = entity.getText();
		} catch (IOException e) {
			logger.info(e.getMessage());
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid request body");
		}
		
		return eventJsonString;
	}
}
