package eu.bavenir.vicinity.gatewayapi.xmpp;

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


/**
 * Extended {@link NetworkMessage NetworkMessage} that represents a request. Same as {@link NetworkMessageRequest request}
 * but with HTTP response.
 * 
 * Use it like this:
 * 
 * In your Gateway API service:
 * 	1. Construct an instance of this class - use the constructor without parameter
 *  2. Call {@link #setResponseCode(int) setResponseCode} and pass the number returned from an Agent.
 *  3. Call {@link #setResponseBody(String) setResponseBody} and pass the response body from an Agent.
 *  4. Build the message by {@link #buildMessageJson buildMessageJson}. By calling its toString you obtain a string to 
 *  	be sent through {@link CommunicationNode CommunicationNode}. 
 * 
 * 
 * @author sulfo
 *
 */
public class NetworkMessageResponse extends NetworkMessage {
	
	/* === CONSTANTS === */
	
	/**
	 * This is how this request type of message should be marked in the {@link #messageType messageType}.
	 */
	public static final int MESSAGE_TYPE = 0x02;
	
	/**
	 * Name of the response status code attribute.
	 */
	public static final String ATTR_RESPONSECODE = "responseCode";
	
	/**
	 * Attribute name for the status code reason, returned by HTTP server on the remote site.  
	 */
	public static final String ATTR_RESPONSECODEREASON = "responseCodeReason";
	
	/**
	 * Name of the response body attribute.
	 */
	public static final String ATTR_RESPONSEBODY = "responseBody";

	
	/* === FIELDS === */
	
	/**
	 * HTTP response code from the remote object.
	 */
	private int responseCode;
	
	/**
	 * HTTP response code reason, in other words the status description.
	 */
	private String responseCodeReason;
	
	/**
	 * If this NetworkMessage was constructed from incoming XMPP message, here is the HTTP response body from the remote
	 * object.
	 */
	private String responseBody;
	
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
	 * Use this when parsing a message that was received from the XMPP network. The parsing will fill all the attributes. 
	 *  
	 * @param xmppMessage A 'raw' XMPP message. 
	 */
	public NetworkMessageResponse(XMLConfiguration config){
		super(config);
		
		jsonRepresentation = null;
		
		messageType = NetworkMessageResponse.MESSAGE_TYPE;
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the XMPP network. 
	 */
	public NetworkMessageResponse(JsonObject json, XMLConfiguration config){
		// always call this guy
		super(config);
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			setValid(false);
		}
	}
	

	/**
	 * Returns the HTTP response status code from the remote object.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @return HTTP response status code from the remote object.
	 */
	public int getResponseCode() {
		return responseCode;
	}


	/**
	 * Sets the HTTP response status code from the remote object.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @param responseCode Response status code of the remote object.
	 */
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	
	
	/**
	 * Returns the HTTP response status code reason from the remote object.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @return HTTP response status code reason phrase from the remote object.
	 */
	public String getResponseCodeReason(){
		return responseCodeReason;
	}
	
	
	/**
	 * Sets the HTTP response status code reason from the remote object.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @param responseCode Response status code reason phrase of the remote object.
	 */
	public void setResponseCodeReason(String responseCodeReason){
		this.responseCodeReason = responseCodeReason;
	}


	/**
	 * Returns the body of a response NetworkMessage. This is the actual data that are returned by the remote object.
	 *  
	 * @return Data from the remote object.
	 */
	public String getResponseBody() {
		return responseBody;
	}


	/**
	 * Sets the body of a response NetworkMessage. Use this when creating a response.
	 *  
	 * @param responseBody The actual data that are returned by the remote object.
	 */
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
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
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Takes the JSON object and fills necessary fields with values.
	 * 
	 * @param json JSON to parse.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJson(JsonObject json){
		// figure out the message type
		messageType = json.getInt(NetworkMessage.ATTR_MESSAGETYPE);
		
		if (messageType != MESSAGE_TYPE){
			// just a formality
			return false;
		}
		
		// the correlation ID of the request
		requestId = json.getInt(NetworkMessage.ATTR_REQUESTID);
		
		// response code
		responseCode = json.getInt(ATTR_RESPONSECODE);
		
		String stringValue = new String();
		
		// response code reason phrase
		if (json.containsKey(ATTR_RESPONSECODEREASON)){
			if (!json.isNull(ATTR_RESPONSECODEREASON)){
				stringValue = removeQuotes(json.getString(ATTR_RESPONSECODEREASON));
			}	
		}
				
		// and the null value got transported more like string... we have to make a rule for it
		if (stringValue == null || stringValue.equals("null")){
			responseCodeReason = null;
		} else {
			responseCodeReason = stringValue;
		}
		
		
		// the same for response body
		if (!json.isNull(ATTR_RESPONSEBODY)){
			stringValue = removeQuotes(json.getString(ATTR_RESPONSEBODY));
		}
				
		if (stringValue == null || stringValue.equals("null")){
			responseBody = null;
		} else {
			responseBody = stringValue;
		}
		
		return true;
	}
	
	
	/**
	 * Takes all the necessary fields, attributes and parameters and assembles a valid JSON that can be sent over XMPP
	 * network. 
	 */
	private void buildMessageJson(){
		// create the factory
		JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
		
		// build the thing
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		
		mainBuilder.add(ATTR_MESSAGETYPE, messageType);
		mainBuilder.add(ATTR_REQUESTID, requestId);
		mainBuilder.add(ATTR_RESPONSECODE, responseCode);
		
		if (responseCodeReason == null){
			mainBuilder.addNull(ATTR_RESPONSECODEREASON);
		} else {
			mainBuilder.add(ATTR_RESPONSECODEREASON, responseCodeReason);
		}
		
		if (responseBody == null){
			mainBuilder.addNull(ATTR_RESPONSEBODY);
		} else {
			mainBuilder.add(ATTR_RESPONSEBODY, responseBody);
		}
				
		// build the thing
		jsonRepresentation = mainBuilder.build(); 
	}

}
