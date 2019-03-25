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
 *   URL: 				[server]:[port]/api/search/sparql
 *   METHODS: 			POST
 *   SPECIFICATION:		@see <a href="https://vicinityh2020.github.io/vicinity-gateway-api/#/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class SearchSparql extends ServerResource {
	
	// === CONSTANTS ===
	
	
	// === OVERRIDEN HTTP METHODS ===
	

	/**
	 * Queries the network as if it were the VICINITY triple store of all objects’ data.
	 * 
	 * @param query The SPARQL query as JSON.
	 */
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
	/**
	 * Performs the actual search.
	 * 
	 * @param sourceOid OID of the caller.
	 * @param sparqlQuery Textual representation of the SPARQL query.
	 * @param parameters Any parameters to be sent along with the request.
	 * @return Search results.
	 */
	private Representation performSearch(String sourceOid, String sparqlQuery, Map<String, String> parameters){

		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.performSparqlSearch(sourceOid, sparqlQuery, parameters));
	
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
