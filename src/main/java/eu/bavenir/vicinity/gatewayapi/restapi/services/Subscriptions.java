package eu.bavenir.vicinity.gatewayapi.restapi.services;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.resource.Get;
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
 *   URL: 				[server]:[port]/api/subscriptions
 *   METHODS: 			GET
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Subscriptions extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Subscription ID attribute.
	 */
	private static final String ATTR_SID = "sid";
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns the list of the subscriptions.
	 * 
	 * @return All VICINITY identifiers of all subscriptions.
	 */
	@Get
	public String represent() {		
		return getSubscriptions();
	}

	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String getSubscriptions(){
		JsonObject json = Json.createObjectBuilder()
				.add("subscriptions", Json.createArrayBuilder()
						.add(Json.createObjectBuilder()
								.add(ATTR_SID, "2734acea-9b8f-43f6-b17e-34f9c73e8513")))
				.build();
		
		return json.toString();
	}
	
}
