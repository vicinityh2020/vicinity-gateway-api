package eu.bavenir.ogwapi.commons.messages;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * 
 * This is a helper class for a local status message - a message that is returned to a requester, but it does not 
 * originate in the P2P Network (unlike NetworkMessage). It provides unified interface for the local end point to obtain
 * a status of requested operation. The issue with obtaining a unified status is, that for the end user it should look
 * the same way no matter if it originated in an OGWAPI sitting on his local infrastructure or somewhere far away, from 
 * where it gets transported by {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage NetworkMessage}, which resembles 
 * a StatusMessage a lot. There was a lot of thought process invested into making it better and not using two very similar
 * classes that mostly copy values between them at the end, but so far this is the most flexible approach :). 
 * 
 * Usually after building it looks like this:
 * 
 * {
 *     "error": false,
 *     "statusCode": 200,
 *     "statusCodeReason": "OK. Operation successful.",
 *     "contentType": "application/json",
 *     "message": [
 *         {message JSON 1}, {message JSON 2}, etc.
 *     ]
 * }
 * 
 * The class is used by first constructing an instance of it, setting the correct values and building the JSON.  
 * 
 * @author sulfo
 *
 */
public class StatusMessage {
	
	
	/* === CONSTANTS === */
	
	
	/**
	 * Name of the error attribute - for JSON building.
	 */
	public static final String ATTR_ERROR = "error";
	
	/**
	 * Name of the response status code attribute.
	 */
	public static final String ATTR_STATUSCODE = "statusCode";
	
	/**
	 * Attribute name for the status code reason.  
	 */
	public static final String ATTR_STATUSCODEREASON = "statusCodeReason";
	
	/**
	 * Attribute name for the content type.
	 */
	public static final String ATTR_CONTENTTYPE = "contentType";
	
	/**
	 * Name of the message attribute - for JSON building.
	 */
	public static final String ATTR_MESSAGE = "message";
	
	/**
	 * Attribute name for the value.
	 */
	public static final String ATTR_VALUE = "value";
	
	/**
	 * Text of the success message.
	 */
	public static final String TEXT_SUCCESS = "success";
	
	/**
	 * Text of the failure message.
	 */
	public static final String TEXT_FAILURE = "failure";
	
	/**
	 * Most used content type.
	 */
	public static final String CONTENTTYPE_APPLICATIONJSON = "application/json";
	
	
	/* === FIELDS === */
	
	/**
	 * Is the message we are creating an error?
	 */
	private boolean error;
	
	/**
	 * Status code from the remote or local object (see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}).
	 */
	private int statusCode;
	
	/**
	 * Response code reason, in other words the status description.
	 */
	private String statusCodeReason;
	
	/**
	 * Content type of the body.
	 */
	private String contentType;
	
	
	/**
	 * Factory for JSON builders.
	 */
	private JsonBuilderFactory jsonBuilderFactory;
	
	/**
	 * The message part is an array of JSONs.
	 */
	private JsonArrayBuilder arrayBuilder;
	
	/**
	 * Builder of the inner objects.
	 */
	private JsonObjectBuilder innerBuilder;
	
	
	/* === PUBLIC METHODS === */
		
	
	/**
	 * Constructor, fills the basic pay load of the status message. A message built
	 * right after it was constructed this way will look like this:
	 * 
	 * {
	 *	"error": booleanValue,
	 *	"statusCode": integerValue,
	 *	"statusCodeReason": "string with status code reason"
	 *  "contentType": "some content type"
	 *	"message": [
	 *		{
	 *        ... returned JSON ...
	 *		},
	 *		{
	 *        ... or multiple, if available ...
	 *		}
	 *  ]
	 * }
	 * 
	 * It is then of course possible to add more JSONs into the 'message' array by using e.g. 
	 * {@link #addSimpleMessage(String, String) addSimpleMessage}.
	 * 
	 * @param error Boolean value telling whether this status message is an error or not.
	 * @param statusCode Status code from the remote or local object (see {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}).
	 * @param statusCodeReason Response code reason, in other words the status description.
	 * @param contentType Content type of the body.
	 */
	public StatusMessage(boolean error, int statusCode, String statusCodeReason, String contentType) {
		
		this.error = error;
		this.statusCode = statusCode;
		this.statusCodeReason = statusCodeReason;
		this.contentType = contentType;
		
		// create the factory
		jsonBuilderFactory = Json.createBuilderFactory(null);
		arrayBuilder = jsonBuilderFactory.createArrayBuilder();
	}
	
	
	/**
	 * Getter for the error flag.
	 * 
	 * @return Whether the current status message is error or not.
	 */
	public boolean isError() {
		return error;
	}


	/**
	 * Getter for the status code.
	 * 
	 * @return Status code. See {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}.
	 */
	public int getStatusCode() {
		return statusCode;
	}


	/**
	 * Getter for the status code reason.
	 * 
	 * @return Status code reason. See {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}.
	 */
	public String getStatusCodeReason() {
		return statusCodeReason;
	}
	
	
	/**
	 * Returns the current array of messages.
	 * 
	 * @return Messages from the array.
	 */
	public JsonArray getCurrentMessageArray() {
		return arrayBuilder.build();
	}


	/**
	 * Returns content type.
	 * 
	 * @return Content type string.
	 */
	public String getContentType() {
		return contentType;
	}


	/**
	 * Adds one simple JSON into the message array. The JSON carries one attribute-value pair. 
	 * 
	 * @param attribute Name of the attribute.
	 * @param value Textual representation of the value.
	 */
	public void addSimpleMessage(String attribute, String value) {
		
		// if attribute is null, there is no need to bother... 
		if (attribute != null) {
			
			innerBuilder = jsonBuilderFactory.createObjectBuilder();
			
			if (value == null) {
				innerBuilder.addNull(attribute);
			} else {
				innerBuilder.add(attribute, value);
			}
			
			arrayBuilder.add(innerBuilder);
		}
	}
	
	
	/**
	 * Adds one simple JSON into the message array. The JSON carries one attribute-value pair. 
	 * 
	 * @param attribute Name of the attribute.
	 * @param value Numerical (integer) representation of the value.
	 */
	public void addSimpleMessage(String attribute, int value) {
		// if attribute is null, there is no need to bother... 
		if (attribute != null) {
			
			innerBuilder = jsonBuilderFactory.createObjectBuilder();
			innerBuilder.add(attribute, value);
			
			arrayBuilder.add(innerBuilder);
		}
		
	}
	
	
	/**
	 * Adds one simple JSON into the message array. The JSON carries one attribute-value pair. 
	 * 
	 * @param attribute Name of the attribute.
	 * @param value Numerical (floating point) representation of the value.
	 */
	public void addSimpleMessage(String attribute, float value) {
		// if attribute is null, there is no need to bother... 
		if (attribute != null) {
			
			innerBuilder = jsonBuilderFactory.createObjectBuilder();
			innerBuilder.add(attribute, value);
			
			arrayBuilder.add(innerBuilder);
		}
	}
	
	
	/**
	 * Adds more complex JSON into the message array. The JSON can be arbitrary.
	 * 
	 * @param messageJsonBuilder Builder of the JSON to be added.
	 */
	public void addMessageJson(JsonObjectBuilder messageJsonBuilder) {
		
		if (messageJsonBuilder != null) {
			arrayBuilder.add(messageJsonBuilder);
		}
	}
	
	
	/**
	 * Adds more complex JSON into the message array. The JSON can be arbitrary.
	 * 
	 * @param messageJsonBuilder JSON object to be added.
	 */
	public void addMessageJson(JsonObject json) {
		
		if (json != null) {
			arrayBuilder.add(json);
		}
	}
	

	/**
	 * Builds the status message JSON. This method can be called repeatedly but the returned value will always reflect
	 * the internal state of the StatusMessage when the method was called for the first time, which means no subsequent
	 * additions of messages can be made.   
	 * 
	 * @return Status message JSON.
	 */
	public JsonObject buildMessage() {
		
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		
		mainBuilder.add(ATTR_ERROR, error);
		mainBuilder.add(ATTR_STATUSCODE, statusCode);
		
		if (statusCodeReason == null) {
			mainBuilder.addNull(ATTR_STATUSCODEREASON);
		} else {
			mainBuilder.add(ATTR_STATUSCODEREASON, statusCodeReason);
		}
		
		
		if (contentType == null) {
			mainBuilder.addNull(ATTR_CONTENTTYPE);
		} else {
			mainBuilder.add(ATTR_CONTENTTYPE, contentType);
		}
		
		mainBuilder.add(ATTR_MESSAGE, arrayBuilder);
			
		return mainBuilder.build();
		
	}
	
	
	/* === PRIVATE METHODS === */
	
}