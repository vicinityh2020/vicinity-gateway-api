package eu.bavenir.vicinity.messages;

/**
 * This is a helper class for a local message - a message that is returned to an Adapter/Agent, but it does not 
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
 * @author sulfo
 *
 */
public class LocalMessage {
	
	/**
	 * Name of the error attribute.
	 */
	public static final String ATTR_ERROR = "error";
	
	/**
	 * Name of the message attribute.
	 */
	public static final String ATTR_MESSAGE = "message";
	
	/**
	 * Attribute name for the status code, returned by HTTP server on the remote site.  
	 */
	public static final String MESSAGE_CODE = "code";
	
	/**
	 * Attribute name for the status code reason, returned by HTTP server on the remote site.  
	 */
	public static final String MESSAGE_REASON = "reason";
	
	
	
	private boolean error;
	
	private String message;
	
	
	
	public LocalMessage() {
		setError(false);
		setMessage(null);
	}



	public boolean isError() {
		return error;
	}



	public void setError(boolean error) {
		this.error = error;
	}



	public String getMessage() {
		return message;
	}



	public void setMessage(String message) {
		this.message = message;
	}

	
	
	
	
	
	
	
}
