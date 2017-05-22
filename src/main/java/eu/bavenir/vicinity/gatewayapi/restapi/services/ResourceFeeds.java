package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceFeeds extends ServerResource {
	
	private static final String ATTR_FID = "fid";
	
	@Get
	public String represent() {
		
		String attrSid = getAttribute(ATTR_FID);
		
		if (attrSid != null){
			return getFeed(attrSid);
		} else {			
			return getFeeds();
		}
	}
	
	
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
	
	
	private String storeFeed(String jsonString){

		return "Header: Location: http://gateway.vicinity.example.com/feeds/66348b54-1609-11e7-93ae-92361f002671\nBody: 66348b54-1609-11e7-93ae-92361f002671";
	}
	
	
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
	
	
	private String getFeeds(){
		JsonObject json = Json.createObjectBuilder()
				.add("subscriptions", Json.createArrayBuilder()
						.add(Json.createObjectBuilder()
								.add(ATTR_FID, "1dae4326-44ae-4b98-bb75-15aa82516cc3")))
				.build();
		
		return json.toString();
	}
	
	
	private void deleteFeed(String fid){
		if (fid.equals("66348b54-1609-11e7-93ae-92361f002671")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
