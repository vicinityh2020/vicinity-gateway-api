package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
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
 *   URL: 				[server]:[port]/api/objects/{oid}
 *   METHODS: 			GET, PUT, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   
 * @author sulfo
 *
 */
public class ObjectsOid extends ServerResource{

	// === CONSTANTS ===
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";

	
	// === OVERRIDEN HTTP METHODS ===

	/**
	 * Returns the description of an available IoT object.
	 * 
	 * @return Object description.
	 */
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
			
		if (attrOid != null){
			return getObject(attrOid);
		} else {
			return null;
		}
	}
	
	
	/**
	 * Updates the description of an already registered exposed IoT object.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param description JSON - New description for an already registered object (from request).
	 * 
	 */
	@Put("json")
	public void store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		
		if (attrOid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Object does not exist under given identifier.");
		}
		
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		// get the json
		String objectJsonString = null;
		try {
			objectJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		updateObject(objectJsonString);
		
	}
	
	
	
	/**
	 * Unregisters an exposed IoT object.
	 * 
	 */
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
	
	
	// === PRIVATE METHODS ===
	
	
	// TODO documentation
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
	
	
	// TODO documentation
	private void updateObject(String jsonString){
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonRequest = jsonReader.readObject();
		
		if (jsonRequest.containsKey(ATTR_OID)){
			// do something
		} else {
			jsonReader.close();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		jsonReader.close();
	}
	
	
	// TODO documentation
	private void deleteObject(String oid){
		if (oid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
}
