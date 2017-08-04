package eu.bavenir.vicinity.gatewayapi.xmpp;

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

/**
 * Extended {@link NetworkMessage NetworkMessage} that represents a request. In order to transport a request across XMPP
 * network, it has to be disassembled at the place of origin (HTTP method, URL attributes, parameters, etc. have to 
 * be parsed), sent over the XMPP network (in this case as JSON string) and then reassembled at the destination into
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
 *  	be sent through {@link CommunicationNode CommunicationNode}.
 *  6. Expect {@link NetworkMessageResponse NetworkMessageResponse} by using 
 *  {@link CommunicationNode#retrieveSingleMessage(String, int) retireveSingleMessage} method.
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
	public static final String ATTR_REQUESTOPERATION = "requestOperation";
	
	/**
	 * Name of the attributes field in JSON that is to be sent. 
	 */
	public static final String ATTR_ATTRIBUTES = "attributes";
	
	/**
	 * Name of the parameters field in JSON that is to be sent. 
	 */
	public static final String ATTR_PARAMETERS = "parameters";
	
	/**
	 * Name of the request body attribute.
	 */
	public static final String ATTR_REQUESTBODY = "requestBody";
	
	/**
	 * Value that is used, when the requested operation is HTTP GET.
	 */
	public static final String REQUEST_OPERATION_GET = "get";
	
	/**
	 * Value that is used, when the requested operation is HTTP PUT.
	 */
	public static final String REQUEST_OPERATION_PUT = "put";
	
	/**
	 * Value that is used, when the requested operation is HTTP DELETE.
	 */
	public static final String REQUEST_OPERATION_DEL = "delete";
	
	/**
	 * Value that is used, when the requested operation is HTTP POST.
	 */
	public static final String REQUEST_OPERATION_POST = "post";
	
	
	/* === FIELDS === */
	
	/**
	 * This variable specifies which HTTP method is to be utilized on remote object. Valid values are:
	 * 
	 * 	{@link #REQUEST_OPERATION_GET REQUEST_OPERATION_GET}
	 *  {@link #REQUEST_OPERATION_PUT REQUEST_OPERATION_PUT}
	 *  {@link #REQUEST_OPERATION_POST REQUEST_OPERATION_POST}
	 *  {@link #REQUEST_OPERATION_DELETE REQUEST_OPERATION_DELETE}
	 */
	private String requestOperation;
	
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
	
	/**
	 * The JSON object that can be a result of two events:
	 *  - Either a JSON arrived in a message and is saved here while it was being parsed and fields of this object
	 *  	were filled. It is useful to keep it here, because parsing can fail, making the message invalid, in which 
	 *  	case the validity {@link NetworkMessage#valid flag} is set to false. While the JSON is stored here, it can 
	 *  	be logged somewhere conveniently.
	 *  - A message to be sent is being built, and after all necessary fields, parameters and attributes are set, the 
	 *  	JSON is assembled using {@link #buildMessageString() build} method. This would overwrite the JSON from the
	 *  	first event, if the same message object is used (which should not happen). 
	 */
	private JsonObject jsonRepresentation;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor of a request message that is to be sent across XMPP network. Request ID is computed during 
	 * object's construction. The rest of attributes, codes, parameters, etc. need to be filled as needed.  
	 * 
	 */
	public NetworkMessageRequest(XMLConfiguration config){
		// always call this guy
		super(config);

		jsonRepresentation = null;
		
		messageType = NetworkMessageRequest.MESSAGE_TYPE;
		
		generateRequestId();
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the XMPP network. 
	 */
	public NetworkMessageRequest(JsonObject json, XMLConfiguration config){
		// always call this guy
		super(config);
		
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
	 * Returns a JSON String that is to be sent over the XMPP network. The String is build from all the attributes that
	 * were set with getters and setters. Use this when you are finished with setting the attributes, parameters etc.
	 * 
	 * @return JSON String that can be sent over the XMPP network.
	 */
	public String buildMessageString(){
		
		buildMessageJson();
		
		return jsonRepresentation.toString();
	}
	
	
	/**
	 * If this NetworkMessage instance is a request, returns the HTTP request method that is to be used.
	 * 
	 * @return 	{@link #REQUEST_OPERATION_GET REQUEST_OPERATION_GET}
	 *  		{@link #REQUEST_OPERATION_PUT REQUEST_OPERATION_PUT}
	 *  		{@link #REQUEST_OPERATION_POST REQUEST_OPERATION_POST}
	 *  		{@link #REQUEST_OPERATION_DELETE REQUEST_OPERATION_DELETE}
	 */
	public String getRequestOperation() {
		return requestOperation;
	}


	/**
	 * If this NetworkMessage instance is a request, sets the HTTP request method that is to be used.
	 * 
	 * @param requestOperation Valid values are:
	 * 			{@link #REQUEST_OPERATION_GET REQUEST_OPERATION_GET}
	 *  		{@link #REQUEST_OPERATION_PUT REQUEST_OPERATION_PUT}
	 *  		{@link #REQUEST_OPERATION_POST REQUEST_OPERATION_POST}
	 *  		{@link #REQUEST_OPERATION_DELETE REQUEST_OPERATION_DELETE}
	 */
	public void setRequestOperation(String requestOperation) {
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
	 * Takes all the necessary fields, attributes and parameters and assembles a valid JSON that can be sent over XMPP
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
		requestOperation = json.getString(NetworkMessageRequest.ATTR_REQUESTOPERATION);
		
		if (
				!(requestOperation.equals(NetworkMessageRequest.REQUEST_OPERATION_GET)
				|| requestOperation.equals(NetworkMessageRequest.REQUEST_OPERATION_DEL)
				|| requestOperation.equals(NetworkMessageRequest.REQUEST_OPERATION_POST)
				|| requestOperation.equals(NetworkMessageRequest.REQUEST_OPERATION_PUT))
			){
				
			setValid(false);
			return false;
		}
		
		if (!json.isNull(NetworkMessageRequest.ATTR_REQUESTBODY)){
			requestBody = removeQuotes(json.getString(NetworkMessageRequest.ATTR_REQUESTBODY));
		}
		
		// here both the parameters and attributes will during reading
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
	
}
