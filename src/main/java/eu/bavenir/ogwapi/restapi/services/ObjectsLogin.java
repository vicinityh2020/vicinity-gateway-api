package eu.bavenir.ogwapi.restapi.services;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.commons.messages.StatusMessage;

/*
 * STRUCTURE
 * - constants
 * - public methods overriding HTTP methods 
 * - private methods
 */

/**
 * This class implements login service for objects, that are trying to authenticate against the network. Although
 * it is not really necessary to have a separate login service while utilising HTTP Authentication Schemes,
 * (since the gateway automatically logs in an object that makes a call and has not been logged in yet), it can be
 * useful for devices that only need to listen (i.e. they don't need to use any of the services, they just need to
 * be reachable). 
 * 
 * The service does actually nothing, all the authentication stuff is happening in the 
 * {@link eu.bavenir.ogwapi.restapi.security.AuthenticationVerifier AuthenticationVerifier} that is called
 * as a RESTLET guard in the API router.
 * 
 *   URL: 				[server]:[port]/api/objects/login
 *   METHODS: 			GET
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
	 * @return A {@link StatusMessage StatusMessage} indicating that login is successful.
	 */
	@Get
	public Representation represent() {
		
		// if the login is unsuccessful, the execution will never reach this place
		// the status message has to be created a new - there is no easy way how to propagate it from the REST
		// authentication verifier.
		StatusMessage statusMessage = new StatusMessage(false, StatusMessage.MESSAGE_LOGIN, "Login successfull.");
		
		return new JsonRepresentation(statusMessage.buildMessage().toString());
	}
	
	// === PRIVATE METHODS ===
	
	// none
}
