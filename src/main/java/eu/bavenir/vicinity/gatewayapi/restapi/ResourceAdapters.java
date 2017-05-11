package eu.bavenir.vicinity.gatewayapi.restapi;

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

public class ResourceAdapters extends ServerResource {
	
	private static final String ATTR_ADID = "adid";
	
	private static final String ATTR_TYPE = "type";
	
	private static final String ATTR_NAME = "name";
	
	private static final String ATTR_ID = "id";
	
	private static final String ATTR_EVENTURI = "eventUri";
	
	
	@Get
	public String represent() {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid != null){
			return getAdapter(attrAdid);
		} else {			
			return getAdapters();
		}
	}
	
	
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
	 * Returns the description of the adapter specified by adapter ID.
	 * 
	 * @param adid VICINITY Identifier of the adapter.
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
	
	
	/**
	 * Returns the list of the adapters.
	 * 
	 * @return All VICINITY identifiers of IoT objects fulfill the type and maximum constraint and own parameter.
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
