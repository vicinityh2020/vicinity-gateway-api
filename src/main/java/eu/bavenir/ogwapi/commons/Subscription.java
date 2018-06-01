package eu.bavenir.ogwapi.commons;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// TODO documentation
public class Subscription {
	
	private String objectID;
	
	// The chosen implementation is HashSet with synchronizedSet wrapper. 
	private Set<String> eventSubscriptions;
	
	
	public Subscription(String objectID) {
		this.setObjectID(objectID);
		eventSubscriptions = Collections.synchronizedSet(new HashSet<String>());
	}
	
	
	public String getObjectID() {
		return objectID;
	}



	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}


	// returns false when it already exists
	public boolean addToSubscriptions(String eventID) {
		
		if (eventSubscriptions.contains(eventID)) {
			return false;
		}
		
		eventSubscriptions.add(eventID);
		
		return true;
	}
	
	
	// returns false when there is no such subscription
	public boolean removeFromSubscriptions(String eventID) {
		
		return eventSubscriptions.remove(eventID);
		
	}
	
	
	public boolean subscriptionExists(String eventID) {
		
		return eventSubscriptions.contains(eventID);
	}
	
	
	public int getNumberOfSubscriptions() {
		return eventSubscriptions.size();
	}
	
}
