package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.util.logging.Logger;

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
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrPid == null){
			logger.info("OID: " + attrOid + " PID: " + attrPid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		System.out.println("STABILITY DEBUG: GET ObjectOidPropertiesPid.");
		
		return getObjectProperty(callerOid, attrOid, attrPid, logger);
		
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
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrPid == null){
			logger.info("OID: " + attrOid + " PID: " + attrPid 
									+ " Object or property does not exist under given identifier.");
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Object or property does not exist under given identifier.");
		}
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("OID: " + attrOid + " PID: " + attrPid 
					+ " Invalid property description.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description.");
		}
		
		// get the json
		String propertyJsonString = null;
		try {
			propertyJsonString = entity.getText();
		} catch (IOException e) {
			logger.info(e.getMessage());
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		System.out.println("STABILITY DEBUG: PUT ObjectOidPropertiesPid.");
		
		return updateProperty(callerOid, attrOid, attrPid, propertyJsonString, logger);
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Updates the property defined as PID.
	 * 
	 * @param sourceOid Caller OID.
	 * @param attrOid Called OID.
	 * @param attrPid Property ID.
	 * @param jsonString New representation of the property.
	 * @param logger Logger taken previously from Context.
	 * @return Response text.
	 */
	private String updateProperty(String sourceOid, String attrOid, String attrPid, String jsonString, Logger logger){
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
			logger.info("Destination object " + attrOid + " is not online.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
		
		// this will wait for response
		NetworkMessageResponse response 
						= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			logger.info("Source object: " + sourceOid + " Destination object: " + attrOid 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			return response.getResponseCode() + " " + response.getResponseCodeReason();
		}
		
		return response.getResponseBody();
		
	}
	
	
	/**
	 * Retrieves the property defined as PID.
	 * 
	 * @param sourceOid Caller OID.
	 * @param attrOid Called OID.
	 * @param attrPid Property ID.
	 * @param logger Logger taken previously from Context.
	 * @return Response text.
	 */
	private String getObjectProperty(String sourceOid, String attrOid, String attrPid, Logger logger){
		
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
			logger.info("Destination object " + attrOid + " is not online.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Destination object is not online.");
		}
		
		// this will wait for response
		NetworkMessageResponse response 
						= (NetworkMessageResponse) communicationNode.retrieveSingleMessage(sourceOid, requestId);
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			logger.info("Source object: " + sourceOid + " Destination object: " + attrOid 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			return response.getResponseCode() + " " + response.getResponseCodeReason();
		}
		
		return response.getResponseBody();
	}
	
}
