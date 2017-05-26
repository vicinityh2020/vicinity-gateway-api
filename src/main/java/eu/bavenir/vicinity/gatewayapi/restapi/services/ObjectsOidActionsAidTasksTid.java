package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

/*
 * STRUCTURE
 * - constants
 * - public methods overriding HTTP methods 
 * - private methods
 */

/**
 * This class implements a {@link org.restlet.resource.ServerResource ServerResource} interface for following
 * Gateway API calls:
 * 
 *   URL: 				[server]:[port]/api/objects/{oid}/actions/{aid}/tasks/{tid}
 *   METHODS: 			GET, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		oid - VICINITY identifier of the object (e.g. 0729a580-2240-11e6-9eb5-0002a5d5c51b).
 *   					aid - Action identifier (as in object description) (e.g. switch).
 *   					tid - Task identifier (e.g. ca43b079-0818-4c39-b896-699c2d31f2db).
 *   
 * @author sulfo
 *
 */
public class ObjectsOidActionsAidTasksTid extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	/**
	 * Name of the Action ID attribute.
	 */
	private static final String ATTR_AID = "aid";
	
	/**
	 * Name of the Task ID attribute.
	 */
	private static final String ATTR_TID = "tid";
	
	//private static final String ATTR_VALUE = "value";
	
	//private static final String ATTR_TIMESTAMP = "timestamp";
	
	//private static final String ATTR_STATUS = "status";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Gets a specific task status to perform an action of an available IoT object.
	 * 
	 * @return Task status.
	 */
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
	
	
	/**
	 * Deletes the given task to perform an action.
	 */
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
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
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
	
	// TODO documentation
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
