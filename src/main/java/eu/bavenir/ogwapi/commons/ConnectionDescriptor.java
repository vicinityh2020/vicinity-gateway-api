package eu.bavenir.ogwapi.commons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.connectors.http.RestAgentConnector;
import eu.bavenir.ogwapi.commons.engines.CommunicationEngine;
import eu.bavenir.ogwapi.commons.engines.xmpp.XmppMessageEngine;
import eu.bavenir.ogwapi.commons.messages.CodesAndReasons;
import eu.bavenir.ogwapi.commons.messages.MessageResolver;
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
	
	private MessageResolver messageResolver;
	
	// the thing that communicates with agent
	private AgentConnector agentConnector;
	
	// credentials
	private String objectId;
	private String password;
	
	// message queue, FIFO structure for holding incoming messages 
	private BlockingQueue<NetworkMessage> messageQueue;
	
	// the engine to use
	private CommunicationEngine commEngine;
	
	/**
	 * Factory for JSON builders.
	 */
	private JsonBuilderFactory jsonBuilderFactory;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * the descriptor will not be able to connect (in the best case scenario, the other being a storm of null pointer 
	 * exceptions).
	 * @param objectId
	 * @param password
	 * @param config
	 * @param logger
	 */
	public ConnectionDescriptor(String objectId, String password, XMLConfiguration config, Logger logger){
		
		
		
		// TODO this all should probably happen after successful login, so it does not use resources for nothing
		
		this.objectId = objectId;
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
		
		messageResolver = new MessageResolver(config, logger);
		
		jsonBuilderFactory = Json.createBuilderFactory(null);
		
		
		
		// build new connection
		// TODO this is also the place, where it should decide what engine to use
		commEngine = new XmppMessageEngine(objectId, password, config, logger, this);
		
		// TODO load the event channels and actions - either from a file or server
		
	}
	
	
	/**
	 * Retrieves the object ID used for this connection.
	 *   
	 * @return Object ID.
	 */
	public String getObjectID() {
		return objectId;
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
		return commEngine.connect(objectId, password);
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
	public StatusMessage startAction(String destinationOid, String actionId, String body, 
			Map<String, String> parameters) {
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_AID, actionId);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_STARTACTION, 
				destinationOid, 
				attributes, 
				parameters,
				body);

	}
	
	
	public StatusMessage updateTaskStatus(String actionId, String newStatus, String returnValue, 
			Map<String, String> parameters) {
		
		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		Action action = searchForAction(actionId);
		
		if (action == null) {
			
			statusCodeReason = new String("No such action " + actionId + ".");
			
			logger.warning(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_404_NOTFOUND, 
					CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason);
			
			return statusMessage;
		}
		
		if (!action.updateTask(newStatus, returnValue, parameters)) {
			
			statusCodeReason = "Running task of action " + actionId + " is not in a state allowing update, "
					+ "or the requested new state is not applicable.";
			
			logger.warning(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason);
			
			return statusMessage;
			
		}
		
		statusCodeReason = "Running task of action " + actionId + " was updated to " + newStatus + ".";
		
		logger.info(statusCodeReason);
		
		statusMessage = new StatusMessage(
				false, 
				CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + statusCodeReason);
		
		return statusMessage;
		
	}
	
	
	
	
	public StatusMessage retrieveTaskStatus(String destinationOid, String actionId, String taskId, 
			Map<String, String> parameters, String body) {
	
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_AID, actionId);
		attributes.put(NetworkMessageRequest.ATTR_TID, taskId);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETTASKSTATUS, 
				destinationOid, 
				attributes, 
				parameters,
				body);
		
	}
	
	
	
	
	public StatusMessage cancelRunningTask(String destinationOid, String actionId, String taskId, 
			Map<String, String> parameters, String body) {
		
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_AID, actionId);
		attributes.put(NetworkMessageRequest.ATTR_TID, taskId);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_CANCELTASK, 
				destinationOid, 
				attributes, 
				parameters,
				body);	
	}
	
	
	/**
	 * Sets the status of the {@link EventChannel EventChannel}. The status can be either active, or inactive, depending
	 * on the 'status' parameter. 
	 * 
	 * If no channel with given 'eventID' exists and the 'status' is set to true, it gets created. If the 'status' is
	 * false, the channel is not created if it does not exists previously.
	 * 
	 * @param eventId Event ID.
	 * @param active If true, the channel will be set to active status.
	 * @return If the channel was found and its status set, returns true. It will also return true, if the channel was
	 * not found, but was created (this is what happens when the status of the non existing channel is being set to
	 * {@link EventChannel#STATUS_ACTIVE active}). If the channel was not found and the status was being set to 
	 * {@link EventChannel#STATUS_INACTIVE inactive}, returns false.
	 */
	public StatusMessage setLocalEventChannelStatus(String eventId, boolean active, Map<String, String> parameters, 
			String body) {
		
		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// search for given event channel
		EventChannel eventChannel = searchForEventChannel(eventId);
		
		if (eventChannel == null) {
			// TODO don't allow this behaviour - if there is no such event channel, stop the processing
			
			// if no event channel was found AND the caller wanted it to be active, create it
			if (active) {
				providedEventChannels.add(new EventChannel(objectId, eventId, true));
				
				statusCodeReason = new String("Object '" + objectId + "' created active event channel '" + eventId + "'");
				logger.info(statusCodeReason);
				
				statusMessage = new StatusMessage(
						false, 
						CodesAndReasons.CODE_200_OK, 
						CodesAndReasons.REASON_200_OK + statusCodeReason);
				
				return statusMessage;
				
				
			} else {
				statusCodeReason = new String("Could not deactivate the event channel '" + eventId + "' of the object '" + objectId 
						+ "'. The event channel does not exist.");
				
				logger.info(statusCodeReason);
				
				statusMessage = new StatusMessage(
						true, 
						CodesAndReasons.CODE_404_NOTFOUND, 
						CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason);
				
				return statusMessage;
			}
			
		}
		
		eventChannel.setActive(active);
		
		statusCodeReason = new String("Object '" + objectId + "' changed the activity of event channel '" 
					+ eventId + "' to " + active);
		
		logger.info(statusCodeReason);
		statusMessage = new StatusMessage(
				false, 
				CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + statusCodeReason);
		
		return statusMessage;
	}
	
	
	
	// TODO documentation
	public StatusMessage getEventChannelStatus(String destinationOid, String eventId, Map<String, String> parameters, 
			String body) {
		
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// when the owner wants to check its own status, there is no need to send it across the network
		if (destinationOid.equals(this.objectId)) {

			EventChannel eventChannel = searchForEventChannel(eventId);
			
			if (eventChannel == null) {
				statusCodeReason = new String("Received a request to provide status of invalid event channel. Request came locally from: "
						+ this.objectId);
				
				logger.info(statusCodeReason);
				
				statusMessage = new StatusMessage(
						true, 
						CodesAndReasons.CODE_404_NOTFOUND, 
						CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason);
				
				return statusMessage;

			} else {
				
				statusCodeReason = new String("Received a request to provide status of event channel " 
						 + eventId + " from " + this.getObjectID() + "(owner).");
				
				statusMessage = new StatusMessage(
						false, 
						CodesAndReasons.CODE_200_OK, 
						CodesAndReasons.REASON_200_OK + statusCodeReason);
				
				// also include that we are not subscribed to our channel
				JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
				JsonObjectBuilder jsonBuilder = jsonBuilderFactory.createObjectBuilder();
				
				jsonBuilder.add(EventChannel.ATTR_ACTIVE, eventChannel.isActive());
				jsonBuilder.add(EventChannel.ATTR_SUBSCRIBED, false);
				
				statusMessage.addMessageJson(jsonBuilder);
				
			}
			
			return statusMessage;
		} 
		
		// otherwise if it is not local continue as normal
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_EID, eventId);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS, 
				destinationOid, 
				attributes, 
				parameters,
				body);	
	}
	
	
	// TODO documentation
	public StatusMessage subscribeToEventChannel(String destinationOid, String eventId, Map<String, String> parameters,
			String body) {
		
		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// first check whether or not this is an attempt to subscribe to our own channel and stop if yes
		if (destinationOid.equals(this.objectId)) {
			statusCodeReason = new String("Can't subscribe to one's own event channel. Object ID " 
					+ destinationOid + ", event ID " + eventId);
			
			logger.info(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason);
			
			return statusMessage;
		}
		
		// check whether or not we are already subscribed
		Subscription subscription = searchForSubscription(destinationOid);
		
		if (subscription != null) {
			if (subscription.subscriptionExists(eventId)) {
				
				statusCodeReason = 
						new String("Already subscribed to " + destinationOid + " event channel " + eventId + ".");
				
				logger.info(statusCodeReason);
				
				statusMessage = new StatusMessage(
						false, 
						CodesAndReasons.CODE_200_OK, 
						CodesAndReasons.REASON_200_OK + statusCodeReason);
				
				return statusMessage;
			}
		} else {
			// create a new subscription object (but don't actually add the subscription to a concrete event yet)
			subscription = new Subscription(destinationOid);
		}
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_EID, eventId);
		
		statusMessage = sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL, 
				destinationOid, 
				attributes, 
				parameters, 
				body);
	
		if (!statusMessage.isError()) {
			// keep the track
			subscription.addToSubscriptions(eventId);
			subscribedEventChannels.add(subscription);
		}
		
		return statusMessage;
	}
	
	
	
	// TODO documentation
	public StatusMessage unsubscribeFromEventChannel(String destinationOid, String eventId, 
			Map<String, String> parameters, String body) {
		
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// first check whether or not this is an attempt to subscribe to our own channel and stop if yes
		if (destinationOid.equals(this.objectId)) {
			statusCodeReason = new String("Can't unsubscribe from one's own event channel. Object ID " 
					+ destinationOid + ", event ID " + eventId);
			
			logger.info(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason);
			
			return statusMessage;
		}
		
		// check whether or not we are already subscribed
		Subscription subscription = searchForSubscription(destinationOid);
		
		if (subscription == null || !subscription.subscriptionExists(eventId)) {
			
			statusCodeReason = 
					new String("No subscription to " + destinationOid + " event channel " + eventId + " exists.");
			
			logger.info(statusCodeReason);
			
			statusMessage = new StatusMessage(
					false, 
					CodesAndReasons.CODE_200_OK, 
					CodesAndReasons.REASON_200_OK + statusCodeReason);
			
			return statusMessage;
		}
		
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_EID, eventId);
		
		statusMessage = sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL, 
				destinationOid, 
				attributes, 
				parameters, 
				body);
		
		if (!statusMessage.isError()) {
			// keep the track
			subscription.removeFromSubscriptions(eventId);
		}
		
		// clean up
		if (subscription.getNumberOfSubscriptions() == 0) {
			subscribedEventChannels.remove(subscription);
		}
		
		return statusMessage;
	}
	
	
	
	// TODO documentation
	public StatusMessage sendEventToSubscribers(String eventId, String body, Map<String, String> parameters) {
		
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// look up the event channel
		EventChannel eventChannel = searchForEventChannel(eventId);
		
		if (eventChannel == null || !eventChannel.isActive()) {
			
			statusCodeReason = new String("Could not distribute an event to the channel '" + eventId 
					+ "' of the object '" + objectId + "'. The event channel does not exist or is not active.");
			
			logger.info(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason);
			
			return statusMessage;
		}
		
		// create the message
		NetworkMessageEvent eventMessage = new NetworkMessageEvent(config, this.objectId, eventId, body, 
				parameters, logger);
		String message = eventMessage.buildMessageString();
		
		// keep track of number of sent messages
		int sentMessages = 0;
		
		// send them
		Set<String> subscribers = eventChannel.getSubscribersSet();
		for (String destinationOid : subscribers) {
			if(commEngine.sendMessage(destinationOid, message)) {
				sentMessages++;
			} else {
				logger.warning("ConnectionDescriptor#sendEventToSubscribers: Destination object ID " + destinationOid 
						+ " is not in the contact list of the sender " + this.objectId + ".");
			}
		}
		
		statusCodeReason = new String("Event " + eventId + " was successfully distributed to " 
				+ sentMessages + " out of " 
				+ subscribers.size() + " subscribers.");
		
		logger.info(statusCodeReason);
		
		statusMessage = new StatusMessage(
				false, 
				CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + statusCodeReason);
		
		return statusMessage;
	}
	
	
	
	/**
	 * Returns the number of subscribers for the {@link EventChannel EventChannel} specified by its event ID. 
	 * 
	 * @param eventId ID of the {@link EventChannel EventChannel}.
	 * @return Number of subscribers, or -1 if no such {@link EventChannel EventChannel} exists. 
	 */
	public int getNumberOfSubscribers(String eventId) {
		
		// look up the event channel
		EventChannel eventChannel = searchForEventChannel(eventId);
		
		if (eventChannel == null) {
			return -1;
		}
		
		return eventChannel.getSubscribersSet().size();
	}
	
	
	// TODO documentation
	public StatusMessage getPropertyOfRemoteObject(String destinationOid, String propertyId, 
			Map<String, String> parameters, String body) {
				
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_PID, propertyId);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETPROPERTYVALUE, 
				destinationOid, 
				attributes, 
				parameters, 
				body);

	}
	
	
	
	
	
	// TODO documentation
	public StatusMessage setPropertyOfRemoteObject(String destinationOid, String propertyId, String body,
			Map<String, String> parameters) {
				
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_PID, propertyId);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_SETPROPERTYVALUE, 
				destinationOid, 
				attributes, 
				parameters, 
				body);
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
	 * @param sourceOid Object ID of the sender.
	 * @param messageString Received message.
	 */
	public void processIncommingMessage(String sourceOid, String messageString){
		
		logger.finest("New message from " + sourceOid + ": " + messageString);
		
		// let's resolve the message 
		NetworkMessage networkMessage = messageResolver.resolveNetworkMessage(messageString);
		
		if (networkMessage != null){
			
			/* next time dont forget to put everywhere the source oids into the response messages omfg
			// just a check whether or not somebody was tampering the message (and forgot to do it properly)
			if (!sourceOid.equals(networkMessage.getSourceOid())) {
				logger.severe("ConnectionDescriptor#processIncommingMessage: The source OID "
						+ sourceOid + " returned by communication engine "
						+ "does not match the internal source OID in the message " + networkMessage.getSourceOid() 
						+ ". Possible message tampering! Discarding the message and aborting.");
				
				return;
			}*/

			switch (networkMessage.getMessageType()){
			
			case NetworkMessageRequest.MESSAGE_TYPE:
				logger.finest("The message is a request. Processing...");
				processMessageRequest(networkMessage);
				break;
				
			case NetworkMessageResponse.MESSAGE_TYPE:
				logger.finest("This message is a response. Adding to incoming queue - message count: " 
						+ messageQueue.size());
				processMessageResponse(networkMessage);
				break;
				
			case NetworkMessageEvent.MESSAGE_TYPE:
				logger.finest("This message is an event. Forwarding...");
				processMessageEvent(networkMessage);
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
	 * @param sourceOid ID of the object that sent the message.
	 * @param networkMessage Message parsed from the incoming message. 
	 */
	private void processMessageRequest(NetworkMessage networkMessage){
		
		// cast it to request message first (it is safe and also necessary)
		NetworkMessageRequest requestMessage = (NetworkMessageRequest) networkMessage;
		
		NetworkMessageResponse response;
		
		// create response and send it back
		switch (requestMessage.getRequestOperation()){
		
		// TODO move the send back lines to one single line after the persistence is finished
		case NetworkMessageRequest.OPERATION_CANCELTASK:
			
			response = respondToCancelRunningTask(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS:
			
			response = respondToEventChannelStatusQuery(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETLISTOFACTIONS:
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETLISTOFEVENTS:
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETLISTOFPROPERTIES:
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETPROPERTYVALUE:
			
			response = respondToGetObjectProperty(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETTASKSTATUS:
			
			response = respondToGetTaskStatus(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_SETPROPERTYVALUE:
			
			response = respondToSetObjectProperty(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_STARTACTION:
			
			response = respondToStartActionRequest(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL:
			
			response = respondToEventSubscriptionRequest(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
			
		case NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL:
			
			response = respondToCancelSubscriptionRequest(requestMessage);
			commEngine.sendMessage(requestMessage.getSourceOid(), response.buildMessageString());
			
			break;
		}
		
	}
	
	
	
	/**
	 * Processing method for {@link NetworkMessageResponse response} type of {@link NetworkMessage NetworkMessage}.
	 * 
	 * @param from ID of the object that sent the message.
	 * @param networkMessage Message parsed from the incoming message.
	 */
	private void processMessageResponse(NetworkMessage networkMessage){
		messageQueue.add(networkMessage);
	}
	
	
	
	// TODO documentation
	private void processMessageEvent(NetworkMessage networkMessage) {
		
		// cast it to event message first (it is safe and also necessary)
		NetworkMessageEvent eventMessage = (NetworkMessageEvent) networkMessage;
		
		logger.info("Event " + eventMessage.getEventId() + " arrived from " + eventMessage.getSourceOid() 
							+ ". Event body: " + eventMessage.getEventBody());
		
		
		// don't process the event if we are not subscribed to it
		Subscription subscription = searchForSubscription(eventMessage.getSourceOid());
		
		if (subscription != null && subscription.subscriptionExists(eventMessage.getEventId())) {
			
			NetworkMessageResponse response = agentConnector.forwardEventToObject(
					eventMessage.getSourceOid(),
					this.objectId,
					eventMessage.getEventId(), 
					eventMessage.getEventBody(),
					eventMessage.getParameters()
					);
			
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
	
	
	private NetworkMessageResponse respondToGetObjectProperty(NetworkMessageRequest requestMessage) {
		
		// call the agent connector
		NetworkMessageResponse response = agentConnector.getObjectProperty(
				requestMessage.getSourceOid(), 
				requestMessage.getDestinationOid(), 
				requestMessage.getAttributes().get(NetworkMessageRequest.ATTR_PID), 
				requestMessage.getRequestBody(), requestMessage.getParameters());
		
		// don't forget to set the correlation id so the other side can identify what 
		// request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
				
		return response;
		
	}
	
	
	private NetworkMessageResponse respondToSetObjectProperty(NetworkMessageRequest requestMessage) {
		// call the agent connector
		NetworkMessageResponse response = agentConnector.setObjectProperty(
				requestMessage.getSourceOid(), 
				requestMessage.getDestinationOid(), 
				requestMessage.getAttributes().get(NetworkMessageRequest.ATTR_PID), 
				requestMessage.getRequestBody(), requestMessage.getParameters());
		
		// don't forget to set the correlation id so the other side can identify what 
		// request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
				
		return response;
		
	}
	
	
	private NetworkMessageResponse respondToEventChannelStatusQuery(NetworkMessageRequest requestMessage) {
		
		String eventId = null;
		EventChannel eventChannel = null;
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		// the event ID should have been sent in attributes
		Map<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			eventId = attributesMap.get(NetworkMessageRequest.ATTR_EID);	
		}
		
		if (eventId != null) {
			eventChannel = searchForEventChannel(eventId);
		}
		
		if (eventChannel == null) {
			logger.info("Object ID " + this.objectId + " received a request to provide status of invalid event channel "
					+ eventId + ". Request came from: " + requestMessage.getSourceOid());
			
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
					+ "Invalid event channel specified.");

		} else {
			
			logger.fine("Object ID " + this.objectId + " received a request to provide status of event channel " 
					+ eventId + " from " + requestMessage.getSourceOid() + ".");
			
			response.setError(false);
			response.setResponseCode(CodesAndReasons.CODE_200_OK);
			response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Event channel status retrieved.");
			response.setResponseBody(createSimpleJsonString(EventChannel.ATTR_ACTIVE, eventChannel.isActive()));
			
		}

		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	private NetworkMessageResponse respondToEventSubscriptionRequest(NetworkMessageRequest requestMessage) {
		
		String eventId = null;
		EventChannel eventChannel = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		// the event ID should have been sent in attributes
		Map<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			eventId = attributesMap.get(NetworkMessageRequest.ATTR_EID);
		}
		
		// check whether the event channel exists
		if (eventId != null) {
			eventChannel = searchForEventChannel(eventId);
		}
		
		// check whether the object is in our roster and whether or not it is already in the list of subscribers
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		
		if (eventChannel == null || !eventChannel.isActive()) { // || !security check
			logger.info("Object ID " + this.objectId + " received a request for subscription to invalid event channel " + eventId 
					+ ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
					+ "Invalid event channel specified.");
			
		} else {
			logger.fine("Object ID " + this.objectId + " received a request for subscription to event channel " 
					+ eventId + " from " + requestMessage.getSourceOid() + ".");
			
			eventChannel.addToSubscribers(requestMessage.getSourceOid());
			
			response.setError(false);
			response.setResponseCode(CodesAndReasons.CODE_200_OK);
			response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Subscribed.");
		}
		
		// set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
		
	}
	
	
	
	// TODO documentation
	private NetworkMessageResponse respondToCancelSubscriptionRequest(NetworkMessageRequest requestMessage) {
		String eventId = null;
		EventChannel eventChannel = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		// the event ID should have been sent in attributes
		Map<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			eventId = attributesMap.get(NetworkMessageRequest.ATTR_EID);
		}
		
		// check whether the event channel exists
		if (eventId != null) {
			eventChannel = searchForEventChannel(eventId);
		}
		
		// check whether the object is in our roster and whether or not it is already in the list of subscribers
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		
		if (eventChannel == null || !eventChannel.isActive()) { // || !security check
			logger.info("Object ID " + this.objectId + " received a request to cancel subscription to invalid event "
					+ "channel " + eventId + ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
					+ "Invalid event channel specified.");
		} else {
			
			logger.fine("Object ID " + this.objectId + " received a request to cancel subscription to event channel " 
					+ eventId + " from " + requestMessage.getSourceOid() + ".");
			
			eventChannel.removeFromSubscribers(requestMessage.getSourceOid());
			
			response.setError(false);
			response.setResponseCode(CodesAndReasons.CODE_200_OK);
			response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Unsubscribed.");
		}
		

		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	// TODO documentation
	private NetworkMessageResponse respondToStartActionRequest(NetworkMessageRequest requestMessage) {
		
		String actionId = null;
		Action action = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		// the action ID should have been sent in attributes
		Map<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			actionId = attributesMap.get(NetworkMessageRequest.ATTR_AID);
		}
		
		// check whether the action exists
		if (actionId != null) {
			action = searchForAction(actionId);
		}
		
		// TODO delete this workaround - the actions should be loaded at the startup and no new action should 
		// be possible
		if (action == null) {
			action = new Action (config, this.objectId, actionId, agentConnector, logger);
			providedActions.add(action);
		}
		
		String statusString;
		
		// check whether the object is in our roster and whether or not the action already exists
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		if (action == null ) { // || security check
			logger.info("Object ID " + this.objectId + " received a request to start non existing action " + actionId 
					+ ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
								+ "Invalid action specified.");
		} else {
			logger.fine("Object ID " + this.objectId + " received a request to start action " + actionId + " from " 
						+ requestMessage.getSourceOid() + ".");
			
			String taskId = action.createNewTask(requestMessage.getSourceOid(), 
					requestMessage.getRequestBody(), requestMessage.getParameters());
			
			if (taskId == null) {
				
				logger.warning("Cannot start action " + actionId + ", too many tasks in queue.");
				
				// responding with error
				response.setError(true);
				response.setResponseCode(CodesAndReasons.CODE_503_SERVICEUNAVAILABLE);
				response.setResponseCodeReason(CodesAndReasons.REASON_503_SERVICENAVAILABLE 
						+ "Too many tasks waiting in queue.");

			} else {
				
				logger.fine("Object " + this.objectId + " created task " + taskId + " of action " 
						+ actionId + " and added it to the queue.");
				
				response.setError(false);
				response.setResponseCode(CodesAndReasons.CODE_201_CREATED);
				response.setResponseCodeReason(CodesAndReasons.REASON_201_CREATED + "New task added to the queue.");
				response.setResponseBody(createSimpleJsonString(Action.ATTR_TASKID, taskId));
			}
		}
		
		// set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	
	private NetworkMessageResponse respondToGetTaskStatus(NetworkMessageRequest requestMessage) {
		String actionId = null;
		String taskId = null;
		Action action = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		// the action ID should have been sent in attributes
		Map<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			actionId = attributesMap.get(NetworkMessageRequest.ATTR_AID);
			taskId = attributesMap.get(NetworkMessageRequest.ATTR_TID);
		}
		
		// check whether the action exists
		if (actionId != null) {
			action = searchForAction(actionId);
		}
		
		
		String statusString;
		
		// check whether the object is in our roster and whether or not the action already exists
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		if (action == null ) { // || security check
			logger.info("Object ID " + this.objectId + " received a request for status report on non existing action " 
					+ actionId + ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
								+ "Invalid action specified.");
		} else {
			logger.fine("Object ID " + this.objectId + " received a request for status report on action " + actionId 
					+ " task " + taskId + " from " + requestMessage.getSourceOid() + ".");
			
			byte statusCode = action.getTaskStatus(taskId);

			if (statusCode == Task.TASKSTATUS_UNKNOWN) {
				
				statusString = new String("Object ID " + this.objectId + " can't find task " + taskId 
						+ ". Its status is unknown.");
				
				logger.warning(statusString);
				
				// responding with error
				response.setError(true);
				response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
				response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
									+ "Invalid task specified.");
			} else {
				
				logger.fine("Object ID " + this.objectId + " task " + taskId + " of action " + actionId 
						+ " status " + action.getTaskStatusString(taskId) + ".");
				
				response.setError(false);
				response.setResponseCode(CodesAndReasons.CODE_200_OK);
				response.setResponseCodeReason(CodesAndReasons.REASON_200_OK 
									+ "Task status retrieved.");
				
				response.setResponseBody(action.createTaskStatusJson(taskId).toString());
			}
		}
		
		// set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
		
	}
	
	
	
	private NetworkMessageResponse respondToCancelRunningTask(NetworkMessageRequest requestMessage) {
		String actionId = null;
		String taskId = null;
		Action action = null;
		
		// this is a network message used to encapsulate the status message
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		// the action ID should have been sent in attributes
		Map<String, String> attributesMap = requestMessage.getAttributes();
		if (!attributesMap.isEmpty()) {
			actionId = attributesMap.get(NetworkMessageRequest.ATTR_AID);
			taskId = attributesMap.get(NetworkMessageRequest.ATTR_TID);
		}
		
		// check whether the action exists
		if (actionId != null) {
			action = searchForAction(actionId);
		}
		
		
		// check whether the object is in our roster and whether or not the action already exists
		// TODO refuse to work if the object is not in the roster - this would need a new class to observe security
		if (action == null ) { // || security check
			logger.info("Object ID " + this.objectId + " received a request for stopping a task of non existing action " 
					+ actionId + ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
								+ "Invalid action specified.");
		} else {
			logger.fine("Object ID " + this.objectId + " received a request for stopping an action ID " 
					+ actionId + " task ID " + taskId + " from object ID " + requestMessage.getSourceOid() + ".");
			
			response = action.cancelTask(taskId, requestMessage.getRequestBody(), requestMessage.getParameters());
			
			if (response == null) {
				
				logger.warning("Task ID " + taskId + " is in a state that does not allow it to be cancelled. It either does not exist, is already finished, or failed.");
				
				response = new NetworkMessageResponse(config, logger);
				
				// responding with error
				response.setError(false);
				response.setResponseCode(CodesAndReasons.CODE_200_OK);
				response.setResponseCodeReason(CodesAndReasons.REASON_200_OK 
									+ "Invalid task specified.");
			} else {
				if (response.isError()) {
					logger.warning("Object ID " + this.objectId + " received an error from agent connector while "
							+ "attempting to stop task ID " + taskId + ". Code " + response.getResponseCode() 
							+ " reason " + response.getResponseCodeReason());
				} else {
					logger.info("Object ID " + this.objectId + " task " + taskId + "stopped.");
				}
				
			}
		}
		
		// set the correlation id so the other side can identify what request does this response belong to
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
	
	
	private Action searchForAction(String actionId) {
		
		if (actionId == null) {
			return null;
		}
		
		// search for given action
		
		for (Action action : providedActions) {
			if (action.getActionId().equals(actionId)) {
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
							> (config.getInt(NetworkMessage.CONFIG_PARAM_REQUESTMESSAGETIMEOUT, 
									NetworkMessage.CONFIG_DEF_REQUESTMESSAGETIMEOUT)*1000));
			
		// until we get our message or the timeout expires
		} while (message == null && !timeoutReached);
	
		return message;	
	}
	
	
	private StatusMessage sendRequestForRemoteOperation(byte operationId, String destinationOid, 
			Map<String, String> attributes, Map<String, String> parameters, String body) {
		
		if (destinationOid == null) {
			return null;
		}

		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		NetworkMessageRequest request = new NetworkMessageRequest(config, logger);
		
		// we will need this newly generated ID, so we keep it
		int requestId = request.getRequestId();
		
		// now fill the thing
		request.setRequestOperation(operationId);
		
		request.setSourceOid(this.objectId);
		request.setDestinationOid(destinationOid);
		
		request.setAttributes(attributes);
		
		request.setParameters(parameters);
		
		request.setRequestBody(body);
		
		
		// all set
		if (!commEngine.sendMessage(destinationOid, request.buildMessageString())){
			
			// TODO or something else wrong happened make a security check for outgoing messages
			statusCodeReason = new String("Destination object " + destinationOid 
					+ " is not in the list of available objects or it was not possible to send the message.");
			
			logger.warning(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_404_NOTFOUND, 
					CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason);
			
			return statusMessage;
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		// nothing came through
		if (response == null){

			statusCodeReason = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectId + " Destination ID: " + destinationOid + " Request ID: " + requestId);
			
			logger.warning(statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_408_REQUESTTIMEOUT, 
					CodesAndReasons.REASON_408_REQUESTTIMEOUT + statusCodeReason); 
			
			return statusMessage;
		}
		
		// response arrived
		statusMessage = new StatusMessage(
				response.isError(),
				response.getResponseCode(),
				response.getResponseCodeReason()
				);
		
		JsonObject json = messageResolver.readJsonObject(response.getResponseBody());
		if (json != null) {
			statusMessage.addMessageJson(json);
		} else {
			logger.info("It was not possible to turn response body into a valid JSON. Original string: " 
							+ response.getResponseBody());
		}
		
		
		return statusMessage;
		
	}
	
	
	private String createSimpleJsonString(String key, String value) {
		// if key is null, there is no need to bother... 
		if (key == null) {
			return null;
		}
			
		JsonObjectBuilder builder = jsonBuilderFactory.createObjectBuilder();
		
		if (value == null) {
			builder.addNull(key);
		} else {
			builder.add(key, value);
		}
		
		return builder.build().toString();
	}
	
	
	private String createSimpleJsonString(String key, boolean value) {
		// if key is null, there is no need to bother... 
		if (key == null) {
			return null;
		}
			
		JsonObjectBuilder builder = jsonBuilderFactory.createObjectBuilder();
		
		builder.add(key, value);
		
		return builder.build().toString();
	}
	
}
