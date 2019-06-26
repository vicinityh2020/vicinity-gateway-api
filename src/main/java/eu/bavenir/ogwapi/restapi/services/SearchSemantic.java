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
 *   URL: 				[server]:[port]/api/search/semantic
 *   METHODS: 			POST
 *   SPECIFICATION:		@see <a href="https://vicinityh2020.github.io/vicinity-gateway-api/#/">Gateway API</a>
 *   
 * @author Andrej
 *
 */
public class SearchSemantic extends ServerResource {

	
	// === CONSTANTS ===
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Used by the Agent / Adapter to post a semantic seaqrch query.
	 * 
	 * @param entity The query packaged as JSON entity.
	 * @return Response after the search.
	 */
	@Post("json")
	public Representation accept(Representation entity) {
		
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		// get the query in body
		String semanticQuery = getRequestBody(entity, logger);
		
		// and perhaps parameters
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		return performSearch(callerOid, semanticQuery, queryParams);
	}
	
	
	// === PRIVATE METHODS ===
	/**
	 * This makes the actual search.
	 * 
	 * @param sourceOid OID of the caller.
	 * @param semanticQuery The query string.
	 * @param parameters Any parameters to be sent along with the request.
	 * @return Search results.
	 */
	private Representation performSearch(String sourceOid, String semanticQuery, Map<String, String> parameters){

		CommunicationManager communicationManager 
		= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.performSemanticSearch(sourceOid, semanticQuery, parameters));
	}
	
	
	/**
	 * Retrieves a request body.
	 * 
	 * @param entity Entity to extract the body from.
	 * @param logger Logger.
	 * @return Text representation of the body.
	 */
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