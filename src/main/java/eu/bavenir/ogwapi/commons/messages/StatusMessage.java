package eu.bavenir.ogwapi.commons.messages;


import java.io.StringReader;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
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
	
	/**
	 * Text of the success message.
	 */
	public static final String TEXT_SUCCESS = "success";
	
	/**
	 * Text of the failure message.
	 */
	public static final String TEXT_FAILURE = "failure";
	
	
	// this is a list of status message names, that can usually occur during operations of the OGWAPI
	
	/**
	 * Attribute name for the status message body.  
	 */
	public static final String MESSAGE_BODY = "body";
	
	
	/**
	 * Attribute name for the status code, returned by HTTP server on the remote site.  
	 */
	public static final String MESSAGE_CODE = "code";
	
	/**
	 * Attribute name for the status code reason, returned by HTTP server on the remote site.  
	 */
	public static final String MESSAGE_REASON = "reason";
	
	/**
	 * Attribute name for login.
	 */
	public static final String MESSAGE_LOGIN = "login";
	
	/**
	 * Attribute name for login.
	 */
	public static final String MESSAGE_LOGOUT = "logout";
	
	/**
	 * Attribute name for Event channel activation.
	 */
	public static final String MESSAGE_EVENT_ACTIVATION = "activateEventChannel";
	
	/**
	 * Attribute name for Event channel deactivation.
	 */
	public static final String MESSAGE_EVENT_DEACTIVATION = "deactivateEventChannel";
	
	/**
	 * Attribute name for sending the Event to subscribers.
	 */
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
	
	
	/**
	 * Attribute name for getting value of a property.
	 */
	public static final String MESSAGE_PROPERTY_GETVALUE = "getValueOfAProperty";
	
	/**
	 * Attribute name for setting value of a property.
	 */
	public static final String MESSAGE_PROPERTY_SETVALUE = "setValueOfAProperty";
	
	
	
	/* === FIELDS === */
	
	/**
	 * Logger for this class. 
	 */
	private Logger logger;
	
	
	/**
	 * Factory for JSON builders.
	 */
	private JsonBuilderFactory jsonBuilderFactory;
	
	/**
	 * We need to return a JSON to the caller. This is the builder for it.
	 */
	private JsonObjectBuilder mainBuilder;
	
	/**
	 * The message part is an array of JSONs.
	 */
	private JsonArrayBuilder arrayBuilder;
	
	/**
	 * Builder of the inner objects.
	 */
	private JsonObjectBuilder innerBuilder;
	
	/**
	 * The JSON to be returned.
	 */
	private JsonObject messageJson;
	
	/**
	 * Is the message we are transporting back an error?
	 */
	private boolean error;
	
	/**
	 * Is this message valid after parsing?
	 */
	private boolean valid;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Default constructor initialises JSON object builders. You now have to set the values using setters, otherwise the
	 * built JSON will look like this:
	 * 
	 * {
	 *	"error": false,
	 *	"message": []
	 * }
	 */
	public StatusMessage(Logger logger) {
		valid = true;
		
		initialize(logger);
	}
	
	
	/**
	 * This constructor attempts to build this object by parsing incoming JSON. If the parsing operation is not 
	 * successful, the result is an object with validity {@link StatusMessage#valid flag} set to false.
	 * 
	 * @param json JSON that arrived from the P2P network.
	 */
	public StatusMessage(String jsonString, Logger logger){
		
		initialize(logger);
		
		// parse the JSON, or mark this message as invalid
		if (!parseJsonString(jsonString)){
			valid = false;
		}
	}
	
	
	
	/**
	 * More useful constructor, can be used to directly fill the basic payload of the status message. A message built
	 * right after it was constructed this way will look like this:
	 * 
	 * {
	 *	"error": error,
	 *	"message": [
	 *	   {
	 *        "attribute": value,
	 *     }
	 *  ]
	 * }
	 * 
	 * It is then of course possible to add more JSONs into the 'message' array by using {@link #addMessage(String, String)}.
	 * 
	 * @param error Boolean value telling whether this status message is an error or not.
	 * @param attribute Attribute name in the message.
	 * @param value A value of the attribute.
	 */
	public StatusMessage(boolean error, String attribute, String value, Logger logger) {
		initialize(logger);
		
		this.error = error;
		
		addMessage(attribute,value);
	}

	/**
	 * Returns whether or not this StatusMessage is valid. This value is meaningful only in case it has been parsed
	 * from a message that came from the network, when it would indicate any error that could occur during parsing. 
	 * If the StatusMessage was created locally, the value would always be true, since there was no parsing.
	 *  
	 * @return Whether this StatusMessage was parsed successfully or not. 
	 */
	public boolean isValid() {
		return valid;
	}
	
	
	/**
	 * Getter of the error flag.
	 * 
	 * @return Whether the current status message is error or not.
	 */
	public boolean isError() {
		return error;
	}


	/**
	 * Setter of the error flag.
	 * 
	 * @param error Whether the current status message should be error or not. 
	 */
	public void setError(boolean error) {
		this.error = error;
	}


	/**
	 * Adds one JSON into the message array. The JSON carries one attribute-value pair. 
	 * 
	 * @param attribute Name of the attribute.
	 * @param value Textual representation of the value.
	 */
	public void addMessage(String attribute, String value) {
		
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
	 * Builds the status message JSON. This method can be called repeatedly but the returned value will always reflect
	 * the internal state of the StatusMessage when the method was called for the first time, which means no subsequent
	 * additions of messages can be made.   
	 * 
	 * @return Status message JSON.
	 */
	public JsonObject buildMessage() {
		
		if (messageJson == null) {
			mainBuilder.add(ATTR_ERROR, error);
			mainBuilder.add(ATTR_MESSAGE, arrayBuilder);
			
			messageJson = mainBuilder.build();
		} 
		
		return messageJson;
		
	}
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Creates JSON builders.
	 */
	private void initialize(Logger logger) {
		error = false;
		messageJson = null;
		
		this.logger = logger;
		
		// create the factory
		jsonBuilderFactory = Json.createBuilderFactory(null);
		mainBuilder = jsonBuilderFactory.createObjectBuilder();
		arrayBuilder = jsonBuilderFactory.createArrayBuilder();
	}
	

	
	/**
	 * Takes the JSON object and fills necessary fields with values.
	 * 
	 * @param json JSON to parse.
	 * @return True if parsing was successful, false otherwise.
	 */
	private boolean parseJsonString(String jsonString){
		
		// make a JSON from the incoming String - IMPORTANT! any string that is not a valid JSON will throw exception
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonObject json;
		
		try {
			json = jsonReader.readObject(); // right here
		} catch (Exception e) {
			
			logger.warning("Error while parsing StatusMessage: Failed to convert string into valid JSON.");
			
			return false;
		}
		
		if (json == null){
			
			logger.warning("Error while parsing StatusMessage: Failed to convert string into valid JSON.");
			
			// it is not a JSON...
			return false;
		}
		
		// first take a look on whether or not the JSON contains what it should
		if (!json.containsKey(ATTR_ERROR)) {
			logger.warning("Error while parsing StatusMessage: Missing key '" + ATTR_ERROR + "'");
			
			return false;
		}
		
		if (!json.containsKey(ATTR_MESSAGE)) {
			logger.warning("Error while parsing StatusMessage: Missing key '" + ATTR_MESSAGE + "'");
			
			return false;
		}
		
		// now do the parsing
		try {
			error = json.getBoolean(ATTR_ERROR);
		} catch (Exception e) {
			logger.warning("Exception while parsing StatusMessage: " + e.getMessage());			
			return false;
		}
		
		messageJson = json;		
		
		// Remember, the parsing is there primarily to create this instance of a StatusMessage with the error indicator
		// so it can be easily found out whether or not the message carries an error or not. Ergo, no more parsing
		// is required.

		return true;
	}
	
	
}