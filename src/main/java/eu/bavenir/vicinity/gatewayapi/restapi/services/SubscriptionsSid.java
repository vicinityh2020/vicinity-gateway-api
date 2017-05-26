package eu.bavenir.vicinity.gatewayapi.restapi.services;

import javax.json.Json;
import javax.json.JsonObject;

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
 *   URL: 				[server]:[port]/api/subscriptions/{sid}
 *   METHODS: 			GET, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		sid - VICINITY identifier of the subscription (e.g. 2734acea-9b8f-43f6-b17e-34f9c73e8513).
 *   
 * @author sulfo
 *
 */
public class SubscriptionsSid extends ServerResource {

	// === CONSTANTS ===
	
	/**
	 * Name of the Subscription ID attribute.
	 */
	private static final String ATTR_SID = "sid";
	
	/**
	 * Name of the URI attribute.
	 */
	private static final String ATTR_URI = "uri";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Return subscription.
	 * 
	 * @return JSON with Subscription configuration.
	 */
	@Get
	public String represent() {
		
		String attrSid = getAttribute(ATTR_SID);
		
		if (attrSid != null){
			return getSubscription(attrSid);
		} else {			
			return null;
		}
	}
	
	
	/**
	 * Delete subscription.
	 */
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
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
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
	
	// TODO documentation
	private void deleteSubscription(String sid){
		if (sid.equals("2734acea-9b8f-43f6-b17e-34f9c73e8513")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
}
