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
 * Extended {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage NetworkMessage} that represents a response. Same as {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest request}
 * but with response from the remote site. There are many similarities between implementation of these two classes.
 * This class contains extra fields for error indication, status code, status code reason and response body. The 
 * similarity to HTTP response is in place, since the OGWAPI's first design was to transport HTTP requests, though it
 * can (and does) work with any other protocol. 
 * 
 * OGWAPI uses it like this:
 * 
 * 	1. Constructs an instance of this class and sets the {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage#requestId requestId}
 *     to match the request ID of the {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest request} that 
 *     arrived previously and was the reason for starting the process that eventually lead into creation of this message.
 *  2. Calls {@link #setResponseCode(int) setResponseCode} and {@link #setResponseCodeReason(String) setResponseCodeReason} 
 *     to values representing the outcome of the operation (either from Agent or internal). Also, it never forgets to 
 *     set {@link #setError(boolean) setError} - error propagation down the line depends on it.
 *  3. Calls {@link #setResponseBody(String) setResponseBody} and passes the response body from an Agent (or internal process).
 *  4. If there is a content type set in the response from an Agent, the OGWAPI sets the same by {@link #setContentType(String) setContentType}.
 *     If not (such as when the response is a product of internal operation), it uses the same method to set it to application/json.
 *  4. Builds the message by {@link #buildMessageJson buildMessageJson} and sends the string through the network.
 * 
 *  If there are any modification to this class, they will probably be about extending the range of fields that will be 
 *  transported over the network. Don't forget to put the new field into the {@link #parseJson(JsonObject) parser} and 
 *  {@link #buildMessageJson() builder}. Note that such modification will make the new OGWAPI incompatible with the 
 *  previous versions.
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
	 * Attribute name for the status code reason.  
	 */
	private static final String ATTR_RESPONSECODEREASON = "responseCodeReason";
	
	/**
	 * Attribute name for the content type.
	 */
	private static final String ATTR_CONTENTTYPE = "contentType";
	
	/**
	 * Name of the response body attribute.
	 */
	private static final String ATTR_RESPONSEBODY = "responseBody";
	
	/**
	 * Name of the response body supplement attribute. Sometimes one body is not enough... in that case, use the second
	 * one.
	 */
	private static final String ATTR_RESPONSEBODYSUPPLEMENT = "responseBodySupplement";

	
	/* === FIELDS === */
	
	/**
	 * Indicates whether or not an error occurred on the other side. 
	 */
	private boolean error;
	
	/**
	 * Response code from the remote object (see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}).
	 */
	private int responseCode;
	
	/**
	 * Response code reason, in other words the status description.
	 */
	private String responseCodeReason;
	
	/**
	 * Content type of the body.
	 */
	private String contentType;
	
	/**
	 * If this NetworkMessage was constructed from incoming message, here is a response body from the remote
	 * object.
	 */
	private String responseBody;
	
	/**
	 * Sometimes one body may not be enough. In such cases, use this supplementary storage as well.
	 */
	private String responseBodySupplement;
	
	
	
	/* === PUBLIC METHODS === */
	/**
	 * Use this when constructing a message that is to be sent across the network.  
	 *  
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NetworkMessageResponse(XMLConfiguration config, Logger logger){
		super(config, logger);
		
		initialise();
		
		messageType = NetworkMessageResponse.MESSAGE_TYPE;
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the P2P network. 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NetworkMessageResponse(JsonObject json, XMLConfiguration config, Logger logger){
		// always call this guy
		super(config, logger);
		
		initialise();
		
		// parse the JSON, or mark this message as invalid
		if (!parseJson(json)){
			setValid(false);
		}
	}
	
	
	/**
	 * Sometimes it can be beneficial to create the response message all at once, and not fill it piece by piece.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 * @param error Indicates an error during operation.
	 * @param responseCode Numerical response code (see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}).
	 * @param responseCodeReason Human readable code reason (see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}).
	 * @param contentType Content type of the body.
	 * @param responseBody Body of the response.
	 */
	public NetworkMessageResponse(XMLConfiguration config, Logger logger, boolean error, int responseCode, 
					String responseCodeReason, String contentType, String responseBody) {
		super(config, logger);
		
		messageType = NetworkMessageResponse.MESSAGE_TYPE;
		
		this.error = error;
		this.responseCode = responseCode;
		this.responseCodeReason = responseCodeReason;
		this.contentType = contentType;
		this.responseBody = responseBody;
	}
	
	
	/**
	 * Was there an error during the operation?
	 * 
	 * @return True or false.
	 */
	public boolean isError() {
		return error;
	}
	
	
	/**
	 * Set this to true if there was an error during the operation.
	 * 
	 * @param error True or false.
	 */
	public void setError(boolean error) {
		this.error = error;
	}
	

	/**
	 * Returns the response status code from the remote object. For more info see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @return Response status code from the operation.
	 */
	public int getResponseCode() {
		return responseCode;
	}


	/**
	 * Sets the response status code from the operation. For more info see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @param responseCode Response status code of the operation.
	 */
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	
	
	/**
	 * Returns the response status code reason from the operation. For more info see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @return HTTP response status code reason phrase from the operation.
	 */
	public String getResponseCodeReason(){
		return responseCodeReason;
	}
	
	
	/**
	 * Sets the response status code reason from the operation. For more info see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status codes</a>
	 * @param responseCode Response status code reason phrase of the operation.
	 */
	public void setResponseCodeReason(String responseCodeReason){
		this.responseCodeReason = responseCodeReason;
	}


	/**
	 * Retrieves the content type of the response body.
	 * 
	 * @return Content type string (e.g. application/json)
	 */
	public String getContentType() {
		return contentType;
	}


	/**
	 * Sets the content type of the response body.
	 * 
	 * @param  Content type string (e.g. application/json)
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}


	/**
	 * Returns the body of a response. This is the actual data that are returned by the operation.
	 *  
	 * @return Data from the operation.
	 */
	public String getResponseBody() {
		return responseBody;
	}


	/**
	 * Sets the body of a response. Use this when creating an instance of this class.
	 *  
	 * @param responseBody The actual data that are returned by the operation.
	 */
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}
	
	
	/**
	 * Retrieves supplemental response body.
	 *   
	 * @return Supplemental response body.
	 */
	public String getResponseBodySupplement() {
		return responseBodySupplement;
	}
	
	
	/**
	 * Sets supplemental response body.
	 * 
	 * @param responseBodySupplement Supplemental response body.
	 */
	public void setResponseBodySupplement(String responseBodySupplement) {
		this.responseBodySupplement = responseBodySupplement;
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
	 * Takes the JSON object that came over the network and fills necessary fields with values.
	 * 
	 * @param json JSON to parse.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJson(JsonObject json){
		
		// first check out whether or not the message has everything it is supposed to have and stop if not
		if (
				!json.containsKey(ATTR_MESSAGETYPE) ||
				!json.containsKey(ATTR_REQUESTID) ||
				!json.containsKey(ATTR_SOURCEOID) ||
				!json.containsKey(ATTR_DESTINATIONOID) ||
				!json.containsKey(ATTR_ERROR) ||
				!json.containsKey(ATTR_RESPONSECODE) ||
				!json.containsKey(ATTR_RESPONSECODEREASON) ||
				!json.containsKey(ATTR_RESPONSEBODY) ||
				!json.containsKey(ATTR_RESPONSEBODYSUPPLEMENT)) {
			
			return false;
		}
		
		// load values from JSON
		try {
			
			messageType = json.getInt(NetworkMessage.ATTR_MESSAGETYPE);
			requestId = json.getInt(NetworkMessage.ATTR_REQUESTID);
			error = json.getBoolean(ATTR_ERROR);
			responseCode = json.getInt(ATTR_RESPONSECODE);
			
			// null values are special cases in JSON, they get transported as "null" string...
			if (!json.isNull(ATTR_RESPONSECODEREASON)) {
				responseCodeReason = json.getString(ATTR_RESPONSECODEREASON);
			}
			
			if (!json.isNull(ATTR_SOURCEOID)) {
				sourceOid = json.getString(ATTR_SOURCEOID);
			}
			
			if (!json.isNull(ATTR_DESTINATIONOID)) {
				destinationOid = json.getString(ATTR_DESTINATIONOID);
			}
			
			if (!json.isNull(ATTR_CONTENTTYPE)) {
				contentType = json.getString(ATTR_CONTENTTYPE);
			}
			
			if (!json.isNull(ATTR_RESPONSEBODY)) {
				responseBody = json.getString(ATTR_RESPONSEBODY);
			}
			
			if (!json.isNull(ATTR_RESPONSEBODYSUPPLEMENT)) {
				responseBodySupplement = json.getString(ATTR_RESPONSEBODYSUPPLEMENT);
			}
			
		} catch (Exception e) {
			logger.severe("NetworkMessageResponse: Exception while parsing NetworkMessageResponse: " + e.getMessage());
			
			return false;
		}
		
		// process non primitives
		sourceOid = removeQuotes(sourceOid);
		destinationOid = removeQuotes(destinationOid);
		responseCodeReason = removeQuotes(responseCodeReason);
		contentType = removeQuotes(contentType);
		responseBody = removeQuotes(responseBody);
		responseBodySupplement = removeQuotes(responseBodySupplement);
		
		return true;
	}
	
	
	/**
	 * Takes all the necessary fields, attributes and parameters and assembles a valid JSON that can be sent over 
	 * network. 
	 */
	private void buildMessageJson(){
		// create the factory
		JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
		
		// build the thing
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		
		mainBuilder.add(ATTR_MESSAGETYPE, messageType);
		mainBuilder.add(ATTR_REQUESTID, requestId);
		mainBuilder.add(ATTR_SOURCEOID, sourceOid);
		mainBuilder.add(ATTR_DESTINATIONOID, destinationOid);
		mainBuilder.add(ATTR_ERROR, error);
		mainBuilder.add(ATTR_RESPONSECODE, responseCode);
		
		
		if (sourceOid == null){
			mainBuilder.addNull(ATTR_SOURCEOID);
		} else {
			mainBuilder.add(ATTR_SOURCEOID, sourceOid);
		}
		
		if (destinationOid == null){
			mainBuilder.addNull(ATTR_DESTINATIONOID);
		} else {
			mainBuilder.add(ATTR_DESTINATIONOID, destinationOid);
		}
		
		if (responseCodeReason == null){
			mainBuilder.addNull(ATTR_RESPONSECODEREASON);
		} else {
			mainBuilder.add(ATTR_RESPONSECODEREASON, responseCodeReason);
		}
		
		if (contentType == null){
			mainBuilder.addNull(ATTR_CONTENTTYPE);
		} else {
			mainBuilder.add(ATTR_CONTENTTYPE, contentType);
		}
		
		if (responseBody == null){
			mainBuilder.addNull(ATTR_RESPONSEBODY);
		} else {
			mainBuilder.add(ATTR_RESPONSEBODY, responseBody);
		}
		
		if (responseBodySupplement == null) {
			mainBuilder.addNull(ATTR_RESPONSEBODYSUPPLEMENT);
		} else {
			mainBuilder.add(ATTR_RESPONSEBODYSUPPLEMENT, responseBodySupplement);
		}
				
		// build the thing
		jsonRepresentation = mainBuilder.build(); 
	}
	
	
	/**
	 * Initialises fields.
	 */
	private void initialise() {
		error = false;
		responseCode = 0;
		responseCodeReason = null;
		contentType = null;
		responseBody = null;
		responseBodySupplement = null;
	}

}
