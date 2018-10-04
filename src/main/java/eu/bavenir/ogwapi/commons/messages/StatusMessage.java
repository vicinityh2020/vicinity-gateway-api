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
 * 1!! provides unified interface ...
 * 
 * This is a helper class for a local status message - a message that is returned to an Adapter/Agent, but it does not 
 * originate in the P2P Network (unlike NetworkMessage). It is created e.g. after a successful login of an Adapter, 
 * when there is an success message that needs to be returned in a unified manner (a JSON), or an error message that
 * needs to be presented to requesting end point about something that happened far away, but only thing that returned 
 * from there is a response code and a reason that needs to be put into a nice JSON representation.
 * 
 * There are always two attributes that are mandatory:
 * 	- boolean 'error', that gives indication on whether or not the message is an error message,
 * 	- array of JSONs called 'message'.
 * 
 * Usually it looks like this:
 * 
 * {
 *	"error": false,
 *	"message": [
 *		{message JSON 1}, {message JSON 2}, etc.
 *	]
 * }
 * 
 * Some often used attributes that are parts of inner message JSON X are included as constants, like MESSAGE_CODE
 * and MESSAGE_REASON (for transferring HTTP errors).
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
	
	public static final String ATTR_STATUSCODE = "statusCode";
	
	public static final String ATTR_STATUSCODEREASON = "statusCodeReason";
	
	
	/**
	 * Name of the message attribute - for JSON building.
	 */
	public static final String ATTR_MESSAGE = "message";
	
	public static final String ATTR_VALUE = "value";
	
	
	
	/**
	 * Text of the success message.
	 */
	public static final String TEXT_SUCCESS = "success";
	
	/**
	 * Text of the failure message.
	 */
	public static final String TEXT_FAILURE = "failure";
	
	
	// this is a list of status message names, that can usually occur during operations of the OGWAPI
	
	/*
	public static final String MESSAGE_BODY = "body";
	
	public static final String MESSAGE_CODE = "code";
	public static final String MESSAGE_REASON = "reason";
	public static final String MESSAGE_LOGIN = "login";
	public static final String MESSAGE_LOGOUT = "logout";
	public static final String MESSAGE_EVENT_ACTIVATION = "activateEventChannel";
	public static final String MESSAGE_EVENT_DEACTIVATION = "deactivateEventChannel";
	public static final String MESSAGE_EVENT_SENDTOSUBSCRIBERS = "sendEventToSubscribers";
	
	
	public static final String MESSAGE_EVENT_GETLOCALEVENTCHANNELSTATUS = "localEventChannelStatus";
	
	public static final String MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS = "remoteEventChannelStatus";
	
	public static final String MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL = "subscribeToEventChannel";
	
	public static final String MESSAGE_EVENT_UNSUBSCRIBEFROMEVENTCHANNEL = "unsubscribeFromEventChannel";
	
	public static final String MESSAGE_ACTION_START = "startAction";
	
	public static final String MESSAGE_TASK_STATUS = "taskStatus";
	
	public static final String MESSAGE_TASK_RETURNVALUE = "returnValue";
	
	public static final String MESSAGE_TASK_STOP = "stopTask";
	
	public static final String MESSAGE_TASKID = "taskID";
	
	*/

	
	/* === FIELDS === */
	
	/**
	 * Is the message we are transporting back an error?
	 */
	private boolean error;
	
	private int statusCode;
	
	private String statusCodeReason;
	
	
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
	 * It is then of course possible to add more JSONs into the 'message' array by using {@link #addMessage(String, String)}.
	 * 
	 * @param error Boolean value telling whether this status message is an error or not.
	 * @param attribute Attribute name in the message.
	 * @param value A value of the attribute.
	 */
	

	public StatusMessage(boolean error, int statusCode, String statusCodeReason) {
		
		this.error = error;
		this.statusCode = statusCode;
		this.statusCodeReason = statusCodeReason;
		
		// create the factory
		jsonBuilderFactory = Json.createBuilderFactory(null);
		arrayBuilder = jsonBuilderFactory.createArrayBuilder();
	}
	
	
	/**
	 * Getter of the error flag.
	 * 
	 * @return Whether the current status message is error or not.
	 */
	public boolean isError() {
		return error;
	}


	public int getStatusCode() {
		return statusCode;
	}


	public String getStatusCodeReason() {
		return statusCodeReason;
	}
	
	
	public JsonArray getCurrentMessageArray() {
		return arrayBuilder.build();
	}


	/**
	 * Adds one JSON into the message array. The JSON carries one attribute-value pair. 
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
	
	
	public void addSimpleMessage(String attribute, int value) {
		// if attribute is null, there is no need to bother... 
		if (attribute != null) {
			
			innerBuilder = jsonBuilderFactory.createObjectBuilder();
			innerBuilder.add(attribute, value);
			
			arrayBuilder.add(innerBuilder);
		}
		
	}
	
	
	public void addSimpleMessage(String attribute, float value) {
		// if attribute is null, there is no need to bother... 
		if (attribute != null) {
			
			innerBuilder = jsonBuilderFactory.createObjectBuilder();
			innerBuilder.add(attribute, value);
			
			arrayBuilder.add(innerBuilder);
		}
	}
	
	// use when wanting to add more complex message
	public void addMessageJson(JsonObjectBuilder messageJsonBuilder) {
		
		if (messageJsonBuilder != null) {
			arrayBuilder.add(messageJsonBuilder);
		}
	}
	
	
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
		mainBuilder.add(ATTR_STATUSCODEREASON, statusCodeReason);
		mainBuilder.add(ATTR_MESSAGE, arrayBuilder);
			
		return mainBuilder.build();
		
	}
	
	
	/* === PRIVATE METHODS === */
	
}