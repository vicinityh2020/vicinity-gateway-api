package eu.bavenir.ogwapi.commons;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;

import eu.bavenir.ogwapi.commons.messages.NetworkMessage;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;
import eu.bavenir.ogwapi.restapi.Api;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/*
 * Unit testing:
 * 1. connecting multiple users
 * 2. getting connection lists
 * 3. retrieving rosters of the users
 * 4. sending messages
 * 		a. to online contacts in the roster while online
 * 		b. to offline contact in the roster while online (sending to offline option is true)
 * 		c. to offline contact in the roster while online (sending to offline option is false)
 * 		d. to a contact not in the roster while online
 * 		e. all the above while offline
 * 5. disconnect
 */


// TODO documentation (this main one - the others are good unless stated otherwise)
// TODO make logging more meaningful - manager (INFO), descriptor (FINE), engine (FINER) and document it in the javadoc.
/**
 * 
 * This class serves as a connection manager for XMPP communication over P2P network. There is usually only need
 * for a single instance of this class, even if there are several devices connecting through the Gateway API. The 
 * instance of this class maintains a pool of connection descriptors, where each descriptor represents one separate 
 * client connection. The thread safe pool is based on a synchronized {@link java.util.HashMap HashMap} implementation.
 * 
 *  It is important that the private methods for operations over the descriptor pool are used when extending or 
 *  modifying this class instead of the direct approach to the descriptorPool HashMap (direct methods can however still
 *  be used if they don't alter the HashMap's structure or values).
 *  
 *  Usual modus operandi of this class is as follows:
 *  
 *  1. establishConnection - use as many times as necessary
 *  2. use all the methods as necessary for communication
 *  3. terminateConnection - if there is a need to close a connection for some device. 
 *  4. terminateAllConnections - cleanup when the application shuts down. 
 *  
 *  After step 4, it is safe to start a new with step 1. 
 *  
 *  
 *  MESSAGES
 *  
 *  All communication among connected XMPP clients is exchanged via XMPP messages. In general there are only 2 types
 *  of messages that are exchanged:
 *  
 *  a) requests  -	An Object is requesting an access to a service of another Object (or Agent). This request needs to 
 *  				be propagated across XMPP network and at the end of the communication pipe, the message has to be
 *  				translated into valid HTTP request to an Agent service. Translating the message is as well as 
 *  				their detailed structure is described in {@link RestAgentConnector AgentCommunicator}. See
 *  				{@link NetworkMessageRequest NetworkMessageRequest}.
 *  
 *  b) responses -	The value returned by Object / Agent services in JSON, that is propagated back to the caller. See
 *  				{@link NetworkMessageResponse NetworkMessageResponse}.
 *  
 *    
 * @author sulfo
 *
 */
public class CommunicationManager {

	
	/* === CONSTANTS === */
	

	
	/* === FIELDS === */
	
	
	// TODO all classes in the commons package should have fields described with javadoc
	// hash map containing connections identified by 
	private Map<String, ConnectionDescriptor> descriptorPool;
	
	
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	
	
	/* === PUBLIC METHODS === */
	
	
	/**
	 * Constructor, initialises necessary objects. All parameters are mandatory, failure to include them can lead 
	 * to a swift end of application.
	 * 
	 * @param config Configuration object.
	 * @param logger Java logger.
	 */
	public CommunicationManager(XMLConfiguration config, Logger logger){
		descriptorPool = Collections.synchronizedMap(new HashMap<String, ConnectionDescriptor>());
		
		this.config = config;
		this.logger = logger;
		
	}
	
	
	
	// ADMINISTRATION METHODS - not directly related to interfaces
	
	/**
	 * Retrieves the object IDs that has open connections to network via this ConnectionManager. 
	 * Based on these strings, the respective connections can be retrieved from the connection descriptor pool.
	 * 
	 * @return Set of object IDs. 
	 */
	public Set<String> getConnectionList(){
		
		Set<String> usernames = descriptorPool.keySet();
		
		logger.finest("-- Object IDs connected to network through this CommunicationManager: --");
		for (String string : usernames) {
			logger.finest(string);
		}
		logger.finest("-- End of list. --");
		return usernames;
	}
	
	
	/**
	 * Checks whether the connection {@link ConnectionDescriptor descriptor} instance exists for given object ID and 
	 * whether or not it is connected to the network. Returns true or false accordingly.  
	 * 
	 * @param objectID Object ID in question. 
	 * @return True if descriptor exists and the connection is established.
	 */
	public boolean isConnected(String objectID){
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor != null){
			return descriptor.isConnected();
		} else {
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			
			return false;
		}
	}
	
	
	/**
	 * Verifies the credentials of an object, when (for example) trying to reach its connection 
	 * {@link ConnectionDescriptor descriptor} instance via RESTLET API Authenticator. This method should be called after 
	 * {@link #isConnected(String) isConnected} is used for making sure, that the object is actually connected. 
	 * It is safe to use this method when processing authentication of every request, even in quick succession.
	 * 
	 * @param objectID Object ID in question.
	 * @param password The password that is to be verified. 
	 * @return True, if the password is valid.
	 */
	public boolean verifyPassword(String objectID, String password){
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor != null){
			return descriptor.verifyPassword(password);
		} else {
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			
			return false;
		}
	}
	
	
	/**
	 * Closes all open connections to network. It will also clear these connection handlers off the connection 
	 * descriptor pool table, preventing the re-connection (they have to be reconfigured to be opened again).
	 */
	public void terminateAllConnections(){
		
		Collection<ConnectionDescriptor> descriptors = descriptorPool.values();
		
		logger.info("Closing all connections.");
		
		for (ConnectionDescriptor descriptor: descriptors){
			if (descriptor != null){
				
				descriptor.disconnect();
				
			} else {
				logger.warning("Null record in the connection descriptor pool.");
			}
		}
		
		descriptorPoolClear();
		logger.finest("Connection descriptor pool flushed.");
	}
	
	
	
	
	
	
	// AUTHENTICATION INTERFACE
	
	
	/**
	 * Establishes a single connection for given object with preferences from configuration file and provided credentials. 
	 * The connection descriptor is then stored in the internal descriptor pool.
	 * 
	 * If the connection descriptor for given object already exists, it get terminated, discarded and then recreated.
	 * 
	 * 
	 * NOTE: This is the equivalent of GET /objects/login in the REST API.
	 * 
	 * @param objectID Object ID.
	 * @param password Password.
	 * @return StatusMessage, with the error flag set as false, if the login was successful. If not, the error flag is
	 * set to true.
	 */
	public StatusMessage establishConnection(String objectID, String password){
		
		// if there is a previous descriptor we should close the connection first, before reopening it again
		ConnectionDescriptor descriptor = descriptorPoolRemove(objectID);
		if (descriptor != null){
	
			descriptor.disconnect();
			
			logger.info("Reconnecting '" + objectID + "' to network.");
		}
		
		descriptor = new ConnectionDescriptor(objectID, password, config, logger);
		
		StatusMessage statusMessage;
		
		if (descriptor.connect()){
			logger.info("Connection for '" + objectID +"' was established.");
			
			// insert the connection descriptor into the pool
			descriptorPoolPut(objectID, descriptor);
			logger.finest("A new connection for '" + objectID +"' was added into connection pool.");
			
			statusMessage = new StatusMessage(false, StatusMessage.MESSAGE_BODY, "Login successfull.");
			
		} else {
			logger.info("Connection for '" + objectID +"' was not established.");
			statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_BODY, "Login unsuccessfull.");
		}
		
		return statusMessage;
	}
	
	
	
	/**
	 * Disconnects a single connection identified by the connection object ID given as the first parameter. 
	 * The second parameter specifies, whether the connection descriptor is to be destroyed after it is disconnected.
	 * If not, the descriptor will remain in the pool and it is possible to use it during eventual reconnection.
	 * Otherwise connection will have to be build a new (which can be useful when the connection needs to be 
	 * reconfigured). 
	 * 
	 * This is the equivalent of GET /objects/logout in the REST API.
	 * 
	 * @param objectID User name used to establish the connection.
	 * @param destroyConnectionDescriptor Whether the connection descriptor should also be destroyed or not. 
	 */
	public void terminateConnection(String objectID, boolean destroyConnectionDescriptor){
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID); 
		
		if (descriptor != null){
			descriptor.disconnect();
		} else {
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
		}
		
		if (destroyConnectionDescriptor){
			descriptorPoolRemove(objectID);
			logger.info("Connection for object ID '" + objectID + "' destroyed.");
		} else {
			// this will keep the connection in the pool
			logger.info("Connection for object ID '" + objectID + "' closed.");
		}

	}
	
	
	
	
	// CONSUMPTION INTERFACE

	
	// TODO documentation
	public StatusMessage getPropertyOfRemoteObject(String objectID, String destinationObjectID, String propertyID) {
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			
			return null;
		} 
		
		return descriptor.getPropertyOfRemoteObject(destinationObjectID, propertyID);
		
	}
	
	
	// TODO documentation
	public StatusMessage setPropertyOfRemoteObject(String objectID, String destinationObjectID, String propertyID, String body) {
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			
			return null;
		} 
		
		return descriptor.setPropertyOfRemoteObject(destinationObjectID, propertyID, body);
	}

	
	
	// DISCOVERY INTERFACE
	
	
	/**
	 * Retrieves a collection of roster entries for object ID (i.e. its contact list). If there is no connection 
	 * established for the given object ID, returns empty {@link java.util.Set Set}. 
	 * 
	 * NOTE: This is the equivalent of GET /objects in the REST API.
	 * 
	 * @param objectID Object ID the roster is to be retrieved for. 
	 * @return Set of roster entries. If no connection is established for the object ID, the collection is empty
	 * (not null). 
	 */
	public Set<String> getRosterEntriesForObject(String objectID){
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			return Collections.emptySet();
		}
		
		Set<String> entries = descriptor.getRoster();
		
		// log it
		logger.finest("-- Roster for '" + objectID +"' --");
		for (String entry : entries) {
			// TODO make it possible for the roster to return presence!
			logger.finest(entry + " Presence: " + "UNKNOWN");
		}
		logger.finest("-- End of roster --");
		
		return entries;
	}
	
	
	
	
	
	
	// EXPOSING INTERFACE
	
	
	/**
	 * Activates the event channel identified by the eventID. From the moment of activation, other devices in the 
	 * network will be able to subscribe to it and will receive events in case they are generated. 
	 * 
	 * If the event channel was never activated before (or there is other reason why it was not saved previously), it 
	 * gets created anew. In that case, the list of subscribers is empty.  
	 * 
	 * NOTE: This is the equivalent of POST /events/[eid] in the REST API.
	 * 
	 * @param objectID Object ID of the event channel owner.
	 * @param eventID Event ID.
	 * @return {@link StatusMessage StatusMessage} with error flag set to false, if the event channel was activated
	 * successfully.
	 */
	public StatusMessage activateEventChannel(String objectID, String eventID) {
		
		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor == null){
			
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			return null;
		}
		
		descriptor.setLocalEventChannelStatus(eventID, EventChannel.STATUS_ACTIVE);
		
		return new StatusMessage(false, StatusMessage.MESSAGE_BODY, "Channel activated.");
	}
	
	
	
	// TODO documentation
	// returns number of sent messages vs the number of subscribers
	public StatusMessage sendEventToSubscribedObjects(String objectID, String eventID, String event) {
		
		String statusMessageText;
		
		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor == null){
			
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			return null;
		}
		
		int numberOfSentMessages = 0;
		int numberOfSubscribers = descriptor.getNumberOfSubscribers(eventID);
		
		// no need to waste cycles for nothing
		if (numberOfSubscribers > 0) {
			numberOfSentMessages = descriptor.sendEventToSubscribers(eventID, event);
		}
		
		statusMessageText = new String("Event " + eventID + " was successfully sent to " 
								+ numberOfSentMessages + " out of " 
								+ numberOfSubscribers + " subscribers.");
		logger.info(statusMessageText);
		
		return new StatusMessage(false, StatusMessage.MESSAGE_BODY, statusMessageText);
		
	}
	
	
	
	/**
	 * De-activates the event channel identified by the eventID. From the moment of de-activation, other devices in the
	 * network will not be able to subscribe to it. Also no events are sent in case they are generated. 
	 * 
	 * The channel will still exist though along with the list of subscribers. If it gets re-activated, the list of
	 * subscribers will be the same as in the moment of de-activation.  
	 * 
	 * NOTE: This is the equivalent of DELETE /events/[eid] in the REST API.
	 * 
	 * @param objectID Object ID of the event channel owner
	 * @param eventID Event ID.
	 * @return {@link StatusMessage StatusMessage} with error flag set to false, if the event channel was activated
	 * successfully.
	 */
	public StatusMessage deactivateEventChannel(String objectID, String eventID) {
		
		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(objectID);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectID + "'.");
			return null;
		}
		
		descriptor.setLocalEventChannelStatus(eventID, EventChannel.STATUS_INACTIVE);
		
		return new StatusMessage(false, StatusMessage.MESSAGE_BODY, "Channel deactivated.");
	}
	
	
	
	/*
	// TODO documentation
	public StatusMessage getEventChannelStatus(String objectID, String EventID) {
		
	}
	
	
	
	// TODO documentation
	public StatusMessage subscribeToEventChannel(String objectID, String eventID) {
		
	}
	
	
	// TODO documentation
	public StatusMessage unsubscribeFromEventChannel(String objectID, String eventID) {
		
	}
	*/
	
	
	// REGISTRY INTERFACE
	
	
	
	
	
	
	
	// QUERY INTERFACE
	
	
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Thread-safe method for inserting an object ID (K) and a descriptor (V) into the descriptor pool. This is a
	 * synchronised equivalent for {@link java.util.HashMap#put(Object, Object) put()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#put(Object, Object) put()} method. 
	 *    
	 * @param objectID The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @param descriptor The value part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The previous value associated with key, or null if there was no mapping for key. 
	 * (A null return can also indicate that the map previously associated null with key, if the implementation 
	 * supports null values.)
	 */
	private ConnectionDescriptor descriptorPoolPut(String objectID, ConnectionDescriptor descriptor){
		synchronized (descriptorPool){
			return descriptorPool.put(objectID, descriptor);
		}
	}
	
	
	/**
	 * Thread-safe method for retrieving a connection descriptor (V) from the descriptor pool by object ID (K). 
	 * This is a synchronised equivalent for {@link java.util.HashMap#get(Object) get()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#get(Object) get()} method. 
	 *    
	 * @param objectID The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The value to which the specified key is mapped, or null if this map contains no mapping for the key.
	 */
	private ConnectionDescriptor descriptorPoolGet(String objectID){
		synchronized (descriptorPool){
			
			return descriptorPool.get(objectID);
		}
	}
	
	
	/**
	 * Thread-safe method for removing a connection descriptor (V) for the object ID (K) from the descriptor pool. 
	 * This is a synchronised equivalent for {@link java.util.HashMap#remove(Object) remove()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#remove(Object) remove()} method.
	 *    
	 * @param objectID The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The previous value associated with key, or null if there was no mapping for key.
	 */
	private ConnectionDescriptor descriptorPoolRemove(String objectID){
		synchronized (descriptorPool){
			
			return descriptorPool.remove(objectID);
		}
	}
	
	
	/**
	 * Thread-safe method for clearing the descriptor pool. This is a synchronised equivalent for 
	 * {@link java.util.HashMap#clear() clear()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#clear() clear()} method. 
	 */
	private void descriptorPoolClear(){
		synchronized (descriptorPool){
			descriptorPool.clear();
		}
	}
}
