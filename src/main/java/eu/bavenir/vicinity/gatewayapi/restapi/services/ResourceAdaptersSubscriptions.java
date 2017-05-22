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
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceAdaptersSubscriptions extends ServerResource {
	private static final String ATTR_ADID = "adid";
	private static final String ATTR_SID = "sid";
	private static final String ATTR_URI = "uri";
	
	@Get
	public String represent() {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid != null){
			return getAdapterSubscriptions(attrAdid);
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
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
					"Invalid subscription hook description");
		}
		
		// get the json
		String subscriptionJsonString = null;
		try {
			subscriptionJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid subscription hook description");
		}
		
		return storeSubscriptions(subscriptionJsonString);
	}
	
	
	
	private String storeSubscriptions(String jsonString){
		JsonArrayBuilder jsonResponseArrayBuilder = Json.createArrayBuilder();
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonArray jsonMainArrayFromRequest = jsonReader.readArray();
		
		// iterate over the incoming array of jsons, extract the ATTR_OID values and build separate response json for
		// each of them
		for (int i = 0; i < jsonMainArrayFromRequest.size(); i++){
		
			JsonObject singleRequestObject = jsonMainArrayFromRequest.getJsonObject(i);
			
			if (singleRequestObject.containsKey(ATTR_URI)){
				jsonResponseArrayBuilder.add(Json.createObjectBuilder()
						.add(ATTR_SID, "2734acea-9b8f-43f6-b17e-34f9c73e8513"));
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Invalid subscription hook description");
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
	
	
	
	private String getAdapterSubscriptions(String adid){
		
		if (adid.equals("1dae4326-44ae-4b98-bb75-15aa82516cc3")){
			JsonObject json = Json.createObjectBuilder()
					.add("subscriptions", Json.createArrayBuilder()
							.add(Json.createObjectBuilder()
									.add(ATTR_SID, "2734acea-9b8f-43f6-b17e-34f9c73e8513")))
					.build();
			return json.toString();
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		
	}
}
