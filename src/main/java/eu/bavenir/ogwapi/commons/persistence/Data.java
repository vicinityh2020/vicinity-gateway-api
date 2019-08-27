package eu.bavenir.ogwapi.commons.persistence;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.configuration2.XMLConfiguration;
import org.json.JSONException;

import eu.bavenir.ogwapi.commons.Action;
import eu.bavenir.ogwapi.commons.EventChannel;
import eu.bavenir.ogwapi.commons.Subscription;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 * - getters
 */

/**
 * Class for storing data, which need to be persistence.
 * Instance of this class exist for each object which is logged in OGWAPI.
 * 
 * Persistence manager {@link u.bavenir.ogwapi.commons.PersistenceManager PersistenceManager} 
 * is used for loading data from file, or in TD json file case first from server.
 * 
 * Loading is called in constructor and if this persistence data exist, create itself from loaded data.
 *  
 * @author Andrej
 *
 */
public class Data implements Serializable {


	
	/* === CONSTANTS === */

	/**
	 * A unique serial version identifier
	 * @see Serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/* === FIELDS === */
	
	/**
	 * A set of event channels served by this object.
	 * 
	 * @Serialize
	 */
	private Set<EventChannel> providedEventChannels;
	
	/**
	 * A set of channels that this object is subscribed to.
	 * 
	 * @Serialize
	 */
	private Set<Subscription> subscribedEventChannels;
	
	/**
	 * A set of actions served by this object.
	 * 
	 */
	private transient Set<Action> providedActions;

	/**
	 * ID of the object connected through this ConnectionDescriptor.
	 * 
	 * @Serialize
	 */
	private String objectId;
	
	/**
	 * json representation of thing description 
	 */
	private transient JsonObject thingDescription; 
	
	/**
	 * Persistence manager
	 */
	private transient PersistenceManager persistenceManager;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private transient Logger logger;
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor
	 */
	public Data(String objectId, XMLConfiguration config, Logger logger) {
		
		this.objectId = objectId;
		
		this.logger = logger;
		
		// create persistence manager
		persistenceManager = new PersistenceManager(config, logger);
		
		// try to load data from file
		Data loadedData = loadData();
		if (loadedData != null) {
			
			if (loadedData.getProvidedEventChannels() != null) {
				this.providedEventChannels = loadedData.getProvidedEventChannels();
			} else {
				providedEventChannels = new HashSet<EventChannel>();
			}

			if (loadedData.getSubscribedEventChannels() != null) {
				this.subscribedEventChannels = loadedData.getSubscribedEventChannels();
			} else {
				subscribedEventChannels = new HashSet<Subscription>();
			}

			providedActions = new HashSet<Action>();
			
		} else {
			
			providedEventChannels = new HashSet<EventChannel>();
			subscribedEventChannels = new HashSet<Subscription>();
			providedActions = new HashSet<Action>();
		}	
		
		// load TD JSON
		this.thingDescription = persistenceManager.loadThingDescription(objectId);
	}

	/**
	 * Add event channel to providedEventChannels set and save data
	 * 
	 * @param eventChannel 
	 */
	public void addProvidedEventChannel(EventChannel eventChannel) {
		providedEventChannels.add(eventChannel);
		saveData();
	}

	/**
	 * Add subscription to subscribedEventChannels set and save data
	 * 
	 * @param subscription 
	 */
	public void addSubscribedEventChannel(Subscription subscription) {
		subscribedEventChannels.add(subscription);
		saveData();
	}

	/**
	 * Add action to providedActions set and save data
	 * 
	 * @param action 
	 */
	public void addProvidedAction(Action action) {
		providedActions.add(action);
	}

	/**
	 * Remove subscription from subscribedEventChannels set and save data
	 * 
	 * @param subscription 
	 */
	public void removeSubscribedEventChannel(Subscription subscription) {
		subscribedEventChannels.remove(subscription);
		saveData();
	}

	/**
	 * Save data through persistence manager to file 
	 */
	public void saveData() {
		persistenceManager.saveData(objectId, this);
	}
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Load data through persistence manager from file
	 * 
	 * @return this class {@link u.bavenir.ogwapi.commons.Data Data}
	 */
	private Data loadData() {
		return (Data) persistenceManager.loadData(objectId);
	}
	
	
	/* === GETTERS === */

	/**
	 * Get object id
	 * 
	 * @return String objectId 
	 */
	public String getObjectId() {
		return objectId;
	}
	
	/**
	 * Get provided event channels
	 * 
	 * @return Set<EventChannel> providedEventChannels 
	 */
	public Set<EventChannel> getProvidedEventChannels() {
		return providedEventChannels;
	}

	/**
	 * Get provided actions
	 * 
	 * @return Set<Action> providedActions 
	 */
	public Set<Action> getProvidedActions() {
		return providedActions;
	}

	/**
	 * Get subscribed event channels
	 * 
	 * @return Set<Subscription> subscribedEventChannels 
	 */
	public Set<Subscription> getSubscribedEventChannels() {
		return subscribedEventChannels;
	}

	/**
	 * Get events
	 * 
	 * @return all events from TD file in JsonObject format 
	 */
	public JsonObject getEvents() {
		
		JsonObject events = null;
		
		if (thingDescription != null) {
			try {
				
				JsonArray eventsArr = thingDescription.getJsonArray("events");
				events = Json.createObjectBuilder().add("events", eventsArr).build();
			
			} catch (JSONException e) {
				
				logger.info("There are no events in TD for object: " + objectId);
			}
		} 
		return events;
	}
	
	/**
	 * Get actions
	 * 
	 * @return all actions from TD file in JsonObject format 
	 */
	public JsonObject getActions() {
		
		JsonObject actions = null;
		
		if (thingDescription != null) {
			try {
				
				JsonArray actionsArr = thingDescription.getJsonArray("actions");
				actions = Json.createObjectBuilder().add("actions", actionsArr).build();
			
			} catch (JSONException e) {
				
				logger.info("There are no actions in TD for object: " + objectId);
			}
		} 
		return actions;
	}
	
	/**
	 * Get properties
	 * 
	 * @return all properties from TD file in JsonObject format 
	 */
	public JsonObject getProperties() {
		
		JsonObject properties = null;
		
		if (thingDescription != null) {
			try {
				
				JsonArray propertiesArr = thingDescription.getJsonArray("properties");
				properties = Json.createObjectBuilder().add("properties", propertiesArr).build();
			
			} catch (JSONException e) {
				
				logger.info("There are no properties in TD for object: " + objectId);
			}
		} 
		return properties;
	}
	
	/**
	 * Get thing description
	 * 
	 * @return TD file in JsonObject format 
	 */
	public JsonObject getThingDescription() {
		
		return thingDescription;
	}
}
