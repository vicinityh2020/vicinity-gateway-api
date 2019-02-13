package eu.bavenir.ogwapi.commons.messages;

import java.util.logging.Logger;

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
 * This is the basis for OGWAPI network messaging protocol and serves as a parent class for {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest request},
 * {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse response} and {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent event}.
 * It contains fields that are common for all extending classes and ensure proper routing across the network, while the
 * functionality gets extended in subclasses.
 * 
 * If there is a need to create a new message class for the OGWAPI's protocol, make it a subclass of this one and give it
 * its own {@link #MESSAGE_TYPE MESSAGE_TYPE} number (look into other message classes to see what number they are using
 * and avoid those).
 * 
 * When extending OGWAPI's code with processes that handle freshly parsed message of any type, always check for 
 * {@link #isValid() validity} and if the message is not valid, discard it.
 * 
 * Recommended general approach should be obvious from a quick glance on {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest request},
 * {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse response} or {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent event}
 * message implementation:
 * 
 *  1. Create a parsing method for your new message class. See {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest#parseJson parseJson}
 *     of the request message to see how to do it.
 *  2. In the constructor of your message use following (or similar) few lines of code:
 *  
 *     if (!parseJson(json)){
 *         setValid(false);
 *     }
 *     
 *  3. Add appropriate lines into {@link eu.bavenir.ogwapi.commons.messages.MessageResolver MessageResolver} constructor.
 *     This will ensure correct translation of the incoming message.  
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
	 * Name of the request ID attribute in JSON that is to be transported. 
	 */
	public static final String ATTR_REQUESTID = "requestId";
	
	/**
	 * Name of the source object ID attribute in JSON that is to be transported. 
	 */
	public static final String ATTR_SOURCEOID = "sourceOid";
	
	/**
	 * Name of the destination object ID attribute in JSON that is to be transported. 
	 */
	public static final String ATTR_DESTINATIONOID = "destinationOid";
	
	/**
	 * Number of seconds to consider request message as no longer relevant. 
	 * After a request is sent from point A to point B, point A waits for 
	 * response. If the response does not arrive until this timeout expires, 
	 * point B is considered unreachable. If the response arrives after this
	 * happens, the response is ignored and discarded and a new request has
	 * to be sent.
	 */
	public static final String CONFIG_PARAM_REQUESTMESSAGETIMEOUT = "general.requestMessageTimeout";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_REQUESTMESSAGETIMEOUT CONFIG_PARAM_REQUESTMESSAGETIMEOUT} 
	 * configuration parameter. This value is taken into account when no suitable value is found in the configuration
	 * file.
	 */
	public static final int CONFIG_DEF_REQUESTMESSAGETIMEOUT = 60;
	
	
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
	 * A service call to Gateway API will produce a message sent across the network for processing by a 
	 * remote object. After processing a response message is sent back, received and inserted into a queue. The service
	 * that was the original sender can then sort through the queue and find a the message it is waiting for based on
	 * this request ID. 
	 */
	protected int requestId;
	
	/**
	 * String with object ID of the destination.
	 */
	protected String destinationOid;
	
	/**
	 * String with the object ID of the destination.
	 */
	protected String sourceOid;
	
	/**
	 * UNIX time stamp generated in the moment the instance of this class is constructed. 
	 */
	protected long timeStamp;
	
	/**
	 * Configuration - necessary to obtain a timeout value. 
	 */
	protected XMLConfiguration config;
	
	/**
	 * Logger - useful when exception during parsing is thrown. 
	 */
	protected Logger logger;
	
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
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NetworkMessage(XMLConfiguration config, Logger logger){
		
		// initialise
		
		timeStamp = System.currentTimeMillis();
		valid = true;
		stale = false;
		messageType = NetworkMessage.MESSAGE_TYPE;
		
		sourceOid = null;
		destinationOid = null;
		jsonRepresentation = null;
		
		this.config = config;
		this.logger = logger;
		
	}
	
	
	/**
	 * Getter for the ID of the request. The request ID of the message is randomly generated when an instance of outgoing
	 * message is constructed. In case when incoming message is being parsed, the request ID is extracted from the
	 * arriving message. 
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
	 * Returns the object ID of the destination.
	 * 
	 * @return Object ID of the destination.
	 */
	public String getDestinationOid() {
		return destinationOid;
	}


	/**
	 * Sets the destination object ID.
	 * 
	 * @param destinationOid Object ID of the destination.
	 */
	public void setDestinationOid(String destinationOid) {
		this.destinationOid = destinationOid;
	}


	/**
	 * Returns the object ID of the source.
	 * 
	 * @return Object ID of the source.
	 */
	public String getSourceOid() {
		return sourceOid;
	}


	/**
	 * Sets the object ID of the source.
	 * 
	 * @param sourceOid Source object ID.
	 */
	public void setSourceOid(String sourceOid) {
		this.sourceOid = sourceOid;
	}


	/**
	 * Retrieves the time stamp of the message. The time stamp is always recorded when instance of this class or its subclass 
	 * is constructed.
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
				> (config.getInt(CONFIG_PARAM_REQUESTMESSAGETIMEOUT, CONFIG_DEF_REQUESTMESSAGETIMEOUT)*1000)){
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
	 * @param quotedString The original DOUBLE quoted string.
	 * @return Unquoted string, or null if the string is either null, or it has zero length. It returns the original 
	 * string if it is not quoted.
	 */
	public String removeQuotes(String quotedString){
		
		if (quotedString == null || quotedString.isEmpty()){
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
