package eu.bavenir.ogwapi.commons.messages;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Map.Entry;

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


/**
 * Extended {@link NetworkMessage NetworkMessage} that represents a request. Aside from fields inherited from parent
 * it also facilitates transport of request body, parameters and attributes (which are basically just identifiers 
 * of respective properties, actions, tasks, ...). 
 * 
 * OGWAPI uses it like this:
 * 
 *  1. It construct an instance of this class - uses the constructor without parameter.
 *  2. Calls {@link #setRequestOperation(byte) setRequestOperation} and sets the requested operation etc.
 *  3. Compiles a {@link java.util.Map Map} of attributes (like a property ID if there is a request that involves some 
 *     property etc.) and calls {@link #setAttributes(Map) setAttributes}.
 *  4. Does the same for parameters.
 *  5. Builds the message by {@link #buildMessageString() buildMessageString} and obtains a string to be sent across
 *     the network.
 *     
 * The final message is a JSON string, which was chosen as universal format to transport messages in the system. It was 
 * preferred over serialisation of the whole NetworkMessageRequest instance and sending it as a byte stream, because:
 * 
 *  a. There is no guarantee that future engines will be able to transmit such byte stream easily, but transport
 *     of a string should be a guaranteed feature of almost any protocol worth consideration (this is non-rigorous 
 *     assumption of course, but necessary in the early stages of the project).
 *  b. The final JSON string is smaller than a byte stream of serialised NetworkMessageRequest instance (common sense :)). 
 *  
 *  If there are any modification to this class, they will probably be about extending the range of operations or 
 *  adding more fields that will be transported over the network. In the first case, add a constant with new operation code 
 *  into the list and don't forget to add it also into the {@link #validateRequestOperation(byte) validateRequestOperation}.
 *  In the second case don't forget to put the new field into the {@link #parseJson(JsonObject) parser} and {@link #buildMessageJson() builder}.
 *  Note that such modification will make the new OGWAPI incompatible with the previous versions.
 *  
 * @author sulfo
 *
 */
public class NetworkMessageRequest extends NetworkMessage {

	/* === CONSTANTS === */
	
	/**
	 * This is how this request type of message should be marked in the {@link #messageType messageType}.
	 */
	public static final int MESSAGE_TYPE = 0x01;
	
	/**
	 * Name of the request operation attribute in JSON that is to be sent. 
	 */
	private static final String ATTR_REQUESTOPERATION = "requestOperation";
	
	/**
	 * Name of the attributes field in JSON that is to be sent. 
	 */
	private static final String ATTR_ATTRIBUTES = "attributes";
	
	/**
	 * Name of the parameters field in JSON that is to be sent. 
	 */
	private static final String ATTR_PARAMETERS = "parameters";
	
	/**
	 * Name of the request body attribute in the JSON.
	 */
	private static final String ATTR_REQUESTBODY = "requestBody";
	
	
	/**
	 * How the property ID is to be marked in the message.
	 */
	public static final String ATTR_PID = "pid";
	
	
	/**
	 * How the action ID is to be marked in the message.
	 */
	public static final String ATTR_AID = "aid";
	
	
	/**
	 * How the event ID is to be marked in the message.
	 */
	public static final String ATTR_EID = "eid";
	
	
	/**
	 * How the task ID is to be marked in the message.
	 */
	public static final String ATTR_TID = "tid";
	
	
	/**
	 * Operation ID for getting a list of properties.
	 */
	public static final byte OPERATION_GETLISTOFPROPERTIES = 0x00;
	
	/**
	 * Operation ID for getting property value.
	 */
	public static final byte OPERATION_GETPROPERTYVALUE = 0x01;
	
	/**
	 * Operation ID for setting property value.
	 */
	public static final byte OPERATION_SETPROPERTYVALUE = 0x02;
	
	/**
	 * Operation ID for getting a list of actions. 
	 */
	public static final byte OPERATION_GETLISTOFACTIONS = 0x03;
	
	/**
	 * Operation ID for starting an action.
	 */
	public static final byte OPERATION_STARTACTION = 0x04;
	
	/**
	 * Operation ID for getting task status.
	 */
	public static final byte OPERATION_GETTASKSTATUS = 0x05;
	
	/**
	 * Operation ID for cancelling a task.
	 */
	public static final byte OPERATION_CANCELTASK = 0x06;
	
	/**
	 * Operation ID for getting a list of events.
	 */
	public static final byte OPERATION_GETLISTOFEVENTS = 0x07;
	
	/**
	 * Operation ID getting event channel status.  
	 */
	public static final byte OPERATION_GETEVENTCHANNELSTATUS = 0x08;
	
	/**
	 * Operation ID for subscribing to event channel.
	 */
	public static final byte OPERATION_SUBSCRIBETOEVENTCHANNEL = 0x09;
	
	/**
	 * Operation ID for cancelling subscription to an event channel.
	 */
	public static final byte OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL = 0x0A;
	
	/**
	 * Operation ID for getting a thing description.
	 */
	public static final byte OPERATION_GETTHINGDESCRIPTION = 0x0B;
	
	// IMPORTANT NOTE: If adding new operation codes, add them also to the verification method at the end 
	// of this class - in the private methods section. 
	

	
	
	/* === FIELDS === */
	
	/**
	 * This variable specifies which operation should be requested from the remote site. Valid values are the constants
	 * of this class starting with OPERATION prefix.
	 */
	private byte requestOperation;
	
	/**
	 * Map with attribute names and their values.
	 */
	private Map<String, String> attributes;
	
	/**
	 * Map with parameter names and their values.
	 */
	private Map<String, String> parameters;
	
	/**
	 * Request body. 
	 */
	private String requestBody;
	
	
	/* === PUBLIC METHODS === */
	/**
	 * Constructor of a request message that is to be sent across the network. Request ID is computed during 
	 * object's construction. The rest of attributes, codes, parameters, etc. need to be filled as needed. 
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NetworkMessageRequest(XMLConfiguration config, Logger logger){
		// always call this guy
		super(config, logger);
		
		initialise();
		
		messageType = NetworkMessageRequest.MESSAGE_TYPE;
		
		generateRequestId();
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the network. 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NetworkMessageRequest(JsonObject json, XMLConfiguration config, Logger logger){
		// always call this guy
		super(config, logger);
		
		initialise();
		
		// remember the json this message was created from
		jsonRepresentation = json;
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			setValid(false);
		}
	}
	
	
	/**
	 * Retrieves the request's body.
	 * 
	 * @return Body of the request.
	 */
	public String getRequestBody() {
		return requestBody;
	}


	/**
	 * Sets the request's body.
	 * 
	 * @param requestBody Body of the request.
	 */
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}


	/**
	 * Returns a JSON String that is to be sent over the network. The String is build from all the fields that
	 * were set with getters and setters. Use this when you are finished with setting the attributes, parameters etc.
	 * 
	 * @return JSON String that can be sent over the network.
	 */
	public String buildMessageString(){
		
		buildMessageJson();
		
		return jsonRepresentation.toString();
	}
	
	
	/**
	 * Returns the request operation that is to be executed. Valid values are the constants
	 * of this class starting with OPERATION prefix.
	 * 
	 * @return 	Request operation identifier.
	 */
	public byte getRequestOperation() {
		return requestOperation;
	}


	/**
	 * Sets the request operation that is to be executed. Valid values are the constants
	 * of this class starting with OPERATION prefix.
	 * 
	 * @param requestOperation Request operation identifier.
	 */
	public void setRequestOperation(byte requestOperation) {
		this.requestOperation = requestOperation;
	}
	
	
	/**
	 * Retrieves the attributes hash map.
	 * 
	 * @return Hash map with attributes.
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}


	/**
	 * Retrieves the parameters hash map.
	 * 
	 * @return Hash map with parameters.
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	
	/**
	 * Sets the {@link java.util.Map Map} filled with parameters.
	 * 
	 * @param parameters Map of parameters.
	 */
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	
	/**
	 * Sets the {@link java.util.Map Map} filled with attributes.
	 * 
	 * @param attributes Map of attributes.
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * This will generate random request ID of the message. 
	 */
	private void generateRequestId() {
		
		Random rand = new Random(); 
		requestId = rand.nextInt(); 
		
		if (requestId < 0){
			requestId = requestId * (-1);
		}
	}
	
	
	/**
	 * Takes all the necessary fields, attributes and parameters and assembles a valid JSON that can be sent over the
	 * network. 
	 * 
	 */
	private void buildMessageJson(){
		
		// create the factory
		JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
		
		// turn attributes into json
		JsonObjectBuilder attributesBuilder = jsonBuilderFactory.createObjectBuilder();
		if (!attributes.isEmpty()){
			for (Map.Entry<String, String> entry : attributes.entrySet()){
				// watch out for nulls
				if (entry.getValue() == null){
					attributesBuilder.addNull(entry.getKey());
				} else {
					attributesBuilder.add(entry.getKey(), entry.getValue());
				}
			}
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
		
		// build the thing
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		mainBuilder.add(ATTR_MESSAGETYPE, messageType)
			.add(ATTR_REQUESTID, requestId)
			.add(ATTR_SOURCEOID, sourceOid)
			.add(ATTR_DESTINATIONOID, destinationOid)
			.add(ATTR_REQUESTOPERATION, requestOperation);
		
		if (requestBody == null){
			mainBuilder.addNull(ATTR_REQUESTBODY);
		} else {
			mainBuilder.add(ATTR_REQUESTBODY, requestBody);
		}
		
		mainBuilder.add(ATTR_ATTRIBUTES, attributesBuilder)
			.add(ATTR_PARAMETERS, parametersBuilder);
		
		jsonRepresentation = mainBuilder.build();
		
	}
	
	
	/**
	 * Takes the JSON object and fills necessary fields with values. There are attributes that needs to be included in 
	 * the JSON that is to be parsed ( see the implementation ). Therefore the JSON is first checked for these attributes
	 * and it is not until the check is complete when the parsing itself starts. 
	 * 
	 * 
	 * @param json JSON to be parsed.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJson(JsonObject json){
		
		// first check out whether or not the message has everything it is supposed to have and stop if not
		if (
				!json.containsKey(ATTR_MESSAGETYPE) ||
				!json.containsKey(ATTR_REQUESTID) ||
				!json.containsKey(ATTR_SOURCEOID) ||
				!json.containsKey(ATTR_DESTINATIONOID) ||
				!json.containsKey(ATTR_REQUESTOPERATION) ||
				!json.containsKey(ATTR_REQUESTBODY) ||
				!json.containsKey(ATTR_ATTRIBUTES) ||
				!json.containsKey(ATTR_PARAMETERS)) {
			
			return false;
		}
		
		// prepare objects for parameters and attributes
		JsonObject attributesJson = null;
		JsonObject parametersJson = null;
		
		// load values from JSON
		try {
			
			messageType = json.getInt(ATTR_MESSAGETYPE);
			requestId = json.getInt(NetworkMessage.ATTR_REQUESTID);
			requestOperation = (byte) json.getInt(ATTR_REQUESTOPERATION);
			
			// null values are special cases in JSON, they get transported as "null" string and it requires special
			// treatment
			if (!json.isNull(ATTR_SOURCEOID)) {
				sourceOid = json.getString(ATTR_SOURCEOID);
			}
			
			if (!json.isNull(ATTR_DESTINATIONOID)) {
				destinationOid = json.getString(ATTR_DESTINATIONOID);
			}
			
			if (!json.isNull(ATTR_ATTRIBUTES)) {
				attributesJson = json.getJsonObject(ATTR_ATTRIBUTES);
			}
			
			if (!json.isNull(ATTR_PARAMETERS)) {
				parametersJson = json.getJsonObject(ATTR_PARAMETERS);
			}
			
			if (!json.isNull(ATTR_REQUESTBODY)) {
				requestBody = json.getString(ATTR_REQUESTBODY);
			}
			
		} catch (Exception e) {
			logger.severe("NetworkMessageRequest: Exception while parsing NetworkMessageRequest: " + e.getMessage());
			
			return false;
		}
		
		// validate the request operation
		if (!validateRequestOperation(requestOperation)) {
			return false;
		}
		
		// process non primitives, start with strings
		
		sourceOid = removeQuotes(sourceOid);
		destinationOid = removeQuotes(destinationOid);
		requestBody = removeQuotes(requestBody);

		// request body can be null, therefore it is not checked, but the variables are important
		if (sourceOid == null || destinationOid == null) {
			return false;
		}

		
		// here both the parameters and attributes will be stored during reading
		Set<Entry<String,JsonValue>> entrySet;
		String stringValue;

		
		// this can be null, but that should not be dangerous. we'll just leave the set clear in such case
		if (attributesJson != null){
			
			entrySet = attributesJson.entrySet();
			for (Entry<String, JsonValue> entry : entrySet) {
				
				// we have to remove the quotes
				stringValue = removeQuotes(entry.getValue().toString());
				
				// and the null value got transported more like string... we have to make a rule for it
				if (stringValue.equals("null")){
					attributes.put(entry.getKey(), null);
				} else {
					attributes.put(entry.getKey(), stringValue);
				}
			}
		}
		
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
	
	
	
	/**
	 * Validates the request operation code. If the code is not among the codes that are enumerated by constants, 
	 * the verification fails. 
	 * 
	 * This check is preventing malicious remote gateways to ask us to do something, that this version of gateway is
	 * not capable of. 
	 * 
	 * NOTE: Every time there is a new operation implemented in the gateway, the operation code must be inserted among
	 * the constants as well as here.  
	 * 
	 * @param requestOperation Operation ID of the incoming message. 
	 * @return True if the operation ID is valid for this gateway, false otherwise.
	 */
	private boolean validateRequestOperation(byte requestOperation) {
		
		if (
				!(requestOperation == NetworkMessageRequest.OPERATION_CANCELTASK
				|| requestOperation == NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS
				|| requestOperation == NetworkMessageRequest.OPERATION_GETLISTOFACTIONS
				|| requestOperation == NetworkMessageRequest.OPERATION_GETLISTOFEVENTS
				|| requestOperation == NetworkMessageRequest.OPERATION_GETLISTOFPROPERTIES
				|| requestOperation == NetworkMessageRequest.OPERATION_GETPROPERTYVALUE
				|| requestOperation == NetworkMessageRequest.OPERATION_GETTASKSTATUS
				|| requestOperation == NetworkMessageRequest.OPERATION_SETPROPERTYVALUE
				|| requestOperation == NetworkMessageRequest.OPERATION_STARTACTION
				|| requestOperation == NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL
				|| requestOperation == NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL
				|| requestOperation == NetworkMessageRequest.OPERATION_GETTHINGDESCRIPTION
				
				)
			){
			
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Initialises empty fields.
	 */
	private void initialise() {
		requestOperation = 0x00;
		attributes = new LinkedHashMap<String, String>();
		parameters = new LinkedHashMap<String, String>();
		requestBody = null;
	}
	
}
