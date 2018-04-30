package eu.bavenir.ogwapi.restapi.services;

import java.util.Set;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.restapi.Api;
import eu.bavenir.ogwapi.commons.CommunicationManager;

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
 *   URL: 				[server]:[port]/api/objects
 *   METHODS: 			GET
 *   
 * @author sulfo
 *
 */
public class Objects extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	/**
	 * Name of the Objects attribute. 
	 */
	private static final String ATTR_OBJECTS = "objects";

	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns all available (both exposed and discovered and all adapters) IoT objects that can be seen by that 
	 * particular object - this does not count objects that are offline.
	 * 
	 * 
	 * @return All VICINITY Identifiers of IoT objects fulfil the type and maximum constraint and own parameter OR 
	 * Object description if the OID is specified.
	 * 
	 */
	@Get
	public Representation represent() {
			
		return getObjects();
	}
	
	
	// === PRIVATE METHODS ===
	
	/**
	 * Goes through the object's roster and creates a JSON from the visible records.
	 * 
	 * @return JSON representation of the list. 
	 */
	private Representation getObjects(){
		
		CommunicationManager communicationManager = (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		Set<String> rosterObjects = communicationManager.getRosterEntriesForObject(
							getRequest().getChallengeResponse().getIdentifier());
		
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
		
		
		for (String entry : rosterObjects) {
			
			mainArrayBuilder.add(
						Json.createObjectBuilder().add(ATTR_OID, entry)
					);
		}
		
		mainObjectBuilder.add(ATTR_OBJECTS, mainArrayBuilder);

		return new JsonRepresentation(mainObjectBuilder.build().toString());
	}
	
	

}
