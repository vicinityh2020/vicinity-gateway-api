package eu.bavenir.ogwapi.restapi.services;

import java.io.IOException;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.commons.CommunicationManager;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;
import eu.bavenir.ogwapi.restapi.Api;


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
 *   URL: 				[server]:[port]/api/events/{eid}
 *   METHODS: 			POST, PUT, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		eid - Alpha numerical Event identifier (as in object description) (e.g. fullyCharged).
 *   
 * @author sulfo
 *
 */
public class EventsEid extends ServerResource {

	// === CONSTANTS ===
	
	/**
	 * Name of the Event ID attribute.
	 */
	private static final String ATTR_EID = "eid";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Used by an Agent/Adapter, that is capable of generating events and is willing to send these events to subscribed 
	 * objects. A call to this end point activates the channel – from that moment, other objects in the network are able
	 * to subscribe for receiving those messages.
	 *  
	 * @param entity Optional JSON with parameters.
	 * @return statusMessage {@link StatusMessage StatusMessage} with the result of the operation.
	 */
	@Post("json")
	public Representation accept(Representation entity) {
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrEid == null){
			logger.info("EID: " + attrEid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		CommunicationManager communicationManager 
						= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		StatusMessage statusMessage = communicationManager.activateEventChannel(callerOid, attrEid);
		
		return new JsonRepresentation(statusMessage.buildMessage().toString());
	}
	
	
	
	/**
	 * Used by an Agent/Adapter that is capable of generating events, to send an event to all subscribed objects on 
	 * the network.
	 * 
	 * @param entity JSON with the event.
	 * @return statusMessage {@link StatusMessage StatusMessage} with the result of the operation.
	 */
	@Put("json")
	public Representation store(Representation entity) {
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		// check the mandatory attributes
		if (attrEid == null){
			logger.info("EID: " + attrEid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		CommunicationManager communicationManager 
						= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		
		// check the body of the event to be sent
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("EID: " + attrEid + "\nInvalid event body - must be a valid JSON.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid event body - must be a valid JSON.");
		}
		
		// get the json
		String eventJsonString = null;
		try {
			eventJsonString = entity.getText();
		} catch (IOException e) {
			logger.info(e.getMessage());
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid event body");
		}
		
		StatusMessage statusMessage 
						= communicationManager.sendEventToSubscribedObjects(callerOid, attrEid, eventJsonString);
		
		return new JsonRepresentation(statusMessage.buildMessage().toString());
	}
	
	
	/**
	 * Used by an Agent/Adapter that is capable of generating events to de-activate an event channel. This will 
	 * prohibit any other new objects to subscribe to that channel, and the objects that are already subscribed are 
	 * notified and removed from subscription list.
	 * 
	 * @return statusMessage {@link StatusMessage StatusMessage} with the result of the operation.
	 */
	@Delete
	public Representation remove() {
		String attrEid = getAttribute(ATTR_EID);
		String callerOid = getRequest().getChallengeResponse().getIdentifier();
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		if (attrEid == null){
			logger.info("EID: " + attrEid + " Given identifier does not exist.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
		CommunicationManager communicationManager 
						= (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		StatusMessage statusMessage = communicationManager.deactivateEventChannel(callerOid, attrEid);
		
		return new JsonRepresentation(statusMessage.buildMessage().toString());
	}
	
	
	// === PRIVATE METHODS ===
	
}
