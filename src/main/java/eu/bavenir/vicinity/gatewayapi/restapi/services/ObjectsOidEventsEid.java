package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
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
 *   URL: 				[server]:[port]/api/objects/{oid}/events/{eid}
 *   METHODS: 			POST
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
	 * Name of the Process ID attribute.
	 */
	private static final String ATTR_EID = "eid";
	
	/**
	 * Name of the 'objects' attribute.
	 */
	private static final String ATTR_OBJECTS = "objects";
	
	/**
	 * Name of the 'events' attribute.
	 */
	private static final String ATTR_EVENTS = "events";
	
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Receives IoT object event from client.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model (from request).
	 * @return A task to perform the action was submitted.
	 */
	@Post("json")
	public String accept(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrEid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}

		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid event description");
		}
		
		// get the json
		String actionJsonString = null;
		try {
			actionJsonString = entity.getText();
		} catch (IOException e) {
			
			logger.warning(e.getMessage());
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid event description");
		}
		
		return storeEvent(callerOid, attrOid, attrEid, actionJsonString);
	}
	
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String storeEvent(String sourceOid, String attrOid, String attrEid, String jsonString){
		
		CommunicationNode communicationNode 
							= (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);
		
		NetworkMessageRequest request = new NetworkMessageRequest();
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.REQUEST_OPERATION_POST);
		request.addAttribute(ATTR_OBJECTS, attrOid);
		request.addAttribute(ATTR_EVENTS, attrEid);
		
		request.setRequestBody(jsonString);
		
		// all set
		if (!communicationNode.sendMessage(sourceOid, "iliolousta_2", request.buildMessageString())){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
		
		// this will wait for response
		NetworkMessageResponse response 
			= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 2) != 1){
			return response.getResponseCode() + " " + response.getResponseCodeReason();
		}
		
		return response.getResponseBody();
	}
}
