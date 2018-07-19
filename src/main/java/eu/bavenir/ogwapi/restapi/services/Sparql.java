package eu.bavenir.ogwapi.restapi.services;


import java.util.logging.Logger;

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
 *   URL: 				[server]:[port]/api/sparql
 *   METHODS: 			POST
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Sparql extends ServerResource {
	
	// === CONSTANTS ===

	/**
	 * Name of the query parameter.
	 */
	private static final String PARAM_SPARQLQUERY = "query";
	
	
	// === OVERRIDEN HTTP METHODS ===
	

	/**
	 * Queries the network as if it were the VICINITY triple store of all objects’ data.
	 * 
	 * @param query New description for an already registered object (from request).
	 */
	@Post("json")
	public Representation accept() {
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		// get the query
		String sparqlQuery = getQueryValue(PARAM_SPARQLQUERY);
		
		if (sparqlQuery == null || sparqlQuery.isEmpty()) {
			logger.warning("Request parameter 'query' is not set.");
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Request parameter 'query' is not set.");
		}
		
		return performSearch(callerOid, sparqlQuery);
	}
	
	
	// === PRIVATE METHODS ===
	
	private Representation performSearch(String sourceOid, String sparqlQuery){

		CommunicationManager communicationManager 
				= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);

		return new JsonRepresentation(communicationManager.performSparqlSearch(sourceOid, sparqlQuery));
	
	}
	
	
}
