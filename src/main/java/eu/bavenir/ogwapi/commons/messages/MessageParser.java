package eu.bavenir.ogwapi.commons.messages;

import java.io.StringReader;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.configuration2.XMLConfiguration;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * Parser to create a valid {@link NetworkMessage NetworkMessage} objects. After parsing it will try to return the 
 * suitable subclass.
 * 
 *    
 * @author sulfo
 *
 */
public class MessageParser {

	/* === CONSTANTS === */
	
	
	/* === FIELDS === */
	private XMLConfiguration config;
	
	private Logger logger;
	
	
	/* === PUBLIC METHODS === */
	
	public MessageParser(XMLConfiguration config, Logger logger){
		this.config = config;
		this.logger = logger;
	}
	
	
	/**
	 * Parses the message that arrived from the network. A valid network message will contain a JSON that
	 * can be decoded as one of the {@link NetworkMessage NetworkMessage} types. 
	 *  
	 * @param String Message body.
	 * @return Some extension of a {@link NetworkMessage NetworkMessage} class, or null if the received message did not
	 * contain a valid or suitable JSON.
	 */
	public NetworkMessage parseNetworkMessage(String message){
		
		// make a JSON from the incoming String - IMPORTANT! any string that is not a valid JSON will throw exception
		JsonReader jsonReader = Json.createReader(new StringReader(message));
		JsonObject json = jsonReader.readObject(); // right here
		
		if (json == null){
			// it is not a JSON...
			return null;
		}
		
		if (!json.containsKey(NetworkMessage.ATTR_MESSAGETYPE)){
			// it is JSON but is malformed
			return null;
		}
		
		// ok seems legit
		switch (json.getInt(NetworkMessage.ATTR_MESSAGETYPE)){
		
		case NetworkMessageRequest.MESSAGE_TYPE:
			return new NetworkMessageRequest(json, config, logger);
			
		case NetworkMessageResponse.MESSAGE_TYPE:
			return new NetworkMessageResponse(json, config, logger);
			
		case NetworkMessageEvent.MESSAGE_TYPE:
			return new NetworkMessageEvent(json, config, logger);
			
			default:
				return null;
		}
	}
	
	
	/* === PRIVATE METHODS === */

}
