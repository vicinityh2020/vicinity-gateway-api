package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceObjectsActionsTasks extends ServerResource {
	private static final String ATTR_OID = "oid";
	
	private static final String ATTR_AID = "aid";
	
	private static final String ATTR_TID = "tid";
	
//	private static final String ATTR_VALUE = "value";
	
	//private static final String ATTR_TIMESTAMP = "timestamp";
	
	//private static final String ATTR_STATUS = "status";
	
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String attrTid = getAttribute(ATTR_TID);
		
		if (attrOid != null && attrAid != null && attrTid != null){
			return getObjectActionTask(attrOid, attrAid, attrTid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	
	@Delete
	public void remove() {
		String attrOid = getAttribute(ATTR_OID);
		String attrAid = getAttribute(ATTR_AID);
		String attrTid = getAttribute(ATTR_TID);
		
		if (attrOid != null && attrAid != null && attrTid != null){
			deleteObjectActionTask(attrOid, attrAid, attrTid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
	
	private String getObjectActionTask(String attrOid, String attrAid, String attrTid){
		
		if (attrOid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b") && attrAid.equals("switch")
				&& attrTid.equals("ca43b079-0818-4c39-b896-699c2d31f2db")){
			/*
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_VALUE, 24.5)
					.add(ATTR_TIMESTAMP, "2017-05-06T15:09:30.204Z")
					.add(ATTR_STATUS, "EXECUTING")
					.build();
			
			return json.toString();*/
			
			return new String("{\"status\": \"RUNNING\",\"input\": {\"value\": true},\"output\": {\"value\": true,\"timestamp\": \"2017-05-06T15:28:13.445Z\",\"status\": \"EXECUTING\"}}");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	private void deleteObjectActionTask(String attrOid, String attrAid, String attrTid){
		if (attrOid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b") && attrAid.equals("switch")
				&& attrTid.equals("ca43b079-0818-4c39-b896-699c2d31f2db")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
