package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/*
 * STRUCTURE
 * - constants
 * - public methods overriding HTTP methods 
 * - private methods
 */

/**
 * This class implements login service for objects, that are trying to authenticate against the XMPP network. Although
 * it is not really necessary to have a separate login service while utilizing HTTP Authentication Schemes,
 * (since the gateway automatically logs in an object that makes a call and has not been logged in yet), it can be
 * useful for devices that only need to listen (i.e. they don't need to use any of the services, they just need to
 * be reachable). The service does actually nothing, all the authentication stuff is happening in the 
 * {@link eu.bavenir.vicinity.gatewayapi.restapi.security.AuthenticationVerifier AuthenticationVerifier} that is called
 * as a RESTLET guard in the API router.
 * 
 *   URL: 				[server]:[port]/api/objects/login
 *   METHODS: 			GET
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class ObjectsLogin extends ServerResource{

	// === CONSTANTS ===
	
	// none
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Answers the GET call, after it is authenticated.
	 * 
	 * @return A String indicating successful login.
	 */
	@Get
	public String represent() {
		
		// if the login is unsuccessful, the execution will never reach this place
		return "Login successful.";
	}
	
	// === PRIVATE METHODS ===
	
	// none
}
