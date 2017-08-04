package eu.bavenir.vicinity.gatewayapi.xmpp;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jivesoftware.smack.packet.Message;

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
	
	/* === PUBLIC METHODS === */
	
	public MessageParser(XMLConfiguration config){
		this.config = config;
	}
	
	
	/**
	 * Parses the XmppMessage that arrived from the XMPP network. A valid network message will contain a JSON that
	 * can be decoded as one of the {@link NetworkMessage NetworkMessage} types. 
	 *  
	 * @param xmppMessage The raw message from XMPP network.
	 * @return Some extension of a {@link NetworkMessage NetworkMessage} class, or null if the received message did not
	 * contain a valid or suitable JSON.
	 */
	public NetworkMessage parseNetworkMessage(Message xmppMessage){
		
		// make a JSON from the incoming String - IMPORTANT! any string that is not a valid JSON will throw exception
		JsonReader jsonReader = Json.createReader(new StringReader(xmppMessage.getBody()));
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
			return new NetworkMessageRequest(json, config);
			
		case NetworkMessageResponse.MESSAGE_TYPE:
			return new NetworkMessageResponse(json, config);
			
			default:
				return null;
		}
	}
	
	
	/* === PRIVATE METHODS === */

}
