package eu.bavenir.ogwapi.commons;

import java.io.Serializable;
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


/**
 * This class represents a subscription to a remote publishing object's event {@link eu.bavenir.ogwapi.commons.EventChannel channels}.
 * For each remote publishing object that this local object is subscribed to, only one instance of this class is created, 
 * even if the publishing object has several {@link eu.bavenir.ogwapi.commons.EventChannel EventChannels} open. The
 * list of channels the local object is subscribed to on a remote object is kept inside this class as a set of 
 * event channel IDs. 
 * 
 * @author sulfo
 *
 */
public class Subscription implements Serializable {
	
	/* === CONSTANTS === */
	
	/**
	 * A unique serial version identifier
	 * @see Serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/* === FIELDS === */
	
	/**
	 * Object ID of the remote publishing object.
	 * 
	 * @Serialize
	 */
	private String objectId;
	
	/**
	 * Thread-safe HashSet with synchronizedSet wrapper to store object IDs that are subscribed.
	 * 
	 * @Serialize
	 */ 
	private Set<String> eventSubscriptions;
	
	/**
	 * Quality of service 
	 * default = 1
	 * 
	 * 1 => don't send ACK
	 * 2 => send ACK
	 * 
	 * @Serialize
	 */
	private int QoS;
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initialises the HashSet.
	 * 
	 * @param objectId ID of the remote publishing object.
	 */
	public Subscription(String objectId, int QoS) {
		this.objectId = objectId;
		this.QoS = QoS;
		eventSubscriptions = Collections.synchronizedSet(new HashSet<String>());
	}
	
	/**
	 * Constructor that initialises the Subscription object with a pre-loaded set of eventSubscriptions.
	 * 
	 * @param objectId ID of the remote publishing object.
	 * @param eventSubscriptions A set eventSubscriptions
	 */
	public Subscription(String objectId, int QoS, Set<String> eventSubscriptions) {
		this.objectId = objectId;
		this.QoS = QoS;
		this.eventSubscriptions = Collections.synchronizedSet(eventSubscriptions);
	}
	
	/**
	 * Getter for the ID of the remote publishing object.
	 * 
	 * @return Publishing object ID.
	 */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * Returns the set of event subscriptions in the form of Set.
	 * 
	 * @return A set of subscribers. 
	 */
	public Set<String> getEventSubscriptionsSet(){
		
		return eventSubscriptions;
	}

	/**
	 * Adds an {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel} into a set of those channels that the local object
	 * is subscribed to on the remote publishing object.
	 * 
	 * @param eventId Event ID.
	 * @return True if there was no such subscription before. False otherwise.
	 */
	public boolean addToSubscriptions(String eventId) {
		
		if (eventSubscriptions.contains(eventId)) {
			return false;
		}
		
		eventSubscriptions.add(eventId);
		
		return true;
	}
	
	
	/**
	 * Removes an {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel} from a set of those channels that the local object
	 * is subscribed to on the remote publishing object.
	 * 
	 * @param eventId Event ID.
	 * @return False when there is no such subscription, true otherwise. 
	 */
	public boolean removeFromSubscriptions(String eventId) {
		
		return eventSubscriptions.remove(eventId);
		
	}
	
	
	/**
	 * Verifies an existing subscription to given {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 * 
	 * @param eventId Event ID.
	 * @return True if subscription exists.
	 */
	public boolean subscriptionExists(String eventId) {
		
		return eventSubscriptions.contains(eventId);
	}
	
	
	/**
	 * Returns a number of event {@link eu.bavenir.ogwapi.commons.EventChannel channels} on this single remote publishing
	 * object this local object is subscribed to. 
	 *  
	 * @return Number of event channels. 
	 */
	public int getNumberOfSubscriptions() {
		return eventSubscriptions.size();
	}
	
	
	/**
	 * Returns a QoS. 
	 *  
	 * @return QoS integer. 
	 */
	public int getQoS() {
		return QoS;
	}
	
	/* === PRIVATE METHODS === */
	
}
