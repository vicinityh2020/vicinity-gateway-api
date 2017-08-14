package eu.bavenir.vicinity.gatewayapi.restapi;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;

import eu.bavenir.vicinity.gatewayapi.restapi.security.AuthenticationVerifier;
import eu.bavenir.vicinity.gatewayapi.restapi.services.Adapters;
import eu.bavenir.vicinity.gatewayapi.restapi.services.AdaptersAdid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.AdaptersAdidObjects;
import eu.bavenir.vicinity.gatewayapi.restapi.services.AdaptersAdidSubscriptions;
import eu.bavenir.vicinity.gatewayapi.restapi.services.Feeds;
import eu.bavenir.vicinity.gatewayapi.restapi.services.FeedsFid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.Objects;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsLogin;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsLogout;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOidActionsAid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOidActionsAidTasksTid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOidEventsEid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOidEventsEidAcknowledge;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOidPropertiesPid;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ObjectsOidSubscriptions;
import eu.bavenir.vicinity.gatewayapi.restapi.services.Sparql;
import eu.bavenir.vicinity.gatewayapi.restapi.services.Subscriptions;
import eu.bavenir.vicinity.gatewayapi.restapi.services.SubscriptionsSid;
import eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * RESTLET application that serves incoming calls for the Gateway API. After being instantialized, it initializes
 * objects necessary to be available to all API services (like Communication Node). It routes the requests to their 
 * respective {@link org.restlet.resource.ServerResource Resources}. The HTTP authentication against Gateway API is 
 * also checked in the {@link eu.bavenir.vicinity.gatewayapi.restapi.security.AuthenticationVerifier AuthenticationVerifier}.
 * 
 * @see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Api extends Application {
	
	/* === CONSTANTS === */
	
	/**
	 * Contextual name of the {@link org.apache.commons.configuration2.XMLConfiguration configuration} object inserted
	 * into the context.
	 */
	public static final String CONTEXT_CONFIG = "config";
	
	/**
	 * Contextual name of the {@link java.util.logging.Logger logger} object inserted into the context. 
	 */
	public static final String CONTEXT_LOGGER = "logger";
	
	/**
	 * Contextual name of the {@link eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode CommunicationNode}, object
	 * inserted into the context.
	 */
	public static final String CONTEXT_COMMNODE = "communicationNode";
	
	/**
	 * Name of the configuration parameter for setting the realm of RESTLET BEARER authentication method. 
	 */
	private static final String CONF_PARAM_AUTHREALM = "api.authRealm";
	
	/**
	 * Name of the configuration parameter for setting the authentication method.
	 */
	private static final String CONF_PARAM_AUTHMETHOD = "api.authMetod";
	
	/**
	 * Default value for setting the realm of RESTLET BEARER authentication schema.
	 */
	private static final String CONF_DEF_AUTHREALM = "vicinity.eu";
	
	/**
	 * Default value for setting the authentication method.
	 */
	private static final String CONF_DEF_AUTHMETHOD = "basic";
	
	
	
	
	/* === FIELDS === */
	
	// obligatory stuff
	private XMLConfiguration config;
	private Logger logger;
	
	// communication node
	private CommunicationNode communicationNode;
	
	// application context
	private Context applicationContext;
	
	// whether to use authentication or not - not using authentication is considered to be for debugging purposes only
	// as the credentials for XMPP network need to be hard coded in such case
	private boolean useAuthentication;
	
	// challenge scheme to use 
	private ChallengeScheme challengeScheme;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initializes necessary objects and inserts the {@link org.apache.commons.configuration2.XMLConfiguration
	 * configuration}, {@link java.util.logging.Logger logger} and {@link eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode
	 * CommunicationNode} into the RESTLET {@link org.restlet.Context context}.
	 * 
	 * All parameters are mandatory, failure to include them will lead to a swift end of application execution.
	 * 
	 * @param config Configuration object.
	 * @param logger Java logger.
	 */
	public Api(XMLConfiguration config, Logger logger){
		this.config = config;
		this.logger = logger;
		
		// this will initialize the CommunicationNode
		communicationNode = new CommunicationNode(config, logger);
		
		// insert stuff into context
		applicationContext = new Context();
		
		applicationContext.getAttributes().put(CONTEXT_CONFIG, config);
		applicationContext.getAttributes().put(CONTEXT_LOGGER, logger);
		applicationContext.getAttributes().put(CONTEXT_COMMNODE, communicationNode);
		
		applicationContext.setLogger(logger);
		
		setContext(applicationContext);
		
		// load authentication challenge scheme method from configuration
		configureChallengeScheme();
	}
	
	
	/**
	 * Creates a root RESTLET that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		
		
		// create a router Restlet that routes each call to a new instance of 
		Router router = new Router(getContext());

		// define routes
		// see https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/
		router.attach("/objects/login", ObjectsLogin.class);
		router.attach("/objects/logout", ObjectsLogout.class);
		
		// registry
		router.attach("/adapters", Adapters.class);
		router.attach("/adapters/{adid}", AdaptersAdid.class);
		router.attach("/adapters/{adid}/objects", AdaptersAdidObjects.class);
		router.attach("/adapters/{adid}/subscriptions", AdaptersAdidSubscriptions.class);
		
		router.attach("/objects", Objects.class);
		router.attach("/objects/{oid}", ObjectsOid.class);
		router.attach("/objects/{oid}/subscriptions", ObjectsOidSubscriptions.class);
		
		router.attach("/subscriptions", Subscriptions.class);
		router.attach("/subscriptions/{sid}", SubscriptionsSid.class);
		
		// discovery
		router.attach("/feeds", Feeds.class);
		router.attach("/feeds/{fid}", FeedsFid.class);
		
		// consumption
		router.attach("/objects/{oid}/properties/{pid}", ObjectsOidPropertiesPid.class);
		router.attach("/objects/{oid}/actions/{aid}", ObjectsOidActionsAid.class);
		router.attach("/objects/{oid}/actions/{aid}/tasks/{tid}", ObjectsOidActionsAidTasksTid.class);
		router.attach("/objects/{oid}/events/{eid}/acknowledge", ObjectsOidEventsEidAcknowledge.class);
		
		// exposing service
		router.attach("/objects/{oid}/events/{eid}", ObjectsOidEventsEid.class);
		
		// query
		router.attach("/sparql", Sparql.class);
		
		// solve the question of API authentication
		if (useAuthentication){
			// create authenticator
			ChallengeAuthenticator authenticator = createAuthenticator();
			
			// enable authentication
			authenticator.setNext(router);
			return authenticator;
		} 
		
		return router;
	}

	
	/* === PRIVATE METHODS === */
	
	/**
	 * Returns an instance of ChallengeAuthenticator, that is able to authenticate requests against the XMPP network.
	 * 
	 * @return A custom ChallengeAuthenticator.
	 */
	private ChallengeAuthenticator createAuthenticator() {
		
		String realm = config.getString(CONF_PARAM_AUTHREALM, CONF_DEF_AUTHREALM);
		
		AuthenticationVerifier authVerifier = new AuthenticationVerifier(communicationNode, logger);

		ChallengeAuthenticator auth = new ChallengeAuthenticator(
								applicationContext, false, challengeScheme, realm, authVerifier);
		
		return auth;
    }

	
	
	/**
	 * Translates the authentication method string from configuration file into RESTLET readable authentication
	 * scheme. If the string is not recognized as valid scheme, or if the configuration string says "none", null
	 * is returned. 
	 * 
	 * @return RESTLET challenge scheme.
	 */
	private void configureChallengeScheme(){
		
		String challengeScheme = config.getString(CONF_PARAM_AUTHMETHOD, CONF_DEF_AUTHMETHOD);
		
		useAuthentication = true;
		
		switch (challengeScheme){
		case "basic":
			logger.config("HTTP Basic challenge authentication scheme configured.");
			this.challengeScheme = ChallengeScheme.HTTP_BASIC;
			break;
			
		case "digest":
			logger.config("HTTP Digest challenge authentication scheme configured.");
			this.challengeScheme = ChallengeScheme.HTTP_DIGEST;
			break;
			
		case "bearer":
			logger.config("HTTP Bearer challenge authentication scheme configured.");
			this.challengeScheme = ChallengeScheme.HTTP_OAUTH_BEARER;
			break;
			
		case "none":
			logger.config("No authentication for API is configured.");
			// this will disable the check for authentication method, otherwise exception is to be expected - that is
			// how the program treats invalid authentication method 
			useAuthentication = false;
			this.challengeScheme = null;
			break;
			
			default:
				logger.config("Invalid API authentication scheme.");
				this.challengeScheme = null;
		}
	}
}
