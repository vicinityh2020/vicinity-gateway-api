package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
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
 *   URL: 				[server]:[port]/api/objects/{oid}/actions/{aid}
 *   METHODS: 			GET, POST
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
	
	/**
	 * Name of the 'objects' attribute.
	 */
	private static final String ATTR_OBJECTS = "objects";
	
	/**
	 * Name of the 'actions' attribute.
	 */
	private static final String ATTR_ACTIONS = "actions";
	
	/// the lower part wont probably be necessary in the future
	/**
	 * Name of the Value attribute.
	 */
	private static final String ATTR_VALUE = "value";
	
	/**
	 * Name of the Time stamp attribute.
	 */
	private static final String ATTR_TIMESTAMP = "timestamp";
	
	/**
	 * Name of the Status attribute.
	 */
	private static final String ATTR_STATUS = "status";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Gets specific action status of an available IoT object.
	 * 
	 * @return Latest action status.
	 */
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		
		if (attrOid != null && attrAid != null){
			return getObjectAction(attrOid, attrAid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	/**
	 * Performs an action on an available IoT object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model (from request).
	 * @return A task to perform the action was submitted.
	 */
	@Post("json")
	public String accept(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		if (attrOid == null || attrAid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}

		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid action description");
		}
		
		// get the json
		String actionJsonString = null;
		try {
			actionJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid feed description");
		}
		
		return storeAction(callerOid, attrOid, attrAid, actionJsonString);
	}
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String storeAction(String sourceOid, String attrOid, String attrAid, String jsonString){

		CommunicationNode communicationNode 
			= (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);

		NetworkMessageRequest request = new NetworkMessageRequest();
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.REQUEST_OPERATION_POST);
		request.addAttribute(ATTR_OBJECTS, attrOid);
		request.addAttribute(ATTR_ACTIONS, attrAid);
		
		request.setRequestBody(jsonString);
		
		// all set
		if (!communicationNode.sendMessage(sourceOid, attrOid, request.buildMessageString())){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
		
		// this will wait for response
		NetworkMessageResponse response 
			= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		// TODO solve return code
		
		return response.getResponseBody();
	}
	
	
	// TODO documentation
	private String getObjectAction(String attrOid, String attrPid){
		
		if (attrOid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b") && attrPid.equals("switch")){
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_VALUE, 24.5)
					.add(ATTR_TIMESTAMP, "2017-05-06T15:09:30.204Z")
					.add(ATTR_STATUS, "EXECUTING")
					.build();
			
			return json.toString();	
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
}
