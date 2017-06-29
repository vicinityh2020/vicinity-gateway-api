package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
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
 *   URL: 				[server]:[port]/api/objects/{oid}/properties/{pid}
 *   METHODS: 			GET, PUT
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   					pid - Property identifier (as in object description) (e.g. temp1).
 *   
 * @author sulfo
 *
 */
public class ObjectsOidPropertiesPid extends ServerResource {

	// === CONSTANTS ===
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	/**
	 * Name of the Process ID attribute.
	 */
	private static final String ATTR_PID = "pid";
	
	/**
	 * Name of the 'objects' attribute.
	 */
	private static final String ATTR_OBJECTS = "objects";
	
	/**
	 * Name of the 'properties' attribute.
	 */
	private static final String ATTR_PROPERTIES = "properties";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Gets the property value of an available IoT object.
	 * 
	 * @return Latest property value.
	 */
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		if (attrOid != null && attrPid != null){
			return getObjectProperty(callerOid, attrOid, attrPid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	/**
	 * Sets the property value of an available IoT object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model.
	 */
	@Put("json")
	public String store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		if (attrOid == null || attrPid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Object or property does not exist under given identifier.");
		}
		
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		// get the json
		String propertyJsonString = null;
		try {
			propertyJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		return updateProperty(callerOid, attrOid, attrPid, propertyJsonString);
	}
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String updateProperty(String sourceOid, String attrOid, String attrPid, String jsonString){
		CommunicationNode communicationNode 
								= (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);
		
		NetworkMessageRequest request = new NetworkMessageRequest();
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.REQUEST_OPERATION_PUT);
		request.addAttribute(ATTR_OBJECTS, attrOid);
		request.addAttribute(ATTR_PROPERTIES, attrPid);
		
		request.setRequestBody(jsonString);
		
		// all set
		if (!communicationNode.sendMessage(sourceOid, attrOid, request.buildMessageString())){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
		
		// this will wait for response
		NetworkMessageResponse response 
						= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		// TODO solve issues with response code
		response.getResponseCode();
		
		return response.getResponseBody();
		
	}
	
	
	// TODO documentation
	private String getObjectProperty(String sourceOid, String attrOid, String attrPid){
		
		// send message to the right object
		CommunicationNode communicationNode 
								= (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);

		NetworkMessageRequest request = new NetworkMessageRequest();
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.REQUEST_OPERATION_GET);
		request.addAttribute(ATTR_OBJECTS, attrOid);
		request.addAttribute(ATTR_PROPERTIES, attrPid);
		
		// all set
		if (!communicationNode.sendMessage(sourceOid, attrOid, request.buildMessageString())){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
	
		// this will wait for response
		NetworkMessageResponse response 
						= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		// TODO solve issues with response code
		
		return response.getResponseBody();
	}
	
}
