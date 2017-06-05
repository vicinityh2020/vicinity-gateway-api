package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import eu.bavenir.vicinity.gatewayapi.restapi.Api;
import eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode;


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
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class ObjectsLogout extends ServerResource{

	// === CONSTANTS ===
	
	// none
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	@Get
	public String represent() {
		
		logoutObject();
		
		return "Logout successful.";
	}
	
	
	// === PRIVATE METHODS ===
	
	private void logoutObject() {
		
		CommunicationNode communicationNode = (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);
		
		communicationNode.terminateConnection(
				getRequest().getChallengeResponse().getIdentifier(), 
				true);
	}
	
}
