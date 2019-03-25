package eu.bavenir.ogwapi.restapi.services;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
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
 *   URL: 				[server]:[port]/api/objects/{oid}/actions/{aid}
 *   METHODS: 			POST, PUT
 *   SPECIFICATION:		@see <a href="https://vicinityh2020.github.io/vicinity-gateway-api/#/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   					aid - Action identifier (as in object description) (e.g. switch).
 *   
 * @author sulfo
 *
 */
public class ObjectsOidActionsAid extends ServerResource {

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
	 * Name of the 'status' parameter.
	 */
	private static final String PARAM_STATUS = "status";
	

	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Performs an action on an available IoT object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @return A task to perform an action was submitted.
	 */
	@Post("json")
	public Representation accept(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Given identifier does not exist.");
		}

		String body = getRequestBody(entity, logger);
	
		return startAction(callerOid, attrOid, attrAid, body, queryParams);
	}
	
	
	/**
	 * Updates the status of the task, running on local object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 */
	@Put("json")
	public Representation store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null){
			logger.info("OID: " + attrOid + " PID: " + attrAid 
									+ " Object or property does not exist under given identifier.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Object or action does not exist under given identifier.");
		}
		
		if (!attrOid.equals(callerOid)) {
			logger.info("OID: " + attrOid + " Caller ID: " + callerOid 
					+ " does not match.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"OID and caller ID must match.");
		}
		
		// get the new status
		String newStatus = getQueryValue(PARAM_STATUS);
		
		String returnValue = getRequestBody(entity, logger);
		
		return updateActionStatus(callerOid, attrAid, newStatus, returnValue, queryParams);
	}
	
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Stores new action.
	 * 
	 * @param sourceOid Caller OID.
	 * @param destinationOid Called OID.
	 * @param actionId Action ID.
	 * @param body New representation of the Action.
	 * @return Response text.
	 */
	private Representation startAction(String sourceOid, String destinationOid, String actionId, String body, 
			Map<String, String> queryParams){

		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.startAction(sourceOid, destinationOid, actionId, body, 
				queryParams).buildMessage().toString());
	
	}
	
	
	/**
	 * Retrieves the Action defined as AID.
	 * 
	 * @param sourceOid Caller OID.
	 * @param status Next status.
	 * @param actionId Action ID.
	 * @param returnValue New return value.
	 * @param queryParams Query parameters to be send along.
	 * @return Response text.
	 */
	private Representation updateActionStatus(String sourceOid, String actionId, String status, String returnValue, 
			Map<String, String> queryParams){
		
		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.updateTaskStatus(sourceOid, actionId, status, returnValue, 
				queryParams).buildMessage().toString());
		
	}
	
	// === PRIVATE METHODS ===
	/**
	 * Retrieves a request body.
	 * 
	 * @param entity Entity to extract the body from.
	 * @param logger Logger.
	 * @return Text representation of the body.
	 */
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
