package eu.bavenir.vicinity.gatewayapi.xmpp;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import eu.bavenir.vicinity.gatewayapi.App;


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
 * 		{@link #verifyPassword() verifyPassword()} is used for this and it should be called by RESTLET authorization 
 * 		verifier every time a request is made.
 * 4. {@link #disconnect() disconnect()}
 *  
 * @author sulfo
 *
 */
public class XmppConnectionDescriptor {

	/* === CONSTANTS === */
	
	
	/**
	 * Name of the configuration parameter for XMPP server URL.
	 */
	private static final String CONFIG_PARAM_XMPPSERVER = "xmpp.server";
	
	/**
	 * Name of the configuration parameter for XMPP server port.
	 */
	private static final String CONFIG_PARAM_XMPPPORT = "xmpp.port";
	
	/**
	 * Name of the configuration parameter for enabling security.
	 */
	private static final String CONFIG_PARAM_XMPPSECURITY = "xmpp.security";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_XMPPSERVER CONFIG_PARAM_XMPPSERVER} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	private static final String CONFIG_DEF_XMPPSERVER = "";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_XMPPPORT CONFIG_PARAM_XMPPPORT} configuration parameter. This value is 
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	private static final int CONFIG_DEF_XMPPPORT = 5222;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_XMPPSECURITY CONFIG_PARAM_XMPPSECURITY} configuration parameter. This value
	 * is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_XMPPSECURITY = true;
	
	/**
	 * This is the resource part of the full JID used in the communication. It is necessary to set it and not leave it
	 * to default value, which is auto generated to random string. 
	 */
	private static final String XMPP_RESOURCE = "communicationNode";
	
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
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	// the thing that communicates with agent
	private AgentCommunicator agentCommunicator;
	
	// credentials and connection to XMPP server
	private String xmppUsername;
	private String xmppPassword;
	private AbstractXMPPConnection connection;
	
	// chat manager for the current connection
	private ChatManager chatManager;
	
	// roster for current connection
	private Roster roster;
	
	// message queue, FIFO structure for holding incoming messages 
	private BlockingQueue<NetworkMessage> messageQueue;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * the descriptor will not be able to connect (in the best case scenario, the other being a storm of null pointer 
	 * exceptions).
	 */
	public XmppConnectionDescriptor(String xmppUsername, String xmppPassword, XMLConfiguration config, Logger logger){
		
		this.xmppUsername = xmppUsername;
		this.xmppPassword = xmppPassword;
		
		this.config = config;
		this.logger = logger;
		
		agentCommunicator = new AgentCommunicator(config, logger);
		
		messageQueue = new LinkedTransferQueue<NetworkMessage>();
		
		// build new connection
		connection = buildNewConnection(xmppUsername, xmppPassword);
		
	}
	
	
	/**
	 * Retrieves the XMPP user name used for this connection.
	 *   
	 * @return XMPP user name, without domain (not a full JID).
	 */
	public String getUsername() {
		return xmppUsername;
	}

	
	/**
	 * Verifies, whether the client using this descriptor is using correct password in its HTTP requests. 
	 * 
	 * @param passwordToVerify The password provided by client.
	 * @return True if the password matches the one used by this connection.
	 */
	public boolean verifyPassword(String passwordToVerify) {
		return passwordToVerify.equals(xmppPassword);
	}
	
	
	/**
	 * Retrieves the connection to XMPP server in use by this descriptor.
	 *  
	 * @return {@link org.jivesoftware.smack.AbstractXMPPConnection AbstractXMPPConnection}.  
	 */
	public AbstractXMPPConnection getConnection() {
		return connection;
	}
	

	/**
	 * Connects to the XMPP server and logs the user in. It also registers a listener for incoming messages for this 
	 * connection (see {@link org.jivesoftware.smack.chat2.ChatManager ChatManager}).  In case of failure it is 
	 * possible to re-try. 
	 * 
	 * @return True on success, false otherwise.
	 */
	public boolean connect(){
		// connect & login
		try {
			if (connection.connect() == null){
				
				logger.warning("Connection to XMPP could not be established for user '" + xmppUsername + "'.");
				
				return false;
			}
			
			connection.login();
			
		} catch (SmackException | IOException | XMPPException | InterruptedException e) {
			
			logger.severe("Exiting due to exception during establishing a connection to XMPP server. Message: " 
					+ e.getMessage());
			
			return false;
		}
		
		// spawn a chat manager and associate a call for incoming messages
		chatManager = ChatManager.getInstanceFor(connection);
		chatManager.addIncomingListener(new IncomingChatMessageListener(){
			
			@Override
			public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
				processMessage(from, message, chat);
			}
			
		});
		
		// spawn a roster and associate calls for changes in roster - if set
		roster = Roster.getInstanceFor(connection);
		
		roster.addRosterListener(new RosterListener() {
			@Override
			public void entriesAdded(Collection<Jid> addresses) {
				processRosterEntriesAdded(addresses);
			}
			
			@Override
			public void entriesDeleted(Collection<Jid> addresses) {
				processRosterEntriesDeleted(addresses);
			}
			
			@Override
			public void entriesUpdated(Collection<Jid> addresses) {
				processRosterEntriesUpdated(addresses);
			}
			
			@Override
			public void presenceChanged(Presence presence) {
				processRosterPresenceChanged(presence);
			}
		});
		
		return true;
	}
	
	
	/**
	 * Closes the connection to the XMPP server. The connection can be re-opened.
	 */
	public void disconnect(){
		
		if (connection != null && connection.isConnected()){
			
			connection.disconnect();
			
			logger.fine("XMPP user '" + xmppUsername + "' disconnected.");
		} else {
			logger.fine("XMPP user '" + xmppUsername + "' is already disconnected.");
		}
		
	}


	/**
	 * Returns true if the descriptor has opened connection to the XMPP server.
	 * 
	 * @return True or false.
	 */
	public boolean isConnected(){
		return connection.isConnected();
	}
	
	
	/**
	 * Retrieves the roster of the current XMPP user.
	 * 
	 * @return An instance of {@link org.jivesoftware.smack.roster.Roster Roster} for this connection. 
	 */
	public Roster getRoster(){
		if (connection == null || !connection.isConnected()){
			logger.warning("Invalid connection in descriptor for username '" + xmppUsername + "'.");
			return null;
		}
		
		try {
			roster.reloadAndWait();
		} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
			logger.warning("Roster could not be reloaded. Exception: " + e.getMessage());
		}
		
		return roster;
	}
	
	
	/**
	 * Sends a string to the destination XMPP user name. The recommended approach is to get the roster first (by the 
	 * {@link #getRoster() getRoster()} method and then send the message, if the contact is online.  
	 * 
	 * @param destinationUsername Destination contact, for which the message is intended. 
	 * @param message A string to send.
	 * @param chat An instance of {@link org.jivesoftware.smack.chat2.Chat Chat} class. When a message is to be sent
	 * over network as a new request, this should be left as null - a new chat will be created automatically and it
	 * will check whether the receiving station is in the transmitting station roster (which serves as an 
	 * authorization method - you can't send messages to stations that are not visible to you). However if the message
	 * is to be sent as a response, the Chat object that was created when the request message arrived should be provided
	 * (as a way to overcome the roster authorization - a station should be able to respond to a request, even if it 
	 * does not see the requesting station).
	 * @return True on success, false if the destination object is offline or if error occurred.
	 */
	public boolean sendMessage(String destinationUsername, String message, Chat chat){

		destinationUsername = destinationUsername + "@" 
								+ config.getString(App.CONFIG_PARAM_XMPPDOMAIN, App.CONFIG_DEF_XMPPDOMAIN);
		
		EntityBareJid jid;
		
		try {
			jid = JidCreate.entityBareFrom(destinationUsername);
		} catch (XmppStringprepException e) {
			logger.warning("Message could not be sent. Exception: " + e.getMessage());
			return false;
		}
		
		// distinguish between this call being a new request a response to one
		if (chat == null){
			try {
				try {
					roster.reloadAndWait();
				} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
					logger.warning("Roster could not be reloaded. Exception: " + e.getMessage());
				}
				
				// check whether the destination is online
				Presence presence = roster.getPresence(jid);
				
				if (presence.isAvailable()){
					chat = chatManager.chatWith(jid);
					chat.send(message);
					
				} else {
					return false;
				}
				
			} catch (NotConnectedException | InterruptedException e) {
				logger.warning("Message could not be sent. Exception: " + e.getMessage());
				return false;
			}
		} else {
			try {
				chat.send(message);
			} catch (NotConnectedException | InterruptedException e) {
				logger.warning("Message could not be sent. Exception: " + e.getMessage());
			}
		}
		
		return true;
	}
	
	
	/**
	 * Sends a string to the destination XMPP JID. The recommended approach is to get the roster first (by the 
	 * {@link #getRoster() getRoster()} method and then send the message, if the contact is online.  
	 * 
	 * @param destinationJid Destination contact, for which the message is intended. 
	 * @param message A string to send.
	 * @param chat An instance of {@link org.jivesoftware.smack.chat2.Chat Chat} class. When a message is to be sent
	 * over network as a new request, this should be left as null - a new chat will be created automatically and it
	 * will check whether the receiving station is in the transmitting station roster (which serves as an 
	 * authorization method - you can't send messages to stations that are not visible to you). However if the message
	 * is to be sent as a response, the Chat object that was created when the request message arrived should be provided
	 * (as a way to overcome the roster authorization - a station should be able to respond to a request, even if it 
	 * does not see the requesting station).
	 * @return True on success, false if the destination object is offline or if error occurred.
	 */
	public boolean sendMessage(EntityBareJid destinationJid, String message, Chat chat){
		
		// distinguish between this call being a new request a response to one
		if (chat == null){
			try {
				try {
					roster.reloadAndWait();
				} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
					logger.warning("Roster could not be reloaded. Exception: " + e.getMessage());
				}
				
				// check whether the destination is online
				Presence presence = roster.getPresence(destinationJid);
				
				if (presence.isAvailable()){
					chat = chatManager.chatWith(destinationJid);
					chat.send(message);
					
				} else {
					return false;
				}
				
			} catch (NotConnectedException | InterruptedException e) {
				logger.warning("Message could not be sent. Exception: " + e.getMessage());
				return false;
			}
		} else {
			try {
				chat.send(message);
			} catch (NotConnectedException | InterruptedException e) {
				logger.warning("Message could not be sent. Exception: " + e.getMessage());
			}
		}
		
		return true;
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
							+ "; Timestamp = " + helperMessage.timeStamp);
					}
					
					// in order not to iterate thousand times a second over one single message, that don't belong
					// to us (or anybody), let's sleep a little to optimize performance
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
	
	
	
	/* === PRIVATE METHODS === */
	
	
	/**
	 * Builds the connection object based on Gateway XML configuration file and the provided credentials.  
	 * 
	 * @param xmppUsername XMPP user name without the served domain (i.e. just 'user' instead of 'user@xmpp.server').
	 * @param xmppPassword Password of the user.
	 * @return The established connection if the attempt was successful, null otherwise.
	 */
	private AbstractXMPPConnection buildNewConnection(String xmppUsername, String xmppPassword){
		// we build a new connection here
		
		String xmppServer = config.getString(CONFIG_PARAM_XMPPSERVER, CONFIG_DEF_XMPPSERVER);
		int xmppPort = config.getInt(CONFIG_PARAM_XMPPPORT, CONFIG_DEF_XMPPPORT);
		String xmppDomain = config.getString(App.CONFIG_PARAM_XMPPDOMAIN, App.CONFIG_DEF_XMPPDOMAIN);
		boolean xmppSecurity = config.getBoolean(CONFIG_PARAM_XMPPSECURITY, CONFIG_DEF_XMPPSECURITY);
		
		logger.config("Creating a new connection to XMPP server '" + xmppServer + ":" + xmppPort + "' as '" 
						+ xmppUsername + "@" + xmppDomain + "'");
		
		// prepare configuration builder by feeding it the preferences
		XMPPTCPConnectionConfiguration.Builder xmppConfigBuilder = XMPPTCPConnectionConfiguration.builder();
		xmppConfigBuilder.setUsernameAndPassword(xmppUsername, xmppPassword);
		xmppConfigBuilder.setHost(xmppServer);
		xmppConfigBuilder.setPort(xmppPort);
		
		try {
			xmppConfigBuilder.setResource(XMPP_RESOURCE);
		} catch (XmppStringprepException e) {
			logger.severe("Exiting due to exception during building a connection to XMPP server. Message: " 
					+ e.getMessage());
			
			return null;
		}
		
		if (!xmppSecurity) {
			xmppConfigBuilder.setSecurityMode(SecurityMode.disabled);
			logger.config("XMPP SECURE CONNECTION IS DISABLED.");
		} else {
			// default is enabled
			logger.config("XMPP secure connection is enabled.");
		}
		
		try {
			// this one throws exception, so needs to be in a block
			xmppConfigBuilder.setXmppDomain(xmppDomain);
			
			// build configuration
			XMPPTCPConnectionConfiguration xmppConnConfig = xmppConfigBuilder.build();
			
			// build connection
			return new XMPPTCPConnection(xmppConnConfig);
			
		} catch (IOException e) {
			logger.severe("Exiting due to exception during building a connection to XMPP server. Message: " 
					+ e.getMessage());
			
			return null;	
		}
	}
	
	
	/**
	 * This is a callback method called when a message arrives. There are two main scenarios that need to be handled:
	 * a. A message arrives ('unexpected') from another node with request for data or action - this need to be routed 
	 * to a specific end point on agent side and then the result needs to be sent back to originating node. 
	 * b. After sending a message with request to another node, the originating node expects an answer, which arrives
	 * as a message. This is stored in a queue and is propagated back to RESTLET services, that are expecting the
	 * results.
	 * 
	 * @param from A JID of the sender.
	 * @param xmppMessage Received message.
	 * @param chat A chat thread in which the message was received. 
	 */
	private void processMessage(EntityBareJid from, Message xmppMessage, Chat chat){
		
		logger.finest("New message from " + from + ": " + xmppMessage.getBody());
		
		// let's parse the xmpp message 
		MessageParser messageParser = new MessageParser(config);
		NetworkMessage networkMessage = messageParser.parseNetworkMessage(xmppMessage);
		
		if (networkMessage != null){
			switch (networkMessage.getMessageType()){
			
			case NetworkMessageRequest.MESSAGE_TYPE:
				logger.finest("The message is a request. Processing...");
				processMessageRequest(from, networkMessage, chat);
				break;
				
			case NetworkMessageResponse.MESSAGE_TYPE:
				logger.finest("This message is a response. Adding to incoming queue. Message count: " 
						+ messageQueue.size());
				processMessageResponse(from, networkMessage);
				break;
			}
		} else {
			logger.warning("Invalid system message received from XMPP network.");
		}
		
	}
	
	
	/**
	 * Processing method for {@link NetworkMessageRequest request} flavor of {@link NetworkMessage NetworkMessage}.
	 * 
	 * @param from Address of the object that sent the message.
	 * @param networkMessage Message parsed from the XMPP message. 
	 */
	private void processMessageRequest(EntityBareJid from, NetworkMessage networkMessage, Chat chat){
		
		// cast it to request message first (it is safe and also necessary)
		NetworkMessageRequest requestMessage = (NetworkMessageRequest) networkMessage;
		
		//String agentResponse = agentCommunicator.processRequestMessage(requestMessage);
		
		NetworkMessageResponse agentResponse = agentCommunicator.processRequestMessage(requestMessage);
		
		// send it back
		sendMessage(from, agentResponse.buildMessageString(), chat);
	}
	
	
	/**
	 * Processing method for {@link NetworkMessageResponse response} flavor of {@link NetworkMessage NetworkMessage}.
	 * 
	 * @param from Address of the object that sent the message.
	 * @param networkMessage Message parsed from the XMPP message.
	 */
	private void processMessageResponse(EntityBareJid from, NetworkMessage networkMessage){
		messageQueue.add(networkMessage);
	}
	
	
	/**
	 * A callback method called when entries are added into the {@link org.jivesoftware.smack.roster.Roster roster}.
	 * 
	 * @param addresses A collection of {@link org.jxmpp.jid.Jid JID} addresses that were added.
	 */
	private void processRosterEntriesAdded(Collection<Jid> addresses){
		
		/*
		for(Jid address : addresses){
			System.out.println("processRosterEntriesAdded: " + address.toString());
		}
		*/
		/*
		try {
			roster.reloadAndWait();
		} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
			logger.warning("Roster could not be reloaded. Exception: " + e.getMessage());
		}*/
	}
	
	
	/**
	 * A callback method called when entries are deleted from the {@link org.jivesoftware.smack.roster.Roster roster}.
	 * 
	 * @param addresses A collection of {@link org.jxmpp.jid.Jid JID} addresses that were deleted.
	 */
	private void processRosterEntriesDeleted(Collection<Jid> addresses){
		/*
		for(Jid address : addresses){
			System.out.println("processRosterEntriesDeleted: " + address.toString());
		}
		*/
		/*
		try {
			roster.reloadAndWait();
		} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
			logger.warning("Roster could not be reloaded. Exception: " + e.getMessage());
		}*/
	}
	
	
	/**
	 * A callback method called when entries are updated in the {@link org.jivesoftware.smack.roster.Roster roster}.
	 * 
	 * @param addresses A collection of {@link org.jxmpp.jid.Jid JID} addresses that were updated.
	 */
	private void processRosterEntriesUpdated(Collection<Jid> addresses) {
		/*
		for(Jid address : addresses){
			System.out.println("processRosterEntriesUpdated: " + address.toString());
		}
		*/
		/*
		try {
			roster.reloadAndWait();
		} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
			logger.warning("Roster could not be reloaded. Exception: " + e.getMessage());
		}*/
		
	}
	
	
	/**
	 * A callback method called when the presence of the current connection is changed.
	 * 
	 * @param presence A new {@link org.jivesoftware.smack.packet.Presence presence}.
	 */
	private void processRosterPresenceChanged(Presence presence) {
		//System.out.println("processRosterPresenceChanged - Presence changed: " + presence.getFrom() + " " + presence);
	}
}
