package eu.bavenir.ogwapi.commons.messages;

import javax.json.JsonObject;

import org.apache.commons.configuration2.XMLConfiguration;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * This class serves as a wrapper for XMPP message, so we can add a few more attributes into it, such as type, 
 * request ID, time stamp etc. This class contains all the common fields and methods, that a NetworkMessage needs.
 * 
 * However in order to store a complete request or a response, this class needs to be extended. 
 * The subclasses of this class, extended with the right fields and methods, should then be used in the actual 
 * communication.
 * 
 * NOTE: When handling freshly parsed message of any type, always check for {@link #isValid() validity} and (after 
 * optional further examination) discard it.
 * 
 * @author sulfo
 *
 */
public class NetworkMessage {

	/* === CONSTANTS === */
	
	/**
	 * This is how this raw type of message should be marked in the {@link #messageType messageType}.
	 */
	public static final int MESSAGE_TYPE = 0x00;
	
	/**
	 * Name of the message type attribute in JSON that is to be transported. 
	 */
	public static final String ATTR_MESSAGETYPE = "messageType";
	
	/**
	 * Name of the request ID field in JSON that is to be sent. 
	 */
	public static final String ATTR_REQUESTID = "requestId";
	
	/**
	 * Name of the configuration parameter for message timeout (expiration will make the message 'stale' and will
	 * be discarded at nearest opportunity). 
	 */
	public static final String CONFIG_PARAM_XMPPMESSAGETIMEOUT = "xmpp.messageTimeout";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_XMPPMESSAGETIMEOUT CONFIG_PARAM_XMPPMESSAGETIMEOUT} 
	 * configuration parameter. This value is taken into account when no suitable value is found in the configuration
	 * file.
	 */
	public static final int CONFIG_DEF_XMPPMESSAGETIMEOUT = 30;
	
	
	/* === FIELDS === */
	
	/**
	 * Determines the type of this message. Each class that extends NetworkMessage should have its own constant of
	 * MESSAGE_TYPE implemented. The decision tree can then be very easily created like:
	 * 
	 * if (someMessage.getMessageType() ==  NetworkMessageRequest.MESSAGE_TYPE){
	 * 		// stuff that needs to be done when NetworkMessageRequest arrives
	 * }
	 */
	protected int messageType;
	
	/**
	 * Defines, whether this network message is valid or not. Implicitly set to true, should be explicitly
	 * marked as invalid if something wrong happens (e.g. when incoming message can't be successfully parsed).
	 */
	protected boolean valid;
	
	/**
	 * Defines, whether this message is older than the {@link #STALE_MESSAGE_TIMEOUT timeout}.
	 */
	protected boolean stale;
	
	/**
	 * A service call to Gateway API will produce a XMPP message sent across the XMPP network for processing by a 
	 * remote object. After processing a response message is sent back, received and inserted into a queue. The service
	 * that was the original sender can then sort through the queue and find a the message it is waiting for based on
	 * this request ID. 
	 */
	protected int requestId;
	
	/**
	 * UNIX time stamp generated in the moment the instance of this class is constructed. 
	 */
	protected long timeStamp;
	
	/**
	 * Configuration - necessary to obtain a timeout value. 
	 */
	protected XMLConfiguration config;
	
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
	protected JsonObject jsonRepresentation;

	
	/* === PUBLIC METHODS === */

	/**
	 * Basic constructor for the message. Only time stamp is computed.
	 */
	public NetworkMessage(XMLConfiguration config){
		timeStamp = System.currentTimeMillis();
		valid = true;
		stale = false;
		messageType = NetworkMessage.MESSAGE_TYPE;
		this.config = config;
		
		jsonRepresentation = null;
	}
	
	
	/**
	 * Getter for the ID of the request. The request ID of the message is randomly generated when an instance of outgoing
	 * message is constructed. In case when incoming message is being parsed, the request ID is extracted from the
	 * arriving XMPP message. 
	 * 
	 * @return Numerical ID of the request.
	 */
	public int getRequestId() {
		return requestId;
	}
	
	
	/**
	 * Sets the request ID (e.g. when the message is being parsed).
	 * 
	 * @param requestId Correlation ID of the request. 
	 */
	public void setRequestId(int requestId){
		this.requestId = requestId;
	}


	/**
	 * Retrieves the time stamp of the message. The time stamp is always recorded when instance of this class is 
	 * constructed.
	 *   
	 * @return UNIX time stamp (milliseconds). 
	 */
	public long getTimeStamp() {
		return timeStamp;
	}


	/**
	 * Check for message validity. This checks both for message's syntactical validity (i.e. whether it can be 
	 * successfully parsed) and for being stale (i.e. whether or not the message was sitting somewhere for too long).
	 * 
	 * This can be considered as correct test on whether or not the message should be discarded.
	 * 
	 * @return True if the message is both valid and not stale. 
	 */
	public boolean isValid() {
		
		if ((System.currentTimeMillis() - timeStamp) 
				> (config.getInt(CONFIG_PARAM_XMPPMESSAGETIMEOUT, CONFIG_DEF_XMPPMESSAGETIMEOUT)*1000)){
			stale = true;
		} else {
			stale = false;
		}
		
		return (valid && !stale);
	}


	/**
	 * Override the value of the message validity.
	 * 
	 * @param valid A value to be set.
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}


	/**
	 * Retrieves the type of instantiated message. 
	 * 
	 * @return Integer with corresponding type number. 
	 */
	public int getMessageType() {
		return messageType;
	}


	/**
	 * Sets the type of instantiated message.
	 * 
	 * @param messageType Integer with corresponding type number. 
	 */
	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	
	/**
	 * Inserting a string into JSON has a side effect of the string becoming quoted when extracted back from the JSON.
	 * This little toy just removes quotes if there are any (it tests for their existence first).
	 *  
	 * @param quotedString The original quoted string.
	 * @return Unquoted string.
	 */
	public String removeQuotes(String quotedString){
		
		if (quotedString.isEmpty()){
			return null;
		}
		
		char first = quotedString.charAt(0);
		char last = quotedString.charAt(quotedString.length() - 1);
		
		if (first == '"' && last == '"'){
			return quotedString.substring(1, quotedString.length() - 1);
		} else {
			return quotedString;
		}
	}
	/* === PRIVATE METHODS === */
}
