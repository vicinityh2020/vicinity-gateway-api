package eu.bavenir.vicinity.gatewayapi.restapi;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceSubscriptions extends ServerResource {
	
	private static final String ATTR_SID = "sid";
	
	private static final String ATTR_URI = "uri";
	
	@Get
	public String represent() {
		
		String attrSid = getAttribute(ATTR_SID);
		
		if (attrSid != null){
			return getSubscription(attrSid);
		} else {			
			return getSubscriptions();
		}
	}

	
	@Delete
	public void remove() {
		String attrSid = getAttribute(ATTR_SID);
		
		if (attrSid != null){
			deleteSubscription(attrSid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
	
	private String getSubscription(String sid){
		
		if (sid.equals("2734acea-9b8f-43f6-b17e-34f9c73e8513")){
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_URI, "http://adapter007.example.com/subscriptionListener")
					.build();
			
			return json.toString();
			
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	private String getSubscriptions(){
		JsonObject json = Json.createObjectBuilder()
				.add("subscriptions", Json.createArrayBuilder()
						.add(Json.createObjectBuilder()
								.add(ATTR_SID, "2734acea-9b8f-43f6-b17e-34f9c73e8513")))
				.build();
		
		return json.toString();
	}
	
	
	private void deleteSubscription(String sid){
		if (sid.equals("2734acea-9b8f-43f6-b17e-34f9c73e8513")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
