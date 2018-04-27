package eu.bavenir.ogwapi.restapi.services;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

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
 * This class implements logout service for object, that needs to log out of the XMPP network. Usually, while 
 * utilizing HTTP Authentication Schemes, it is not necessary to log out, since the RESTLET provides state-less 
 * services. However, since the Gateway API works as a translator between state-less API and the state-full XMPP 
 * network, it is necessary to include such a service to log out of the network. 
 * 
 *   URL: 				[server]:[port]/api/objects/logout
 *   METHODS: 			GET
 *   
 * @author sulfo
 *
 */
public class ObjectsLogout extends ServerResource{

	// === CONSTANTS ===
	
	// none
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	@Get
	public Representation represent() {
		
		logoutObject();
		
		StatusMessage statusMessage = new StatusMessage(false, "logout", "success");
		
		return new JsonRepresentation(statusMessage.buildMessage().toString());
	}
	
	
	// === PRIVATE METHODS ===
	
	private void logoutObject() {
		
		CommunicationManager communicationManager = (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		communicationManager.terminateConnection(
				getRequest().getChallengeResponse().getIdentifier(), 
				true);
	}
	
}
