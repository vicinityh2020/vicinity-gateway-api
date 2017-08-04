package eu.bavenir.vicinity.gatewayapi.restapi.services;


import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.vicinity.gatewayapi.restapi.Api;
import eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode;
import eu.bavenir.vicinity.gatewayapi.xmpp.NetworkMessageRequest;
import eu.bavenir.vicinity.gatewayapi.xmpp.NetworkMessageResponse;

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
	 * Name of the 'objects' attribute.
	 */
	private static final String ATTR_OBJECTS = "objects";
	
	/**
	 * Name of the 'actions' attribute.
	 */
	private static final String ATTR_ACTIONS = "actions";
	
	/**
	 * Name of the 'tasks' attribute.
	 */
	private static final String ATTR_TASKS = "tasks";
	
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
		
		// send message to the right object
		CommunicationNode communicationNode 
									= (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);
		
		XMLConfiguration config = (XMLConfiguration) getContext().getAttributes().get(Api.CONTEXT_CONFIG);
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.REQUEST_OPERATION_GET);
		request.addAttribute(ATTR_OBJECTS, attrOid);
		request.addAttribute(ATTR_ACTIONS, attrAid);
		request.addAttribute(ATTR_TASKS, attrTid);
		
		// all set
		if (!communicationNode.sendMessage(sourceOid, attrOid, request.buildMessageString())){
			logger.info("Destination object " + attrOid + " is not online.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
	
		// this will wait for response
		NetworkMessageResponse response 
						= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		if (response == null){
			logger.info("No response message received. Source ID: " 
				+ sourceOid + " Destination ID: " + attrOid + " Action ID: " + attrAid + " Task ID: " + attrTid 
				+ " Request ID: " + requestId);
			throw new ResourceException(Status.CONNECTOR_ERROR_CONNECTION,
					"No valid response from remote object, possible message timeout.");
		}
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			logger.info("Source object: " + sourceOid + " Destination object: " + attrOid 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			return response.getResponseCode() + " " + response.getResponseCodeReason();
		}
		
		return response.getResponseBody();
		
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
