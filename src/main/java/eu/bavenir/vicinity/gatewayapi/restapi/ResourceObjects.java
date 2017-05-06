package eu.bavenir.vicinity.gatewayapi.restapi;

import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;

import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;



public class ResourceObjects extends ServerResource {
	
	private static final String ATTR_OID = "oid";
	
	private static final String QUERY_TYPE = "type";
	
	private static final String QUERY_LIMIT = "limit";
	
	private static final String QUERY_OWN = "own";
	
	/**
	 * Returns all available IoT objects if 'oid' is set.
	 * 
	 * @return All UUIDs of the available IoT objects in a JSON.
	 */
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		
		String queryType = getQueryValue(QUERY_TYPE);
		String queryLimit = getQueryValue(QUERY_LIMIT);
		String queryOwn = getQueryValue(QUERY_OWN);
		
		System.out.println(queryType + " " + queryLimit + " " + queryOwn);
		
		if (attrOid != null){
			return getObject(attrOid);
		} else {			
			return getObjects(queryType, queryLimit, queryOwn);
		}
		
	}
	
	
	@Post
	public void accept() {
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}
	}
	
	
	@Put
	public void store() {
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}
	}
	
	@Delete
	public void remove() {
		String attrOid = getAttribute(ATTR_OID);
		
		if (attrOid != null){
			deleteObject(attrOid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid IoT object identifier.");
		}
	}
	
	
	
	private String getObject(String oid){
		if (oid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b")){
			
			return new String("{\"type\": \"Thermostate\",\"base\": \"http://gateway.vicinity.example.com/objects/0729a580-2240-11e6-9eb5-0002a5d5c51b\",\"oid\": \"0729a580-2240-11e6-9eb5-0002a5d5c51b\",\"owner\": \"d27ad211-cf1f-4cc9-9c22-238e7b46677d\",\"properties\": [{\"type\": [\"Property\"],\"pid\": \"temp1\",\"monitors\": \"Temperature\",\"output\": {\"units\": \"Celsius\",\"datatype\": \"float\"},\"writable\": false,\"links\": [{\"href\": \"properties/temp1\",\"mediaType\": \"application/json\"}]}],\"actions\": [{\"type\": [\"Action\"],\"aid\": \"switch\",\"affects\": \"OnOffStatus\",\"links\": [{\"href\": \"actions/switch\",\"mediaType\": \"application/json\"}],\"input\": {\"units\": \"Adimensional\",\"datatype\": \"boolean\"}}],\"location\": {\"latitude\": 34.43234,\"longitude\": -3.869}}");
			/*
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_TYPE, "generic.adapter.vicinity.eu")
					.add(ATTR_NAME, "My VICINITY Adapter")
					.add(ATTR_ID, "5603ff1b-e6cc-4897-8045-3724e8a3a56c")
					.add(ATTR_ADID, "1dae4326-44ae-4b98-bb75-15aa82516cc3")
					.add(ATTR_EVENTURI, "adapter007.vicinity.exemple.org/eventHandler")
					.build();
			
			return json.toString();
			*/
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
	
	private String getObjects(String queryType, String queryLimit, String queryOwn){
		
		JsonObject json = Json.createObjectBuilder()
				.add("objects", Json.createArrayBuilder()
						.add(Json.createObjectBuilder()
								.add(ATTR_OID, "0729a580-2240-11e6-9eb5-0002a5d5c51b")))
				.build();
		
		return json.toString();
	}
	
	
	private void deleteObject(String oid){
		if (oid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
