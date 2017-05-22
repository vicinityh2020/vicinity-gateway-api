package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceObjectsActions extends ServerResource {

	private static final String ATTR_OID = "oid";
	
	private static final String ATTR_AID = "aid";
	
	private static final String ATTR_VALUE = "value";
	
	private static final String ATTR_TIMESTAMP = "timestamp";
	
	private static final String ATTR_STATUS = "status";
	
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		
		if (attrOid != null && attrAid != null){
			return getObjectAction(attrOid, attrAid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	@Post("json")
	public String accept(Representation entity) {
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid action description");
		}
		
		// get the json
		String actionJsonString = null;
		try {
			actionJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid feed description");
		}
		
		return storeAction(actionJsonString);
	}
	
	
	private String storeAction(String jsonString){

		return "Header: Location: http://gateway.vicinity.example.com/objects/0729a580-2240-11e6-9eb5-0002a5d5c51b/actions/switch/tasks/ca43b079-0818-4c39-b896-699c2d31f2db\nBody: ca43b079-0818-4c39-b896-699c2d31f2db";
	}
	
	
	private String getObjectAction(String attrOid, String attrPid){
		
		if (attrOid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b") && attrPid.equals("switch")){
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_VALUE, 24.5)
					.add(ATTR_TIMESTAMP, "2017-05-06T15:09:30.204Z")
					.add(ATTR_STATUS, "EXECUTING")
					.build();
			
			return json.toString();	
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
}
