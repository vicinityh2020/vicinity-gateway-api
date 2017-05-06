package eu.bavenir.vicinity.gatewayapi.restapi;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceAdaptersObjects extends ServerResource {
	
	private static final String ATTR_ADID = "adid";
	private static final String ATTR_OID = "oid";
	
	@Get
	public String represent() {
		
		String attrAdid = getAttribute(ATTR_ADID);
		
		if (attrAdid != null){
			return getAdapterObjects(attrAdid);
		} else {
			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
						"Adapter does not exist under given identifier.");
		}
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
