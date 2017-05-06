package eu.bavenir.vicinity.gatewayapi.restapi;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceAdapters extends ServerResource {
	
	private static final String ATTR_ADID = "adid";
	
	private static final String ATTR_TYPE = "type";
	
	private static final String ATTR_NAME = "name";
	
	private static final String ATTR_ID = "id";
	
	private static final String ATTR_EVENTURI = "eventUri";

	//private static final String ATTR_DESCRIPTION = "description";
	
	
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
	public void accept(Representation entity) {
		//final Form form = new Form(entity);
		
		
		//String type = form.getFirstValue(ATTR_TYPE);
		
		
		//System.out.println(form.toString());
		
		/*
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}*/
	}
	
	
	@Put
	public void store() {
		/*if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}*/
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
