package eu.bavenir.ogwapi.commons.messages;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.configuration2.XMLConfiguration;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


// TODO documentation
// explain why there are two constructors
// explain that it works as a wrapper for the incomming json
/**
 * Although the destination OID is not passed in the arriving JSON, during the execution it is wise to set 
 * this field to an ID of the object that is the destination for this event (by the appropriate 
 * {@link ConnectionDescriptor ConnectionDescriptor} AFTER it arrives). 
 * 
 * This is not mandatory, but it helps keeping track and better control of where this should be routed.
 *   
 */

public class NetworkMessageEvent extends NetworkMessage{

	/* === CONSTANTS === */

	/**
	 * This is how this request type of message should be marked in the {@link #messageType messageType}.
	 */
	public static final int MESSAGE_TYPE = 0x03;
	
	/**
	 * Name of the event source object ID attribute.
	 */
	private static final String ATTR_EVENTID = "eid";
	
	/**
	 * Name of the event source object ID attribute.
	 */
	private static final String ATTR_EVENTBODY = "body";
	
	private static final String ATTR_PARAMETERS = "parameters";
	
	
	
	/* === FIELDS === */
			
	/**
	 * ID of the event. 
	 */
	private String eventId;
	
	/**
	 * The event itself. 
	 */
	private String eventBody;
	
	/**
	 * Map with parameter names and their values.
	 */
	private Map<String, String> parameters;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor of an event message that is to be sent across the network.  
	 */
	public NetworkMessageEvent(XMLConfiguration config, String sourceOid, String eventId, String eventBody, 
			Map<String, String> parameters, Logger logger) {
		// always call this guy
		super(config, logger);
		
		this.sourceOid = sourceOid;
		this.eventId = eventId;
		this.eventBody = eventBody;
		this.parameters = parameters;
		
		// mark down the correct type of message
		messageType = NetworkMessageEvent.MESSAGE_TYPE;
		
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the network. 
	 */
	public NetworkMessageEvent(JsonObject json, XMLConfiguration config, Logger logger){
		// always call this guy
		super(config, logger);
		
		// remember the json this message was created from
		jsonRepresentation = json;
		
		eventId = null;
		eventBody = null;
		parameters = new LinkedHashMap<String, String>();
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			
			setValid(false);
		}
	}


	/**
	 * 
	 * @return
	 */
	public String getEventId() {
		return eventId;
	}


	/**
	 * 
	 * @param eventId
	 */
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}


	public String getEventBody() {
		return eventBody;
	}


	public void setEventBody(String eventBody) {
		this.eventBody = eventBody;
	}

	
	/**
	 * Retrieves the parameters hash map.
	 * 
	 * @return Hash map with parameters.
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}	
	
	

	/**
	 * Returns a JSON String that is to be sent over the network. The String is build from all the attributes that
	 * were set with getters and setters. Use this when you are finished with setting the attributes, parameters etc.
	 * 
	 * @return JSON String that can be sent over the network.
	 */
	public String buildMessageString(){
		
		buildMessageJson();
		
		return jsonRepresentation.toString();
	}
	
	
	/* === PRIVATE METHODS === */
	
	
	/**
	 * Takes all the necessary fields, attributes and parameters and assembles a valid JSON that can be sent over the
	 * network. 
	 * 
	 */
	// remind of certain attributes that must not be null
	private void buildMessageJson(){
		
		// create the factory
		JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
		
		// build the thing
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		mainBuilder.add(ATTR_MESSAGETYPE, messageType)
			.add(ATTR_SOURCEOID, sourceOid)
			.add(ATTR_EVENTID, eventId);
		
		if (eventBody == null){
			mainBuilder.addNull(ATTR_EVENTBODY);
		} else {
			mainBuilder.add(ATTR_EVENTBODY, eventBody);
		}
		
		
		// turn parameters into json
		JsonObjectBuilder parametersBuilder = jsonBuilderFactory.createObjectBuilder();
		if (!parameters.isEmpty()){
			for (Map.Entry<String, String> entry : parameters.entrySet()){
				// watch out for nulls
				if (entry.getValue() == null){
					parametersBuilder.addNull(entry.getKey());
				} else {
					parametersBuilder.add(entry.getKey(), entry.getValue());
				}
			}
		}
		
		mainBuilder.add(ATTR_PARAMETERS, parametersBuilder);
		
		jsonRepresentation = mainBuilder.build();
		
	}
	
	
	
	/**
	 * Takes the JSON object and fills necessary fields with values.
	 * 
	 * @param json JSON to parse.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJson(JsonObject json){
		
		// first check out whether or not the message has everything it is supposed to have and stop if not
		if (
				!json.containsKey(ATTR_MESSAGETYPE) ||
				!json.containsKey(ATTR_SOURCEOID) ||
				!json.containsKey(ATTR_EVENTID) ||
				!json.containsKey(ATTR_EVENTBODY) ||
				!json.containsKey(ATTR_PARAMETERS)) {
			
			return false;
		}
		
		// prepare objects for parameters and attributes
		JsonObject parametersJson = null;
		
		// load values from JSON
		try {
			
			messageType = json.getInt(NetworkMessage.ATTR_MESSAGETYPE);
			
			// null values are special cases in JSON, they get transported as "null" string and must be treated
			// separately 
			if (!json.isNull(ATTR_SOURCEOID)) {
				sourceOid = json.getString(ATTR_SOURCEOID);
			}
			
			if (!json.isNull(ATTR_EVENTID)) {
				eventId = json.getString(ATTR_EVENTID);
			}
			
			if (!json.isNull(ATTR_EVENTBODY)) {
				eventBody = json.getString(ATTR_EVENTBODY);
			}
			
			if (!json.isNull(ATTR_PARAMETERS)) {
				parametersJson = json.getJsonObject(ATTR_PARAMETERS);
			}
			
		} catch (Exception e) {
			logger.severe("NetworkMessageEvent: Exception while parsing NetworkMessageEvent: " + e.getMessage());
			
			return false;
		}
		
		// process non primitives, start with strings
		
		sourceOid = removeQuotes(sourceOid);
		eventId = removeQuotes(eventId);
		eventBody = removeQuotes(eventBody);
		
		// important
		if (sourceOid == null || eventId == null) {
			return false;
		}
		 
		// here the parameters will be stored during reading
		Set<Entry<String,JsonValue>> entrySet;
		String stringValue;
				
		// this can be null, but that should not be dangerous. we'll just leave the set clear in such case
		if (parametersJson != null){
			
			entrySet = parametersJson.entrySet();
			for (Entry<String, JsonValue> entry : entrySet) {

				// we have to remove the quotes
				stringValue = removeQuotes(entry.getValue().toString());
				
				// and the null value got transported more like string... we have to make a rule for it
				if (stringValue.equals("null")){
					parameters.put(entry.getKey(), null);
				} else {
					parameters.put(entry.getKey(), stringValue);
				}
			}
		}
		
		return true;
	}
}
