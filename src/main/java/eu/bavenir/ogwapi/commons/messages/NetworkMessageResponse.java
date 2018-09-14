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


/**
 * Extended {@link NetworkMessage NetworkMessage} that represents a response. Same as {@link NetworkMessageRequest request}
 * but with HTTP response.
 * 
 * Use it like this:
 * 
 * In your Gateway API service:
 * 	1. Construct an instance of this class - use the constructor without parameter
 *  2. Call {@link #setResponseCode(int) setResponseCode} and pass the number returned from an Agent.
 *  3. Call {@link #setResponseBody(String) setResponseBody} and pass the response body from an Agent.
 *  4. Build the message by {@link #buildMessageJson buildMessageJson}. By calling its toString you obtain a string to 
 *  	be sent through {@link CommunicationManager CommunicationNode}. 
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
	 * Name of the error indicator attribute.
	 */
	private static final String ATTR_ERROR = "error";
	
	/**
	 * Name of the response status code attribute.
	 */
	private static final String ATTR_RESPONSECODE = "responseCode";
	
	/**
	 * Attribute name for the status code reason, returned by HTTP server on the remote site.  
	 */
	private static final String ATTR_RESPONSECODEREASON = "responseCodeReason";
	
	/**
	 * Name of the response body attribute.
	 */
	private static final String ATTR_RESPONSEBODY = "responseBody";

	
	/* === FIELDS === */
	
	/**
	 * Indicates whether or not an error occurred on the other side. 
	 */
	private boolean error;
	
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
	
	
	
	/* === PUBLIC METHODS === */
	/**
	 * Use this when parsing a message that was received from the XMPP network. The parsing will fill all the attributes. 
	 *  
	 * @param xmppMessage A 'raw' XMPP message. 
	 */
	public NetworkMessageResponse(XMLConfiguration config, Logger logger){
		super(config, logger);
		
		messageType = NetworkMessageResponse.MESSAGE_TYPE;
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the P2P network. 
	 */
	public NetworkMessageResponse(JsonObject json, XMLConfiguration config, Logger logger){
		// always call this guy
		super(config, logger);
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			setValid(false);
		}
	}
	
	
	public NetworkMessageResponse(XMLConfiguration config, Logger logger, boolean error, int responseCode, 
					String responseCodeReason, String responseBody) {
		super(config, logger);
		
		messageType = NetworkMessageResponse.MESSAGE_TYPE;
		
		this.error = error;
		this.responseCode = responseCode;
		this.responseCodeReason = responseCodeReason;
		this.responseBody = responseBody;
	}
	
	
	public boolean isError() {
		return error;
	}
	
	public void setError(boolean error) {
		this.error = error;
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
		
		try {
			
			// figure out the message type
			messageType = json.getInt(NetworkMessage.ATTR_MESSAGETYPE);
			
		} catch (Exception e) {
			logger.warning("Exception while parsing NetworkMessageResponse: " + e.getMessage());
			
			return false;
		}
		
		if (messageType != MESSAGE_TYPE){
			logger.warning("Error while parsing NetworkMessageResponse: The messageType does not match. Should be " 
								+ MESSAGE_TYPE + " but is " + messageType);
			
			// just a formality
			return false;
		}
		
		try {
			
			// the correlation ID of the request (in superclass)
			requestId = json.getInt(NetworkMessage.ATTR_REQUESTID);
			
			// error indicator
			error = json.getBoolean(ATTR_ERROR);
			
			// response code
			responseCode = json.getInt(ATTR_RESPONSECODE);
			
		} catch (Exception e) {
			logger.severe("Exception while parsing NetworkMessageResponse: " + e.getMessage());
			
			return false;
		}
		
		
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
		mainBuilder.add(ATTR_ERROR, error);
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
