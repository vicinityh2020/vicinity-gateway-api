package eu.bavenir.ogwapi.commons.messages;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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


// explain why there are two constructors
/**
 * Extended {@link NetworkMessage NetworkMessage} that represents a request. In order to transport a request across P2P
 * network, it has to be disassembled at the place of origin (HTTP method, URL attributes, parameters, etc. have to 
 * be parsed), sent over the network (in this case as JSON string) and then reassembled at the destination into
 * valid HTTP request.
 * 
 * Use it like this:
 * 
 * In your Gateway API Service implementation:
 *  1. Construct an instance of this class - use the constructor without parameter.
 *  2. Call {@link #setRequestOperation(String) setRequestOperation} and set GET, POST, etc.
 *  3. Call {@link #addAttribute(String, String) addAttribute} as many times as needed.
 *  4. Do the same as for parameters.
 *  5. Build the message by {@link #buildMessageJson buildMessageJson}. By calling its toString you obtain a string to 
 *  	be sent through {@link CommunicationManager CommunicationNode}.
 *  6. Expect {@link NetworkMessageResponse NetworkMessageResponse} by using 
 *  {@link CommunicationManager#retrieveSingleMessage(String,int) retireveSingleMessage} method.
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
	 * Name of the request method field in JSON that is to be sent. 
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
	 * Name of the request body attribute.
	 */
	private static final String ATTR_REQUESTBODY = "requestBody";
	
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
	
	// IMPORTANT NOTE: If adding new operation codes, add them also to the verification method at the end 
	// of this class - in the private methods section. 
	
	// TODO to be removed
	
	/**
	 * Value that is used, when the requested operation is HTTP GET.
	 */
	public static final byte REQUEST_OPERATION_GET = 0x10;
	
	/**
	 * Value that is used, when the requested operation is HTTP PUT.
	 */
	public static final byte REQUEST_OPERATION_PUT = 0x11;
	
	/**
	 * Value that is used, when the requested operation is HTTP DELETE.
	 */
	public static final byte REQUEST_OPERATION_DEL = 0x12;
	
	/**
	 * Value that is used, when the requested operation is HTTP POST.
	 */
	public static final byte REQUEST_OPERATION_POST = 0x13;
	
	// ========================
	
	
	
	/* === FIELDS === */
	
	/**
	 * This variable specifies which operation should be requested from the remote site. Valid values are the constants
	 * of this class starting with OPERATION prefix.
	 */
	private byte requestOperation;
	
	/**
	 * Linked hash map with attribute names and their values.
	 */
	private LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>();
	
	/**
	 * Linked hash map with parameter names and their values.
	 */
	private LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
	
	/**
	 * Request body. 
	 */
	private String requestBody;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor of a request message that is to be sent across the network. Request ID is computed during 
	 * object's construction. The rest of attributes, codes, parameters, etc. need to be filled as needed.  
	 * 
	 */
	public NetworkMessageRequest(XMLConfiguration config){
		// always call this guy
		super(config);
		
		messageType = NetworkMessageRequest.MESSAGE_TYPE;
		
		generateRequestId();
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the network. 
	 */
	public NetworkMessageRequest(JsonObject json, XMLConfiguration config){
		// always call this guy
		super(config);
		
		// remember the json this message was created from
		jsonRepresentation = json;
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			setValid(false);
		}
	}
	
	
	/**
	 * Retrieves the request's body.
	 * @return
	 */
	public String getRequestBody() {
		return requestBody;
	}


	/**
	 * Sets the request's body.
	 * @param requestBody
	 */
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
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
	 * Inserts attributes into the message. For the purposes of this message protocol implementation, attributes are 
	 * the parts of URL, that may contain keys (attribute names) with or without values.
	 * 
	 * /objects/{oid}/subscriptions -> 	example of a key with value (objects and {oid}) and a key with no value 
	 * 									(subscription). 
	 *  
	 * @param name Attribute name - a key.
	 * @param value Value of the attribute. Can be null.
	 */
	public void addAttribute(String name, String value){
		// we don't want null keys (although it is possible to have one...)
		if (name != null){
			attributes.put(name, value);
		}
	}
	
	
	/**
	 * Inserts parameters into the message. For the purposes of this message protocol implementation, parameters are
	 * the regular HTTP parameters, passed in the request.
	 * 
	 * @param name Parameter name - a key.
	 * @param value Value of the parameter. Can be null.
	 */
	public void addParameter(String name, String value){
		// we don't want null keys (although it is possible to have one...)
		if (name != null){
			parameters.put(name, value);
		}
	}
	
	
	/**
	 * Retrieves the attributes hash map.
	 * 
	 * @return Hash map with attributes.
	 */
	public LinkedHashMap<String, String> getAttributes() {
		return attributes;
	}


	/**
	 * Retrieves the parameters hash map.
	 * 
	 * @return Hash map with parameters.
	 */
	public LinkedHashMap<String, String> getParameters() {
		return parameters;
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
	 * Takes the JSON object and fills necessary fields with values.
	 * 
	 * @param json JSON to parse.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJson(JsonObject json){
		
		// figure out the message type
		messageType = json.getInt(NetworkMessage.ATTR_MESSAGETYPE);

		// the correlation ID of the request
		requestId = json.getInt(NetworkMessage.ATTR_REQUESTID);
		
		// get and validate the request operation
		requestOperation = (byte) json.getInt(NetworkMessageRequest.ATTR_REQUESTOPERATION);
		
		if (!validateRequestOperation(requestOperation)) {
			setValid(false);
			return false;
		}
		
		if (!json.isNull(NetworkMessageRequest.ATTR_REQUESTBODY)){
			requestBody = removeQuotes(json.getString(NetworkMessageRequest.ATTR_REQUESTBODY));
		}
		
		// here both the parameters and attributes will be stored during reading
		Set<Entry<String,JsonValue>> entrySet;
		String stringValue;

		
		// this can return null, but that should not be dangerous... much... we'll just leave the set clear
		JsonObject attributesJson = json.getJsonObject(ATTR_ATTRIBUTES);
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
		
		// this can return null, but that should not be dangerous... much... we'll just leave the set clear
		JsonObject parametersJson = json.getJsonObject(ATTR_PARAMETERS);
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
				
				// TODO remove these old ones
				|| requestOperation == NetworkMessageRequest.REQUEST_OPERATION_DEL
				|| requestOperation == NetworkMessageRequest.REQUEST_OPERATION_GET
				|| requestOperation == NetworkMessageRequest.REQUEST_OPERATION_POST
				|| requestOperation == NetworkMessageRequest.REQUEST_OPERATION_PUT
				)
			){
			
			return false;
		}
		
		return true;
	}
	
}
