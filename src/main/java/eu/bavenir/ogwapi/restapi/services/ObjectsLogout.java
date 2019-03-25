package eu.bavenir.ogwapi.restapi.services;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.commons.messages.CodesAndReasons;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;
import eu.bavenir.ogwapi.restapi.Api;
import eu.bavenir.ogwapi.commons.CommunicationManager;


/*
 * STRUCTURE
 * - constants
 * - public methods overriding HTTP methods 
 * - private methods
 */

/**
 * This class implements logout service for object, that needs to log out of the network. Usually, while 
 * utilising HTTP Authentication Schemes, it is not necessary to log out, since the RESTLET provides state-less 
 * services. However, since the Gateway API works as a translator between state-less API and the state-full P2P 
 * network, it is necessary to include such a service to log out of the network. 
 * 
 *   URL: 				[server]:[port]/api/objects/logout
 *   METHODS: 			GET
 *   SPECIFICATION:		@see <a href="https://vicinityh2020.github.io/vicinity-gateway-api/#/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class ObjectsLogout extends ServerResource{

	// === CONSTANTS ===
	
	// none
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Logs the object that is calling this method out from the network.
	 * 
	 * @return {@link StatusMessage StatusMessage} with a success message.
	 */
	@Get
	public Representation represent() {
		
		logoutObject();
		
		// the status message has to be created a new - there is no easy way how to propagate it from the REST
		// authentication verifier.
		StatusMessage statusMessage = new StatusMessage(false, CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + "Logout successfull.", StatusMessage.CONTENTTYPE_APPLICATIONJSON);
		
		return new JsonRepresentation(statusMessage.buildMessage().toString());
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Destroys the connection descriptor for given object ID.
	 */
	private void logoutObject() {
		
		CommunicationManager communicationManager = 
						(CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		communicationManager.terminateConnection(
				getRequest().getChallengeResponse().getIdentifier(), 
				true);
	}
	
}
