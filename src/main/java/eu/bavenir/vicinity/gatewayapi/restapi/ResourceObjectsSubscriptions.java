package eu.bavenir.vicinity.gatewayapi.restapi;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceObjectsSubscriptions extends ServerResource {

	private static final String ATTR_OID = "oid";
	private static final String ATTR_SID = "sid";
	
	@Get
	public String represent() {
		
		String attrOid = getAttribute(ATTR_OID);
		
		if (attrOid != null){
			return getObjectSubscriptions(attrOid);
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
	
	@Post("json")
	public void accept(Representation entity) {
		//final Form form = new Form(entity);
		
		
		//String type = form.getFirstValue(ATTR_TYPE);
		
		
		//System.out.println(form.toString());
		
		/*
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}*/
	}
	
	
	private String getObjectSubscriptions(String oid){
		
		if (oid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b")){
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
