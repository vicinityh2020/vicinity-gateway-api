package eu.bavenir.ogwapi.restapi.services;


import java.util.logging.Logger;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

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
	
	private int needReview;
	
	
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
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String attrTid = getAttribute(ATTR_TID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null || attrTid == null){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " TID: " + attrTid 
					+ " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		return getObjectActionTask(callerOid, attrOid, attrAid, attrTid, logger);
	}
	
	
	/**
	 * Deletes the given task to perform an action.
	 */
	@Delete
	public void remove() {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String attrTid = getAttribute(ATTR_TID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrAid == null || attrTid == null){
			logger.info("OID: " + attrOid + " AID: " + attrAid + " TID: " + attrTid 
					+ " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		deleteObjectActionTask(callerOid, attrOid, attrAid, attrTid, logger);
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Retrieves the Task status.
	 * 
	 * @param sourceOid OID of the caller. 
	 * @param attrOid OID of the destination.
	 * @param attrAid AID of the Action.
	 * @param attrTid TID of the Task.
	 * @param logger Logger taken previously from Context.
	 * 
	 * @return Response from the remote station. 
	 */
	private String getObjectActionTask(String sourceOid, String attrOid, String attrAid, String attrTid, Logger logger){
		
		return null;
		
	}
	
	
	// TODO documentation
	private void deleteObjectActionTask(
						String SourceOid, String attrOid, String attrAid, String attrTid, Logger logger){
		if (attrOid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b") && attrAid.equals("switch")
				&& attrTid.equals("ca43b079-0818-4c39-b896-699c2d31f2db")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
