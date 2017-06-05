package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jivesoftware.smack.packet.Message;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.vicinity.gatewayapi.restapi.Api;
import eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode;

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
	
	/**
	 * Name of the Value attribute.
	 */
	private static final String ATTR_VALUE = "value";
	
	/**
	 * Name of the Time stamp attribute.
	 */
	private static final String ATTR_TIMESTAMP = "timestamp";
	
	
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
	public void store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		
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
		
		updateProperty(propertyJsonString);
	}
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private void updateProperty(String jsonString){
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonRequest = jsonReader.readObject();
		
		if (jsonRequest.containsKey(ATTR_VALUE)){
			//TODO do something
		} else {
			jsonReader.close();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		jsonReader.close();
	}
	
	// TODO documentation
	private String getObjectProperty(String sourceOid, String attrOid, String attrPid){
		
		// send message to the right object
		CommunicationNode communicationNode = (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);

		// we will bend the JSON attributes to our obedience... in /objects/{oid}, the 'objects' part is the key,
		// {oid} is a value - it will be easier to re-assemble a URL in the XmppConnectionDescriptor
		JsonObject json = Json.createObjectBuilder()
				.add(CommunicationNode.ATTR_REQUESTOPERATION, "get")
				.add(ATTR_OBJECTS, attrOid)
				.add(ATTR_PROPERTIES, attrPid)
				.build();
		
		if (!communicationNode.sendMessage(sourceOid, attrOid, json.toString())){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Given identifier does not exist.");
		}
		
		// TODO OMG OMFG WTF!?
		
		Message message = null;
		do {
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			message = communicationNode.retrieveSingleMessage(sourceOid);
		} while (message == null);
		
		return message.getBody();
	}
	
}
