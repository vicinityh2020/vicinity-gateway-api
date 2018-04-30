package eu.bavenir.ogwapi.commons;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.engines.CommunicationEngine;
import eu.bavenir.ogwapi.commons.engines.xmpp.XmppMessageEngine;
import eu.bavenir.ogwapi.commons.messages.MessageParser;
import eu.bavenir.ogwapi.commons.messages.NetworkMessage;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;
import eu.bavenir.ogwapi.xmpp.AgentCommunicator;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * A representation of a connection to XMPP network. In essence, it is a client connected into XMPP network, able to 
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
	
	// TODO this stays here during re-factoring 
	
	private Set<EventChannel> eventChannels;
	
	//////////////////////////////////////////////////////////////////////
	
	
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	// the thing that communicates with agent
	private AgentCommunicator agentCommunicator;
	
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
	 */
	/**
	 * 
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
		
		agentCommunicator = new AgentCommunicator(config, logger);
		
		messageQueue = new LinkedTransferQueue<NetworkMessage>();
		
		eventChannels = new HashSet<EventChannel>();
		
		// build new connection
		// TODO spawn a comm engine and connect
		// this is also the place, where it should decide what engine to use
		commEngine = new XmppMessageEngine(objectID, password, config, logger, this);
		
		
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
	public boolean sendMessage(String destinationObjectID, String message){
		return commEngine.sendMessage(destinationObjectID, message);
	}
	
	
	/**
	 * Retrieves a {@link NetworkMessage NetworkMessage} from the queue of incoming messages based on the correlation 
	 * request ID. It blocks the invoking thread if there is no message in the queue until it arrives or until timeout
	 * is reached. The check for timeout is scheduled every {@link #POLL_INTERRUPT_INTERVAL_MILLIS POLL_INTERRUPT_INTERVAL_MILLIS}.
	 * 
	 * @param requestId Correlation request ID. 
	 * @return {@link NetworkMessage NetworkMessage} from the queue.
	 */
	public NetworkMessage retrieveMessage(int requestId){
		
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
		
		NetworkMessageResponse agentResponse = agentCommunicator.processRequestMessage(requestMessage);
		
		// send it back
		sendMessage(from, agentResponse.buildMessageString());
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
	
	

}
