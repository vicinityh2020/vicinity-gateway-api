package eu.bavenir.ogwapi.commons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;

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
	private Map<String, String> subscribedEventChannels;
	
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
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
		
		this.objectID = objectID;
		this.password = password;
		
		this.config = config;
		this.logger = logger;
		
		// TODO decide what type of connector to use
		agentConnector = new RestAgentConnector(config, logger);
		
		messageQueue = new LinkedTransferQueue<NetworkMessage>();
		
		providedEventChannels = new HashSet<EventChannel>();
		subscribedEventChannels = new HashMap<String, String>();
		
		
		// build new connection
		// TODO this is also the place, where it should decide what engine to use
		commEngine = new XmppMessageEngine(objectID, password, config, logger, this);
		
		// TODO load the event channels - either from a file or server
		
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
	public boolean setEventChannelStatus(String eventID, boolean status) {
		
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
	// -1 if there is no such eventchannel 
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
			
			StatusMessage statusMessage = new StatusMessage(true, StatusMessage.MESSAGE_BODY, 
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
			
			return new StatusMessage(true, StatusMessage.MESSAGE_BODY, statusMessageText);
		}
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			
			statusMessageText = new String("Source object: " + objectID + " Destination object: " + destinationObjectID 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			
			logger.info(statusMessageText);
			
			return new StatusMessage(true, StatusMessage.MESSAGE_BODY, statusMessageText);
		}
		
		return new StatusMessage(false, StatusMessage.MESSAGE_BODY, response.getResponseBody());
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
			
			return new StatusMessage(true, StatusMessage.MESSAGE_BODY, statusMessageText);
		}
		
		// this will wait for response
		NetworkMessageResponse response = (NetworkMessageResponse) retrieveMessage(requestId);
		
		if (response == null){
			
			statusMessageText = new String("No response message received. The message might have got lost. Source ID: " 
				+ objectID + " Destination ID: " + destinationObjectID + " Property ID: " + propertyID  
				+ " Request ID: " + requestId);
			
			logger.info(statusMessageText);

			return new StatusMessage(true, StatusMessage.MESSAGE_BODY, statusMessageText);
		}
		
		// if the return code is different than 2xx, make it visible
		if ((response.getResponseCode() / 200) != 1){
			
			statusMessageText = new String("Source object: " + objectID + " Destination object: " + destinationObjectID 
					+ " Response code: " + response.getResponseCode() + " Reason: " + response.getResponseCodeReason());
			
			logger.info(statusMessageText);

			return new StatusMessage(true, StatusMessage.MESSAGE_BODY, statusMessageText);
		}
		
		return new StatusMessage(false, StatusMessage.MESSAGE_BODY, response.getResponseBody());
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
				processMessageEvent(networkMessage);
			}
		} else {
			logger.warning("Invalid message received from the network.");
		}
		
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
			
			break;
			
		case NetworkMessageRequest.OPERATION_GETEVENTCHANNELSTATUS:
			
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
			
			break;
			
		case NetworkMessageRequest.OPERATION_SETPROPERTYVALUE:
			
			response = agentConnector.setObjectProperty(requestMessage);

			// send it back
			commEngine.sendMessage(from, response.buildMessageString());
			break;
			
		case NetworkMessageRequest.OPERATION_STARTACTION:
			
			break;
			
		case NetworkMessageRequest.OPERATION_SUBSCRIBETOEVENTCHANNEL:
			
			break;
			
		case NetworkMessageRequest.OPERATION_UNSUBSCRIBEFROMEVENTCHANNEL:
			
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
	private void processMessageEvent(NetworkMessage networkMessage) {
		
		// TODO you are here
		
		// cast it to event message first (it is safe and also necessary)
		NetworkMessageEvent eventMessage = (NetworkMessageEvent) networkMessage;
		
		
		
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
