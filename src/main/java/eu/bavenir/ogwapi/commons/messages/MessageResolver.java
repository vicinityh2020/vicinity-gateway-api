package eu.bavenir.ogwapi.commons.messages;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.Queue;
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
 * Resolver to create a valid {@link NetworkMessage NetworkMessage} objects. After parsing it will try to return the 
 * suitable subclass. During the resolving process it also watches for duplicated messages (a flaw in some XMPP 
 * server implementations) and discards any such duplicate. 
 * 
 *    
 * @author sulfo
 *
 */
public class MessageResolver {

	/* === CONSTANTS === */
	
	/**
	 * Size of the array that keeps record of the request IDs. In other words, how many recent request IDs should
	 * the OGWAPI keep in memory to effectively defend against the duplicated messages. This number should be big 
	 * enough to cover at least 10 minutes of intensive traffic and small enough not to fill the RAM of a machine 
	 * that has many objects connected to its OGWAPI.  
	 */
	private static final int REQUEST_ID_ARRAY_SIZE = 6000;
	
	/* === FIELDS === */
	
	/**
	 * Configuration of the OGWAPI.
	 */
	private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * Queue that keeps record of recent received request IDs, for protection the underlying infrastructure against
	 * duplicated messages. 
	 */
	private Queue<Integer> requestIds;
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public MessageResolver(XMLConfiguration config, Logger logger){
		this.config = config;
		this.logger = logger;
		
		requestIds = new LinkedList<Integer>();
	}
	
	
	/**
	 * Resolves the message that arrived from the network. A valid network message will contain a JSON that
	 * can be decoded as one of the {@link NetworkMessage NetworkMessage} types. 
	 *  
	 * @param String Message body.
	 * @return Some extension of a {@link NetworkMessage NetworkMessage} class, or null if the received message did not
	 * contain a valid or suitable JSON.
	 */
	public NetworkMessage resolveNetworkMessage(String message){
		
		JsonObject json = readJsonObject(message);
		
		if (json == null){
			
			// it is not a JSON...
			return null;
		}
		
		if (!json.containsKey(NetworkMessage.ATTR_MESSAGETYPE)){
			// it is JSON but is malformed
			return null;
		}
		
		// check for message duplication
		if (checkForDuplicates(json.getInt(NetworkMessage.ATTR_REQUESTID))) {
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
	
	
	/**
	 * Creates a JSON object from a string. 
	 * 
	 * @param jsonString A string that is to be decoded as a JSON.
	 * @return JsonObject if the decoding was successful, or null if something went wrong (string is not a valid JSON etc.).  
	 */
	public JsonObject readJsonObject(String jsonString) {
		
		if (jsonString == null) {
			return null;
		}
		
		// make a JSON from the incoming String - any string that is not a valid JSON will throw exception
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		
		JsonObject json;
		
		try {
			json = jsonReader.readObject();
		} catch (Exception e) {
			logger.severe("Exception during reading JSON object: " 
						+ e.getMessage());
			
			return null;
		} finally {
			jsonReader.close();
		}
		
		return json;
	}
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Duplicated messages were discovered when using XMPP engine, however they are not necessarily bound solely to 
	 * XMPP and can possibly manifest themselves in other engines as well. The probable cause seems to be lost 
	 * ACK packet on low quality lines, causing the server to re-send the last packet again, resulting in two
	 * identical messages being received. This class keeps track of recent request IDs (the number is configurable by
	 * {@link #REQUEST_ID_ARRAY_SIZE REQUEST_ID_ARRAY_SIZE} constant) and this method serves the purpose of verifying
	 * whether or not a message with such request ID has already been recently received. 
	 *   
	 * @param requestId The request ID to be checked for duplicates.
	 * @return True if there already was a message with such request ID received recently, false otherwise.
	 */
	private boolean checkForDuplicates(int requestId) {
		
		// if there is no such request ID existing in the queue, add it and watch for overflow
		if (!requestIds.contains(requestId)) {
			
			if (requestIds.size() >= REQUEST_ID_ARRAY_SIZE) {
				
				requestIds.poll();
			}
			
			requestIds.add(requestId);
			
			return false;
		}
		
		logger.info("Duplicated message detected. Request ID: " + requestId);
		
		return true;
	}

}
