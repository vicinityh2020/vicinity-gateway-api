package eu.bavenir.ogwapi.restapi.services;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.restapi.Api;
import eu.bavenir.ogwapi.commons.CommunicationManager;
import eu.bavenir.ogwapi.commons.messages.MessageResolver;

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
 *   SPECIFICATION:		@see <a href="https://vicinityh2020.github.io/vicinity-gateway-api/#/">Gateway API</a>
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

	/**
	 * Name of the Thing descriptions attribute.
	 */
	private static final String ATTR_TDS = "thingDescriptions";
	
	/**
	 * Name of the Thing description attribute.
	 */
	private static final String ATTR_TD = "thingDescription";
	
	/**
	 * Name of the Pagen attribute.
	 */
	private static final String ATTR_PAGE = "page";
	
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
		
		Map<String, String> queryParams = getQuery().getValuesMap();
		
		boolean attrObjectsWithTDs = false;
		
		if (queryParams.get(ATTR_TDS) != null) {
			attrObjectsWithTDs = Boolean.parseBoolean(queryParams.get(ATTR_TDS));
		}
		
		int attrPage = 0;
		
		if (queryParams.get(ATTR_PAGE) != null) {
			attrPage = Integer.parseInt(queryParams.get(ATTR_PAGE));
		}
		
		if (attrObjectsWithTDs) {
			return getObjectsTDs(attrPage);
		} else {
			return getObjects();	
		}
		
		
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
	
	/**
	 * Goes through the object's roster and creates a JSON TDs from the visible records.
	 * 
	 * 
	 * @return JSON representation of the list. 
	 */
	private Representation getObjectsTDs(int pageNumber){
		
		CommunicationManager communicationManager = (CommunicationManager) getContext().getAttributes().get(Api.CONTEXT_COMMMANAGER);
		
		return communicationManager.getThingDescriptions(getRequest().getChallengeResponse().getIdentifier(), pageNumber);
	}

	/**
	 * Creates a JSON array from a string. 
	 * 
	 * @param jsonString A string that is to be decoded as a JSON.
	 * @return JsonArray if the decoding was successful, or null if something went wrong (string is not a valid JSON etc.).  
	 */
	public JsonArray readJsonArray(String jsonString) {
		
		if (jsonString == null) {
			return null;
		}
		
		// make a JSON from the incoming String - any string that is not a valid JSON will throw exception
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		
		JsonArray json;
		
		try {
			json = jsonReader.readArray();
		} catch (Exception e) {
			System.out.println("Exception during reading JSON array: " 
						+ e.getMessage());
			
			return null;
		} finally {
			jsonReader.close();
		}
		
		return json;
	}
}
