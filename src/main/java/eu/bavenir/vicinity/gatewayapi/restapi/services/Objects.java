package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.jivesoftware.smack.roster.RosterEntry;
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
 * This class implements a {@link org.restlet.resource.ServerResource ServerResource} interface for following
 * Gateway API calls:
 * 
 *   URL: 				[server]:[port]/api/objects
 *   METHODS: 			GET
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
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
	 * Name of the Type request parameter.
	 */
	private static final String QUERY_TYPE = "type";
	
	/**
	 * Name of the Limit request parameter.
	 */
	private static final String QUERY_LIMIT = "limit";
	
	/**
	 * Name of the Own request parameter.
	 */
	private static final String QUERY_OWN = "own";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns all available (both exposed and discovered and all adapters) IoT objects managed by VICINITY 
	 * Open Gateway API
	 * 
	 * @param type Filter by object type identifier (from common VICINITY format) (from request).
	 * @param limit Maximum number of objects should be returned (from request).
	 * @param own True returns only exposed objects, false return all objects (from request).
	 * 
	 * @return All VICINITY Identifiers of IoT objects fulfill the type and maximum constraint and own parameter OR 
	 * Object description if the OID is specified.
	 * 
	 */
	@Get
	public String represent() {
		
		String queryType = getQueryValue(QUERY_TYPE);
		String queryLimit = getQueryValue(QUERY_LIMIT);
		String queryOwn = getQueryValue(QUERY_OWN);
			
		return getObjects(queryType, queryLimit, queryOwn);
	}
	
	
	// === PRIVATE METHODS ===
	
	// TODO documentation
	private String getObjects(String queryType, String queryLimit, String queryOwn){
		
		CommunicationNode communicationNode = (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);
		
		Collection<RosterEntry> rosterObjects = communicationNode.getRosterEntriesForUser(
							getRequest().getChallengeResponse().getIdentifier());
		
		// TODO logs!!!
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
		
		
		for (RosterEntry entry : rosterObjects) {
			
			mainArrayBuilder.add(
						Json.createObjectBuilder().add(ATTR_OID, entry.getJid().toString())
					);
		}
		
		mainObjectBuilder.add(ATTR_OBJECTS, mainArrayBuilder);
		JsonObject json = mainObjectBuilder.build();
		
		return json.toString();
	}
	
	

}
