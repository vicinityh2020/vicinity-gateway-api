package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
 *   URL: 				[server]:[port]/api/adapters
 *   METHODS: 			GET, POST
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Adapters extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Adapter ID attribute.
	 */
	private static final String ATTR_ADID = "adid";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns the list of the adapters.
	 * 
	 * @return All VICINITY identifiers of all adapters.
	 */
	@Get
	public String represent() {
					
		return getAdapters();
	}
	
	
	/**
	 * Registers new adapter.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param description Description of the new Adapter.
	 * 
	 * @return JSON with the new Adapter ID.
	 */
	@Post("json")
	public String accept(Representation entity) {
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid adapter description");
		}
		
		// get the json
		String adapterJsonString = null;
		try {
			adapterJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid adapter description");
		}
		
		return storeAdapter(adapterJsonString);
	}
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String storeAdapter(String jsonString){
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonRequest = jsonReader.readObject();
		
		String responseAdid;
		
		if (jsonRequest.containsKey(ATTR_ADID)){
			responseAdid = jsonRequest.getString(ATTR_ADID);
		} else {
			jsonReader.close();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid adapter description");
		}
		
		jsonReader.close();
		
		JsonObject jsonResponse = Json.createObjectBuilder()
				.add(ATTR_ADID, responseAdid)
				.build();
		
		return jsonResponse.toString();
	}
	
	

	/**
	 * Returns the list of the adapters.
	 * 
	 * @return All identifiers of IoT objects that fulfill the type and maximum constraint and own parameter.
	 */
	private String getAdapters(){
		JsonObject json = Json.createObjectBuilder()
				.add("adapters", Json.createArrayBuilder()
						.add(Json.createObjectBuilder()
								.add(ATTR_ADID, "1dae4326-44ae-4b98-bb75-15aa82516cc3")))
				.build();
		
		return json.toString();
	}
}
