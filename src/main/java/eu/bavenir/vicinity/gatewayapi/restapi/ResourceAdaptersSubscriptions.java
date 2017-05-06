package eu.bavenir.vicinity.gatewayapi.restapi;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceAdaptersSubscriptions extends ServerResource {
	private static final String ATTR_ADID = "adid";
	private static final String ATTR_SID = "sid";
	
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
	public void accept(Representation entity) {
		//final Form form = new Form(entity);
		
		
		//String type = form.getFirstValue(ATTR_TYPE);
		
		
		//System.out.println(form.toString());
		
		/*
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}*/
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
