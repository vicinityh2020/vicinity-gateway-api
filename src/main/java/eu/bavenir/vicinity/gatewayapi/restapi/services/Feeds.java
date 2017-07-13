package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
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
 *   URL: 				[server]:[port]/api/feeds
 *   METHODS: 			GET, POST
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Feeds extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Subscription ID attribute.
	 */
	private static final String ATTR_FID = "fid";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns all active discovery feeds.
	 * 
	 * @return JSON List of identifiers of active feeds.
	 */
	@Get
	public String represent() {
		return getFeeds();
	}
	
	
	/**
	 * Creates a new discovery feed from a given search criteria.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param criteria Search criteria based on the common VICINITY format (from request).
	 * @return Feed created (the corresponding VTED is received from the VICINITY Cloud).
	 */
	@Post("json")
	public String accept(Representation entity) {
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid feed description");
		}
		
		// get the json
		String feedJsonString = null;
		try {
			feedJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid feed description");
		}
		
		return storeFeed(feedJsonString);
	}

	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String storeFeed(String jsonString){

		return "Header: Location: http://gateway.vicinity.example.com/feeds/66348b54-1609-11e7-93ae-92361f002671\nBody: 66348b54-1609-11e7-93ae-92361f002671";
	}
	
	
	// TODO documentation
	private String getFeeds(){
		JsonObject json = Json.createObjectBuilder()
				.add("subscriptions", Json.createArrayBuilder()
						.add(Json.createObjectBuilder()
								.add(ATTR_FID, "1dae4326-44ae-4b98-bb75-15aa82516cc3")))
				.build();
		
		return json.toString();
	}
	
	
}
