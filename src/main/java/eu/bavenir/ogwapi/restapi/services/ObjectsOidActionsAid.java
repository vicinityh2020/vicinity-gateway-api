package eu.bavenir.ogwapi.restapi.services;

import java.io.IOException;
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
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
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
	
	private static final String PARAM_STATUS = "status";
	

	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Performs an action on an available IoT object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model (from request).
	 * @return A task to perform an action was submitted.
	 */
	@Post("json")
	public Representation accept(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}

		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " Invalid action description.");
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid action description.");
		}
		
		// get the json
		String actionRequestBody = null;
		try {
			actionRequestBody = entity.getText();
		} catch (IOException e) {
			logger.info(e.getMessage());
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid action description");
		}
	
		return startAction(callerOid, attrOid, attrAid, actionRequestBody);
	}
	
	
	/**
	 * Updates the status of the task, running on local object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model.
	 */
	@Put("json")
	public Representation store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null){
			logger.info("OID: " + attrOid + " PID: " + attrAid 
									+ " Object or property does not exist under given identifier.");
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Object or property does not exist under given identifier.");
		}
		
		if (!attrOid.equals(callerOid)) {
			logger.info("OID: " + attrOid + " Caller ID: " + callerOid 
					+ " does not match.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"OID and caller ID must match.");
		}
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("OID: " + attrOid + " PID: " + attrAid 
					+ " Invalid property description - must be a valid JSON.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description - must be a valid JSON.");
		}
		
		// get the new status
		String newStatus = getQueryValue(PARAM_STATUS);
		
		// TODO delete after test
		System.out.println("!!!New status from the request: " + newStatus);
		
		
		// get the json
		String returnValue = null;
		try {
			returnValue = entity.getText();
		} catch (IOException e) {
			logger.info(e.getMessage());
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		return updateActionStatus(callerOid, attrAid, newStatus,returnValue);
	}
	
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Stores new action.
	 * 
	 * @param sourceOid Caller OID.
	 * @param attrOid Called OID.
	 * @param attrAid Action ID.
	 * @param actionRequestBody New representation of the Action.
	 * @param logger Logger taken previously from Context.
	 * @return Response text.
	 */
	private Representation startAction(String sourceOid, String attrOid, String attrAid, String actionRequestBody){

		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.startAction(sourceOid, attrOid, attrAid, actionRequestBody));
	
	}
	
	
	/**
	 * Retrieves the Action defined as AID.
	 * 
	 * @param sourceOid Caller OID.
	 * @param attrOid Called OID.
	 * @param attrAid Action ID.
	 * @param logger Logger taken previously from Context.
	 * @return Response text.
	 */
	private Representation updateActionStatus(String sourceOid, String attrAid, String status, String returnValue){
		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.updateTaskStatus(sourceOid, attrAid, status, returnValue));
		
	}
}
