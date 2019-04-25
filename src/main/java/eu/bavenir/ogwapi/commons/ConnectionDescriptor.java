package eu.bavenir.ogwapi.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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
import eu.bavenir.ogwapi.commons.search.SemanticQuery;
import eu.bavenir.ogwapi.commons.search.SparqlQuery;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * A representation of a connection to network. In essence, it is a client connected into the P2P network through an 
 * instance of {@link eu.bavenir.ogwapi.commons.engines.CommunicationEngine engine}, able to 
 * send / receive messages and process the requests that arrive in them. Each object ID connected through this
 * OGWAPI has its own instance of this class, however all share the same {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}
 * instance to connect to local infrastructure.  
 * 
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
	
	/**
	 * ID of the object connected through this ConnectionDescriptor
	 */
	private String objectId;
	
	/**
	 * Configuration of the OGWAPI.
	 */
	private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * Sparql query search engine.
	 */
	private SparqlQuery sparql;
	
	/**
	 * Semantic query search engine.
	 */
	private SemanticQuery semantic;
	
	/**
	 * Message resolver for incoming messages. 
	 */
	private MessageResolver messageResolver;
	
	/**
	 * The thing that communicates with an agent.
	 */
	private AgentConnector agentConnector;
	
	/**
	 * Reference to the CommunicationManager that spawned this ConnectionDescriptor. 
	 */
	private CommunicationManager commManager;
	
	/**
	 * Password.
	 */
	private String password;
	
	/**
	 * Necessary for passive session recovery setting.
	 */
	private long lastConnectionTimerReset;
	
	/**
	 * Message queue, FIFO structure for holding incoming messages. 
	 */
	private BlockingQueue<NetworkMessage> messageQueue;
	
	/**
	 * The communication engine to use.
	 */
	private CommunicationEngine commEngine;
	
	/**
	 * Factory for JSON builders.
	 */
	private JsonBuilderFactory jsonBuilderFactory;
	
	/**
	 * Data class
	 */
	private Data data;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * the descriptor will not be able to connect (in the best case scenario, the other being a storm of null pointer 
	 * exceptions).
	 * 
	 * @param objectId Object ID used by this ConnectionDescriptor.
	 * @param password Password to access the network.
	 * @param config OGWAPI configuration.
	 * @param logger OGWAPI logger.
	 * @param commManager The main CommunicationManager.
	 */
	public ConnectionDescriptor(String objectId, String password, XMLConfiguration config, Logger logger, 
			CommunicationManager commManager){
		
		this.objectId = objectId;
		this.password = password;
		
		this.config = config;
		this.logger = logger;
		
		this.commManager = commManager;
		
		this.sparql = new SparqlQuery(config, this, logger);
		this.semantic = new SemanticQuery(config, logger);
		
		// TODO decide here what type of connector to use
		agentConnector = new RestAgentConnector(config, logger);
		
		messageQueue = new LinkedTransferQueue<NetworkMessage>();
		
		messageResolver = new MessageResolver(config, logger);
		
		jsonBuilderFactory = Json.createBuilderFactory(null);
		
		// build new connection
		// TODO this is also the place, where it should decide what engine to use
		commEngine = new XmppMessageEngine(objectId, password, config, logger, this);
		
		// load the event channels and actions - either from a file or server
		data = new Data(objectId, config, logger);
	}
	
	/**
	 * Retrieves the object ID used for this connection.
	 *   
	 * @return Object ID.
	 */
	public String getObjectId() {
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
	
	
	/**
	 * Connects the object into to the network with provided credentials. 
	 *  
	 * @return True if the connection was successful, false otherwise.
	 */
	public boolean connect(){
		
		lastConnectionTimerReset = System.currentTimeMillis();
		
		return commEngine.connect();
	}
	
	
	/**
	 * Disconnects the object from the network. 
	 */
	public void disconnect(){
		commEngine.disconnect();
	}


	/**
	 * Verifies the object is connected. 
	 * 
	 * @return True if it is connected, false otherwise. 
	 */
	public boolean isConnected(){
		
		return commEngine.isConnected();
	}
	
	
	/**
	 * The connection timer is important in some levels of the OGWAPI's session recovery settings 
	 * (see {@link eu.bavenir.ogwapi.commons.CommunicationManager#CONFIG_PARAM_SESSIONRECOVERY CONFIG_PARAM_SESSIONRECOVERY}).
	 * 
	 * The {@link eu.bavenir.ogwapi.commons.CommunicationManager CommunicationManager} uses this method to reset the timer.
	 */
	public void resetConnectionTimer() {
		lastConnectionTimerReset = System.currentTimeMillis();
	}
	
	
	/**
	 * The connection timer is important in some levels of the OGWAPI's session recovery settings 
	 * (see {@link eu.bavenir.ogwapi.commons.CommunicationManager#CONFIG_PARAM_SESSIONRECOVERY CONFIG_PARAM_SESSIONRECOVERY}).
	 * 
	 * The {@link eu.bavenir.ogwapi.commons.CommunicationManager CommunicationManager} uses this method to retrieve 
	 * the time of the last timer reset.
	 */
	public long getLastConnectionTimerReset() {
		return lastConnectionTimerReset;
	}
	
	

	/**
	 * Retrieves the contact list for this object.
	 * 
	 * @return Set of object IDs that this object can see.
	 */
	public Set<String> getRoster(){
		
		return commEngine.getRoster();
	}
	
	
	/**
	 * Starts an action on a remote object.
	 * 
	 * @param destinationOid ID of the remote object.
	 * @param actionId ID of the action.
	 * @param body Body that will be transported to the object via its {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 * @param parameters Parameters that will be transported along the body. 
	 * @return Status message.
	 */
	public StatusMessage startAction(String destinationOid, String actionId, String body, 
			Map<String, String> parameters) {
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_AID, actionId);
		
		logger.info(this.objectId + ": Sending request to start action " + actionId + " on " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_STARTACTION, 
				destinationOid, 
				attributes, 
				parameters,
				body);

	}
	
	
	/**
	 * Updates a status and a return value of locally run job of an action. See the {@link eu.bavenir.ogwapi.commons.Task Task}
	 * and {@link eu.bavenir.ogwapi.commons.Action Action} for more information about valid states.  
	 * 
	 * @param actionId ID of the action that is being worked on right now (this is NOT a task ID).
	 * @param newStatus New status of the job. 
	 * @param returnValue New value to be returned to the object that ordered the job.
	 * @param parameters Parameters that goes with the return value.
	 * @return Status message.
	 */
	public StatusMessage updateTaskStatus(String actionId, String newStatus, String returnValue, 
			Map<String, String> parameters) {
		
		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		Action action = searchForAction(actionId);
		
		if (action == null) {
			
			statusCodeReason = new String("No such action " + actionId + ".");
			
			logger.warning(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_404_NOTFOUND, 
					CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
			return statusMessage;
		}
		
		if (!action.updateTask(newStatus, returnValue, parameters)) {
			
			statusCodeReason = new String("Running task of action " + actionId + " is not in a state allowing update, "
					+ "or the requested new state is not applicable.");
			
			logger.warning(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
			return statusMessage;
			
		}
		
		statusCodeReason = new String ("Running task of action " + actionId + " was updated to " + newStatus + ".");
		
		logger.info(this.objectId + ": " + statusCodeReason);
		
		statusMessage = new StatusMessage(
				false, 
				CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + statusCodeReason,
				StatusMessage.CONTENTTYPE_APPLICATIONJSON);
		
		return statusMessage;
		
	}
	
	
	/**
	 * Retrieves a status of a remotely running {@link eu.bavenir.ogwapi.commons.Task Task}.
	 * 
	 * @param destinationOid The remote object running the task. 
	 * @param actionId ID of the action.
	 * @param taskId ID of the task.
	 * @param parameters Any parameters that are necessary to be transported to other side (usually none).
	 * @param body Any body that needs to be transported to the other side (usually none).
	 * @return Status message.
	 */
	public StatusMessage retrieveTaskStatus(String destinationOid, String actionId, String taskId, 
			Map<String, String> parameters, String body) {
	
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_AID, actionId);
		attributes.put(NetworkMessageRequest.ATTR_TID, taskId);
		
		logger.info(this.objectId + ": Sending request to retrieve status of task " + taskId + " of action " + actionId 
				+ " on " + destinationOid + " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETTASKSTATUS, 
				destinationOid, 
				attributes, 
				parameters,
				body);
		
	}
	
	
	
	/**
	 * Cancels remotely running task.
	 * 
	 * @param destinationOid The remote object running the task. 
	 * @param actionId ID of the action.
	 * @param taskId ID of the task.
	 * @param parameters Any parameters that are necessary to be transported to other side (usually none).
	 * @param body Any body that needs to be transported to the other side (usually none).
	 * @return Status message.
	 */
	public StatusMessage cancelRunningTask(String destinationOid, String actionId, String taskId, 
			Map<String, String> parameters, String body) {
		
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_AID, actionId);
		attributes.put(NetworkMessageRequest.ATTR_TID, taskId);
		
		logger.info(this.objectId + ": Sending request to cancel task " + taskId + " of action " + actionId 
				+ " on " + destinationOid + " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
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
	 * If no channel with given 'eventId' exists and the 'status' is set to true, it gets created. If the 'status' is
	 * false, the channel is not created if it does not exists previously.
	 * 
	 * @param eventId Event ID.
	 * @param active If true, the channel will be set to active status.
	 * @return If the channel was found and its status set, returns true. It will also return true, if the channel was
	 * not found, but was created (this is what happens when the status of the non existing channel is being set to
	 * {@link EventChannel#STATUS_ACTIVE active}). If the channel was not found and the status was being set to 
	 * {@link EventChannel#STATUS_INACTIVE inactive}, returns false. It gets wrapped into Status message.
	 */
	public StatusMessage setLocalEventChannelStatus(String eventId, boolean active, Map<String, String> parameters, 
			String body) {
		
		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// search for given event channel
		EventChannel eventChannel = searchForEventChannel(eventId);
		
		if (eventChannel == null) {
			
			// if no event channel was found AND the caller wanted it to be active, create it
			if (active) {
				data.addProvidedEventChannel(new EventChannel(objectId, eventId, true));
				
				statusCodeReason = new String("Created active event channel " + eventId + ".");
				logger.info(this.objectId + ": " + statusCodeReason);
				
				statusMessage = new StatusMessage(
						false, 
						CodesAndReasons.CODE_200_OK, 
						CodesAndReasons.REASON_200_OK + statusCodeReason,
						StatusMessage.CONTENTTYPE_APPLICATIONJSON);
				
				return statusMessage;
				
				
			} else {
				statusCodeReason = new String("Could not deactivate the event channel " 
						+ eventId + ". The event channel does not exist.");
				
				logger.warning(this.objectId + ": " + statusCodeReason);
				
				statusMessage = new StatusMessage(
						true, 
						CodesAndReasons.CODE_404_NOTFOUND, 
						CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason,
						StatusMessage.CONTENTTYPE_APPLICATIONJSON);
				
				return statusMessage;
			}
			
		}
		
		// this change of setting will not be written to the file (persistence)
		eventChannel.setActive(active);
		// write to the file manually (persistence)
		data.saveData();
		
		statusCodeReason = new String("Changed the activity of event channel " 
					+ eventId + " to " + active);
		
		logger.info(this.objectId + ": " + statusCodeReason);
		statusMessage = new StatusMessage(
				false, 
				CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + statusCodeReason,
				StatusMessage.CONTENTTYPE_APPLICATIONJSON);
		
		return statusMessage;
	}
	
	
	/**
	 * Retrieves the status of local or remote event channel. 
	 * 
	 * @param destinationOid ID of the object that is to be polled (it can be the local owner verifying the state of its channel.
	 * @param eventId ID of the event.
	 * @param parameters Any parameters to be sent (usually none).
	 * @param body Any body to be sent (usually none).
	 * @return Status message.
	 */
	public StatusMessage getEventChannelStatus(String destinationOid, String eventId, Map<String, String> parameters, 
			String body) {
		
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// when the owner wants to check its own status, there is no need to send it across the network
		if (destinationOid.equals(this.objectId)) {

			EventChannel eventChannel = searchForEventChannel(eventId);
			
			if (eventChannel == null) {
				statusCodeReason = new String("Local request for providing status of invalid event "
						+ "channel was received.");
				
				logger.info(this.objectId + ": " + statusCodeReason);
				
				statusMessage = new StatusMessage(
						true, 
						CodesAndReasons.CODE_404_NOTFOUND, 
						CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason,
						StatusMessage.CONTENTTYPE_APPLICATIONJSON);
				
				return statusMessage;

			} else {
				
				statusCodeReason = new String("Local request for providing status of event channel " 
						 + eventId + ".");
				
				logger.info(this.objectId + ": " + statusCodeReason);
				
				statusMessage = new StatusMessage(
						false, 
						CodesAndReasons.CODE_200_OK, 
						CodesAndReasons.REASON_200_OK + statusCodeReason,
						StatusMessage.CONTENTTYPE_APPLICATIONJSON);
				
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
		
		logger.info(this.objectId + ": Sending request to get status of remote event channel " + eventId + " on " 
					+ destinationOid + " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS, 
				destinationOid, 
				attributes, 
				parameters,
				body);	
	}
	
	
	/**
	 * Subscribes the current object to the destination object ID's {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 *  
	 * @param destinationOid ID of the object to which event channel a subscription is attempted.
	 * @param eventId Event ID.
	 * @param parameters Any parameters to be sent (usually none).
	 * @param body Any body to be sent (usually none).
	 * @return Status message.
	 */
	public StatusMessage subscribeToEventChannel(String destinationOid, String eventId, Map<String, String> parameters,
			String body) {
		
		// message to be returned
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// first check whether or not this is an attempt to subscribe to our own channel and stop if yes
		if (destinationOid.equals(this.objectId)) {
			statusCodeReason = new String("Can't subscribe to one's own event channel. Event ID " + eventId);
			
			logger.info(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
			return statusMessage;
		}
		
		// check whether or not we are already subscribed
		Subscription subscription = searchForSubscription(destinationOid);
		
		if (subscription != null) {
			if (subscription.subscriptionExists(eventId)) {
				
				statusCodeReason = 
						new String("Already subscribed to " + destinationOid + " event channel " + eventId + ".");
				
				logger.info(this.objectId + ": " + statusCodeReason);
				
				statusMessage = new StatusMessage(
						false, 
						CodesAndReasons.CODE_200_OK, 
						CodesAndReasons.REASON_200_OK + statusCodeReason,
						StatusMessage.CONTENTTYPE_APPLICATIONJSON);
				
				return statusMessage;
			}
		} else {
			// create a new subscription object (but don't actually add the subscription to a concrete event yet)
			subscription = new Subscription(destinationOid);
		}
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_EID, eventId);
		
		logger.info(this.objectId + ": Sending request for subscribing to remote event channel " + eventId + " on " 
				+ destinationOid + " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		statusMessage = sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL, 
				destinationOid, 
				attributes, 
				parameters, 
				body);
	
		if (!statusMessage.isError()) {
			// keep the track
			subscription.addToSubscriptions(eventId);
			data.addSubscribedEventChannel(subscription);
		}
		
		return statusMessage;
	}
	
	
	
	/**
	 * Un-subscribes the current object from the destination object ID's {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 *  
	 * @param destinationOid ID of the object from which event channel a un-subscription is attempted.
	 * @param eventId Event ID.
	 * @param parameters Any parameters to be sent (usually none).
	 * @param body Any body to be sent (usually none).
	 * @return Status message.
	 */
	public StatusMessage unsubscribeFromEventChannel(String destinationOid, String eventId, 
			Map<String, String> parameters, String body) {
		
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// first check whether or not this is an attempt to subscribe to our own channel and stop if yes
		if (destinationOid.equals(this.objectId)) {
			statusCodeReason = new String("Can't unsubscribe from one's own event channel. Event ID " + eventId);
			
			logger.info(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
			return statusMessage;
		}
		
		// check whether or not we are already subscribed
		Subscription subscription = searchForSubscription(destinationOid);
		
		if (subscription == null || !subscription.subscriptionExists(eventId)) {
			
			statusCodeReason = 
					new String("No subscription to " + destinationOid + " event channel " + eventId + " exists.");
			
			logger.info(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					false, 
					CodesAndReasons.CODE_200_OK, 
					CodesAndReasons.REASON_200_OK + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
			return statusMessage;
		}
		
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_EID, eventId);
		
		logger.info(this.objectId + ": Sending request to cancel subscription to remote event channel " + eventId + " on " 
				+ destinationOid + " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		statusMessage = sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL, 
				destinationOid, 
				attributes, 
				parameters, 
				body);
		
		if (!statusMessage.isError()) {
			// keep the track
			subscription.removeFromSubscriptions(eventId);
			
			logger.fine(this.objectId + ": Unsubscribed from event channel " + eventId + " on " + destinationOid + ".");
		}
		
		// clean up
		if (subscription.getNumberOfSubscriptions() == 0) {
			data.removeSubscribedEventChannel(subscription);
		}
		
		return statusMessage;
	}
	
	
	/**
	 * Distributes an {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent event} to subscribers. 
	 * 
	 * @param eventId Event ID.
	 * @param body Body of the event.
	 * @param parameters Any parameters to be transported with the event body.
	 * @return Status message.
	 */
	public StatusMessage sendEventToSubscribers(String eventId, String body, Map<String, String> parameters) {
		
		String statusCodeReason;
		StatusMessage statusMessage;
		
		// look up the event channel
		EventChannel eventChannel = searchForEventChannel(eventId);
		
		if (eventChannel == null || !eventChannel.isActive()) {
			
			statusCodeReason = new String("Could not distribute an event to the channel " + eventId 
					+ ". The event channel does not exist or is not active.");
			
			logger.info(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_400_BADREQUEST, 
					CodesAndReasons.REASON_400_BADREQUEST + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
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
			if(sendMessage(this.objectId, destinationOid, message)) {
				sentMessages++;
			} else {
				logger.warning(this.objectId + ": Destination object ID " + destinationOid 
						+ " is not in the contact list during event distribution.");
			}
		}
		
		statusCodeReason = new String("Event " + eventId + " was successfully distributed to " 
				+ sentMessages + " out of " 
				+ subscribers.size() + " subscribers.");
		
		logger.info(this.objectId + ": " + statusCodeReason);
		
		statusMessage = new StatusMessage(
				false, 
				CodesAndReasons.CODE_200_OK, 
				CodesAndReasons.REASON_200_OK + statusCodeReason,
				StatusMessage.CONTENTTYPE_APPLICATIONJSON);
		
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
			
			logger.warning(this.objectId + ": No such event channel when checking for number of subscribers " + eventId);
			return -1;
		}
		
		int size = eventChannel.getSubscribersSet().size();
		
		logger.info(this.objectId + ": Received a request to retreive number of subscribers on event channel " + eventId);
		
		return size;
	}
	
	/**
	 * Retrieves a events of a remote object. 
	 * 
	 * @param destinationOid ID of the object that owns the events. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getEventsOfRemoteObject(String destinationOid, 
			Map<String, String> parameters, String body) {
				
		Map<String, String> attributes = new HashMap<String,String>();
		
		logger.info(this.objectId + ": Sending request to get events of remote object " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETLISTOFEVENTS, 
				destinationOid, 
				attributes, 
				parameters, 
				body);

	}
	
	/**
	 * Retrieves a actions of a remote object. 
	 * 
	 * @param destinationOid ID of the object that owns the actions. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getActionsOfRemoteObject(String destinationOid, 
			Map<String, String> parameters, String body) {
				
		Map<String, String> attributes = new HashMap<String,String>();
		
		logger.info(this.objectId + ": Sending request to get actions of remote object " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETLISTOFACTIONS, 
				destinationOid, 
				attributes, 
				parameters, 
				body);

	}
	
	/**
	 * Retrieves a thing description of a remote object. 
	 * 
	 * @param destinationOid ID of the object that owns the thing description. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getThingDescriptionOfRemoteObject(String destinationOid, 
			Map<String, String> parameters, String body) {
				
		Map<String, String> attributes = new HashMap<String,String>();
		
		logger.info(this.objectId + ": Sending request to get thing description of remote object " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETTHINGDESCRIPTION, 
				destinationOid, 
				attributes, 
				parameters, 
				body);

	}
	
	/**
	 * Retrieves a properties of a remote object. 
	 * 
	 * @param destinationOid ID of the object that owns the properties. 
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getPropertiesOfRemoteObject(String destinationOid, 
			Map<String, String> parameters, String body) {
				
		Map<String, String> attributes = new HashMap<String,String>();
		
		logger.info(this.objectId + ": Sending request to get events of remote object " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETLISTOFPROPERTIES, 
				destinationOid, 
				attributes, 
				parameters, 
				body);

	}
	
	/**
	 * Retrieves a property of a remote object. 
	 * 
	 * @param destinationOid ID of the object that owns the property. 
	 * @param propertyId ID of the property.
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (if needed).
	 * @return Status message. 
	 */
	public StatusMessage getPropertyOfRemoteObject(String destinationOid, String propertyId, 
			Map<String, String> parameters, String body) {
				
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_PID, propertyId);
		
		logger.info(this.objectId + ": Sending request to get property " + propertyId + " of remote object " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_GETPROPERTYVALUE, 
				destinationOid, 
				attributes, 
				parameters, 
				body);

	}
	
	
	/**
	 * Sets a new value of a property on a remote object. 
	 * 
	 * @param destinationOid ID of the object that owns the property. 
	 * @param propertyId ID of the property.
	 * @param parameters Any parameters to be sent with the request (if needed).
	 * @param body Body to be sent (a new value will probably be stored here).
	 * @return Status message. 
	 */
	public StatusMessage setPropertyOfRemoteObject(String destinationOid, String propertyId, String body,
			Map<String, String> parameters) {
				
		
		Map<String, String> attributes = new HashMap<String,String>();
		attributes.put(NetworkMessageRequest.ATTR_PID, propertyId);
		
		logger.info(this.objectId + ": Sending request to set property " + propertyId + " of remote object " + destinationOid 
				+ " with parameters: \n" + parameters.toString() + "\nand body: \n" + body);
		
		return sendRequestForRemoteOperation(
				NetworkMessageRequest.OPERATION_SETPROPERTYVALUE, 
				destinationOid, 
				attributes, 
				parameters, 
				body);
	}
	
	
	/**
	 * This is a callback method called when a message arrives. There are three main scenarios that need to be handled:
	 * a. A {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest NetworkMessageRequest} arrives ('unexpected') 
	 * from another node with request for data or action - this need to be routed 
	 * to a specific end point on agent side and then the result needs to be sent back to originating node. 
	 * b. After sending a message with request to another node, the originating node expects an answer, which arrives
	 * as a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse NetworkMessageResponse}. This is stored in 
	 * a queue and is propagated back to originating services, that are expecting the results.
	 * c. A {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent NetworkMessageEvent} arrives and it is necessary
	 * to process it and forward to respective object via an {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 * 
	 * NOTE: This method is to be called by the {@link CommunicationEngine engine } subclass instance.
	 * 
	 * @param sourceOid Object ID of the sender.
	 * @param messageString Received message.
	 */
	public void processIncommingMessage(String sourceOid, String messageString){
		
		logger.info(this.objectId + ": New message from " + sourceOid);
		
		logger.fine(this.objectId + ": Message string: \n" + messageString + "\n");
		
		// let's resolve the message 
		NetworkMessage networkMessage = messageResolver.resolveNetworkMessage(messageString);
		
		if (networkMessage != null){
			
			// just a check whether or not somebody was tampering the message (and forgot to do it properly)
			if (!sourceOid.equals(networkMessage.getSourceOid())) {
				logger.warning(this.objectId + ": The source OID "
						+ sourceOid + " returned by communication engine "
						+ "does not match the internal source OID in the message " + networkMessage.getSourceOid() 
						+ ". Possible message tampering! Discarding the message and aborting.");
				
				return;
			}

			switch (networkMessage.getMessageType()){
			
			case NetworkMessageRequest.MESSAGE_TYPE:
				logger.info(this.objectId + ": The message is a request. Processing...");
				processMessageRequest(networkMessage);
				break;
				
			case NetworkMessageResponse.MESSAGE_TYPE:
				logger.info(this.objectId + ": This message is a response. Adding to incoming queue - message count: " 
						+ messageQueue.size());
				processMessageResponse(networkMessage);
				break;
				
			case NetworkMessageEvent.MESSAGE_TYPE:
				logger.info(this.objectId + ": This message is an event. Forwarding to agent...");
				processMessageEvent(networkMessage);
			}
		} else {
			logger.warning(this.objectId + ": Invalid message received from the network.");
		}
		
	}
	
	
	/**
	 * Performs a SPARQL search on all objects in the contact list.
	 * 
	 * @param query SPARQL query.
	 * @param parameters Any parameters (if needed).
	 * @return JSON with results. 
	 */
	public String performSparqlQuery(String query, Map<String, String> parameters) {
		
		if (query == null) {
			
			logger.warning(this.objectId + ": Can't execute null SPARQL query with parameters: \n" + parameters.toString());
			
			return null;
		}
		
		logger.info(this.objectId + ": Executing SPARQL query: \n" + query + "\nwith parameters: \n" + parameters.toString());
		
		return sparql.performQuery(query, parameters);
	}

	
	/**
	 * Performs a Semantic search 
	 * 
	 * @param query Semantic query.
	 * @param parameters Any parameters (if needed).
	 * @return JSON with results. 
	 */
	public String performSemanticQuery(String query, Map<String, String> parameters) {
		
		if (query == null) {
			
			logger.warning(this.objectId + ": Can't execute null Semantic query with parameters: \n" + parameters.toString());
			
			return null;
		}
		
		logger.info(this.objectId + ": Executing Semantic query: \n" + query + "\nwith parameters: \n" + parameters.toString());
		
		return semantic.performQuery(query, parameters);
	}
	
	

	/* === PRIVATE METHODS === */
	
	
	/**
	 * Processing method for {@link NetworkMessageRequest request} type of {@link NetworkMessage NetworkMessage}.
	 * 
	 * @param networkMessage Message parsed from the incoming message. 
	 */
	private void processMessageRequest(NetworkMessage networkMessage){
		
		// cast it to request message first (it is safe and also necessary)
		NetworkMessageRequest requestMessage = (NetworkMessageRequest) networkMessage;
		
		NetworkMessageResponse response = null;
		
		if (objectIsInMyRoster(requestMessage.getSourceOid())) {
			
			// create response and send it back
			switch (requestMessage.getRequestOperation()){
			
			case NetworkMessageRequest.OPERATION_CANCELTASK:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is CANCELTASK.");
				
				response = respondToCancelRunningTask(requestMessage);
				
				break;
				
			case NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETEVENTCHANNELSTATUS.");
				
				response = respondToEventChannelStatusQuery(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_GETLISTOFACTIONS:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETLISTOFACTIONS.");
				response = respondToGetObjectActions(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_GETLISTOFEVENTS:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETLISTOFEVENTS.");
				response = respondToGetObjectEvents(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_GETLISTOFPROPERTIES:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETLISTOFPROPERTIES.");
				
				response = respondToGetObjectProperties(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_GETPROPERTYVALUE:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETPROPERTYVALUE.");
				
				response = respondToGetObjectProperty(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_GETTASKSTATUS:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETTASKSTATUS.");
				response = respondToGetTaskStatus(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_SETPROPERTYVALUE:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is SETPROPERTYVALUE.");
				response = respondToSetObjectProperty(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_STARTACTION:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is STARTACTION.");
				response = respondToStartActionRequest(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is SUBSCRIBETOEVENTCHANNEL.");
				response = respondToEventSubscriptionRequest(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is UNSUBSCRIBEFROMEVENTCHANNEL.");
				response = respondToCancelSubscriptionRequest(requestMessage);
				break;
				
			case NetworkMessageRequest.OPERATION_GETTHINGDESCRIPTION:
				
				logger.info(this.objectId + ": Request ID is " + requestMessage.getRequestId() + ", operation is GETTHINGDESCRIPTION.");
				response = respondToGetObjectThingDescription(requestMessage);
				break;
			}
		
			
			if (response != null) {
				response.setSourceOid(objectId);
				// don't get confused, our response destination is the request source ;) 
				response.setDestinationOid(requestMessage.getSourceOid());
			}
			
			sendMessage(this.objectId, requestMessage.getSourceOid(), response.buildMessageString());
			
		} else {
			
			logger.warning(this.objectId + ": The source OID " + requestMessage.getSourceOid() + " of the request " 
					+ requestMessage.getRequestId() + " is not in the roster of this object.");
		}
		
		
		
	}
	
	
	/**
	 * Processing method for {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse response} type of 
	 * {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage NetworkMessage}. It adds it to the message queue
	 * where it will wait to be reaped (or expired).  
	 * 
	 * @param networkMessage Message parsed from the incoming message.
	 */
	private void processMessageResponse(NetworkMessage networkMessage){
		
		logger.info(this.objectId + ": This is a response to request ID " + networkMessage.getRequestId());
		
		messageQueue.add(networkMessage);
	}
	
	
	/**
	 * Processing method for {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageEvent event} type of
	 * {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage NetworkMessage}. It forwards it to the respective
	 * object via the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 * 
	 * @param networkMessage Message parsed from the incoming message.
	 */
	private void processMessageEvent(NetworkMessage networkMessage) {
		
		// cast it to event message first (it is safe and also necessary)
		NetworkMessageEvent eventMessage = (NetworkMessageEvent) networkMessage;
		
		logger.info(this.objectId + ": Event " + eventMessage.getEventId() + " arrived from " + eventMessage.getSourceOid() 
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
			
			if (response != null) {
				// if the return code is different than 2xx, make it visible
				if ((response.getResponseCode() / 200) != 1){
					
					logger.warning(this.objectId + ": Event was not forwarded successfully. Response code: " + response.getResponseCode() 
												+ ". Response reason: " + response.getResponseCodeReason());
												
				} else {
					logger.info(this.objectId + ": Event forwarded successfully.");
				}
			} else {
				
				logger.warning(this.objectId + ": Null response received from the Agent after event frowarding. Moving "
						+ "on, it'd get discarded anyway.");
			}
			
		}
		
		// no need to send the response message back to sender
	}
	
	/**
	 * Responds to a request for getting the object events. It creates a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse
	 * response} that is then sent back to the requesting object.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
	private NetworkMessageResponse respondToGetObjectEvents(NetworkMessageRequest requestMessage) {
		
		JsonObject events = data.getEvents();
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		response.setResponseBody(events.toString());
		response.setContentType("application/json");
		response.setError(false);
		response.setResponseCode(CodesAndReasons.CODE_200_OK);
		response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Events retrieved.");
		
		// don't forget to set the correlation id so the other side can identify what 
		// request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
				
		return response;
		
	}
	
	/**
	 * Responds to a request for getting the object actions. It creates a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse
	 * response} that is then sent back to the requesting object.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
	private NetworkMessageResponse respondToGetObjectActions(NetworkMessageRequest requestMessage) {
		
		JsonObject actions = data.getActions();
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		response.setResponseBody(actions.toString());
		response.setContentType("application/json");
		response.setError(false);
		response.setResponseCode(CodesAndReasons.CODE_200_OK);
		response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Actions retrieved.");
		
		// don't forget to set the correlation id so the other side can identify what 
		// request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
				
		return response;
		
	}
	
	/**
	 * Responds to a request for getting the object properties. It creates a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse
	 * response} that is then sent back to the requesting object.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
	private NetworkMessageResponse respondToGetObjectProperties(NetworkMessageRequest requestMessage) {
		
		JsonObject properties = data.getProperties();
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		response.setResponseBody(properties.toString());
		response.setContentType("application/json");
		response.setError(false);
		response.setResponseCode(CodesAndReasons.CODE_200_OK);
		response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Properties retrieved.");
		
		// don't forget to set the correlation id so the other side can identify what 
		// request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
				
		return response;
		
	}
	
	/**
	 * Responds to a request for getting the object thing description. It creates a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse
	 * response} that is then sent back to the requesting object.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
	private NetworkMessageResponse respondToGetObjectThingDescription(NetworkMessageRequest requestMessage) {
		
		JsonObject thingDescription = data.getThingDescription();
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		response.setResponseBody(thingDescription.toString());
		response.setContentType("application/json");
		response.setError(false);
		response.setResponseCode(CodesAndReasons.CODE_200_OK);
		response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Thing description retrieved.");
		
		// don't forget to set the correlation id so the other side can identify what 
		// request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
				
		return response;
		
	}
	
	/**
	 * Responds to a request for getting the object property. It creates a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse
	 * response} that is then sent back to the requesting object.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
	
	/**
	 * Responds to a request for setting the object property. It creates a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse
	 * response} that is then sent back to the requesting object.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
	
	
	/**
	 * Responds to a request to check the status of an {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
			logger.warning(this.objectId + ": Received a request to provide status of invalid event channel "
					+ eventId + ". Request came from: " + requestMessage.getSourceOid());
			
			response.setError(true);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
					+ "Invalid event channel specified.");

		} else {
			
			response.setError(false);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_200_OK);
			response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Event channel status retrieved.");
			response.setResponseBody(createSimpleJsonString(EventChannel.ATTR_ACTIVE, eventChannel.isActive()));
			
		}

		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	/**
	 * Responds to a request for subscription to an {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
		
		// check whether the object is already in the list of subscribers
		if (eventChannel == null || !eventChannel.isActive()) {
			logger.warning(this.objectId + ": Received a request for subscription to invalid event channel " + eventId 
					+ ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
					+ "Invalid event channel specified.");
			
		} else {
			
			eventChannel.addToSubscribers(requestMessage.getSourceOid());
			
			// manually save data to file because eventChannel was changed
			data.saveData();
			
			response.setError(false);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_200_OK);
			response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Subscribed.");
			
		}
		
		// set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
		
	}
	
	
	/**
	 * Responds to a request for cancelling a subscription to an {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel}.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
		
		if (eventChannel == null || !eventChannel.isActive()) {
			logger.warning(this.objectId + ": Received a request to cancel subscription to invalid event "
					+ "channel " + eventId + ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
					+ "Invalid event channel specified.");
		} else {
			
			eventChannel.removeFromSubscribers(requestMessage.getSourceOid());
			
			// manually save data (persistence) (htofix/VIC-749)
			data.saveData();
			
			response.setError(false);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_200_OK);
			response.setResponseCodeReason(CodesAndReasons.REASON_200_OK + "Unsubscribed.");
			
			logger.info(this.objectId + ": Object " + requestMessage.getSourceOid() + " unsubscribed from event channel " 
					+ eventId);
		}

		// don't forget to set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	/**
	 * Responds to a request for starting an {@link eu.bavenir.ogwapi.commons.Action Action}.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
		
		// the workaround
		if (action == null) {
			action = new Action (config, this.objectId, actionId, agentConnector, logger);
			data.addProvidedAction(action);
		}
		// end of workaround
		
		/*
		 * Uncomment this if you want the OGWAPI behaviour, where no new actions are possible and delete the 
		 * workaround a few lines higher. 
		 
		if (action == null ) {
			logger.warning(this.objectId + ": Received a request to start non existing action " + actionId 
					+ ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
								+ "Invalid action specified.");
		} else {
		
		*/
		
			String taskId = action.createNewTask(requestMessage.getSourceOid(), 
					requestMessage.getRequestBody(), requestMessage.getParameters());
			
			if (taskId == null) {
				
				logger.warning(this.objectId + ": Cannot start action " + actionId + ", too many tasks in queue.");
				
				// responding with error
				response.setError(true);
				response.setContentType("application/json");
				response.setResponseCode(CodesAndReasons.CODE_503_SERVICEUNAVAILABLE);
				response.setResponseCodeReason(CodesAndReasons.REASON_503_SERVICENAVAILABLE 
						+ "Too many tasks waiting in queue.");

			} else {
				
				logger.info(this.objectId + ": Created task " + taskId + " of action " + actionId + " and added it to the queue.");
				
				response.setError(false);
				response.setContentType("application/json");
				response.setResponseCode(CodesAndReasons.CODE_201_CREATED);
				response.setResponseCodeReason(CodesAndReasons.REASON_201_CREATED + "New task added to the queue.");
				response.setResponseBody(createSimpleJsonString(Action.ATTR_TASKID, taskId));
			}
		// }
		
		// set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	/**
	 * Responds to a request for getting a status of a {@link eu.bavenir.ogwapi.commons.Task Task}.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
		
		if (action == null ) {
			logger.warning(this.objectId + ": Received a request for status report on non existing action " 
					+ actionId + ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
								+ "Invalid action specified.");
		} else {
			
			byte statusCode = action.getTaskStatus(taskId);

			if (statusCode == Task.TASKSTATUS_UNKNOWN) {
				
				statusString = new String("Object ID " + this.objectId + " can't find task " + taskId 
						+ ". Its status is unknown.");
				
				logger.warning(this.objectId + ": " + statusString);
				
				// responding with error
				response.setError(true);
				response.setContentType("application/json");
				response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
				response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
									+ "Invalid task specified.");
			} else {
				
				logger.info(this.objectId + ": Task " + taskId + " of the action " + actionId 
						+ " has status " + action.getTaskStatusString(taskId) + ".");
				
				response.setError(false);
				response.setContentType("application/json");
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
	

	/**
	 * Responds to a request for cancelling a running {@link eu.bavenir.ogwapi.commons.Task Task}.
	 * 
	 * @param requestMessage A message that came from the network.
	 * @return Response to be sent back.
	 */
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
		
		if (action == null ) {
			logger.warning(this.objectId + ": Received a request for stopping a task of non existing action " 
					+ actionId + ". Request came from: " + requestMessage.getSourceOid());
			
			// responding with error
			response.setError(true);
			response.setContentType("application/json");
			response.setResponseCode(CodesAndReasons.CODE_404_NOTFOUND);
			response.setResponseCodeReason(CodesAndReasons.REASON_404_NOTFOUND 
								+ "Invalid action specified.");
		} else {
			logger.info(this.objectId + ": Stopping an task ID " + taskId + " of the action " + actionId);
			
			response = action.cancelTask(taskId, requestMessage.getRequestBody(), requestMessage.getParameters());
			
			if (response == null) {
				
				logger.warning(this.objectId + ": Task ID " + taskId + " is in a state that does not allow it to be "
						+ "cancelled. It either does not exist, is already finished, or failed.");
				
				response = new NetworkMessageResponse(config, logger);
				
				// responding with error
				response.setError(false);
				response.setContentType("application/json");
				response.setResponseCode(CodesAndReasons.CODE_200_OK);
				response.setResponseCodeReason(CodesAndReasons.REASON_200_OK 
									+ "Invalid task specified.");
			} else {
				if (response.isError()) {
					logger.warning(this.objectId + ": Received an error from agent connector while "
							+ "attempting to stop task ID " + taskId + ". Code " + response.getResponseCode() 
							+ " reason " + response.getResponseCodeReason());
				} else {
					logger.info(this.objectId + ": Task " + taskId + "stopped.");
				}
				
			}
		}
		
		// set the correlation id so the other side can identify what request does this response belong to
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}
	
	
	/**
	 * Searches for {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel} with provided eventID. 
	 * 
	 * @param eventId ID of the event.
	 * @return {@link eu.bavenir.ogwapi.commons.EventChannel EventChannel} with the ID, or null if no such channel exists. 
	 */
	private EventChannel searchForEventChannel(String eventId) {
		// search for given event channel
		
		if (eventId == null) {
			
			logger.warning(this.objectId + ": Null event ID provided for search.");
			
			return null;
		}
		
		logger.finer(this.objectId + ": Searching for event channel " + eventId);
		
		for (EventChannel eventChannel : data.getProvidedEventChannels()) {
			if (eventChannel.getEventId().equals(eventId)) {
				// found it
				logger.finer(this.objectId + ": Event channel found.");
				return eventChannel;
			}
		}

		logger.finer(this.objectId + ": Event channel not found.");
		return null;
	}
	
	
	/**
	 * Searches for {@link eu.bavenir.ogwapi.commons.Action Action} with provided action ID.
	 * 
	 * @param actionId ID of the action.
	 * @return {@link eu.bavenir.ogwapi.commons.Action Action} with the ID, or null if no such channel exists.
	 */
	private Action searchForAction(String actionId) {
		
		if (actionId == null) {
			
			logger.warning(this.objectId + ": Null action ID provided for search.");
			
			return null;
		}
		
		logger.finer(this.objectId + ": Searching for action " + actionId);
		
		// search for given action
		
		for (Action action : data.getProvidedActions()) {
			if (action.getActionId().equals(actionId)) {
				// found it
				logger.finer(this.objectId + ": Action found.");
				return action;
			}
		}
		
		logger.finer(this.objectId + ": Action not found.");

		return null;
	}
	
	
	/**
	 * Searches for {@link eu.bavenir.ogwapi.commons.Subscription Subscription} based on a remote object ID. 
	 *  
	 * @param remoteObjectId ID of the object that this object has a {@link eu.bavenir.ogwapi.commons.Subscription Subscription} created to.
	 * @return
	 */
	private Subscription searchForSubscription(String remoteObjectId) {
		
		if (remoteObjectId == null) {
			
			logger.warning(this.objectId + ": Null remote object ID provided for subscription search.");
			
			return null;
		}
		
		logger.finer(this.objectId + ": Searching for subscriptions to " + remoteObjectId);
		
		for (Subscription subscription : data.getSubscribedEventChannels()) {
			if (subscription.getObjectId().equals(remoteObjectId)) {
				// found it
				logger.finer(this.objectId + ": Subscription found.");
				return subscription;
			}
		}
		
		logger.finer(this.objectId + ": Subscription not found.");

		return null;
	}

	
	/**
	 * Retrieves a {@link eu.bavenir.ogwapi.commons.messages.NetworkMessage NetworkMessage} from the queue of incoming 
	 * messages based on the correlation request ID. It blocks the invoking thread if there is no message in the queue 
	 * until it arrives or until timeout is reached. The check for timeout is scheduled every 
	 * {@link #POLL_INTERRUPT_INTERVAL_MILLIS POLL_INTERRUPT_INTERVAL_MILLIS}.
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
	
	
	/**
	 * This is common method for sending all request messages to remote objects. Respective methods, like 
	 * {@link #getPropertyOfRemoteObject(String, String, Map, String) getPropertyOfRemoteObject} will 
	 * just fill the appropriate operation ID. The operation ID is chosen from 
	 * {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest NetworkMessageRequest} constants. 
	 * 
	 * @param operationId The ID of the operation, chosen from constants in {eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest NetworkMessageRequest}.
	 * @param destinationOid Object ID of the destination.
	 * @param attributes Attributes that are specific to given operation, usually common definition of JSON attribute with 
	 * appropriate value. E.g. when a request to get a remote property is to be sent, an attribute pair needs to be 
	 * created, where there will be "pid" as key and property ID as the value. Valid attributes are also to be found among 
	 * constants in {eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest NetworkMessageRequest}.
	 * @param parameters Any parameters to be sent with the request. 
	 * @param body Any body to be sent with the request. 
	 * @return Status message.
	 */
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
		
		if (!sendMessage(this.objectId, destinationOid, request.buildMessageString())){
			
			statusCodeReason = new String("Destination object " + destinationOid 
					+ " is either not in the list of available objects or it was not possible to send the message.");
			
			logger.warning(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_404_NOTFOUND, 
					CodesAndReasons.REASON_404_NOTFOUND + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON);
			
			return statusMessage;
		}
		
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		// nothing came through
		if (response == null){

			statusCodeReason = new String("No response message received. The message might have got lost. Source ID: " 
					+ objectId + " Destination ID: " + destinationOid + " Request ID: " + requestId);
			
			logger.warning(this.objectId + ": " + statusCodeReason);
			
			statusMessage = new StatusMessage(
					true, 
					CodesAndReasons.CODE_408_REQUESTTIMEOUT, 
					CodesAndReasons.REASON_408_REQUESTTIMEOUT + statusCodeReason,
					StatusMessage.CONTENTTYPE_APPLICATIONJSON); 
			
			return statusMessage;
		}
		
		// response arrived
		statusMessage = new StatusMessage(
				response.isError(),
				response.getResponseCode(),
				response.getResponseCodeReason(),
				response.getContentType()
				);
		
		JsonObject json = messageResolver.readJsonObject(response.getResponseBody());
		if (json != null) {
			statusMessage.addMessageJson(json);
		} else {
			logger.warning(this.objectId + ": It was not possible to turn response body into a valid JSON. Original string: " 
							+ response.getResponseBody());
		}
		
		return statusMessage;
		
	}
	
	
	/**
	 * Simple method for creating JSON string with one string value. 
	 *  
	 * @param key Name of the attribute.
	 * @param value String value.
	 * @return JSON string.
	 */
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
	
	
	/**
	 * Simple method for creating JSON with one boolean value. 
	 * 
	 * @param key Name of the attribute.
	 * @param value Boolean value. 
	 * @return JSON String.
	 */
	private String createSimpleJsonString(String key, boolean value) {
		// if key is null, there is no need to bother... 
		if (key == null) {
			return null;
		}
			
		JsonObjectBuilder builder = jsonBuilderFactory.createObjectBuilder();
		
		builder.add(key, value);
		
		return builder.build().toString();
	}

	
	/**
	 * Private method capable of sending message, by first trying to distribute it locally, then via network.
	 *  
	 * @param sourceOid Source OID.
	 * @param destinationOid Destination OID.
	 * @param message Message to be sent.
	 * @return True if the message was successfully sent via either local routing or by network. False otherwise.
	 */
	private boolean sendMessage(String sourceOid, String destinationOid, String message) {
		
		logger.info(this.objectId + ": Sending message: \n" + message);
		
		// try internal routing first
		if (commManager.tryToSendLocalMessage(sourceOid, destinationOid, message)) {
			
			logger.fine(this.objectId + ": Message was routed locally.");
			return true;
		}
		
		// if not successful, try it via network 
		if (commEngine.sendMessage(destinationOid, message)) {
			
			logger.fine(this.objectId + ": Message was sent through network.");
			return true;
		}
		
		logger.warning(this.objectId + ": Error while sending message: \n" + message + "\nMessage could not be sent.");
		
		// both failed
		return false;
	}
	
	
	/**
	 * Verifies that the destination is in our roster. This check is done to make sure the communication is performed
	 * only between objects that know each other and have signed contracts. 
	 * 
	 * @param destiantionOid Destination OID.
	 * @return True if the destination OID is in the roster, false otherwise.  
	 */
	private boolean objectIsInMyRoster(String destiantionOid) {
		
		Set<String> roster = commEngine.getRoster();
		
		return roster.contains(destiantionOid);
	}

}
