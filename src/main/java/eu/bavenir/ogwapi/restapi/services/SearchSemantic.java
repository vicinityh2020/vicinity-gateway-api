package eu.bavenir.ogwapi.restapi.services;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
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
 *   URL: 				[server]:[port]/api/search/semantic
 *   METHODS: 			POST
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class SearchSemantic extends ServerResource {
	
	// === CONSTANTS ===
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	@Post("json")
	public Representation accept(Representation entity) {
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		// get the query in body
		String sparqlQuery = getRequestBody(entity, logger);
		
		// and perhaps parameters
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		return performSearch(callerOid, sparqlQuery, queryParams);
	}
	
	
	// === PRIVATE METHODS ===
	
	private Representation performSearch(String sourceOid, String sparqlQuery, Map<String, String> parameters){

		String jsonString = "{ \"semanticInterfaces\":[\" eu.shar_q.bat:genericBat:0.0.1\",\" eu.shar_q.bat:genericBat:0.0.2\",\" eu.shar_q.bat:FroniuscBat:0.0.1\"] }";

		return new JsonRepresentation(jsonString);
	
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
