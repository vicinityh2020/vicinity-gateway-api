package eu.bavenir.ogwapi.commons;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;

import eu.bavenir.ogwapi.commons.connectors.NeighbourhoodManagerConnector;
import eu.bavenir.ogwapi.commons.messages.CodesAndReasons;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;
import eu.bavenir.ogwapi.commons.monitoring.MessageCounter;

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
 * This class serves as a connection manager for OGWAPI's communication over P2P network. There is usually only need
 * for a single instance of this class, even if there are several devices connecting through the OGWAPI. The 
 * instance of this class maintains a pool of connection descriptors, where each descriptor represents one separate 
 * client connection. The thread safe pool is based on a synchronised {@link java.util.HashMap HashMap} implementation.
 * 
 *  It is important that the private methods for operations over the descriptor pool {@link #descriptorPoolClear() descriptorPoolClear},
 *  {@link #descriptorPoolGet(String) descriptorPoolGet}, {@link #descriptorPoolPut(String, ConnectionDescriptor) descriptorPoolPut},
 *  {@link #descriptorPoolRemove(String) descriptorPoolRemove}) are used when extending or 
 *  modifying this class instead of the direct approach to the descriptorPool's HashMap (which can cause significant 
 *  race conditions).
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
 * @author sulfo
 *
 */
public class CommunicationManager {

	/**
	 * This records the build and version number.
	 */
	private static final String OGWAPI_VERSION = "0.7";
	
	/* === CONSTANTS === */
	
	/**
	 * This parameter defines how the sessions that went down should be recovered.
	 * A session is a connection created when individual object logs in and the
	 * OGWAPI tries as much as possible to keep it open. 
	 * 
	 * However there are cases when the OGWAPI will give up its attempts to 
	 * maintain the session, e.g. when the communication with server is 
	 * interrupted for prolonged time (depends on engine in use). In such cases
	 * there usually is a need to recover the sessions after communication is 
	 * restored. 'Aggressiveness' of OGWAPI required to recover lost sessions is 
	 * scenario dependent. Following are accepted values for this parameter, 
	 * along with explanations:
	 * 
	 * 
	 * proactive
	 * 
	 * After you log your objects in and a session will go down for some reason,
	 * OGWAPI will be trying hard to reconnect every 30 seconds until it succeeds 
	 * or until you log your objects out. It does not care if your object is 
	 * ready to listen to incoming requests or not. Incoming requests may 
	 * therefore still time out if your adapter is not ready, although it will 
	 * look online in the infrastructure. Good for objects that are expected 
	 * to be always online and will likely be ready to respond, e.g. VAS or 
	 * other services.
	 * NOTE OF CAUTION: When you are testing (or better said debugging...) your
	 * scenarios on two machines with identical credentials, the machine that 
	 * runs OGWAPI with this parameter set to 'proactive' will keep stealing
	 * your connection. If both of them are configured to do so, it will produce
	 * plenty of exceptions. 
	 * 
	 * 
	 * none
	 * 
	 * The OGWAPI will not make any extra effort to recover the sessions.
	 * If you log your object in and the session for some reason fails, it will
	 * remain so until you explicitly re-log your object. This was the original
	 * behaviour in previous versions of OGWAPI.
	 * 
	 * 
	 * passive
	 * 
	 * This will make OGWAPI terminate (!) sessions that are not refreshed
	 * periodically. Refreshing a connection means calling the login method at
	 * least every 30 seconds by default, although this number can be altered 
	 * with sessionExpiration parameter. 
	 * Call to login method is optimised, so there is no 
	 * overhead and it will not attempt to actually log an object in if it already
	 * is. Good for integrators that like to have things under control, implement
	 * adapters on small end user devices or have a need to implement a kind of
	 * presence into their application.
	 */
	private static final String CONFIG_PARAM_SESSIONRECOVERY = "general.sessionRecovery";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_SESSIONRECOVERY CONFIG_PARAM_SESSIONRECOVERY} parameter. 
	 */
	private static final String CONFIG_DEF_SESSIONRECOVERY = "proactive";
	
	/**
	 * When sessionRecovery is set to passive, use this to set the interval
	 * after which a connection without refreshing will be terminated. 
	 * 
	 * Note that this can't be smaller number than 5 seconds.
	 */
	private static final String CONFIG_PARAM_SESSIONEXPIRATION = "general.sessionExpiration";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_SESSIONEXPIRATION CONFIG_PARAM_SESSIONEXPIRATION} parameter. 
	 */
	private static final int CONFIG_DEF_SESSIONEXPIRATION = 30;
	
	/**
	 * Session recovery policy code after translation from its string format in configuration file. This one means that 
	 * somebody entered wrong string.  
	 */
	private static final int SESSIONRECOVERYPOLICY_INT_ERROR = 0;
	
	/**
	 * Session recovery policy code after translation from its string format in configuration file. This one means that 
	 * somebody entered {@link #SESSIONRECOVERYPOLICY_STRING_PASSIVE SESSIONRECOVERYPOLICY_STRING_PASSIVE}.  
	 */
	private static final int SESSIONRECOVERYPOLICY_INT_PASSIVE = 1;
	
	/**
	 * Session recovery policy code after translation from its string format in a configuration file.This one means that 
	 * somebody entered {@link #SESSIONRECOVERYPOLICY_STRING_NONE SESSIONRECOVERY_POLICY_STRING_NONE}.
	 */
	private static final int SESSIONRECOVERYPOLICY_INT_NONE = 2;
	
	/**
	 * Session recovery policy code after translation from its string format in configuration file. This one means that 
	 * somebody entered {@link #SESSIONRECOVERYPOLICY_STRING_PROACTIVE SESSIONRECOVERY_POLICY_STRING_PROACTIVE}. 
	 */
	private static final int SESSIONRECOVERYPOLICY_INT_PROACTIVE = 3;
	
	/**
	 * Session recovery policy string, one of the valid values that are to be entered in the configuration file.
	 */
	private static final String SESSIONRECOVERYPOLICY_STRING_PASSIVE = "passive";
	
	/**
	 * Session recovery policy string, one of the valid values that are to be entered in the configuration file.
	 */
	private static final String SESSIONRECOVERYPOLICY_STRING_NONE = "none";
	
	/**
	 * Session recovery policy string, one of the valid values that are to be entered in the configuration file.
	 */
	private static final String SESSIONRECOVERYPOLICY_STRING_PROACTIVE = "proactive";
	
	/**
	 * How often will gateway check whether or not the connections are still up when proactive session recovery is 
	 * on (ms).
	 */
	private static final int SESSIONRECOVERY_CHECKINTERVAL_PROACTIVE = 30000;
	
	/**
	 * How often will gateway check whether or not the connections are still up when passive session recovery is 
	 * on (ms).
	 */
	private static final int SESSIONRECOVERY_CHECKINTERVAL_PASSIVE = 5000;
	
	/**
	 * Minimal number of seconds allowed in the configuration parameter {@link #CONFIG_PARAM_SESSIONEXPIRATION CONFIG_PARAM_SESSIONEXPIRATION}
	 */
	private static final int SESSIONEXPIRATION_MINIMAL_VALUE = 5;
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OID = "oid";
	
	/**
	 * Name of the Objects attribute. 
	 */
	private static final String ATTR_OBJECTS = "objects";
	
	/**
	 * Name of the ThingDescriptions attribute. 
	 */
	private static final String ATTR_TDS = "thingDescriptions";
	
	/**
	 * TODO
	 */
	private static final String CONFIG_PARAM_PAGE_SIZE = "general.pageSize";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_PAGE_SIZE CONFIG_PARAM_PAGE_SIZE} parameter. 
	 */
	private static final int CONFIG_DEF_PAGE_SIZE = 5;
	
	/* === FIELDS === */
	
	/**
	 * Hash map containing connections of local objects.
	 */
	private Map<String, ConnectionDescriptor> descriptorPool;
	
	/**
	 * Indicates the policy that the OGWAPI should take during session recovery.
	 */
	private int sessionRecoveryPolicy;
	
	/**
	 * Expiration time when passive session recovery policy is on.
	 */
	private int sessionExpiration;
	
	/**
	 * Configuration of the OGWAPI.
	 */
	private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * A connector used for connecting to NM Manager.
	 */
	private NeighbourhoodManagerConnector nmConnector;
	
	/**
	 * pageSize parameter 
	 */
	private int pageSize;
	
	/**
	 * MessageCounter
	 */
	private MessageCounter messageCounter;
	
	/* === PUBLIC METHODS === */
	
	
	/**
	 * Constructor, initialises necessary objects. All parameters are mandatory, failure to include them can lead 
	 * to a swift end of application.
	 * 
	 * @param config Configuration object.
	 * @param logger Java logger.
	 */
	public CommunicationManager(XMLConfiguration config, Logger logger){
		
		logger.config("OGWAPI version: " + OGWAPI_VERSION);
		
		this.descriptorPool = Collections.synchronizedMap(new HashMap<String, ConnectionDescriptor>());
		
		this.nmConnector = new NeighbourhoodManagerConnector(config, logger);		
		
		this.messageCounter = new MessageCounter(config, logger);
		
		// load the configuration for the pageSize param
		pageSize = config.getInt(CONFIG_PARAM_PAGE_SIZE, CONFIG_DEF_PAGE_SIZE);
		
		this.sessionExpiration = 0;
		
		this.config = config;
		this.logger = logger;
		
		// load the configuration for the session recovery policy
		String sessionRecoveryPolicyString = config.getString(CONFIG_PARAM_SESSIONRECOVERY, CONFIG_DEF_SESSIONRECOVERY);
		
		translateSessionRecoveryConf(sessionRecoveryPolicyString);
		
		if (sessionRecoveryPolicy == SESSIONRECOVERYPOLICY_INT_ERROR) {
			// wrong configuration parameter entered - set it to default
			logger.warning("Wrong parameter entered for " + CONFIG_PARAM_SESSIONRECOVERY + " in the configuration file: "  
					+ sessionRecoveryPolicyString + ". Setting to default: " + CONFIG_DEF_SESSIONRECOVERY);
			
			translateSessionRecoveryConf(CONFIG_DEF_SESSIONRECOVERY);
			
		} else {
			logger.config("The session recovery policy is set to " + sessionRecoveryPolicyString + ".");
		}
				
		// timer for session checking - N/A if the session recovery is set to none
		if (sessionRecoveryPolicy != SESSIONRECOVERYPOLICY_INT_NONE) {
			
			int checkInterval;
			
			switch(sessionRecoveryPolicy) {
			case SESSIONRECOVERYPOLICY_INT_PASSIVE:
				checkInterval = SESSIONRECOVERY_CHECKINTERVAL_PASSIVE;
				
				sessionExpiration = config.getInt(CONFIG_PARAM_SESSIONEXPIRATION, CONFIG_DEF_SESSIONEXPIRATION);
				
				// if somebody put there too small number, we will turn it to default
				if (sessionExpiration < SESSIONEXPIRATION_MINIMAL_VALUE) {
					logger.warning("Wrong parameter entered for " + CONFIG_PARAM_SESSIONEXPIRATION + " in the configuration file: "  
							+ sessionRecoveryPolicyString + ". Setting to default: " + CONFIG_DEF_SESSIONEXPIRATION);
					sessionExpiration = CONFIG_DEF_SESSIONEXPIRATION;
				}
				
				sessionExpiration = sessionExpiration * 1000;
				
				logger.config("Session expiration is set to " + sessionExpiration + "ms");
				break;
				
			case SESSIONRECOVERYPOLICY_INT_PROACTIVE:
				checkInterval = SESSIONRECOVERY_CHECKINTERVAL_PROACTIVE;
				break;
				
				default:
					// if something goes wrong, don't let the timer stand in our way
					checkInterval = Integer.MAX_VALUE;
			}
			
			
			Timer timerForSessionRecovery = new Timer();
			timerForSessionRecovery.schedule(new TimerTask() {
				@Override
				public void run() {
					recoverSessions();
				}
			}, checkInterval, checkInterval);
		}
	}
	
	
	
	// ADMINISTRATION METHODS - not directly related to interfaces
	
	/**
	 * Retrieves the object IDs that has open connections to network via this CommunicationManager. 
	 * Based on these strings, the respective connections can be retrieved from the connection descriptor pool.
	 * 
	 * @return Set of object IDs. 
	 */
	public Set<String> getConnectionList(){
		
		return descriptorPool.keySet();
	}
	
	
	/**
	 * Checks whether the connection {@link ConnectionDescriptor descriptor} instance exists for given object ID and 
	 * whether or not it is connected to the network. Returns true or false accordingly.  
	 * 
	 * @param objectId Object ID in question. 
	 * @return True if descriptor exists and the connection is established.
	 */
	public boolean isConnected(String objectId){
		
		if (sessionRecoveryPolicy == SESSIONRECOVERYPOLICY_INT_ERROR) {
			return false;
		}
		
		if (objectId == null) {
			logger.warning("CommunicationManager.isConnected: Invalid object ID.");
			return false;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectId);
		
		if (descriptor != null){
			return descriptor.isConnected();
		} else {
			logger.info("Object ID: '" + objectId + "' is not connected yet.");
			
			return false;
		}
	}
	
	
	/**
	 * Verifies the credentials of an object, when (for example) trying to reach its connection 
	 * {@link ConnectionDescriptor descriptor} instance via RESTLET API Authenticator. This method should be called after 
	 * {@link #isConnected(String) isConnected} is used for making sure, that the object is actually connected. 
	 * It is safe to use this method when processing authentication of every request, even in quick succession.
	 * 
	 * @param objectId Object ID in question.
	 * @param password The password that is to be verified. 
	 * @return True, if the password is valid.
	 */
	public boolean verifyPassword(String objectId, String password){
		ConnectionDescriptor descriptor = descriptorPoolGet(objectId);
		
		if (descriptor != null){
			return descriptor.verifyPassword(password);
		} else {
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectId + "'.");
			
			return false;
		}
	}
	
	
	/**
	 * Closes all open connections to network. It will also clear these connection handlers off the connection 
	 * descriptor pool table, (they have to be reconfigured to be opened again, thus this should be done 
	 * only when the application is about to be closed).
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
		logger.fine("Connection descriptor pool flushed.");
	}
	
	
	
	// AUTHENTICATION INTERFACE
	
	
	/**
	 * Establishes a single connection for given object with preferences from configuration file and provided credentials. 
	 * The connection descriptor is then stored in the internal descriptor pool.
	 * 
	 * If the connection descriptor for given object already exists, it get terminated, discarded and then recreated.
	 * 
	 * 
	 * @param objectId Object ID.
	 * @param password Password.
	 * @return StatusMessage, with the error flag set as false, if the login was successful. If not, the error flag is
	 * set to true.
	 */
	public StatusMessage establishConnection(String objectId, String password){
		
		ConnectionDescriptor descriptor;
		boolean verifiedOrConnected;
		StatusMessage statusMessage;
		
		if (sessionRecoveryPolicy == SESSIONRECOVERYPOLICY_INT_PASSIVE) {
			descriptor = descriptorPoolGet(objectId);
			
			if (descriptor != null) {
				if (descriptor.isConnected()) {
					
					if (descriptor.verifyPassword(password)) {
						descriptor.resetConnectionTimer();
						verifiedOrConnected = true;
					} else {
						verifiedOrConnected = false;
					}
					
				} else {
					verifiedOrConnected = descriptor.connect();
				}
			} else {
				verifiedOrConnected = false;
			}
			
			
		} else {
			// if there is a previous descriptor we should close the connection first, before reopening it again
			descriptor = descriptorPoolRemove(objectId);
			if (descriptor != null){
		
				descriptor.disconnect();
				
				logger.info("Reconnecting '" + objectId + "' to network.");
			}
			
			descriptor = new ConnectionDescriptor(objectId, password, config, logger, this, messageCounter);
			
			verifiedOrConnected = descriptor.connect();
		}
		
		if (verifiedOrConnected){
			logger.info("Connection for '" + objectId +"' was established.");
			
			// insert the connection descriptor into the pool
			descriptorPoolPut(objectId, descriptor);
			
			statusMessage = new StatusMessage(false, CodesAndReasons.CODE_200_OK, 
					CodesAndReasons.REASON_200_OK + "Login successfull.", StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
		} else {
			logger.info("Connection for '" + objectId +"' was not established.");
			statusMessage = new StatusMessage(true, CodesAndReasons.CODE_401_UNAUTHORIZED, 
					CodesAndReasons.REASON_401_UNAUTHORIZED + "Login unsuccessfull.", 
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
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
	 * 
	 * @param objectId User name used to establish the connection.
	 * @param destroyConnectionDescriptor Whether the connection descriptor should also be destroyed or not. 
	 */
	public void terminateConnection(String objectId, boolean destroyConnectionDescriptor){
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectId); 
		
		if (descriptor != null){
			descriptor.disconnect();
		} else {
			logger.info("Attempting to terminate nonexisting connection. Object ID: '" + objectId + "'.");
		}
		
		if (destroyConnectionDescriptor){
			descriptorPoolRemove(objectId);
			logger.info("Connection for object ID '" + objectId + "' destroyed.");
		} else {
			// this will keep the connection in the pool
			logger.info("Connection for object ID '" + objectId + "' closed.");
		}

	}
	
	
	
	
	// CONSUMPTION INTERFACE

	/**
	 * Retrieves a events of a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that owns the events. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getEventsOfRemoteObject(String sourceOid, String destinationOid, 
			String body, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when getting events of remote object. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when getting events of remote object. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.getEventsOfRemoteObject(destinationOid, parameters, body);
		
	}
	
	/**
	 * Retrieves a actions of a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that owns the actions. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getActionsOfRemoteObject(String sourceOid, String destinationOid, 
			String body, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when getting actions of remote object. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when getting actions of remote object. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.getActionsOfRemoteObject(destinationOid, parameters, body);
		
	}
	
	/**
	 * Retrieves a thing description of a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that owns the thing description. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getThingDescriptionOfRemoteObject(String sourceOid, String destinationOid, 
			String body, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when getting thing description of remote object. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when getting thing description of remote object. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.getThingDescriptionOfRemoteObject(destinationOid, parameters, body);
		
	}
	
	/**
	 * Retrieves a properties of a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that owns the properties. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getPropertiesOfRemoteObject(String sourceOid, String destinationOid, 
			String body, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when getting properties of remote object. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when getting properties of remote object. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.getPropertiesOfRemoteObject(destinationOid, parameters, body);
		
	}
	
	/**
	 * Retrieves a property of a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that owns the property. 
	 * @param propertyId ID of the property.
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getPropertyOfRemoteObject(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when getting property of remote object. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when getting property of remote object. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		if (propertyId == null){
			logger.warning("Error when getting property of remote object. The property ID is null. "
					+ "Source object: '" + sourceOid + "', destination object: '" + destinationOid);
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.getPropertyOfRemoteObject(destinationOid, propertyId, parameters, body);
		
	}
	
	
	/**
	 * Sets a new value of a property on a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that owns the property. 
	 * @param propertyId ID of the property.
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (a new value will probably be stored here).
	 * @return Status message. 
	 */
	public StatusMessage setPropertyOfRemoteObject(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when setting property of remote object. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when setting property of remote object. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		if (propertyId == null){
			logger.warning("Error when setting property of remote object. The property ID is null. "
					+ "Source object: '" + sourceOid + "', destination object: '" + destinationOid);
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.setPropertyOfRemoteObject(destinationOid, propertyId, body, parameters);
	}
	
	
	/**
	 * Starts an action on a remote object. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the remote object.
	 * @param actionId ID of the action.
	 * @param body Body that will be transported to the object via its {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 * @param parameters Parameters that will be transported along the body. 
	 * @return Status message.
	 */
	public StatusMessage startAction(String sourceOid, String destinationOid, String actionId, String body, 
			Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when starting action. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when starting action. Destination object ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		if (actionId == null){
			logger.warning("Error when starting action of remote object. The action ID is null. "
					+ "Source object: '" + sourceOid + "', destination object: '" + destinationOid);
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.startAction(destinationOid, actionId, body, parameters);
		
	}
	
	
	/**
	 * Updates a status and a return value of locally run job of an action. See the {@link eu.bavenir.ogwapi.commons.Task Task}
	 * and {@link eu.bavenir.ogwapi.commons.Action Action} for more information about valid states. The source object must 
	 * be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param actionId ID of the action that is being worked on right now (this is NOT a task ID).
	 * @param newStatus New status of the job. 
	 * @param returnValue New value to be returned to the object that ordered the job.
	 * @param parameters Parameters that goes with the return value.
	 * @return Status message.
	 */
	public StatusMessage updateTaskStatus(String sourceOid, String actionId, 
					String newStatus, String returnValue, Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when updating task status. Source object ID is null.");
			
			return null;
		}
		
		if (actionId == null){
			logger.warning("Error when updating task status. The action ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		if (newStatus == null){
			logger.warning("Error when updating task status. The new status is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null) {
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		}
		
		return descriptor.updateTaskStatus(actionId, newStatus, returnValue, parameters);
	}
	
	
	/**
	 * Retrieves a status of a remotely running {@link eu.bavenir.ogwapi.commons.Task Task}. The source object must 
	 * be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid The remote object running the task. 
	 * @param actionId ID of the action.
	 * @param taskId ID of the task.
	 * @param parameters Any parameters that are necessary to be transported to other side (usually none).
	 * @param body Any body that needs to be transported to the other side (usually none).
	 * @return Status message.
	 */
	public StatusMessage retrieveTaskStatus(String sourceOid, String destinationOid, String actionId, String taskId, 
			Map<String, String> parameters, String body) {
		
		if (sourceOid == null){
			logger.warning("Error when retrieving task status. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when retrieving task status. Destination object ID is null.");
			
			return null;
		}
		
		if (actionId == null){
			logger.warning("Error when retrieving task status. The action ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		if (taskId == null){
			logger.warning("Error when retrieving task status. The task ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.retrieveTaskStatus(destinationOid, actionId, taskId, parameters, body);
	}
	
	
	/**
	 * Cancels remotely running task. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object. 
	 * @param destinationOid The remote object running the task. 
	 * @param actionId ID of the action.
	 * @param taskId ID of the task.
	 * @param parameters Any parameters that are necessary to be transported to other side (usually none).
	 * @param body Any body that needs to be transported to the other side (usually none).
	 * @return Status message.
	 */
	public StatusMessage cancelRunningTask(String sourceOid, String destinationOid, String actionId, String taskId, 
			Map<String, String> parameters, String body) {
		
		if (sourceOid == null){
			logger.warning("Error when canceling task. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when canceling task. Destination object ID is null.");
			
			return null;
		}
		
		if (actionId == null){
			logger.warning("Error when canceling task. The action ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		if (taskId == null){
			logger.warning("Error when canceling task. The task ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.cancelRunningTask(destinationOid, actionId, taskId, parameters, body);
	}
	
	
	
	
	// DISCOVERY INTERFACE
	
	
	/**
	 * Retrieves a collection of roster entries for object ID (i.e. its contact list). If there is no connection 
	 * established for the given object ID, returns empty {@link java.util.Set Set}. 
	 * 
	 * 
	 * @param objectId Object ID the roster is to be retrieved for. 
	 * @return Set of roster entries. If no connection is established for the object ID, the collection is empty
	 * (not null). 
	 */
	public Set<String> getRosterEntriesForObject(String objectId){
		
		if (objectId == null){
			logger.warning("Error when retrieving contact list. Object ID is null.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectId);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectId + "'.");
			return Collections.emptySet();
		}
		
		Set<String> entries = descriptor.getRoster();
		
		if (entries == null) {
			return null;
		}
		
		// log it
		logger.fine("-- Roster for '" + objectId +"' --");
		for (String entry : entries) {
			logger.fine(entry + " Presence: " + "UNKNOWN");
		}
		logger.fine("-- End of roster --");
		
		return entries;
	}
	
	/**
	 * TODO
	 */
	public Set<String> getRosterEntriesForObject(String objectId, int pageNumber){
		
		if (objectId == null){
			logger.warning("Error when retrieving contact list. Object ID is null.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(objectId);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + objectId + "'.");
			return Collections.emptySet();
		}
		
		Set<String> entries = descriptor.getRoster();
		
		if (entries == null || entries.size() == 0) {
			return null;
		}
		
		Set<String> set = new HashSet<String>();
		
		//range 
		int min = pageNumber * pageSize;
		int max = min + pageSize;
			
		if (min+1 <= entries.size()) {
			for (int i = min; i < max && i < entries.size(); i++) {
				set.add(entries.toArray()[i].toString());
			}
		} else {
			logger.warning("There are no avaliable object for objectId: " + objectId + " and page number: " + pageNumber);
			
			return null;
		}
		
		// log it
		logger.fine("-- Roster for '" + objectId +"' -- page number: " + pageNumber);
		for (String entry : set) {
			logger.fine(entry + " Presence: " + "UNKNOWN");
		}
		logger.fine("-- End of roster --");
		
		return set;
	}
	
	
	
	
	// EXPOSING INTERFACE
	
	
	/**
	 * Activates the event channel identified by the event ID. From the moment of activation, other devices in the 
	 * network will be able to subscribe to it and will receive events in case they are generated. 
	 * 
	 * If the event channel was never activated before (or there is other reason why it was not saved previously), it 
	 * gets created anew. In that case, the list of subscribers is empty.  
	 * 
	 * 
	 * @param sourceOid Object ID of the event channel owner.
	 * @param eventId Event ID.
	 * @return {@link StatusMessage StatusMessage} with error flag set to false, if the event channel was activated
	 * successfully.
	 */
	public StatusMessage activateEventChannel(String sourceOid, String eventId, Map<String, String> parameters, 
				String body) {
		
		if (sourceOid == null){
			logger.warning("Error when activating event channel. Source object ID is null.");
			
			return null;
		}
		
		if (eventId == null){
			logger.warning("Error when activating event channel. The event ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			return null;
		}
		
		return descriptor.setLocalEventChannelStatus(eventId, true, parameters, body);
	}
	
	
	/**
	 * Distributes an {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent event} to subscribers. 
	 * The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param eventId Event ID.
	 * @param body Body of the event.
	 * @param parameters Any parameters to be transported with the event body.
	 * @return Status message.
	 */
	public StatusMessage sendEventToSubscribedObjects(String sourceOid, String eventId, String body, 
			Map<String, String> parameters) {
		
		if (sourceOid == null){
			logger.warning("Error when sending event to subscribers. Source object ID is null.");
			
			return null;
		}
		
		if (eventId == null){
			logger.warning("Error when sending event to subscribers. The event ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}

		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			return null;
		}
		
		return descriptor.sendEventToSubscribers(eventId, body, parameters);
		
	}
	
	
	
	/**
	 * De-activates the event channel identified by the event ID. From the moment of de-activation, other devices in the
	 * network will not be able to subscribe to it. Also no events are sent in case they are generated. 
	 * 
	 * The channel will still exist though along with the list of subscribers. If it gets re-activated, the list of
	 * subscribers will be the same as in the moment of de-activation.  
	 * 
	 * The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object. 
	 * @param objectId Object ID of the event channel owner
	 * @param eventID Event ID.
	 * @return {@link StatusMessage StatusMessage} with error flag set to false, if the event channel was activated
	 * successfully.
	 */
	public StatusMessage deactivateEventChannel(String sourceOid, String eventId, Map<String, String> parameters, 
				String body) {
		
		if (sourceOid == null){
			logger.warning("Error when deactivating event channel. Source object ID is null.");
			
			return null;
		}
		
		if (eventId == null){
			logger.warning("Error when deactivating event channel. The event ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			return null;
		}
		
		return descriptor.setLocalEventChannelStatus(eventId, false, parameters, body);
	}
	
	
	/**
	 * Retrieves the status of local or remote event channel. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object that is to be polled (it can be the local owner verifying the state of its channel.
	 * @param eventId ID of the event.
	 * @param parameters Any parameters to be sent (usually none).
	 * @param body Any body to be sent (usually none).
	 * @return Status message.
	 */
	public StatusMessage getEventChannelStatus(String sourceOid, String destinationOid, String eventId, 
			Map<String, String> parameters, String body) {
		
		if (sourceOid == null){
			logger.warning("Error when retrieving event channel status. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when retrieving event channel status. Destination object ID is null.");
			
			return null;
		}
		
		if (eventId == null){
			logger.warning("Error when retrieving event channel status. The event ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.getEventChannelStatus(destinationOid, eventId, parameters, body);
	}
	
	
	/**
	 * Subscribes the current object to the destination object ID's {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 * The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object. 
	 * @param destinationOid ID of the object to which event channel a subscription is attempted.
	 * @param eventId Event ID.
	 * @param parameters Any parameters to be sent (usually none).
	 * @param body Any body to be sent (usually none).
	 * @return Status message.
	 */
	public StatusMessage subscribeToEventChannel(String sourceOid, String destinationOid, String eventId, 
			Map<String, String> parameters, String body) {
		
		if (sourceOid == null){
			logger.warning("Error when subscribing to an event channel. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when subscribing to an event channel. Destination object ID is null.");
			
			return null;
		}
		
		if (eventId == null){
			logger.warning("Error when subscribing to an event channel. The event ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.subscribeToEventChannel(destinationOid, eventId, parameters, body);
	}
	
	
	/**
	 * Un-subscribes the current object from the destination object ID's {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 * The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object.
	 * @param destinationOid ID of the object from which event channel a un-subscription is attempted.
	 * @param eventId Event ID.
	 * @param parameters Any parameters to be sent (usually none).
	 * @param body Any body to be sent (usually none).
	 * @return Status message.
	 */
	public StatusMessage unsubscribeFromEventChannel(String sourceOid, String destinationOid, String eventId, 
			Map<String, String> parameters, String body) {
		
		if (sourceOid == null){
			logger.warning("Error when unsubscribing from an event channel. Source object ID is null.");
			
			return null;
		}
		
		if (destinationOid == null){
			logger.warning("Error when unsubscribing from an event channel. Destination object ID is null.");
			
			return null;
		}
		
		if (eventId == null){
			logger.warning("Error when unsubscribing from an event channel. The event ID is null. "
					+ "Source object: '" + sourceOid + "'.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceOid);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceOid + "'.");
			
			return null;
		} 
		
		return descriptor.unsubscribeFromEventChannel(destinationOid, eventId, parameters, body);
		
	}

	
	
	// REGISTRY INTERFACE
	
	/**
	 * Retrieves the list of IoT objects registered under given Agent from the Neighbourhood Manager. 
	 * 
	 * @param agid The ID of the Agent in question.
	 * @return All VICINITY identifiers of objects registered under specified agent.
	 */
	public Representation getAgentObjects(String agid) {
		
		if (agid == null) {
			logger.warning("Error when retrieving objects registered under local agent. Agent ID is null.");
			
			return null;
		}
		
		return nmConnector.getAgentObjects(agid);
	}
	
	
	/**
	 * Register the IoT object(s) of the underlying eco system e.g. devices, VA service.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be registered 
	 * (from request).
	 * @return All VICINITY identifiers of objects registered the Agent by this call.
	 */
	public Representation storeObjects(Representation json) {
		
		if (json == null) {
			
			logger.warning("Error when registering objects under local agent. JSON with TDs is null.");
			
			return null;
		}
		
		return nmConnector.storeObjects(json);
		
	}
	
	
	/**
	 * Update the thing descriptions of objects registered under the Agent. This will delete the old records and 
	 * replace them with new ones.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be updated 
	 * (from request).
	 * @return The list of approved devices to be registered in agent configuration. Approved devices means only 
	 * devices, that passed the validation in semantic repository and their instances were created. 
	 */
	public Representation heavyweightUpdate(Representation json) {
		if (json == null) {
			
			logger.warning("Error when updating objects under local agent. JSON with TDs is null.");
			
			return null;
		}
		
		return nmConnector.heavyweightUpdate(json);
	}
	
	
	/**
	 * Update the thing descriptions of objects registered under the Agent. This will only change the required fields.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be updated 
	 * (from request).
	 * @return The list of approved devices to be registered in agent configuration. Approved devices means only 
	 * devices, that passed the validation in semantic repository and their instances were created. 
	 */
	public Representation lightweightUpdate(Representation json){
		if (json == null) {
			
			logger.warning("Error when updating objects under local agent. JSON with TDs is null.");
			
			return null;
		}
		
		return nmConnector.lightweightUpdate(json);
	}
	
	
	/**
	 * Deletes - unregisters the IoT object(s).
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be removed 
	 * (taken from request).
	 * @return Notification of success or failure.
	 */
	public Representation deleteObjects(Representation json){
		if (json == null) {
			
			logger.warning("Error when deleting objects under local agent. JSON with TDs is null.");
			
			return null;
		}
		
		return nmConnector.deleteObjects(json);
	}
	
	
	/**
	 * getThingDescriptions - Return one page of the thing descriptions of IoT object(s).
	 * 
	 * @param page number (0 <-> countOfObjects/5 + 1)
	 * @param sourceObjectId 
	 * 
	 * @return JsonObject with the list of thing descriptions 
	 */
	public Representation getThingDescriptions(String sourceObjectId, int pageNumber){
		
		if (sourceObjectId == null || sourceObjectId.isEmpty()) {
			logger.warning("Method parameter sourceObjectId can't be null nor empty.");
			
			return null;
		}
		
		if (pageNumber < 0) {
			logger.warning("Method parameter pageNumber can't be smaller than 0;");
			logger.warning("Set pageNumber to 0;");
			
			pageNumber = 0;
		}
		
		Set<String> rosterObjects = getRosterEntriesForObject(sourceObjectId, pageNumber);
		
		if (rosterObjects == null) {
			return null;
		}
		
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
		
		rosterObjects.forEach(item -> {
			mainArrayBuilder.add(
					Json.createObjectBuilder().add(ATTR_OID, item)
				);
		});
		
		mainObjectBuilder.add(ATTR_OBJECTS, mainArrayBuilder);
		JsonObject json = mainObjectBuilder.build();
		
		return nmConnector.getThingDescriptions(new JsonRepresentation(json.toString()));
	}
	
	/**
	 * getThingDescriptions - Return all pages of the thing descriptions of IoT object(s).
	 * 
	 * @param sourceObjectId 
	 * 
	 * @return The list of thing descriptions 
	 */
	public Representation getThingDescriptions(String sourceObjectId) {
		
		if (sourceObjectId == null || sourceObjectId.isEmpty()) {
			logger.warning("Method parameter sourceObjectId can't be null nor empty.");
			
			return null;
		}
		
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
		
		Representation r = null;
		int i = 0;
		
		do {
			
			r = getThingDescriptions(sourceObjectId, i++);
			
			if (r == null) {
				break;
			}
			
			JsonArray tds = parseThingDescriptionsFromRepresentation(r);
			tds.forEach(item -> {
				mainArrayBuilder.add(item);
			});
		}
		while (true);
		
		mainObjectBuilder.add(ATTR_TDS, mainArrayBuilder);
		JsonObject json = mainObjectBuilder.build();
		
		return new JsonRepresentation(json.toString());
	}
	
	public JsonArray parseThingDescriptionsFromRepresentation(Representation tds) {
		
		JsonObject json = null;
		
		try {
			json = readJsonObject(tds.getText());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (json == null) {
			logger.warning("Can't parse representation.");
			return null;
		}
		
		JsonArray thingDescriptions = null;
		
		JsonObject message = json.getJsonObject("message");
		if (message == null) {
			
			thingDescriptions = json.getJsonArray("thingDescriptions");
		} else {
			thingDescriptions = message.getJsonArray("thingDescriptions");
		}
		
		if (thingDescriptions == null) {
			logger.warning("Can't parse representation.");
			return null;
		}
		
		return thingDescriptions;
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
	
	// QUERY INTERFACE
	
	/**
	 * Performs a SPARQL search on all objects in the contact list. The source object must be logged in first. 
	 * 
	 * @param sourceOid ID of the source object. 
	 * @param query SPARQL query.
	 * @param parameters Any parameters (if needed).
	 * @return JSON with results. 
	 */
	public String performSparqlSearch(String sourceObjectId, String sparqlQuery, Map<String, String> parameters) {
		
		if (sourceObjectId == null || sourceObjectId.isEmpty() || sparqlQuery == null || sparqlQuery.isEmpty()) {
			logger.warning("Method parameters can't be null nor empty.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceObjectId);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceObjectId + "'.");
			
			return null;
		} 
		
		return descriptor.performSparqlQuery(sparqlQuery, parameters);
	}
	
	/**
	 * Performs a Semantic search 
	 * 
	 * @param sourceOid ID of the source object. 
	 * @param query Semantic query.
	 * @param parameters Any parameters (if needed).
	 * @return JSON with results. 
	 */
	public String performSemanticSearch(String sourceObjectId, String semanticQuery, Map<String, String> parameters) {
		
		if (sourceObjectId == null || sourceObjectId.isEmpty() || semanticQuery == null || semanticQuery.isEmpty()) {
			logger.warning("Method parameters can't be null nor empty.");
			
			return null;
		}
		
		ConnectionDescriptor descriptor = descriptorPoolGet(sourceObjectId);
		
		if (descriptor == null){
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + sourceObjectId + "'.");
			
			return null;
		}
		
		Representation r = getThingDescriptions(sourceObjectId);
		JsonArray tds = parseThingDescriptionsFromRepresentation(r);
		
		return descriptor.performSemanticQuery(sourceObjectId, semanticQuery, parameters, tds);
	}
	
	
	/* === METHODS AVAILABLE ONLY TO CLASSES FROM THIS PACKAGE === */
	
	/**
	 * This methods directly inserts a message into the respective {@link ConnectionDescriptor}'s incoming queue, 
	 * bypassing the communication server when the destination OID is connected through this CommunicationManager.
	 * This saves overall resources of the whole communication . 
	 *  
	 * @param sourceObjectId The message source.
	 * @param destinationObjectId Message destination.
	 * @param message Message to be sent. 
	 * @return True if it was possible to send the message this way. False if the destination is not connected through
	 * 	this CommunicationManager.
	 */
	boolean tryToSendLocalMessage(String sourceObjectId, String destinationObjectId, String message) {
		
		// is the object connected through this CommunicationManager?
		if (!descriptorPool.containsKey(destinationObjectId)) {	
			
			logger.fine("Can't send the message locally, the destination OID is not from this infrastructure.");
			return false;
		}
			
		// check the validity of the calling object
		ConnectionDescriptor descriptor = descriptorPoolGet(destinationObjectId);
		
		if (descriptor == null){
			
			logger.warning("Null record in the connection descriptor pool. Object ID: '" + destinationObjectId + "'.");
			return false;
		}
		
		
		descriptor.processIncommingMessage(sourceObjectId, message);
		
		return true;
	}
	
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Thread-safe method for inserting an object ID (K) and a descriptor (V) into the descriptor pool. This is a
	 * synchronised equivalent for {@link java.util.HashMap#put(Object, Object) put()} method of HashMap table.
	 * 
	 *    IMPORTANT: It is imperative to use only this method to interact with the descriptor pool when adding
	 *    or modifying functionality of this class and avoid the original HashMap's
	 *    {@link java.util.HashMap#put(Object, Object) put()} method. 
	 *    
	 * @param objectId The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @param descriptor The value part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The previous value associated with key, or null if there was no mapping for key. 
	 * (A null return can also indicate that the map previously associated null with key, if the implementation 
	 * supports null values.)
	 */
	private ConnectionDescriptor descriptorPoolPut(String objectId, ConnectionDescriptor descriptor){
		synchronized (descriptorPool){
			return descriptorPool.put(objectId, descriptor);
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
	 * @param objectId The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The value to which the specified key is mapped, or null if this map contains no mapping for the key.
	 */
	private ConnectionDescriptor descriptorPoolGet(String objectId){
		synchronized (descriptorPool){
			
			return descriptorPool.get(objectId);
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
	 * @param objectId The key part of the {@link java.util.HashMap HashMap} key-value pair in the descriptor pool.
	 * @return The previous value associated with key, or null if there was no mapping for key.
	 */
	private ConnectionDescriptor descriptorPoolRemove(String objectId){
		synchronized (descriptorPool){
			
			return descriptorPool.remove(objectId);
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
	
	
	/**
	 * Translates the string value from configuration file into a valid code for the recovery policy. The recovery
	 * policy is checked quite often, therefore it is a good idea to make it numerical value.
	 * 
	 * @param recoveryConfigString The string from configuration file.
	 */
	private void translateSessionRecoveryConf(String recoveryConfigString) {
		
		switch (recoveryConfigString) {
		case SESSIONRECOVERYPOLICY_STRING_PASSIVE:
			sessionRecoveryPolicy = SESSIONRECOVERYPOLICY_INT_PASSIVE;
			break;
			
		case SESSIONRECOVERYPOLICY_STRING_NONE:
			sessionRecoveryPolicy = SESSIONRECOVERYPOLICY_INT_NONE;
			break;
			
		case SESSIONRECOVERYPOLICY_STRING_PROACTIVE:
			sessionRecoveryPolicy = SESSIONRECOVERYPOLICY_INT_PROACTIVE;
			break;
			
			default:
				sessionRecoveryPolicy = SESSIONRECOVERYPOLICY_INT_ERROR;
				break;
				
		}
	}
	
	
	/**
	 * Periodically called to recover sessions according to the configuration file. 
	 */
	private void recoverSessions() {
		
		Set<String> connectionList = getConnectionList();
		ConnectionDescriptor descriptor;
		
		// remember we have to use our own methods (descriptorPoolGet etc) to access the hash map in a thread safe manner
		for (String oid : connectionList) {
			descriptor = descriptorPoolGet(oid);
			
			if (descriptor != null) {
				if (sessionRecoveryPolicy == SESSIONRECOVERYPOLICY_INT_PROACTIVE) {
					if (!descriptor.isConnected()) {
						logger.warning("Connection for " + descriptor.getObjectId() + " was interrupted. Reconnecting.");
						
						descriptor.connect();
					}
				}
				
				if (sessionRecoveryPolicy == SESSIONRECOVERYPOLICY_INT_PASSIVE) {
					
					// if the descriptor is connected but the connection timer is expired, disconnect
					if(descriptor.isConnected() 
							&& (System.currentTimeMillis() - descriptor.getLastConnectionTimerReset()) > sessionExpiration) {
						
						logger.warning("Session expired for object ID " + descriptor.getObjectId() + ". Disconnecting.");
						descriptor.disconnect();
						
					}
				}	
			}
		}
		
		
	}
}
