package eu.bavenir.ogwapi.commons;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.connectors.http.RestAgentConnector;
import eu.bavenir.ogwapi.commons.engines.CommunicationEngine;
import eu.bavenir.ogwapi.commons.engines.xmpp.XmppMessageEngine;
import eu.bavenir.ogwapi.commons.messages.MessageParser;
import eu.bavenir.ogwapi.commons.messages.NetworkMessage;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;
import eu.bavenir.ogwapi.commons.search.SparqlQuery;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

// TODO documentation
/**
 * A representation of a connection to network. In essence, it is a client connected into XMPP network, able to 
 * send / receive messages and process the requests that arrive in them. Basically, the flow is like this:
 * 
 * 1. construct an instance
 * 2. {@link #connect() connect()}
 * 3. use - since the clients connecting to XMPP network are going to use HTTP authentication, they will send
 * 		their credentials in every request. It is therefore necessary to verify, whether the password is correct. The 
 * 		{@link #verifyPassword() verifyPassword()} is used for this and it should be called by RESTLET authorisation 
 * 		verifier every time a request is made.
 * 4. {@link #disconnect() disconnect()}
 *  
 * @author sulfo
 *
 */
public class ConnectionDescriptor {

	/* === CONSTANTS === */
	
	
	/**
	 * How long is the thread supposed to wait for message arrival before checking whether timeout was reached. After
	 * the check, the thread resumes to waiting for message and the cycle repeats until either message arrives or
	 * timeout is reached. 
	 */
	private static final long POLL_INTERRUPT_INTERVAL_MILLIS = 500;
	
	/**
	 * Worst case scenario is, when there is a single message in the queue that nobody wants and is just waiting for 
	 * expiration. Until the timeout is reached it would eat all CPU on message queue's polling call. Although unlikely,
	 * the remedy would be to sleep a little if there is just one message in the queue before checking again. To 
	 * sleep this long... 
	 */
	private static final long THREAD_SLEEP_MILLIS = 100;
	
	
	
	/* === FIELDS === */
	
	// a set of event channels served by this object
	private Set<EventChannel> providedEventChannels;
	
	// a map of channels that this object is subscribed to (OID / EID)
	private Set<Subscription> subscribedEventChannels;
	
	// a set of actions served by this object
	private Set<Action> providedActions;
	
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	// sparql query search engine
	private SparqlQuery sparql;
	
	
	// the thing that communicates with agent
	private AgentConnector agentConnector;
	
	// credentials
	private String objectID;
	private String password;
	
	// message queue, FIFO structure for holding incoming messages 
	private BlockingQueue<NetworkMessage> messageQueue;
	
	// the engine to use
	private CommunicationEngine commEngine;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * the descriptor will not be able to connect (in the best case scenario, the other being a storm of null pointer 
	 * exceptions).
	 * @param objectID
	 * @param password
	 * @param config
	 * @param logger
	 */
	public ConnectionDescriptor(String objectID, String password, XMLConfiguration config, Logger logger){
		
		
		
		// TODO this all should probably happen after successful login, so it does not use resources for nothing
		
		this.objectID = objectID;
		this.password = password;
		
		this.config = config;
		this.logger = logger;
		
		sparql = new SparqlQuery(this, logger);
		
		
		// TODO decide what type of connector to use
		agentConnector = new RestAgentConnector(config, logger);
		
		messageQueue = new LinkedTransferQueue<NetworkMessage>();
		
		providedEventChannels = new HashSet<EventChannel>();
		
		subscribedEventChannels = new HashSet<Subscription>();
		
		providedActions = new HashSet<Action>();
		
		
		
		
		// build new connection
		// TODO this is also the place, where it should decide what engine to use
		commEngine = new XmppMessageEngine(objectID, password, config, logger, this);
		
		// TODO load the event channels and actions - either from a file or server
		
	}
	
	
	/**
	 * Retrieves the object ID used for this connection.
	 *   
	 * @return Object ID.
	 */
	public String getObjectID() {
		return objectID;
	}

	
	/**
	 * Verifies, whether the client using this descriptor is using correct password. 
	 * 
	 * @param passwordToVerify The password provided by client.
	 * @return True if the password matches the one used by this connection.
	 */
	public boolean verifyPassword(String passwordToVerify) {
		return passwordToVerify.equals(password);
	}
	
	
	// TODO documentation
	public boolean connect(){
		return commEngine.connect(objectID, password);
	}
	
	
	// TODO documentation
	public void disconnect(){
		commEngine.disconnect();
	}


	// TODO documentation
	public boolean isConnected(){
		return commEngine.isConnected();
	}
	
	

	// TODO documentation
	public Set<String> getRoster(){
		return commEngine.getRoster();
	}
	
	


	// TODO documentation
	public String startAction(String destinationObjectID, String actionID, String body) {
		
		if (destinationObjectID == null || actionID == null) {
			logger.info("Invalid object ID or action ID.");
			return null;
		}
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_STARTACTION);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, this.objectID); // we are sending the ID of this object
		request.addAttribute(NetworkMessageRequest.ATTR_AID, actionID);
		request.setRequestBody(body);
		
		
		// message to be returned
		String statusMessageText;
		
		
		// all set
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_ACTION_START, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectID + " Destination ID: " + destinationObjectID + " Action ID: " + actionID  
					+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_ACTION_START, 
					statusMessageText); 
			
			return statusMessage.buildMessage().toString();
		}
		
		
		// TODO a status message needs to be parsed here and returned
		return response.getResponseBody();

	}
	
	
	
	
	
	public StatusMessage updateTaskStatus(String actionID, String newStatus, String returnValue) {
		
		if (actionID == null) {
			logger.warning("Invalid action ID or task ID.");
			return null;
		}
		
		Action action = searchForAction(actionID);
		
		if (action == null) {
			logger.warning("No such action " + actionID);
			return null;
		}
		
		StatusMessage statusMessage;
		String messageString;
		
		if (!action.updateTask(newStatus, returnValue)) {
			
			messageString = "Running task of action " + actionID + " is not in a state allowing update, "
					+ "or the requested new state is not aplicable.";
			
			logger.warning(messageString);
			
			statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_TASK_STATUS, messageString);
			
		}
		
		messageString = "Running task of action " + actionID + " was updated.";
		
		logger.info(messageString);
		statusMessage = new StatusMessage(false, StatusMessage.MESSAGE_TASK_STATUS, messageString);
		
		return statusMessage;
		
	}
	
	
	
	
	public String retrieveTaskStatus(String destinationObjectID, String actionID, String taskID) {
		
		if (destinationObjectID == null || actionID == null || taskID == null) {
			logger.info("Invalid object ID, action ID or task ID.");
			return null;
		}
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_GETTASKSTATUS);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, this.objectID); // we are sending the ID of this object
		request.addAttribute(NetworkMessageRequest.ATTR_AID, actionID);
		request.addAttribute(NetworkMessageRequest.ATTR_TID, taskID);
		
		// message to be returned
		String statusMessageText;
		
		
		// all set
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_TASK_STATUS, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectID + " Destination ID: " + destinationObjectID + " Action ID: " + actionID  
					+ " Task ID: " + taskID + " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_TASK_STATUS, 
					statusMessageText); 
			
			return statusMessage.buildMessage().toString();
		}
		
		
		// TODO a status message needs to be parsed here and returned
		return response.getResponseBody();
		
	}
	
	
	
	
	public String cancelRunningTask(String destinationObjectID, String actionID, String taskID) {
		
		if (destinationObjectID == null || actionID == null || taskID == null) {
			logger.info("Invalid object ID, action ID or task ID.");
			return null;
		}
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_CANCELTASK);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, this.objectID); // we are sending the ID of this object
		request.addAttribute(NetworkMessageRequest.ATTR_AID, actionID);
		request.addAttribute(NetworkMessageRequest.ATTR_TID, taskID);
		
		// message to be returned
		String statusMessageText;
		
		
		// all set
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_TASK_STOP, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectID + " Destination ID: " + destinationObjectID + " Action ID: " + actionID  
					+ " Task ID: " + taskID + " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_TASK_STOP, 
					statusMessageText); 
			
			return statusMessage.buildMessage().toString();
		}
		
		
		// TODO a status message needs to be parsed here and returned
		return response.getResponseBody();
		
	}
	

	
	
	
	
	/**
	 * Sets the status of the {@link EventChannel EventChannel}. The status can be either active, or inactive, depending
	 * on the 'status' parameter. 
	 * 
	 * If no channel with given 'eventID' exists and the 'status' is set to true, it gets created. If the 'status' is
	 * false, the channel is not created if it does not exists previously.
	 * 
	 * @param eventID Event ID.
	 * @param status If true, the channel will be set to active status.
	 * @return If the channel was found and its status set, returns true. It will also return true, if the channel was
	 * not found, but was created (this is what happens when the status of the non existing channel is being set to
	 * {@link EventChannel#STATUS_ACTIVE active}). If the channel was not found and the status was being set to 
	 * {@link EventChannel#STATUS_INACTIVE inactive}, returns false.
	 */
	public boolean setLocalEventChannelStatus(String eventID, boolean status) {
		
		boolean result = true;
		
		// search for given event channel
		EventChannel eventChannel = searchForEventChannel(eventID);
		
		if (eventChannel != null) {
			eventChannel.setActive(status);
			logger.fine("Object '" + objectID + "' changed the activeness of existing event channel '" 
					+ eventID + "' to " + status);
		} else {
			
			// if no event channel was found AND the caller wanted it to be active, create it
			if (status) {
				providedEventChannels.add(new EventChannel(objectID, eventID, true));
				logger.fine("Object '" + objectID + "' created active event channel '" + eventID + "'");
			} else {
				logger.fine("Could not deactivate the event channel '" + eventID + "' of the object '" + objectID 
						+ "'. The event channel does not exist.");
				
				result = false;
			}
		}
		
		return result;
	}
	
	
	
	// TODO documentation
	// TODO make it return statusmessage, not a string
	// also make it return whether we are subscribed or not
	public String getRemoteEventChannelStatus(String objectID, String eventID) {
		
		if (objectID == null || eventID == null) {
			logger.info("Invalid object ID or event ID.");
			return null;
		}
		
		// when the owner wants to check its own status, there is no need to send it across the network
		if (objectID.equals(this.objectID)) {

			EventChannel eventChannel = searchForEventChannel(eventID);
			StatusMessage statusMessage;
			
			if (eventChannel == null) {
				logger.info("Received a request to provide status of invalid event channel. Request came from: " + this.objectID);
				
				// responding with error
				// remember! we are going to include the outcome of the operation as the status message
				statusMessage = new StatusMessage(
						true, 
						StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
						new String("Invalid event channel specified."));

			} else {
				
				logger.fine("Received a request to provide status of event channel " + eventID + " from " + this.getObjectID() + ".");
				
				if (eventChannel.isActive()) {
					statusMessage = new StatusMessage(
							false, 
							StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
							EventChannel.STATUS_STRING_ACTIVE);
				} else {
					statusMessage = new StatusMessage(
							false, 
							StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
							EventChannel.STATUS_STRING_INACTIVE);
				}
				
			}
			
			return statusMessage.buildMessage().toString();
		} 
		
		
		// otherwise continue as normal
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// message to be returned
		String statusMessageText;
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, objectID);
		request.addAttribute(NetworkMessageRequest.ATTR_EID, eventID);
		
		// all set
		
		if (!commEngine.sendMessage(objectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + objectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectID + " Destination ID: " + objectID + " Event ID: " + eventID  
					+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
					statusMessageText); 
			
			return statusMessage.buildMessage().toString();
		}
		
		// TODO a status message needs to be parsed here and returned
		return response.getResponseBody();
	}
	
	
	// TODO documentation
	// TODO make it return status message
	public String subscribeToEventChannel(String destinationObjectID, String eventID) {
		if (destinationObjectID == null || eventID == null) {
			logger.info("Invalid object ID or event ID.");
			return null;
		}
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// message to be returned
		String statusMessageText;
		
		// first check whether or not we are already subscribed
		Subscription subscription = searchForSubscription(destinationObjectID);
		
		if (subscription != null) {
			if (subscription.subscriptionExists(eventID)) {
				statusMessageText = 
						new String("Already subscribed to " + destinationObjectID + " event channel " + eventID + ".");
				logger.info(statusMessageText);
				
				StatusMessage statusMessage = new StatusMessage(false, StatusMessage.MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL, 
						statusMessageText);
				
				return statusMessage.buildMessage().toString();
			}
		} else {
			// create a new subscription object (but don't actually add the subscription to a concrete event yet)
			subscription = new Subscription(destinationObjectID);
		}
		
		
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, objectID); // we are sending the ID of this object
		request.addAttribute(NetworkMessageRequest.ATTR_EID, eventID);
		
		// all set
		
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ destinationObjectID + " Destination ID: " + destinationObjectID + " Event ID: " + eventID  
					+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL, 
					statusMessageText); 
			
			return statusMessage.buildMessage().toString();
		}
		
		// keep the track
		subscription.addToSubscriptions(eventID);
		subscribedEventChannels.add(subscription);
		
		// TODO a status message needs to be parsed here and returned
		return response.getResponseBody();
	}
	
	
	
	// TODO documentation
	// TODO make it return status message
	public String unsubscribeFromEventChannel(String destinationObjectID, String eventID) {
		if (destinationObjectID == null || eventID == null) {
			logger.info("Invalid object ID or event ID.");
			return null;
		}
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// message to be returned
		String statusMessageText;
		
		// first check whether or not we are already subscribed
		Subscription subscription = searchForSubscription(destinationObjectID);
		
		if (subscription == null || !subscription.subscriptionExists(eventID)) {
			statusMessageText = 
					new String("Can't unsubscribe from " + destinationObjectID + " event channel " + eventID + ". We are not subscribed.");
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL);
		request.addAttribute(NetworkMessageRequest.ATTR_EID, eventID);
		
		// all set
		
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_UNSUBSCRIBEFROMEVENTCHANNEL, 
					statusMessageText);
			
			return statusMessage.buildMessage().toString();
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ destinationObjectID + " Destination ID: " + destinationObjectID + " Event ID: " + eventID  
					+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_EVENT_UNSUBSCRIBEFROMEVENTCHANNEL, 
					statusMessageText); 
			
			return statusMessage.buildMessage().toString();
		}
		
		// keep the track
		subscription.removeFromSubscriptions(eventID);
		
		// clean up
		if (subscription.getNumberOfSubscriptions() == 0) {
			subscribedEventChannels.remove(subscription);
		}
		
		
		// TODO a status message needs to be parsed here and returned
		return response.getResponseBody();
	}
	
	
	
	// TODO documentation
	// -1 if there is no such event channel 
	public int sendEventToSubscribers(String eventID, String event) {
		
		// look up the event channel
		EventChannel eventChannel = searchForEventChannel(eventID);
		
		if (eventChannel == null || !eventChannel.isActive()) {
			
			logger.fine("Could not distribute an event to the channel '" + eventID + "' of the object '" + objectID 
					+ "'. The event channel does not exist.");
			return -1;
		}
		
		if (!eventChannel.isActive()) {
			
			logger.fine("Could not distribute an event to the channel '" + eventID + "' of the object '" + objectID 
					+ "'. The event channel is not active.");
			
			return -1;
		}
		
		// create the message
		NetworkMessageEvent eventMessage = new NetworkMessageEvent(config, objectID, eventID, event);
		String message = eventMessage.buildMessageString();
		
		// keep track of number of sent messages
		int sentMessages = 0;
		
		// send them
		Set<String> subscribers = eventChannel.getSubscribersSet();
		for (String destinationOid : subscribers) {
			if(commEngine.sendMessage(destinationOid, message)) {
				sentMessages++;
			} 
		}
		
		return sentMessages;
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Returns the number of subscribers for the {@link EventChannel EventChannel} specified by its event ID. 
	 * 
	 * @param eventID ID of the {@link EventChannel EventChannel}.
	 * @return Number of subscribers, or -1 if no such {@link EventChannel EventChannel} exists. 
	 */
	public int getNumberOfSubscribers(String eventID) {
		
		// look up the event channel
		EventChannel eventChannel = searchForEventChannel(eventID);
		
		if (eventChannel == null) {
			return -1;
		}
		
		return eventChannel.getSubscribersSet().size();
	}
	
	
	
	
	// TODO documentation
	public StatusMessage getPropertyOfRemoteObject(String destinationObjectID, String propertyID) {

		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// message to be returned in case something goes wrong
		String statusMessageText;
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_GETPROPERTYVALUE);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, destinationObjectID);
		request.addAttribute(NetworkMessageRequest.ATTR_PID, propertyID);
		
		// all set
		
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_PROPERTY_GETVALUE, 
					statusMessageText);
			
			return statusMessage;
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){

			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectID + " Destination ID: " + destinationObjectID + " Property ID: " + propertyID  
					+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);
			
			return new StatusMessage(true, StatusMessage.MESSAGE_PROPERTY_GETVALUE, statusMessageText);
		}
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			
			statusMessageText = new String("Source object: " + objectID + " Destination object: " + destinationObjectID 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			
			logger.info(statusMessageText);
			
			return new StatusMessage(true, StatusMessage.MESSAGE_PROPERTY_GETVALUE, statusMessageText);
		}
		
		StatusMessage okStatus = new StatusMessage();
		okStatus.setError(false);
		okStatus.setBody(response.getResponseBody());
		
		return okStatus;
	}
	
	
	
	
	
	// TODO documentation
	public StatusMessage setPropertyOfRemoteObject(String destinationObjectID, String propertyID, String body) {
		
		NetworkMessageRequest request = new NetworkMessageRequest(config);
		
		// message to be returned in case something goes wrong
		String statusMessageText;
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(NetworkMessageRequest.OPERATION_SETPROPERTYVALUE);
		request.addAttribute(NetworkMessageRequest.ATTR_OID, destinationObjectID);
		request.addAttribute(NetworkMessageRequest.ATTR_PID, propertyID);
		
		request.setRequestBody(body);
		
		// all set
		if (!commEngine.sendMessage(destinationObjectID, request.buildMessageString())){
			
			statusMessageText = new String("Destination object " + destinationObjectID + " is not online.");
			
			logger.info(statusMessageText);
			
			return new StatusMessage(true, StatusMessage.MESSAGE_PROPERTY_SETVALUE, statusMessageText);
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){
			
			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
				+ objectID + " Destination ID: " + destinationObjectID + " Property ID: " + propertyID  
				+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);

			return new StatusMessage(true, StatusMessage.MESSAGE_PROPERTY_SETVALUE, statusMessageText);
		}
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			
			statusMessageText = new String("Source object: " + objectID + " Destination object: " + destinationObjectID 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			
			logger.info(statusMessageText);

			return new StatusMessage(true, StatusMessage.MESSAGE_PROPERTY_SETVALUE, statusMessageText);
		}
		
		StatusMessage okStatus = new StatusMessage();
		okStatus.setError(false);
		okStatus.setBody(response.getResponseBody());
		
		return okStatus;
	}
	
	
	
	
	/**
	 * This is a callback method called when a message arrives. There are two main scenarios that need to be handled:
	 * a. A message arrives ('unexpected') from another node with request for data or action - this need to be routed 
	 * to a specific end point on agent side and then the result needs to be sent back to originating node. 
	 * b. After sending a message with request to another node, the originating node expects an answer, which arrives
	 * as a message. This is stored in a queue and is propagated back to originating services, that are expecting the
	 * results.
	 * 
	 * NOTE: This method is to be called by the {@link CommunicationEngine engine } subclass instance.
	 * 
	 * @param from Object ID of the sender.
	 * @param message Received message.
	 */
	public void processIncommingMessage(String from, String message){
		
		logger.finest("New message from " + from + ": " + message);
		
		// let's parse the message 
		MessageParser messageParser = new MessageParser(config);
		NetworkMessage networkMessage = messageParser.parseNetworkMessage(message);
		
		if (networkMessage != null){
			switch (networkMessage.getMessageType()){
			
			case NetworkMessageRequest.MESSAGE_TYPE:
				logger.finest("The message is a request. Processing...");
				processMessageRequest(from, networkMessage);
				break;
				
			case NetworkMessageResponse.MESSAGE_TYPE:
				logger.finest("This message is a response. Adding to incoming queue - message count: " 
						+ messageQueue.size());
				processMessageResponse(from, networkMessage);
				break;
				
			case NetworkMessageEvent.MESSAGE_TYPE:
				logger.finest("This message is an event. Forwarding...");
				processMessageEvent(from, networkMessage);
			}
		} else {
			logger.warning("Invalid message received from the network.");
		}
		
	}
	
	
	
	public String performSparqlQuery(String query) {
		
		if (query == null) {
			return null;
		}
		
		return sparql.performQuery(query);
	}
	
	
	
	/* === PRIVATE METHODS === */
	
	
	
	
	/**
	 * Processing method for {@link NetworkMessageRequest request} type of {@link NetworkMessage NetworkMessage}.
	 * 
	 * @param from ID of the object that sent the message.
	 * @param networkMessage Message parsed from the incoming message. 
	 */
	private void processMessageRequest(String from, NetworkMessage networkMessage){
		
		// cast it to request message first (it is safe and also necessary)
		NetworkMessageRequest requestMessage = (NetworkMessageRequest) networkMessage;
		
		NetworkMessageResponse response;
		
		
		switch (requestMessage.getRequestOperation()){
		
		case NetworkMessageRequest.OPERATION_CANCELTASK:
			
			response = respondToCancelRunningTask(from, requestMessage);
			commEngine.sendMessage(from, response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS:
			
			response = respondToEventChannelStatusQuery(from, requestMessage);
			commEngine.sendMessage(from, response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETLISTOFACTIONS:
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETLISTOFEVENTS:
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETLISTOFPROPERTIES:
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETPROPERTYVALUE:
			
			response = agentConnector.getObjectProperty(requestMessage);

			// send it back
			commEngine.sendMessage(from, response.buildMessageString());
			break;
			
		case NetworkMessageRequest.OPERATION_GETTASKSTATUS:
			
			response = respondToGetTaskStatus(from, requestMessage);
			
			// send it back
			commEngine.sendMessage(from, response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_SETPROPERTYVALUE:
			
			response = agentConnector.setObjectProperty(requestMessage);

			// send it back
			commEngine.sendMessage(from, response.buildMessageString());
			break;
			
		case NetworkMessageRequest.OPERATION_STARTACTION:
			
			response = respondToStartActionRequest(from, requestMessage);
			
			// send it back
			commEngine.sendMessage(from, response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL:
			
			response = respondToEventSubscriptionRequest(from, requestMessage);
			
			// answer
			commEngine.sendMessage(from, response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL:
			
			response = respondToCancelSubscriptionRequest(from, requestMessage);
			
			// answer
			commEngine.sendMessage(from, response.buildMessageString());
			
			break;
		}
		
	}
	
	
	
	/**
	 * Processing method for {@link NetworkMessageResponse response} type of {@link NetworkMessage NetworkMessage}.
	 * 
	 * @param from ID of the object that sent the message.
	 * @param networkMessage Message parsed from the incoming message.
	 */
	private void processMessageResponse(String from, NetworkMessage networkMessage){
		messageQueue.add(networkMessage);
	}
	
	
	
	// TODO documentation
	private void processMessageEvent(String from, NetworkMessage networkMessage) {
		
		
		
		// cast it to event message first (it is safe and also necessary)
		NetworkMessageEvent eventMessage = (NetworkMessageEvent) networkMessage;
		
		logger.info("Event " + eventMessage.getEventID() + " arrived from " + eventMessage.getEventSource() 
							+ ". Event body: " + eventMessage.getEventBody());
		
		
		// don't process the event if we are not subscribed to it
		Subscription subscription = searchForSubscription(from);
		
		if (subscription != null && subscription.subscriptionExists(eventMessage.getEventID())) {
			NetworkMessageResponse response = agentConnector.forwardEventToObject(
					this.objectID, 
					eventMessage.getEventID(), 
					eventMessage.getEventBody());

			// if the return code is different than 2xx, make it visible
			if ((response.getResponseCode() / 200) != 1){
				
				logger.warning("Event was not forwarded successfully. Response code: " + response.getResponseCode() 
											+ ". Response reason: " + response.getResponseCodeReason());
											
			} else {
				logger.info("Event forwarded successfully.");
			}
		}
		
		// no need to send the response message back to sender
	}
	
	
	
	// TODO documentation
	private NetworkMessageResponse respondToEventChannelStatusQuery(String from, NetworkMessageRequest requestMessage) {
		
		String eventID = null;
		EventChannel eventChannel = null;
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config);
		StatusMessage statusMessage;
		
		
		// the event ID should have been sent in attributes
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			eventID = attributesMap.get(NetworkMessageRequest.ATTR_EID);	
		}
		
		if (eventID != null) {
			eventChannel = searchForEventChannel(eventID);
		}
		
		if (eventChannel == null) {
			logger.info("Received a request to provide status of invalid event channel. Request came from: " + from);
			
			// responding with error
			// remember! we are going to include the outcome of the operation as the status message
			statusMessage = new StatusMessage(
					true, 
					StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
					new String("Invalid event channel specified."));

		} else {
			
			logger.fine("Received a request to provide status of event channel " + eventID + " from " + from + ".");
			
			if (eventChannel.isActive()) {
				statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
						EventChannel.STATUS_STRING_ACTIVE);
			} else {
				statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_EVENT_GETREMOTEEVENTCHANNELSTATUS, 
						EventChannel.STATUS_STRING_INACTIVE);
			}
			
		}
		
		response.setResponseBody(statusMessage.buildMessage().toString());
		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	// TODO documentation
	private NetworkMessageResponse respondToEventSubscriptionRequest(String from, NetworkMessageRequest requestMessage) {
		
		String eventID = null;
		EventChannel eventChannel = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config);
		StatusMessage statusMessage;
		
		// the event ID should have been sent in attributes
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			eventID = attributesMap.get(NetworkMessageRequest.ATTR_EID);
		}
		
		// check whether the event channel exists
		if (eventID != null) {
			eventChannel = searchForEventChannel(eventID);
		}
		
		// check whether the object is in our roster and whether or not it is already in the list of subscribers
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		
		if (eventChannel == null || !eventChannel.isActive()) { // || !security check
			logger.info("Received a request for subscription to invalid event channel. Request came from: " + from);
			
			// responding with error
			// remember! we are going to include the outcome of the operation as the status message
			statusMessage = new StatusMessage(
					true, 
					StatusMessage.MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL, 
					new String("Invalid event channel specified."));
		} else {
			logger.fine("Received a request for subscription to event channel " + eventID + " from " + from + ".");
			
			eventChannel.addToSubscribers(from);
			
			statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_EVENT_SUBSCRIBETOEVENTCHANNEL, 
						StatusMessage.TEXT_SUCCESS);
		}
		
		response.setResponseBody(statusMessage.buildMessage().toString());
		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
		
	}
	
	
	
	// TODO documentation
	private NetworkMessageResponse respondToCancelSubscriptionRequest(String from, NetworkMessageRequest requestMessage) {
		String eventID = null;
		EventChannel eventChannel = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config);
		StatusMessage statusMessage;
		
		// the event ID should have been sent in attributes
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			eventID = attributesMap.get(NetworkMessageRequest.ATTR_EID);
		}
		
		// check whether the event channel exists
		if (eventID != null) {
			eventChannel = searchForEventChannel(eventID);
		}
		
		// check whether the object is in our roster and whether or not it is already in the list of subscribers
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		
		if (eventChannel == null || !eventChannel.isActive()) { // || !security check
			logger.info("Received a request to cancel subscription to invalid event channel. Request came from: " + from);
			
			// responding with error
			// remember! we are going to include the outcome of the operation as the status message
			statusMessage = new StatusMessage(
					true, 
					StatusMessage.MESSAGE_EVENT_UNSUBSCRIBEFROMEVENTCHANNEL, 
					new String("Invalid event channel specified."));
		} else {
			logger.fine("Received a request to cancel subscription to event channel " + eventID + " from " + from + ".");
			
			eventChannel.removeFromSubscribers(from);
			
			statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_EVENT_UNSUBSCRIBEFROMEVENTCHANNEL, 
						StatusMessage.TEXT_SUCCESS);
		}
		
		response.setResponseBody(statusMessage.buildMessage().toString());
		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	// TODO documentation
	private NetworkMessageResponse respondToStartActionRequest(String from, NetworkMessageRequest requestMessage) {
		
		String actionID = null;
		Action action = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config);
		StatusMessage statusMessage;
		
		// the action ID should have been sent in attributes
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			actionID = attributesMap.get(NetworkMessageRequest.ATTR_AID);
		}
		
		// check whether the action exists
		if (actionID != null) {
			action = searchForAction(actionID);
		}
		
		// TODO delete this workaround - the actions should be loaded at the startup and no new action should 
		// be possible
		if (action == null) {
			action = new Action (config, this.objectID, actionID, agentConnector);
			providedActions.add(action);
		}
		
		String statusString;
		
		// check whether the object is in our roster and whether or not the action already exists
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		if (action == null ) { // || security check
			logger.info("Received a request to start non existing action. Request came from: " + from);
			
			// responding with error
			// remember! we are going to include the outcome of the operation as the status message
			statusMessage = new StatusMessage(
					true, 
					StatusMessage.MESSAGE_ACTION_START, 
					new String("Invalid action specified."));
		} else {
			logger.fine("Received a request to start action " + actionID + " from " + from + ".");
			
			String taskID = action.createNewTask(from, requestMessage.getRequestBody());
			
			if (taskID == null) {
				
				statusString = new String("Cannot start action " + actionID + ", too many tasks in queue.");
				
				logger.warning(statusString);
				
				statusMessage = new StatusMessage(
						true, 
						StatusMessage.MESSAGE_ACTION_START, 
						new String(statusString));
			} else {
				statusString = new String("Task " + taskID + " of action " + actionID + " was created and added to the queue.");
				
				logger.fine(statusString);
				
				statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_ACTION_START, 
						StatusMessage.TEXT_SUCCESS);
				
				statusMessage.addMessage(StatusMessage.MESSAGE_TASKID, taskID);
			}
		}
		
		response.setResponseBody(statusMessage.buildMessage().toString());
		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	private NetworkMessageResponse respondToGetTaskStatus(String from, NetworkMessageRequest requestMessage) {
		String actionID = null;
		String taskID = null;
		Action action = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config);
		StatusMessage statusMessage;
		
		// the action ID should have been sent in attributes
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			actionID = attributesMap.get(NetworkMessageRequest.ATTR_AID);
			taskID = attributesMap.get(NetworkMessageRequest.ATTR_TID);
		}
		
		// check whether the action exists
		if (actionID != null) {
			action = searchForAction(actionID);
		}
		
		
		String statusString;
		
		// check whether the object is in our roster and whether or not the action already exists
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		if (action == null ) { // || security check
			logger.info("Received a request for status report on non existing action/task. Request came from: " + from);
			
			// responding with error
			// remember! we are going to include the outcome of the operation as the status message
			statusMessage = new StatusMessage(
					true, 
					StatusMessage.MESSAGE_TASK_STATUS, 
					new String("Invalid action specified."));
		} else {
			logger.fine("Received a request for status report on action " + actionID + " task " + taskID + " from " + from + ".");
			
			byte statusCode = action.getTaskStatus(taskID);
			String status = action.getTaskStatusString(taskID);
			String returnValue = action.getTaskReturnValue(taskID);
			// running time?
			
			if (statusCode == Task.TASKSTATUS_UNKNOWN) {
				
				statusString = new String("Cannot find task " + taskID + ". Its status is unknown.");
				
				logger.warning(statusString);
				
				statusMessage = new StatusMessage(
						true, 
						StatusMessage.MESSAGE_TASK_STATUS, 
						new String(statusString));
			} else {
				statusString = new String("Task " + taskID + " of action " + actionID + " status " + status + " return value " + returnValue);
				
				logger.fine(statusString);
				
				statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_TASK_STATUS, 
						status);
				
				statusMessage.addMessage(StatusMessage.MESSAGE_TASK_RETURNVALUE, returnValue);
			}
		}
		
		response.setResponseBody(statusMessage.buildMessage().toString());
		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
		
	}
	
	
	
	private NetworkMessageResponse respondToCancelRunningTask(String from, NetworkMessageRequest requestMessage) {
		String actionID = null;
		String taskID = null;
		Action action = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config);
		StatusMessage statusMessage;
		
		// the action ID should have been sent in attributes
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			actionID = attributesMap.get(NetworkMessageRequest.ATTR_AID);
			taskID = attributesMap.get(NetworkMessageRequest.ATTR_TID);
		}
		
		// check whether the action exists
		if (actionID != null) {
			action = searchForAction(actionID);
		}
		
		
		String statusString;
		
		// check whether the object is in our roster and whether or not the action already exists
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		if (action == null ) { // || security check
			logger.info("Received a request for stopping a non existing action/task. Request came from: " + from);
			
			// responding with error
			// remember! we are going to include the outcome of the operation as the status message
			statusMessage = new StatusMessage(
					true, 
					StatusMessage.MESSAGE_TASK_STOP, 
					new String("Invalid action specified."));
		} else {
			logger.fine("Received a request for for stopping an action " + actionID + " task " + taskID + " from " + from + ".");
			
			NetworkMessageResponse response2 = action.cancelTask(taskID);
			
			if (response2 == null) {
				
				statusString = new String("Cannot find task " + taskID + ". Its status is unknown.");
				
				logger.warning(statusString);
				
				statusMessage = new StatusMessage(
						true, 
						StatusMessage.MESSAGE_TASK_STOP, 
						new String(statusString));
			} else {
				statusString = new String("Task " + taskID + " of action " + actionID 
						+ " cancelation result: " + response2.getResponseCode() + " " + response2.getResponseCodeReason());
				
				logger.fine(statusString);
				
				statusMessage = new StatusMessage(
						false, 
						StatusMessage.MESSAGE_TASK_STOP, statusString);
			}
		}
		
		response.setResponseBody(statusMessage.buildMessage().toString());
		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	/**
	 * Searches for {@link EventChannel EventChannel} with provided eventID. 
	 * 
	 * @param eventID ID of the event.
	 * @return {@link EventChannel EventChannel} with the ID, or null if no such channel exists. 
	 */
	private EventChannel searchForEventChannel(String eventID) {
		// search for given event channel
		
		for (EventChannel eventChannel : providedEventChannels) {
			if (eventChannel.getEventID().equals(eventID)) {
				// found it
				return eventChannel;
			}
		}

		return null;
	}
	
	
	private Action searchForAction(String actionID) {
		
		// TODO remove
		for (Action action : providedActions) {
			System.out.println("List of actions: " + action.getActionID());
		}
		
		// search for given action
		
		for (Action action : providedActions) {
			if (action.getActionID().equals(actionID)) {
				// found it
				return action;
			}
		}

		return null;
	}
	
	
	// TODO documentation
	private Subscription searchForSubscription(String remoteObjectID) {
		
		for (Subscription subscription : subscribedEventChannels) {
			if (subscription.getObjectID().equals(remoteObjectID)) {
				// found it
				return subscription;
			}
		}

		return null;
	}

	
	
	/**
	 * Retrieves a {@link NetworkMessage NetworkMessage} from the queue of incoming messages based on the correlation 
	 * request ID. It blocks the invoking thread if there is no message in the queue until it arrives or until timeout
	 * is reached. The check for timeout is scheduled every {@link #POLL_INTERRUPT_INTERVAL_MILLIS POLL_INTERRUPT_INTERVAL_MILLIS}.
	 * 
	 * @param requestId Correlation request ID. 
	 * @return {@link NetworkMessage NetworkMessage} from the queue.
	 */
	private NetworkMessage retrieveMessage(int requestId){
		
		NetworkMessage message = null;
		
		// retrieve the timeout from configuration
		long startTime = System.currentTimeMillis();
		boolean timeoutReached = false;
		
		do {
			NetworkMessage helperMessage = null;
			try {
				// take the first element or wait for one
				helperMessage = messageQueue.poll(POLL_INTERRUPT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// bail out
				return null;
			}
			
			if (helperMessage != null){
				// we have a message now
				if (helperMessage.getRequestId() != requestId){
					// ... but is not our message. let's see whether it is still valid and if it is, return it to queue
					if (helperMessage.isValid()){
						messageQueue.offer(helperMessage);
					} else {
						logger.fine("Discarding stale message: ID = " + helperMessage.getRequestId() 
							+ "; Timestamp = " + helperMessage.getTimeStamp());
					}
					
					// in order not to iterate thousand times a second over one single message, that don't belong
					// to us (or anybody), let's sleep a little to optimise performance
					if (messageQueue.size() == 1){
						try {
							Thread.sleep(THREAD_SLEEP_MILLIS);
						} catch (InterruptedException e) {
							return null;
						}
					}
				} else {
					// it is our message :-3
					message = helperMessage;
				}
			}
			
			timeoutReached = ((System.currentTimeMillis() - startTime) 
							> (config.getInt(NetworkMessage.CONFIG_PARAM_XMPPMESSAGETIMEOUT, 
									NetworkMessage.CONFIG_DEF_XMPPMESSAGETIMEOUT)*1000));
			
		// until we get our message or the timeout expires
		} while (message == null && !timeoutReached);
	
		return message;	
	}
	
}
