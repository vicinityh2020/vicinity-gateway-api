package eu.bavenir.vicinity.gatewayapi.restapi.services;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.Status;
import org.restlet.resource.Get;
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
 *   URL: 				[server]:[port]/api/adapters/{adid}
 *   METHODS: 			GET, POST
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		adid - VICINITY Identifier of the adapter (e.g. 1dae4326-44ae-4b98-bb75-15aa82516cc3).
 *   
 * @author sulfo
 *
 */
public class AdaptersAdid extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Adapter ID attribute.
	 */
	private static final String ATTR_ADID = "adid";
	
	/**
	 * Name of the Type attribute.
	 */
	private static final String ATTR_TYPE = "type";
	
	/**
	 * Name of the Name attribute.
	 */
	private static final String ATTR_NAME = "name";
	
	/**
	 * Name of the ID attribute.
	 */
	private static final String ATTR_ID = "id";
	
	/**
	 * Name of the Event URI attribute.
	 */
	private static final String ATTR_EVENTURI = "eventUri";

	
	// === OVERRIDEN HTTP METHODS ===
	
	
	/**
	 * Returns the description of the adapter.
	 * 
	 * @return Adapter description.
	 */
	@Get
	public String represent() {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid != null){
			return getAdapter(attrAdid);
		} else {
			return null;
		}
	}
	
	
	// === PRIVATE METHODS ===

	/**
	 * Returns the description of the adapter specified by adapter ID.
	 * 
	 * @param adid Adapter ID.
	 * @return Adapter description.
	 */
	private String getAdapter(String adid){
		
		if (adid.equals("1dae4326-44ae-4b98-bb75-15aa82516cc3")){
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_TYPE, "generic.adapter.vicinity.eu")
					.add(ATTR_NAME, "My VICINITY Adapter")
					.add(ATTR_ID, "5603ff1b-e6cc-4897-8045-3724e8a3a56c")
					.add(ATTR_ADID, "1dae4326-44ae-4b98-bb75-15aa82516cc3")
					.add(ATTR_EVENTURI, "adapter007.vicinity.exemple.org/eventHandler")
					.build();
			
			return json.toString();
			
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Adapter does not exist under given identifier.");
		}
		
	}
}
