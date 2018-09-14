package eu.bavenir.ogwapi.commons.messages;

import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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
public class NetworkMessageEvent extends NetworkMessage{

	/* === CONSTANTS === */

	/**
	 * This is how this request type of message should be marked in the {@link #messageType messageType}.
	 */
	public static final int MESSAGE_TYPE = 0x03;
	
	/**
	 * Name of the event source object ID attribute.
	 */
	private static final String ATTR_EVENTSOURCEOBJECTID = "sourceOid";
	
	/**
	 * Name of the event source object ID attribute.
	 */
	private static final String ATTR_EVENTID = "eid";
	
	/**
	 * Name of the event source object ID attribute.
	 */
	private static final String ATTR_EVENTBODY = "body";
	
	
	/* === FIELDS === */
	
	
	/**
	 * Although the destination OID is not passed in the arriving JSON, during the execution it is wise to set 
	 * this field to an ID of the object that is the destination for this event (by the appropriate 
	 * {@link ConnectionDescriptor ConnectionDescriptor} AFTER it arrives). 
	 * 
	 * This is not mandatory, but it helps keeping track and better control of where this should be routed.
	 *   
	 */
	private String destinationOid;
	
	/**
	 * ID of the object that generated this event.
	 */
	private String sourceOid;
	
	/**
	 * ID of the event. 
	 */
	private String eventID;
	
	/**
	 * The event itself. 
	 */
	private String eventBody;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor of an event message that is to be sent across the network.  
	 */
	public NetworkMessageEvent(XMLConfiguration config, String sourceOid, String eventID, String eventBody, Logger logger) {
		// always call this guy
		super(config, logger);
		
		destinationOid = null;
		
		this.sourceOid = sourceOid;
		this.eventID = eventID;
		this.eventBody = eventBody;
		
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
		
		destinationOid = null;
		sourceOid = null;
		eventID = null;
		eventBody = null;
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			setValid(false);
		}
	}
	
	
	
	
	public String getDestinationOid() {
		return destinationOid;
	}


	public void setDestinationOid(String destinationOid) {
		this.destinationOid = destinationOid;
	}


	/**
	 * 
	 * @return
	 */
	public String getEventSource() {
		return sourceOid;
	}


	/**
	 * 
	 * @param eventSource
	 */
	public void setEventSource(String eventSource) {
		this.sourceOid = eventSource;
	}


	/**
	 * 
	 * @return
	 */
	public String getEventID() {
		return eventID;
	}


	/**
	 * 
	 * @param eventID
	 */
	public void setEventID(String eventID) {
		this.eventID = eventID;
	}


	public String getEventBody() {
		return eventBody;
	}


	public void setEventBody(String eventBody) {
		this.eventBody = eventBody;
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
			.add(ATTR_EVENTSOURCEOBJECTID, sourceOid)
			.add(ATTR_EVENTID, eventID);
		
		if (eventBody == null){
			mainBuilder.addNull(ATTR_EVENTBODY);
		} else {
			mainBuilder.add(ATTR_EVENTBODY, eventBody);
		}
		
		jsonRepresentation = mainBuilder.build();
		
	}
	
	
	
	/**
	 * Takes the JSON object and fills necessary fields with values.
	 * 
	 * @param json JSON to parse.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJson(JsonObject json){
		
		// figure out the message type
		messageType = json.getInt(NetworkMessage.ATTR_MESSAGETYPE);

		// get the event source object ID - can't be null
		if (!json.isNull(NetworkMessageEvent.ATTR_EVENTSOURCEOBJECTID)){
			sourceOid = removeQuotes(json.getString(NetworkMessageEvent.ATTR_EVENTSOURCEOBJECTID));
		} else {
			return false;
		}
		
		// get the event ID - can't be null
		if (!json.isNull(NetworkMessageEvent.ATTR_EVENTID)){
			eventID = removeQuotes(json.getString(NetworkMessageEvent.ATTR_EVENTID));
		} else {
			return false;
		}
		
		// get the event body - this can be be null
		if (!json.isNull(NetworkMessageEvent.ATTR_EVENTBODY)){
			eventBody = removeQuotes(json.getString(NetworkMessageEvent.ATTR_EVENTBODY));
		} 
		
		return true;
	}
}
