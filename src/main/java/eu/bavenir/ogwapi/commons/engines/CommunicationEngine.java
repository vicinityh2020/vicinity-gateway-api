package eu.bavenir.ogwapi.commons.engines;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.ConnectionDescriptor;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * This abstract class provides a little more than an interface for specific communication engines used by OGWAPI. 
 * By extending this class a new communication engine can be implemented. Keep in mind, that any new engine that is about
 * to be used should be reliable and secure. Usage of protocols that are open source and evidently suitable for IoT 
 * communication is encouraged (reference XMPP message engine based on Ignite Realtime's SMACK library can be found in 
 * {@link eu.bavenir.ogwapi.commons.engines.xmpp.XmppMessageEngine XmppMessageEngine}. Also try not to make the engine
 * too resource hungry. Each {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} has its own 
 * instance of the engine. What will happen if 100 objects will be connected through this gateway? Think big.
 * Try to make it as optimised as possible. 
 * 
 * It is recommended to create a separate package for your new engine in eu.bavenir.ogwapi.commons.engines.{protocol} and
 * put there classes that you need to support the engine. It is also advised to use some common parameters from the 
 * configuration file (those in 'general' section) before making a section of your own with additional parameters. 
 * 
 * You'd also like to add your new engine into the list of available engines in the configuration file and add it into 
 * {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor}'s constructor decision tree in order to be
 * loaded.
 * 
 * In order for your engine to work, you'll of course need to implement all methods mentioned here. They will be called
 * by the {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} during various phases of the 
 * OGWAPI operation. The respective requirements are described in each method's JavaDoc. Maybe you'll find out
 * that the methods here are not sufficient for your engine to function properly and that a new method needs to be created.
 * This is discouraged, because nobody likes refactoring :) Try to do it without it first and only add it as a last option.
 * 
 * Lastly, you might have noticed that there is a {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} 
 * included as a field in this class (and therefore also in the class of your new engine). If you wonder what is the purpose 
 * of this field, look no further: this is how incoming message gets transferred into the ConnectionDescriptor for the 
 * current object for processing. Any time your communication engine receives a new message intended for the object that owns
 * the {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} (and therefore a particular instance 
 * of your engine), it needs to call {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor#processIncommingMessage(String, String) processIncommingMessage}
 * method, with the source object ID and the message itself.
 *     
 * @author sulfo
 *
 */
public abstract class CommunicationEngine {

	/* === CONSTANTS === */
	
	
	/* === FIELDS === */

	/**
	 * Configuration of the OGWAPI.
	 */
	protected XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	protected Logger logger;

	/**
	 * String with the object ID that connects via this engine.
	 */
	protected String objectId;
	
	/**
	 * Password string for authentication.
	 */
	protected String password;
	
	/**
	 * This {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} provides access to a 
	 * {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor#processIncommingMessage(String, String) processIncommingMessage}
	 * method.
	 */
	protected ConnectionDescriptor connectionDescriptor;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor for field initialisation. Your implementation needs to call the super(objectId, password, config, logger, connectionDescriptor)
	 * method of this class as the first thing it ever does. 
	 * 
	 * @param objectId String with the object ID that connects via this engine.
	 * @param password Password string for authentication.
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 * @param connectionDescriptor Connection descriptor that is using this particular instance of engine. 
	 */
	public CommunicationEngine(String objectId, String password, XMLConfiguration config, Logger logger, 
																	ConnectionDescriptor connectionDescriptor) {
		
		this.config = config;
		this.logger = logger;
		
		this.objectId = objectId;
		this.password = password;
		
		this.connectionDescriptor = connectionDescriptor;
		
	}
	
	
	/**
	 * This method will attempt to establish a connection with the network. The connection must be able to be closed
	 * and re-opened without a need to throw this object into a garbage collector and creating a new one. 
	 * 
	 * @return True if the connection attempt was successful, false otherwise.
	 */
	public abstract boolean connect();
	

	/**
	 * This method will disconnect from the network. No contact of the network with the engine should be possible and
	 * all objects on the network should see the current object as offline. On the other hand, it should not prevent the
	 * object from reestablishing connection by using {@link #connect() connect} method without re-creating the instance of 
	 * your engine.    
	 */
	public abstract void disconnect();
	
	
	/**
	 * Serves as a check whether or not the owner object is connected to the network or not. 
	 * 
	 * @return True if connected, false otherwise.
	 */
	public abstract boolean isConnected();
	
	
	/**
	 * Serves to retrieve a set of object IDs that are visible to the owner object. 
	 * 
	 * @return Set of object IDs visible to the object.
	 */
	public abstract Set<String> getRoster();

	
	/**
	 * Sends a message string to a destination object identified by an ID. 
	 * 
	 * @param destinationObjectId Destination object ID.
	 * @param message Message string to be sent.
	 * @return True if the sending of message was successful, false otherwise. 
	 */
	public abstract boolean sendMessage(String destinationObjectId, String message);
	
		
	/* === PRIVATE METHODS === */
}
