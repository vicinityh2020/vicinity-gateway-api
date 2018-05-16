package eu.bavenir.ogwapi.commons.messages;


import javax.json.Json;
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
	public static final String MESSAGE_EVENT_ACTIVATION = "eventChannelActivation";
	
	/**
	 * Attribute name for Event channel deactivation.
	 */
	public static final String MESSAGE_EVENT_DEACTIVATION = "eventChannelDeactivation";
	
	/**
	 * Attribute name for sending the Event to subscribers.
	 */
	public static final String MESSAGE_EVENT_SENDINGTOSUBSCRIBERS = "eventSendingToSubscribers";
	
	
	/* === FIELDS === */
	
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
	private boolean error = false;
	

	
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
	public StatusMessage() {
		initializeBuilders();
	}
	
	
	/**
	 * More useful constructor, can be used to directly fill the basic pay load of the status message. A message built
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
	public StatusMessage(boolean error, String attribute, String value) {
		initializeBuilders();
		
		error = this.error;
		
		addMessage(attribute,value);
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
	private void initializeBuilders() {
		// create the factory
		jsonBuilderFactory = Json.createBuilderFactory(null);
		mainBuilder = jsonBuilderFactory.createObjectBuilder();
		arrayBuilder = jsonBuilderFactory.createArrayBuilder();
	}
}