package eu.bavenir.vicinity.gatewayapi.xmpp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

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
 *  				their detailed structure is described in {@link AgentCommunicator AgentCommunicator}. See
 *  				{@link NetworkMessageRequest NetworkMessageRequest}.
 *  
 *  b) responses -	The value returned by Object / Agent services in JSON, that is propagated back to the caller. See
 *  				{@link NetworkMessageResponse NetworkMessageResponse}.
 *  
 *    
 * @author sulfo
 *
 */
public class CommunicationNode {

	
	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for XMPP .
	 */
	private static final String CONFIG_PARAM_XMPPDEBUG = "xmpp.debug";
	
	/**
	 * Default value of xmpp.debug configuration parameter. This value is taken into account when no suitable
	 * value is found in the configuration file. 
	 */
	private static final boolean CONFIG_DEF_XMPPDEBUG = false;
		

	
	/* === FIELDS === */
	
	// hash map containing XMPP connections identified by 
	private Map<String, XmppConnectionDescriptor> descriptorPool;
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	
	
	/* === PUBLIC METHODS === */
	
	
	/**
	 * Constructor, initializes necessary objects. All parameters are mandatory, failure to include them can lead 
	 * to a swift end of application execution.
	 * 
	 * @param config Configuration object.
	 * @param logger Java logger.
	 */
	public CommunicationNode(XMLConfiguration config, Logger logger){
		descriptorPool = Collections.synchronizedMap(new HashMap<String, XmppConnectionDescriptor>());
		 
		this.config = config;
		this.logger = logger;
		 
		// enable debugging if desired
		boolean debuggingEnabled = config.getBoolean(CONFIG_PARAM_XMPPDEBUG, CONFIG_DEF_XMPPDEBUG);
		if (debuggingEnabled) {
			SmackConfiguration.DEBUG = debuggingEnabled;
			logger.config("XMPP debugging enabled.");
		}
	}
	
	
	
	/**
	 * Establishes a connection to XMPP server with preferences from configuration file and provided credentials. 
	 * The connection descriptor is then stored in a hash map.
	 * 
	 * If the connection descriptor for given user already exists, it is closed by the SMACK 
	 * {@link AbstractXMPPConnection#disconnect() disconnect()} call, discarded and then recreated.
	 * 
	 * @param xmppUsername XMPP user name without the served domain (i.e. just 'user' instead of 'user@xmpp.server').
	 * @param xmppPassword Password of the user.
	 * @return The established connection descriptor if the attempt was successful, null otherwise.
	 */
	public XmppConnectionDescriptor establishConnection(String xmppUsername, String xmppPassword){
		
		System.out.println("STABILITY DEBUG: establishConnection - Start - Count of connection descriptors: " + descriptorPool.size());
		
		// if there is a previous descriptor we should close the connection first, before reopening it again
		XmppConnectionDescriptor descriptor = descriptorPoolRemove(xmppUsername);
		if (descriptor != null){
	
			descriptor.disconnect();
			
			logger.info("Reconnecting '" + xmppUsername + "' to XMPP.");
		}
		
		descriptor = new XmppConnectionDescriptor(xmppUsername, xmppPassword, config, logger);
		
		if (descriptor.connect()){
			logger.info("XMPP connection for '" + xmppUsername +"' was established.");
		} else {
			
			logger.info("XMPP connection for '" + xmppUsername +"' was not established.");
			return null;
		}
		
		// insert the connection descriptor into the pool
		descriptorPoolPut(xmppUsername, descriptor);
		logger.finest("A new connection for '" + xmppUsername +"' was added into connection pool.");
		
		System.out.println("STABILITY DEBUG: establishConnection - End - Count of connection descriptors: " + descriptorPool.size());
		
		return descriptor;
	}
	
	
	
	/**
	 * Disconnects a single XMPP connection identified by the connection user name given as the first parameter. 
	 * The second parameter specifies, whether the connection descriptor is to be destroyed after it is disconnected.
	 * If not, the descriptor will remain in the pool and it is possible to use it during eventual reconnection.
	 * Otherwise connection will have to be build a new (which can be useful when the connection needs to be 
	 * reconfigured). 
	 * 
	 * @param xmppUsername User name used to establish the connection.
	 * @param destroyConnectionDescriptor Whether the connection descriptor should also be destroyed or not. 
	 */
	public void terminateConnection(String xmppUsername, boolean destroyConnectionDescriptor){
		
		System.out.println("STABILITY DEBUG: terminateConnection - Start - Count of connection descriptors: " + descriptorPool.size());
		
		XmppConnectionDescriptor descriptor = descriptorPoolGet(xmppUsername); 
		
		if (descriptor != null){
			descriptor.disconnect();
		} else {
			logger.warning("Null record in the connection descriptor pool. XMPP user: '" 
					+ xmppUsername + "'.");
		}
		
		if (destroyConnectionDescriptor){
			descriptorPoolRemove(xmppUsername);
			logger.info("Connection to XMPP for user '" + xmppUsername + "' destroyed.");
		} else {
			// this will keep the connection in the pool
			logger.info("Connection to XMPP for user '" + xmppUsername + "' closed.");
		}
		
		System.out.println("STABILITY DEBUG: terminateConnection - End - Count of connection descriptors: " + descriptorPool.size());
	}
	
	
	
	/**
	 * Closes all open connections to XMPP server. It will also clear these connection handlers off the connection 
	 * descriptor pool table, preventing the re-connection (they have to be reconfigured to be opened again).
	 */
	public void terminateAllConnections(){
		
		Collection<XmppConnectionDescriptor> descriptors = descriptorPool.values();
		
		logger.info("Closing all connections to XMPP server.");
		
		for (XmppConnectionDescriptor descriptor: descriptors){
			if (descriptor != null){
				
				descriptor.disconnect();
				
			} else {
				logger.warning("Null record in the connection descriptor pool.");
			}
		}
		
		descriptorPoolClear();
		logger.finest("XMPP connection descriptor pool flushed.");
	}
	
	
	
	/**
	 * Retrieves the XMPP user names that has open connections to XMPP server. Based on these strings, the respective
	 * connections can be retrieved from the connection descriptor pool.
	 * 
	 * @return Set of user names. 
	 */
	public Set<String> getConnectionList(){
		
		Set<String> usernames = descriptorPool.keySet();
		
		logger.finest("-- XMPP users connected to the server: --");
		for (String string : usernames) {
			logger.finest(string);
		}
		logger.finest("-- End of list. --");
		return usernames;
	}
	
	
	/**
	 * Checks whether the XMPP connection {@link XmppConnectionDescriptor descriptor} instance exists for given user and 
	 * whether or not it is connected to the XMPP server. Returns true or false accordingly.  
	 * 
	 * @param xmppUsername XMPP user name in question. 
	 * @return True if descriptor exists and the connection is established.
	 */
	public boolean isConnected(String xmppUsername){
		XmppConnectionDescriptor descriptor = descriptorPoolGet(xmppUsername);
		
		if (descriptor != null){
			return descriptor.isConnected();
		} else {
			logger.warning("Null record in the connection descriptor pool. XMPP user: '" 
					+ xmppUsername + "'.");
			
			return false;
		}
	}
	
	
	/**
	 * Verifies the credentials of a client, (for example) trying to reach its XMPP connection 
	 * {@link XmppConnectionDescriptor descriptor} instance via RESTLET API. This method should be called after 
	 * {@link #isConnected(String) isConnected} is called first, otherwise will always return false. 
	 * It is safe to use this method when processing authentication of every request.
	 * 
	 * @param xmppUsername XMPP user name in question.
	 * @param xmppPassword The password that is to be verified. 
	 * @return True, if the password is valid.
	 */
	public boolean verifyConnection(String xmppUsername, String xmppPassword){
		XmppConnectionDescriptor descriptor = descriptorPoolGet(xmppUsername);
		
		if (descriptor != null){
			return descriptor.verifyPassword(xmppPassword);
		} else {
			logger.warning("Null record in the connection descriptor pool. XMPP user: '" 
					+ xmppUsername + "'.");
			
			return false;
		}
	}
	
	
	/**
	 * Retrieves a collection of roster entries for given user name. If there is no connection established with the 
	 * given user name, returns empty {@link java.util.Collection Collection}. 
	 * 
	 * @param username XMPP user name the roster is to be retrieved for. 
	 * @return Collection of roster entries. If no connection is established for the user name, the collection is empty
	 * (not null). 
	 */
	public Collection<RosterEntry> getRosterEntriesForUser(String username){
		
		XmppConnectionDescriptor descriptor = descriptorPoolGet(username);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool.");
			return Collections.emptyList();
		}
		
		Roster roster = descriptor.getRoster();
		Collection<RosterEntry> entries = roster.getEntries();
		
		// log it
		logger.finest("-- Roster for '" + username +"' --");
		for (RosterEntry entry : entries) {
			logger.finest(entry.getJid().toString() + " Presence: " + roster.getPresence(entry.getJid()).toString());
		}
		logger.finest("-- End of roster --");
		
		return roster.getEntries();
	}
	
	
	/**
	 * Sends a message (string) from source user name, to destination user name. In case there are some doubts about
	 * security in sending the message this way (especially by having the option to define from which user name the 
	 * message originates), they are not much justified. The safety is inherent in the fact, that the source user name 
	 * must have established connection on this instance of CommunicationNode first (i.e. the credentials must be known,
	 * so the connection descriptor can be created). Moreover the device is authenticated with every request.  
	 * 
	 * It is thus impossible to act as a device from some other gateway/owner.
	 *  
	 * @param sourceUsername User name of the originating device (without the XMPP domain).
	 * @param destinationUsername User name of the destination device (without the XMPP domain).
	 * @param message Message string.
	 * @return True on success, false if the destination was offline or if some error occurred.
	 */
	public boolean sendMessage(String sourceUsername, String destinationUsername, String message){
		
		System.out.println("STABILITY DEBUG: sendMessage - Start - Count of connection descriptors: " + descriptorPool.size());
		
		// check the validity of source user
		XmppConnectionDescriptor descriptor = descriptorPoolGet(sourceUsername);
		
		if (descriptor == null){
			logger.warning("No descriptor exist for source username '" + sourceUsername + "'. The device has not logged"
					+ " into the Gateway API yet.");
			return false;
		}
		
		// if the connection disintegrated for some reason, be proactive and reconnect
		if (descriptor.isConnected() == false) {
			logger.info("The connection is closed for username '" + sourceUsername + "'. Attempting to reconnect.");
			
			if (!descriptor.connect()){
				logger.warning("The connection for username '" + sourceUsername + "' can't be established.");
				return false;
			}
		}

		System.out.println("STABILITY DEBUG: sendMessage - End - Count of connection descriptors: " + descriptorPool.size());
		
		return descriptor.sendMessage(destinationUsername, message);
	}
	
	
	/**
	 * Retrieves a single {@link NetworkMessage NetworkMessage} from the queue of incoming messages. It is important
	 * to provide a user name of the recipient and correlation request ID. This method blocks the thread when there are
	 * no messages in the queue and waits for the arrival.
	 * 
	 * @param forUsername Recipient user name.
	 * @param requestId Correlation request ID.
	 * @return {@link NetworkMessage NetworkMessage} received over XMPP message.
	 */
	public NetworkMessage retrieveSingleMessage(String forUsername, int requestId){
		
		System.out.println("STABILITY DEBUG: retrieveSingleMessage - Start - Count of connection descriptors: " + descriptorPool.size());
		
		// check the validity of source user
		XmppConnectionDescriptor descriptor = descriptorPoolGet(forUsername);
		
		if (descriptor == null){
			logger.warning("No descriptor exist for source username '" + forUsername + "'. The device has not logged"
					+ " into the Gateway API yet.");
			return null;
		}
		
		// TODO enable this and delete later construct
		//return descriptor.retrieveMessage(requestId);
		
		NetworkMessage networkMessage = descriptor.retrieveMessage(requestId); 
		if (networkMessage != null){
			NetworkMessageResponse helperMessage = (NetworkMessageResponse) networkMessage;
			System.out.println("STABILITY DEBUG: Response message body: " + helperMessage.getResponseBody());
		}
		
		System.out.println("STABILITY DEBUG: retrieveSingleMessage - End - Count of connection descriptors: " + descriptorPool.size());
		
		return networkMessage;
	}
	
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Thread-safe method for inserting a XMPP user name (K) and a descriptor (V) into the descriptor pool. This is a
	 * synchronized equivalent for {@link java.util.HashMap#put(Object, Object) put()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#put(Object, Object) put()} method. 
	 *    
	 * @param xmppUsername The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @param descriptor The value part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The previous value associated with key, or null if there was no mapping for key. 
	 * (A null return can also indicate that the map previously associated null with key, if the implementation 
	 * supports null values.)
	 */
	private XmppConnectionDescriptor descriptorPoolPut(String xmppUsername, XmppConnectionDescriptor descriptor){
		synchronized (descriptorPool){
			
			System.out.println("STABILITY DEBUG: descriptorPoolPut - Before operation - Count of connection descriptors: " + descriptorPool.size());
			return descriptorPool.put(xmppUsername, descriptor);
		}
	}
	
	
	/**
	 * Thread-safe method for retrieving a connection descriptor (V) from the descriptor pool by XMPP user name (K). 
	 * This is a synchronized equivalent for {@link java.util.HashMap#get(Object) get()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#get(Object) get()} method. 
	 *    
	 * @param xmppUsername The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The value to which the specified key is mapped, or null if this map contains no mapping for the key.
	 */
	private XmppConnectionDescriptor descriptorPoolGet(String xmppUsername){
		synchronized (descriptorPool){
			
			System.out.println("STABILITY DEBUG: descriptorPoolGet - Before operation - Count of connection descriptors: " + descriptorPool.size());
			
			return descriptorPool.get(xmppUsername);
		}
	}
	
	
	/**
	 * Thread-safe method for removing a connection descriptor (V) from the descriptor pool by XMPP user name (K). 
	 * This is a synchronized equivalent for {@link java.util.HashMap#remove(Object) remove()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#remove(Object) remove()} method.
	 *    
	 * @param xmppUsername The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The previous value associated with key, or null if there was no mapping for key.
	 */
	private XmppConnectionDescriptor descriptorPoolRemove(String xmppUsername){
		synchronized (descriptorPool){
			
			System.out.println("STABILITY DEBUG: descriptorPoolRemove - Before operation - Count of connection descriptors: " + descriptorPool.size());
			
			return descriptorPool.remove(xmppUsername);
		}
	}
	
	
	/**
	 * Thread-safe method for clearing the descriptor pool. This is a synchronized equivalent for 
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
