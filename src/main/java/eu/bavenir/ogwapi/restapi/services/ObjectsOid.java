package eu.bavenir.ogwapi.restapi.services;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
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
 *   URL: 				[server]:[port]/api/objects/{oid}
 *   METHODS: 			GET
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   
 * @author Andrej
 *
 */
public class ObjectsOid extends ServerResource{

	// === CONSTANTS ===
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Answers the GET call
	 * 
	 * @return A {@link StatusMessage StatusMessage} 
	 */
	@Get
	public Representation represent(Representation entity) {
		
		String attrOid = getAttribute(ATTR_OID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrOid == null){
			
			logger.info("OID: " + attrOid + " Given identifier does not exist.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Given identifier does not exist.");
		}
		

		String body = getRequestBody(entity, logger);
		
		return getObjectThingDescription(callerOid, attrOid, body, queryParams);
		
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Retrieves thing description of specific object
	 * 
	 * @param sourceOid Caller OID.
	 * @param attrOid Called OID.
	 * @param logger Logger taken previously from Context.
	 * @return Response text.
	 */ 
	private Representation getObjectThingDescription(String sourceOid, String destinationOid, String body, 
			Map<String, String> queryParams){
		
		CommunicationManager communicationManager 
			= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		return new JsonRepresentation(communicationManager.getThingDescriptionOfRemoteObject(sourceOid, destinationOid, body, queryParams).buildMessage().toString());
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
