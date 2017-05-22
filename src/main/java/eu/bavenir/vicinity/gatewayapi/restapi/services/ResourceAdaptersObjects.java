package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceAdaptersObjects extends ServerResource {
	
	private static final String ATTR_ADID = "adid";
	private static final String ATTR_OID = "oid";
	private static final String ATTR_ID = "id";
	
	@Get
	public String represent() {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Adapter does not exist under given identifier.");
		}
		
		return getAdapterObjects(attrAdid);
	}
	
	
	@Post("json")
	public String accept(Representation entity) {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Adapter does not exist under given identifier.");
		}
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		// get the json
		String objectsJsonString = null;
		try {
			objectsJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		return storeObjects(objectsJsonString);
	}
	
	
	@Put("json")
	public void store(Representation entity) {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Adapter does not exist under given identifier.");
		}
		
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		// get the json
		String adapterJsonString = null;
		try {
			adapterJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		updateAdapter(adapterJsonString);
	}
	
	
	private void updateAdapter(String jsonString){
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonRequest = jsonReader.readObject();
		
		if (jsonRequest.containsKey(ATTR_ID)){
			//TODO do something
		} else {
			jsonReader.close();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object description");
		}
		
		jsonReader.close();
	}
	
	
	private String storeObjects(String jsonString){
		
		JsonArrayBuilder jsonResponseArrayBuilder = Json.createArrayBuilder();
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonArray jsonMainArrayFromRequest = jsonReader.readArray();
		
		// iterate over the incoming array of jsons, extract the ATTR_OID values and build separate response json for
		// each of them
		for (int i = 0; i < jsonMainArrayFromRequest.size(); i++){
		
			JsonObject singleRequestObject = jsonMainArrayFromRequest.getJsonObject(i);
			
			if (singleRequestObject.containsKey(ATTR_OID)){
				jsonResponseArrayBuilder.add(Json.createObjectBuilder()
						.add(ATTR_OID, singleRequestObject.get(ATTR_OID)));
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Invalid object description");
			}
		}

		// json magic
		JsonArray responseArray = jsonResponseArrayBuilder.build();
		StringWriter stringWriter = new StringWriter();
		JsonWriter jsonWriter = Json.createWriter(stringWriter);
		jsonWriter.writeArray(responseArray);
		jsonWriter.close();
		
		return stringWriter.toString();
	}
	
	
	private String getAdapterObjects(String adid){
		
		if (adid.equals("1dae4326-44ae-4b98-bb75-15aa82516cc3")){
			JsonObject json = Json.createObjectBuilder()
					.add("objects", Json.createArrayBuilder()
							.add(Json.createObjectBuilder()
									.add(ATTR_OID, "0729a580-2240-11e6-9eb5-0002a5d5c51b")))
					.build();
			return json.toString();
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Adapter does not exist under given identifier.");
		}
	}
}
