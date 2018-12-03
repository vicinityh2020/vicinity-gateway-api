package eu.bavenir.ogwapi.commons.engines.xmpp;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import eu.bavenir.ogwapi.commons.ConnectionDescriptor;
import eu.bavenir.ogwapi.commons.engines.CommunicationEngine;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * This is the reference implementation of XMPP message engine based on Ignite Realtime's SMACK library for OGWAPI.
 * It uses messages to transport data across the XMPP network (as opposed to streams).
 * 
 * @author sulfo
 *
 */
public class XmppMessageEngine extends CommunicationEngine {

	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for a domain served by the XMPP server.
	 */
	public static final String CONFIG_PARAM_XMPPDOMAIN = "xmpp.domain";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_XMPPDOMAIN CONFIG_PARAM_XMPPDOMAIN} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	public static final String CONFIG_DEF_XMPPDOMAIN = "bavenir.eu";
	
	/**
	 * Enables debugging of the XMPP communication between the Gateway and the
	 * server / other Gateways. Note that this is to be used in conjunction
	 * with the SMACK debugger, which is external tool.
	 * @see http://download.igniterealtime.org/smack/docs/latest/documentation/debugging.html.
	 */
	private static final String CONFIG_PARAM_XMPPDEBUG = "xmpp.debugging";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_XMPPDEBUG CONFIG_PARAM_XMPPDEBUG} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	private static final boolean CONFIG_DEF_XMPPDEBUG = false;
	
	/**
	 * Name of the configuration parameter for server URL. If not set, the application exits.
	 */
	private static final String CONFIG_PARAM_SERVER = "general.server";
	
	/**
	 * Name of the configuration parameter for server port.
	 */
	private static final String CONFIG_PARAM_PORT = "general.port";
	
	/**
	 * Name of the configuration parameter for enabling encryption.
	 */
	private static final String CONFIG_PARAM_ENCRYPTION = "general.encryption";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_SERVER CONFIG_PARAM_SERVER} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	private static final String CONFIG_DEF_SERVER = "";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_PORT CONFIG_PARAM_PORT} configuration parameter. This value is 
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	private static final int CONFIG_DEF_PORT = 5222;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_ENCRYPTION CONFIG_PARAM_ENCRYPTION} configuration parameter. This value
	 * is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_ENCRYPTION = true;
	
	/**
	 * This is the resource part of the full JID used in the communication. It is necessary to set it and not leave it
	 * to default value, which is auto generated to random string. 
	 */
	private static final String XMPP_RESOURCE = "communicationNode";
	
	/**
	 * Presence string.
	 */
	private static final String XMPP_PRESENCE_STRING = "online";
	
	/**
	 * Minimum time (in seconds) the roster will go without reloading. See the constructor implementation, the part with timer.
	 */
	private static final int ROSTER_RELOAD_TIME_MIN = 60;
	
	/**
	 * Maximum time (in seconds) the roster will go without reloading. See the constructor implementation, the part with timer.
	 */
	private static final int ROSTER_RELOAD_TIME_MAX = 120;
	
	
	/* === FIELDS === */
	
	/**
	 * Connection to the server.
	 */
	private AbstractXMPPConnection connection;
	
	/**
	 * Chat manager for the current connection.
	 */
	private ChatManager chatManager;
	
	/**
	 * Roster for current connection.
	 */
	private Roster roster;
	
	/**
	 * A list of opened chats.
	 */
	private HashMap<EntityBareJid, Chat> openedChats;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor for the XMPP message engine. It initialises fields and a {@link java.util.Timer Timer} that periodically
	 * in interval that is randomly set, executes the {@link #renewPresenceAndRoster() renewPresenceAndRoster} method.
	 * 
	 * @param objectId String with the object ID that connects via this engine.
	 * @param password Password string for authentication.
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI. 
	 * @param connectionDescriptor Connection descriptor that is using this particular instance of engine.
	 */
	public XmppMessageEngine(String objectId, String password, XMLConfiguration config, Logger logger, 
																		ConnectionDescriptor connectionDescriptor) {
		super(objectId, password, config, logger, connectionDescriptor);
		
		connection = null;
		chatManager = null;
		roster = null;
		
		// initialise map with opened chats
		openedChats = new HashMap<EntityBareJid, Chat>();
		
		// compute the random time in seconds after which a roster will be renewed
		long timeForRosterRenewal = (long) ((Math.random() * ((ROSTER_RELOAD_TIME_MAX - ROSTER_RELOAD_TIME_MIN) + 1)) 
				+ ROSTER_RELOAD_TIME_MIN) * 1000;
		
		logger.finest("XmppMessageEngine: The roster for " + objectId + " will be renewed every " 
				+ timeForRosterRenewal + "ms.");
		
		
		// timer for roster renewing
		Timer timerForRosterRenewal = new Timer();
		timerForRosterRenewal.schedule(new TimerTask() {
			@Override
			public void run() {
				renewPresenceAndRoster();
			}
		}, timeForRosterRenewal, timeForRosterRenewal);
		
		// enable debugging if desired
		boolean debuggingEnabled = config.getBoolean(CONFIG_PARAM_XMPPDEBUG, CONFIG_DEF_XMPPDEBUG);
		if (debuggingEnabled) {
			SmackConfiguration.DEBUG = debuggingEnabled;
			logger.config("XMPP debugging enabled.");
		}
	}

	

	/**
	 * Connects to the XMPP server and logs the user in. It also registers a listener for incoming messages for this
	 * connection (see {@link org.jivesoftware.smack.chat2.ChatManager ChatManager}).  In case of failure it is
	 * possible to re-try. It attempts to be optimised in a sense that it does not rebuilds a 
	 * {@link org.jivesoftware.smack.AbstractXMPPConnection connection} object if it already exists.
	 * 
	 * @param objectId ID that serves as XMPP user name.
	 * @param password Password for authentication.
	 * @return True on success, false otherwise.
	 */
	@Override
	public boolean connect() {
		
		if (connection == null) {
			logger.fine("XmppMessageEngine: Connection object not yet exists for " + objectId 
					+ ". Attempting to build a new one.");
			connection = buildNewConnection(objectId, password);
		} else {
			logger.fine("XmppMessageEngine: Connection object already exists for " + objectId 
					+ ". Not attempting to build a new one.");
		}
		
		// connect & login
		try {
			if (connection.connect() == null){
				
				logger.warning("Connection to XMPP could not be established for '" + objectId + "'.");
				
				return false;
			}
			
			connection.login();
			
		} catch (SmackException | IOException | XMPPException | InterruptedException e) {
			
			logger.severe("Exiting due to exception during establishing a connection to XMPP server. Message: " 
					+ e.getMessage());
			
			return false;
		}
		
		chatManager = ChatManager.getInstanceFor(connection);
		chatManager.addIncomingListener(new IncomingChatMessageListener(){
			
			@Override
			public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
				processMessage(from, message, chat);
			}
			
		});
		
		// spawn a roster and associate calls for changes in roster - if necessary
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
		
		
		renewPresenceAndRoster();
		
		return true;
	}
	

	/**
	 * Closes the connection to the XMPP server. The connection can be re-opened.
	 */
	@Override
	public void disconnect() {
		
		if (connection != null && connection.isConnected()){
			
			connection.disconnect();
			
			logger.fine("XMPP user '" + objectId + "' disconnected.");
		} else {
			logger.fine("XMPP user '" + objectId + "' is already disconnected.");
		}		
	}

	
	/**
	 * Returns true if the connection to the XMPP server is opened.
	 * 
	 * @return True or false.
	 */
	@Override
	public boolean isConnected() {
		return connection.isConnected();
	}

	
	/**
	 * Retrieves a contact list of the current XMPP user.
	 * 
	 * @return A set of object IDs from the {@link org.jivesoftware.smack.roster.Roster Roster} for this 
	 * connection. In case of error it just returns empty set (not null).
	 */
	@Override
	public Set<String> getRoster() {
		
		Set<String> rosterSet = new HashSet<String>();
		
		if (connection == null || !connection.isConnected()){
			logger.warning("Invalid connection in descriptor for username '" + objectId + "'.");
			return Collections.emptySet();
		}
		
		Collection<RosterEntry> entries = roster.getEntries();
		for (RosterEntry entry : entries) {
			rosterSet.add(entry.getJid().getLocalpartOrNull().toString());
		}
		
		return rosterSet;
	}

	
	
	/**
	 * Sends a string to the destination XMPP user name.  
	 * 
	 * @param destinationUsername Destination contact, for which the message is intended. 
	 * @param message A string to send.
	 * @return True on success, false if the destination object is offline or if error occurred.
	 */
	@Override
	public boolean sendMessage(String destinationObjectID, String message) {
		destinationObjectID = destinationObjectID + "@" 
									+ config.getString(CONFIG_PARAM_XMPPDOMAIN, CONFIG_DEF_XMPPDOMAIN);

		EntityBareJid jid;

		try {
			jid = JidCreate.entityBareFrom(destinationObjectID);
		} catch (XmppStringprepException e) {
			logger.warning("XMPPMessageEngine: Destination can't be resolved. Exception: " + e.getMessage());
			return false;
		}

		
		// better do this
		if (roster.isLoaded()) {
			logger.finest("XMPPMessageEngine: Status of the roster before message is sent: ready");
		} else {
			logger.finest("XMPPMessageEngine: Roster is not loaded yet when sending message. Attempting a reload...");
			try {
				roster.reloadAndWait();
				logger.finest("XMPPMessageEngine: Roster reloaded.");
			} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
				logger.warning("XMPPMessageEngine: Roster could not be reloaded. Exception: " + e.getMessage());
			}
		}
		
		
		// check whether the destination is in our contact list
		Chat chat;
		
		if (roster.contains(jid)) {
			
			// try to find older opened chat, so we don't have to open a new one
			chat = openedChats.get(jid);
			
			if (chat == null){
				chat = chatManager.chatWith(jid);
				openedChats.put(jid, chat);
			} 
			
			// fire the thing
			try {
				
				chat.send(message);
			} catch (NotConnectedException | InterruptedException e) {
				logger.warning("XMPPMessageEngine: Message could not be sent. Exception: " + e.getMessage());
				return false;
			}
				
		} else {
			
			// the destination is not in the contact list or the sending of the message failed 
			logger.warning("XMPPMessageEngine: Message not sent. The OID " + destinationObjectID + " is not in the roster.");
			return false;
		}
		
		logger.finest("XMPPMessageEngine: Message sent. Content: " + message);
		return true;
	}
	
	
	
	

	/* === PRIVATE METHODS === */
	
	/**
	 * Builds the connection object based on OGWAPI configuration and the provided credentials.  
	 * 
	 * @param xmppUsername XMPP user name without the served domain (i.e. just 'user' instead of 'user@xmpp.server').
	 * @param xmppPassword Password of the user.
	 * @return The established connection if the attempt was successful, null otherwise.
	 */
	private AbstractXMPPConnection buildNewConnection(String xmppUsername, String xmppPassword){
		// we build a new connection here
		
		String xmppServer = config.getString(CONFIG_PARAM_SERVER, CONFIG_DEF_SERVER);
		int xmppPort = config.getInt(CONFIG_PARAM_PORT, CONFIG_DEF_PORT);
		String xmppDomain = config.getString(CONFIG_PARAM_XMPPDOMAIN, CONFIG_DEF_XMPPDOMAIN);
		boolean xmppSecurity = config.getBoolean(CONFIG_PARAM_ENCRYPTION, CONFIG_DEF_ENCRYPTION);
		
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
			logger.config("XMPP secure connection is disabled.");
		} else {
			xmppConfigBuilder.setSecurityMode(SecurityMode.required);
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
	 * This method gets executed when a new message for this object ID arrives. It then gets forwarded to its 
	 * {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor}.
	 * 
	 * @param from XMPP JID of the originating object (with domain).
	 * @param xmppMessage Received XMPP message.
	 * @param chat A chat in which the messages were exchanged.
	 */
	private void processMessage(EntityBareJid from, Message xmppMessage, Chat chat) {
		
		// try to find an opened chat
		Chat openedChat = openedChats.get(from);
		
		if (openedChat == null) {

			openedChats.put(from, chat);
		}
	
		connectionDescriptor.processIncommingMessage(from.getLocalpart().toString(), xmppMessage.getBody());
	}
	
	
	/**
	 * The rosters in SMACK sometimes failed to initialise in the real production environment, especially when 
	 * OGWAPI is trying to log in many users at once. This method gets triggered by a timer initialised in the constructor
	 * and periodically retrieves new version of roster, while also sending a presence stanza to server.
	 *  
	 * This might not be the best approach, but it seems to have solved some issues that users of OGWAPI were
	 * experiencing.
	 *    
	 */
	private void renewPresenceAndRoster() {
		
		if (connection != null && connection.isConnected() && roster != null) {
			// send our presence first
			Presence presence = new Presence(Presence.Type.available);
			presence.setStatus(XMPP_PRESENCE_STRING);
			
			try {
				connection.sendStanza(presence);
			} catch (Exception e) {
				
				logger.warning("XmppMessageEngine: Can't send presence stanza for " + objectId + ". Exception: " 
						+ e.getMessage());
			} 
			
			
			// then reload the roster
			try {
				roster.reloadAndWait();
				
				logger.finest("XmppMessageEngine: The roster for " + objectId + " was renewed.");
				
			} catch (NotLoggedInException | NotConnectedException | InterruptedException e) {
				logger.warning("Roster could not be reloaded for " + objectId + ". Exception: " + e.getMessage());
			}
			
			
		}
		
	}
	
	
	

	/**
	 * A callback method called when entries are added into the {@link org.jivesoftware.smack.roster.Roster roster}.
	 * 
	 * In this implementation it serves nothing, but can be helpful when debugging.
	 * 
	 * @param addresses A collection of {@link org.jxmpp.jid.Jid JID} addresses that were added.
	 */
	private void processRosterEntriesAdded(Collection<Jid> addresses){
		
		/*
		for(Jid address : addresses){
			System.out.println("processRosterEntriesAdded: " + address.toString());
		}
		
		System.out.println("Roster entries added");
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
	 * In this implementation it serves nothing, but can be helpful when debugging.
	 * 
	 * @param addresses A collection of {@link org.jxmpp.jid.Jid JID} addresses that were deleted.
	 */
	private void processRosterEntriesDeleted(Collection<Jid> addresses){
		
		/*
		for(Jid address : addresses){
			System.out.println("processRosterEntriesDeleted: " + address.toString());
		}
		
		System.out.println("Roster entries deleted");
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
	 * In this implementation it serves nothing, but can be helpful when debugging.
	 * 
	 * @param addresses A collection of {@link org.jxmpp.jid.Jid JID} addresses that were updated.
	 */
	private void processRosterEntriesUpdated(Collection<Jid> addresses) {

		/*
		for(Jid address : addresses){
			System.out.println("processRosterEntriesUpdated: " + address.toString());
		}
		
		System.out.println("Roster entries updated");
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
	 * In this implementation it serves nothing, but can be helpful when debugging.
	 * 
	 * @param presence A new {@link org.jivesoftware.smack.packet.Presence presence}.
	 */
	private void processRosterPresenceChanged(Presence presence) {
		//System.out.println("processRosterPresenceChanged - Presence changed: " + presence.getFrom() + " " + presence);
	}
}
