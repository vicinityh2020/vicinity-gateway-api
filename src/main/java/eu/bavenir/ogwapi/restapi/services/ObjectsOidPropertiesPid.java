package eu.bavenir.ogwapi.restapi.services;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.restapi.Api;
import eu.bavenir.ogwapi.commons.CommunicationManager;

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
	

	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Gets the property value of an available IoT object.
	 * 
	 * @return Latest property value.
	 */
	@Get
	public Representation represent(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrPid == null){
			logger.info("OID: " + attrOid + " PID: " + attrPid + " Invalid identifier.");
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid identifier.");
		}
		
		String body = getRequestBody(entity, logger);
		
		return getObjectProperty(callerOid, attrOid, attrPid, body, queryParams);
		
	}
	
	
	/**
	 * Sets the property value of an available IoT object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param object Model.
	 */
	@Put("json")
	public Representation store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null || attrPid == null){
			logger.info("OID: " + attrOid + " PID: " + attrPid 
									+ " Invalid identifier.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid identifier.");
		}
		
		String body = getRequestBody(entity, logger);
		
		return updateProperty(callerOid, attrOid, attrPid, body, queryParams);
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Updates the property defined as PID.
	 * 
	 * @param sourceOid Caller OID.
	 * @param destinationOid Called OID.
	 * @param propertyId Property ID.
	 * @param body New representation of the property.
	 * @param logger Logger taken previously from Context.
	 * @return Response text.
	 */
	private Representation updateProperty(String sourceOid, String destinationOid, String propertyId, String body, 
			Map<String, String> queryParams){
		
		CommunicationManager communicationManager 
								= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		return new JsonRepresentation(communicationManager.setPropertyOfRemoteObject(sourceOid, destinationOid, 
				propertyId, body, queryParams).buildMessage().toString());
		
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
	private Representation getObjectProperty(String sourceOid, String destinationOid, String propertyId, String body, 
			Map<String, String> queryParams){
		
		CommunicationManager communicationManager 
			= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		return new JsonRepresentation(communicationManager.getPropertyOfRemoteObject(sourceOid, destinationOid, 
				propertyId, body, queryParams).buildMessage().toString());
	}
	
	
	
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
