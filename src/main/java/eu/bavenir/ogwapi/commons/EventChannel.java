package eu.bavenir.ogwapi.commons;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


// TODO documentation

// napisat ze je to thread safe a ako sa to pouziva.
/**
 * 
 * 
 * 
 * @author sulfo
 *
 */
public class EventChannel {


	/* === CONSTANTS === */

	/**
	 * Inactive status of the channel. 
	 */
	public static final boolean STATUS_INACTIVE = false;
	
	/**
	 * Active status of the channel.
	 */
	public static final boolean STATUS_ACTIVE = true;
	
	public static final String STATUS_STRING_ACTIVE = "active";
	
	public static final String STATUS_STRING_INACTIVE = "inactive";
	
	public static final String ATTR_ACTIVE = "active";
	
	public static final String ATTR_SUBSCRIBED = "subscribed";
	
	/* === FIELDS === */
	
	/**
	 * This is the object ID of the channel owner (usually the local object, represented by its 
	 * {@link ConnectionDescriptor ConnectionDescriptor}). 
	 */
	private String objectID;
	
	/**
	 * The ID of the event channel. 
	 */
	private String eventID;
	
	/**
	 * Status of the event channel.
	 */
	private boolean active;
	
	/**
	 * A Set of remote object IDs that are subscribed to this event. The chosen implementation is HashSet with 
	 * synchronizedSet wrapper. 
	 */
	private Set<String> subscribers;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor that initialises the EventChannel object with empty set of subscribers. 
	 * 
	 * @param objectID Object ID of the owner of this event channel.
	 * @param eventID ID of the event channel.
	 * @param active Status of the channel. 
	 */
	public EventChannel(String objectID, String eventID, boolean active) {
		this.objectID = objectID;
		this.eventID = eventID;
		this.active = active;
		subscribers = Collections.synchronizedSet(new HashSet<String>());
	}
	
	
	/**
	 * Constructor that initialises the EventChannel object with a pre-loaded set of subscribers.
	 * 
	 * @param objectID Object ID of the owner of this event channel.
	 * @param eventID ID of the event channel. 
	 * @param active Status of the channel.
	 * @param subscribers A set of subscribers to this EventChannel. 
	 */
	public EventChannel(String objectID, String eventID, boolean active, HashSet<String> subscribers) {
		this.objectID = objectID;
		this.eventID = eventID;
		this.active = active;
		this.subscribers = Collections.synchronizedSet(subscribers);
		
	}

	
	/**
	 * Returns the set of subscribers in the form of Set.
	 * 
	 * @return A set of subscribers. 
	 */
	public Set<String> getSubscribersSet(){
		
		return subscribers;
	}
	
	
	/**
	 * Return the set of subscribers in the form of array of strings.
	 * 
	 * @return String array of subscribers.
	 */
	public String[] getSubscribersArray() {
		
		String[] arrayOfSubscribers = new String[subscribers.size()];
		
		return subscribers.toArray(arrayOfSubscribers);
	}
	
	
	/**
	 * This method adds a new subscriber into the set of subscribers. If the object ID is already in the set of 
	 * subscribers, nothing happens (meaning, it will not result in duplicates).
	 * 
	 *  This is a thread safe operation.
	 * 
	 * @param objectID Object ID of the new subscriber.
	 */
	public void addToSubscribers(String objectID) {
		synchronized(subscribers) {
			subscribers.add(objectID);
		}
	}
	
	
	/**
	 * Removes an object ID from the set of subscribers. If there is no such ID, nothing happens.
	 * 
	 * This is a thread safe operation.
	 *  
	 * @param objectID Object ID of the subscriber to be removed. 
	 */
	public void removeFromSubscribers(String objectID) {
		synchronized(subscribers) {
			subscribers.remove(objectID);
		}
	}
	
	
	/**
	 * Checks whether the given object ID is in the set of subscribers for the current event. 
	 * 
	 * @param objectID Object ID to be checked. 
	 * @return True if the object ID is among the subscribers.
	 */
	public boolean isSubscribed(String objectID) {
		return subscribers.contains(objectID);
	}
	
	
	/**
	 * Returns the object ID of the object that owns this event channel. 
	 * 
	 * @return Object ID of the object that generates the events. 
	 */
	public String getOwnerObjectID() {
		return objectID;
	}

	
	/**
	 * Returns the event ID.
	 * 
	 * @return Event ID.
	 */
	public String getEventID() {
		return eventID;
	}


	/**
	 * Returns the status of this event channel.
	 * 
	 * @return True if the channel is active.
	 */
	public boolean isActive() {
		return active;
	}


	/**
	 * Changes the status of this event channel.
	 * 
	 * @param active Boolean value indicating, whether the channel should be active or not. 
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	
	/* === PRIVATE METHODS === */
	
}
