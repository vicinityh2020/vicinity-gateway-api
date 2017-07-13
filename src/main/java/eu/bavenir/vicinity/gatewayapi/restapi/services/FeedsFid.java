package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
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
 *   URL: 				[server]:[port]/api/feeds/{fid}
 *   METHODS: 			GET, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		fid - VICINITY Identifier of the feed (e.g. 66348b54-1609-11e7-93ae-92361f002671)
 *   
 * @author sulfo
 *
 */
public class FeedsFid extends ServerResource {

	// === CONSTANTS ===
	
	/**
	 * Name of the Subscription ID attribute.
	 */
	private static final String ATTR_FID = "fid";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns the given feed.
	 * 
	 * @return Information about the feed.
	 */
	@Get
	public String represent() {
		
		String attrFid = getAttribute(ATTR_FID);
		
		if (attrFid != null){
			return getFeed(attrFid);
		} else {			
			return null;
		}
	}
	
	
	/**
	 * Deletes the given feed (all exclusive discovered objects by this feed won’t be available).
	 */
	@Delete
	public void remove() {
		String attrFid = getAttribute(ATTR_FID);
		
		if (attrFid != null){
			deleteFeed(attrFid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
	
	// === PRIVATE METHODS ===
	// TODO documentation
	private String getFeed(String sid){
		
		if (sid.equals("66348b54-1609-11e7-93ae-92361f002671")){
			/*JsonObject json = Json.createObjectBuilder()
					.add(ATTR_URI, "http://adapter007.example.com/subscriptionListener")
					.build();
			
			return json.toString();*/
			
			return new String("{\"id\": \"66348b54-1609-11e7-93ae-92361f002671\",\"criteria\": {\"properties\": {\"$elemMatch\": {\"monitors\": {\"$in\": [\"Temperature\"]}}},\"location\": {\"latitude\": {\"$gt\": 34.22,\"$lt\": 34.25},\"longitude\": {\"$gt\":-3.4,\"$lt\": -3.3}}},\"created\": \"2017-05-06T14:33:08.695Z\",\"results\": [{\"type\": \"Thermostate\",\"base\":\"http://gateway.vicinity.example.com/objects/0729a580-2240-11e6-9eb5-0002a5d5c51b\",\"oid\":\"0729a580-2240-11e6-9eb5-0002a5d5c51b\",\"owner\": \"d27ad211-cf1f-4cc9-9c22-238e7b46677d\",\"properties\": [{\"type\": [\"Property\"],\"pid\": \"temp1\",\"monitors\": \"Temperature\",\"output\": {\"units\": \"Celsius\",\"datatype\": \"float\"},\"writable\": false,\"links\": [{\"href\": \"properties/temp1\",\"mediaType\": \"application/json\"}]}],\"actions\": [{\"type\": [\"Action\"],\"aid\": \"switch\",\"affects\": \"OnOffStatus\",{\"id\":\"66348b54-1609-11e7-93ae-92361f002671\",\"criteria\": {\"properties\":{\"$elemMatch\": {\"monitors\": {\"$in\": [\"Temperature\"]}}},\"location\": {\"latitude\": {\"$gt\": 34.22,\"$lt\": 34.25},\"longitude\": {\"$gt\": -3.4,\"$lt\": -3.3}}},\"created\": \"2017-05-06T14:33:08.695Z\",\"results\": [{\"type\": \"Thermostate\",\"base\": \"http://gateway.vicinity.example.com/objects/0729a580-2240-11e6-9eb5-0002a5d5c51b\",\"oid\": \"0729a580-2240-11e6-9eb5-0002a5d5c51b\",\"owner\": \"d27ad211-cf1f-4cc9-9c22-238e7b46677d\",\"properties\": [{\"type\": [\"Property\"],\"pid\": \"temp1\",\"monitors\": \"Temperature\",\"output\": {\"units\": \"Celsius\",\"datatype\": \"float\"},\"writable\": false,\"links\": [{\"href\": \"properties/temp1\",\"mediaType\": \"application/json\"}]}],\"actions\": [{\"type\": [\"Action\"],\"aid\": \"switch\",\"affects\": \"OnOffStatus\",\"links\": [{\"href\": \"actions/switch\",\"mediaType\": \"application/json\"}],\"input\": {\"units\": \"Adimensional\",\"datatype\": \"boolean\"}}],\"location\": {\"latitude\": 34.43234,\"longitude\": -3.869}}],\"ttl\": 0}\"links\": [{\"href\": \"actions/switch\",\"mediaType\": \"application/json\"}],\"input\": {\"units\": \"Adimensional\",\"datatype\": \"boolean\"}}],\"location\": {\"latitude\": 34.43234,\"longitude\": -3.869}}],\"ttl\": 0}");
			
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	// TODO documentation
	private void deleteFeed(String fid){
		if (fid.equals("66348b54-1609-11e7-93ae-92361f002671")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
