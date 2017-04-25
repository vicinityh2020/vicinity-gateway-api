package eu.bavenir.vicinity.gatewayapi.xmpp;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * - what classes use this class and why
 * - how connections are stored
 * - how are retrieved
 * - connection and disconnection
 * 
 * @author sulfo
 *
 */
public class XmppConnector {

	
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
	 * Name of the configuration parameter for a domain served by the XMPP server.
	 */
	private static final String CONFIG_PARAM_XMPPDOMAIN = "xmpp.domain";
	
	/**
	 * Name of the configuration parameter for enabling security.
	 */
	private static final String CONFIG_PARAM_XMPPSECURITY = "xmpp.security";
	
	/**
	 * Name of the configuration parameter for XMPP .
	 */
	private static final String CONFIG_PARAM_XMPPDEBUG = "xmpp.debug";
	
	/**
	 * Default value of xmpp.server configuration parameter. This value is taken into account when no suitable
	 * value is found in the configuration file. 
	 */
	private static final String CONFIG_DEF_XMPPSERVER = "";
	
	/**
	 * Default value of xmpp.port configuration parameter. This value is taken into account when no suitable
	 * value is found in the configuration file. 
	 */
	private static final int CONFIG_DEF_XMPPPORT = 5222;
	
	/**
	 * Default value of xmpp.domain configuration parameter. This value is taken into account when no suitable
	 * value is found in the configuration file. 
	 */
	private static final String CONFIG_DEF_XMPPDOMAIN = "vicinity.eu";
	
	/**
	 * Default value of xmpp.debug configuration parameter. This value is taken into account when no suitable
	 * value is found in the configuration file. 
	 */
	private static final boolean CONFIG_DEF_XMPPDEBUG = false;
	
	/**
	 * Default value of xmpp.security configuration parameter. This value is taken into account when no suitable
	 * value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_XMPPSECURITY = true;
	

	
	/* === FIELDS === */
	
	// hash map containing XMPP connections identified by 
	private Map<String, AbstractXMPPConnection> connectionPool;
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	
	
	/* === PUBLIC METHODS === */
	
	
	/**
	 * Constructor, initializes necessary objects. All parameters are mandatory, failure to include them can lead 
	 * to a swift end of application execution.
	 * 
	 * @param config Configuration object.
	 * @param logger Java logger.
	 */
	public XmppConnector(XMLConfiguration config, Logger logger){
		connectionPool = Collections.synchronizedMap(new HashMap<String, AbstractXMPPConnection>());
		 
		this.config = config;
		this.logger = logger;
		 
		// enable debugging if desired
		boolean debuggingEnabled = config.getBoolean(CONFIG_PARAM_XMPPDEBUG, CONFIG_DEF_XMPPDEBUG);
		if (debuggingEnabled) {
			SmackConfiguration.DEBUG = debuggingEnabled;
			logger.config("XMPP debugging enabled.");
		}
	}
	
	
	
	/**
	 * Establishes a connection to XMPP server with preferences from configuration file and provided credentials.
	 * The connection is then stored in a hash map.
	 * 
	 * @param xmppUsername XMPP user name without the served domain (i.e. just 'user' instead of 'user@xmpp.server').
	 * @param xmppPassword Password of the user.
	 * @return The established connection if the attempt was successful, null otherwise.
	 */
	public AbstractXMPPConnection connect(String xmppUsername, String xmppPassword){
		
		AbstractXMPPConnection connection = connectionPool.get(xmppUsername);
		
		if (connection == null){
			
			connection = buildNewConnection(xmppUsername, xmppPassword);
			
			if (connection == null){
				logger.severe("Something got seriously wrong when building a new XMPP connection.");
				return null;
			}
			
		} else {
			logger.config("Reconnecting '" + xmppUsername + "' to XMPP.");
			// rather close the thing...
			connection.disconnect();
		}
		
		// connect & login
		try {
			
			if (connection.connect() == null){
				
				// oops
				logger.warning("Connection to XMPP could not be established for user '" + xmppUsername + "'.");
				return null;
			}
			
			connection.login();
			
			
			
		} catch (SmackException | IOException | XMPPException | InterruptedException e) {
			
			// double oops
			logger.severe("Exiting due to exception during establishing a connection to XMPP server. Message: " 
					+ e.getMessage());
			
			return null;
		}
		
		logger.info("XMPP connection for '" + xmppUsername +"' was established.");
		
		
		// insert the connection into the hash map - if there was a previous connection, put method replaces it and
		// returns the old one, which we can close
		if (connectionPool.put(xmppUsername, connection) == null){
			logger.finest("A new connection for '" + xmppUsername +"' was added into connection pool.");
		} else {
			logger.finest("The old connection for '" + xmppUsername +"' was replaced in connection pool.");
		}
		
		return connection;
	}
	
	
	
	/**
	 * Disconnects a single XMPP connection identified by the connection user name given as the first parameter. 
	 * The second parameter specifies, whether the connection handler is to be destroyed after it is disconnected.
	 * If not, the handler will remain in the pool and it is possible to use it during eventual reconnection.
	 * Otherwise connection will not be able to reconnect and will have to be build a new (which can be useful when 
	 * the connection needs to be reconfigured). 
	 * 
	 * @param xmppUsername User name used to establish the connection.
	 * @param destroyConnection Whether the connection should also be destroyed or not. 
	 */
	public void disconnect(String xmppUsername, boolean destroyConnection){
		
		//TODO when the connection is closed without being destroyed, there is a SMACK warning about roster not
		// loaded during presence stanza processing. Therefore NOT destroying the connection is as of May 2017 not
		// recommended.
		
		if (connectionPool.containsKey(xmppUsername)){
			
			AbstractXMPPConnection connection;
			
			if (destroyConnection){
				connection = connectionPool.remove(xmppUsername);
				logger.info("Closing connection to XMPP for user '" + xmppUsername + "'.");
			} else {
				// this will keep the connection in the pool
				connection = connectionPool.get(xmppUsername);
				logger.info("Destroying connection to XMPP for user '" + xmppUsername + "'.");
			}
			
			if (connection != null){
				
				if (connection.isConnected()){
					connection.disconnect();
					logger.fine("XMPP user '" + xmppUsername + "' disconnected.");
				} else {
					logger.fine("XMPP user '" + xmppUsername + "' is already disconnected.");
				}
				
			} else {
				logger.warning("Null record in the connection pool. XMPP user: '" 
						+ xmppUsername + "'.");
			}
			
		} else {
			logger.warning("Can't disconnect XMPP user '" + xmppUsername + "', connection does not exist.");
		}
	}
	
	
	
	/**
	 * Closes all open connections to XMPP server. If the destroyConnections parameter is set to true, it will also 
	 * clear these connection handlers off the connection pool table, preventing the re-connection (they have to be
	 * reconfigured to be opened again).
	 *  
	 * @param destroyConnections Whether the connections should be destroyed or just disconnected.
	 */
	public void disconnectAll(boolean destroyConnections){
		Collection<AbstractXMPPConnection> connections = connectionPool.values();
		
		logger.info("Closing all connections to XMPP server.");
		
		for (AbstractXMPPConnection connection : connections){
			if (connection != null){
				if (connection.isConnected()){
					connection.disconnect();
					logger.fine("XMPP user '" + connection.getUser() + "' disconnected.");
				} else {
					logger.fine("XMPP user '" + connection.getUser() + "' is already disconnected.");
				}
			} else {
				logger.warning("Null record in the connection pool.");
			}
		}
		
		//TODO when the connection is closed without being destroyed, there is a SMACK warning about roster not
		// loaded during presence stanza processing. Therefore NOT destroying the connection is as of May 2017 not
		// recommended.
		
		if (destroyConnections){
			connectionPool.clear();
			logger.finest("XMPP connection pool flushed.");
		}
	}
	
	
	
	/**
	 * Retrieves the XMPP user names that has open connections to XMPP server. Based on these strings, the respective
	 * connections can be retrieved from the connection pool.
	 * 
	 * @return Set of user names. 
	 */
	public Set<String> getConnectedUsernames(){
		
		Set<String> usernames = connectionPool.keySet();
		
		logger.finest("-- XMPP users connected to the server: --");
		for (String string : usernames) {
			logger.finest(string);
		}
		logger.finest("-- End of list. --");
		return usernames;
	}
	
	
	
	/**
	 * Retrieves a collection of roster entries for given user name. If there is no connection established with the 
	 * given user name, returns null. 
	 * 
	 * @param username XMPP user name for which the roster is to be retrieved. 
	 * @return Collection of roster entries, or null if no connection is established for the usr name. 
	 */
	public Collection<RosterEntry> getRosterForUser(String username){
		
		AbstractXMPPConnection connection = connectionPool.get(username);
		
		if (connection == null || !connection.isConnected()){
			return null;
		}
		
		Roster roster = Roster.getInstanceFor(connection);
		Collection<RosterEntry> entries = roster.getEntries();
		
		// log it
		logger.finest("-- Roster for '" + username +"' --");
		for (RosterEntry entry : entries) {
			logger.finest(entry.getJid().toString() + " Presence: " + roster.getPresence(entry.getJid()).toString());
		}
		logger.finest("-- End of roster --");
		
		return roster.getEntries();
	}
	
	
	
	/*
	// create a chat manager
	chatManager = ChatManager.getInstanceFor(connection);
	
	// TODO
	chatManager.addIncomingListener(new IncomingChatMessageListener() {
		@Override
		public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
			//////// TODO
			System.out.println("New message from " + from + ": " + message.getBody());
		}
	});
	
	
		// TODO 
	public void sendMessage(){

		EntityBareJid jid;
		try {
			jid = JidCreate.entityBareFrom("user1@vicinity.eu");
			Chat chat = chatManager.chatWith(jid);
			chat.send("Hello from User0!");
					
		} catch (XmppStringprepException | NotConnectedException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	*/
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Builds the connection object from Gateway configuration and provided credentials. 
	 * 
	 * @param xmppUsername XMPP user name without the served domain (i.e. just 'user' instead of 'user@xmpp.server').
	 * @param xmppPassword Password of the user.
	 * @return The established connection if the attempt was successful, null otherwise.
	 */
	private AbstractXMPPConnection buildNewConnection(String xmppUsername, String xmppPassword){
		// we build a new connection here
		
		String xmppServer = config.getString(CONFIG_PARAM_XMPPSERVER, CONFIG_DEF_XMPPSERVER);
		int xmppPort = config.getInt(CONFIG_PARAM_XMPPPORT, CONFIG_DEF_XMPPPORT);
		String xmppDomain = config.getString(CONFIG_PARAM_XMPPDOMAIN, CONFIG_DEF_XMPPDOMAIN);
		boolean xmppSecurity = config.getBoolean(CONFIG_PARAM_XMPPSECURITY, CONFIG_DEF_XMPPSECURITY);
		
		logger.config("Creating a new connection to XMPP server '" + xmppServer + ":" + xmppPort + "' as '" 
						+ xmppUsername + "@" + xmppDomain + "'");
		
		// prepare configuration builder by feeding it the preferences
		XMPPTCPConnectionConfiguration.Builder xmppConfigBuilder = XMPPTCPConnectionConfiguration.builder();
		xmppConfigBuilder.setUsernameAndPassword(xmppUsername, xmppPassword);
		xmppConfigBuilder.setHost(xmppServer);
		xmppConfigBuilder.setPort(xmppPort);
		
		if (!xmppSecurity) {
			xmppConfigBuilder.setSecurityMode(SecurityMode.disabled);
			logger.config("XMPP security is DISABLED.");
		} else {
			// default is enabled
			logger.config("XMPP security is ENABLED");
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
}
