package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;


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
 *   METHODS: 			GET, POST
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Sparql extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Sparql query attribute.
	 */
	private static final String QUERY_SPARQLQUERY = "query";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Queries the network as if it were the VICINITY triple store of all objects’ data.
	 * 
	 * @param query SPARQL query (from request).
	 * @return
	 */
	@Get
	public String represent() {

		String querySparqlQuery = getQueryValue(QUERY_SPARQLQUERY);
		
		if (querySparqlQuery != null){
			return "SPARQL results";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid query.");
		}
	}
	
	
	/**
	 * Queries the network as if it were the VICINITY triple store of all objects’ data.
	 * 
	 * @param query New description for an already registered object (from request).
	 */
	@Post()
	public void accept() {
		//final Form form = new Form(entity);
		
		
		//String type = form.getFirstValue(ATTR_TYPE);
		
		
		//System.out.println(form.toString());
		
		/*
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}*/
	}
	
	// === PRIVATE METHODS ===
	
}
