package eu.bavenir.vicinity.gatewayapi.restapi;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.restlet.security.Verifier;

import eu.bavenir.vicinity.gatewayapi.restapi.security.RestletJwtVerifier;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceAdapters;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceAdaptersObjects;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceAdaptersSubscriptions;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceFeeds;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceObjects;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceObjectsActions;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceObjectsActionsTasks;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceObjectsLogin;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceObjectsProperties;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceObjectsSubscriptions;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceSparql;
import eu.bavenir.vicinity.gatewayapi.restapi.services.ResourceSubscriptions;
import eu.bavenir.vicinity.gatewayapi.xmpp.XmppController;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

// TODO dopisat detaily autentifikacie

/**
 * RESTLET application that serves incoming calls for the Gateway API. After being instantialized, it initializes
 * objects necessary to be available to all API services (like XMPP controller). It routes the requests to their 
 * respective {@link org.restlet.resource.ServerResource Resources}. The HTTP authentication against Gateway API is 
 * also provided by this class.
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
	 * Contextual name of the {@link eu.bavenir.vicinity.gatewayapi.xmpp.XmppController XMPPcontroller}, object
	 * inserted into the context.
	 */
	public static final String CONTEXT_XMPPCONTROLLER = "xmppController";
	
	/**
	 * Name of the configuration parameter for setting the realm of RESTLET BEARER authentication schema. 
	 */
	private static final String CONF_PARAM_AUTHREALM = "authRealm";
	
	/**
	 * Default value for setting the realm of RESTLET BEARER authentication schema.
	 */
	private static final String CONF_DEF_AUTHREALM = "vicinity.eu";
	
	
	/* === FIELDS === */
	
	// obligatory stuff
	private XMLConfiguration config;
	private Logger logger;
	
	// communication controller
	private XmppController xmppController;
	
	private Context applicationContext;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initializes necessary objects and inserts the {@link org.apache.commons.configuration2.XMLConfiguration
	 * configuration}, {@link java.util.logging.Logger logger} and {@link eu.bavenir.vicinity.gatewayapi.xmpp.XmppController
	 * XMPPcontroller} into the RESTLET {@link org.restlet.Context context}.
	 * 
	 * All parameters are mandatory, failure to include them will lead to a swift end of application execution.
	 * 
	 * @param config Configuration object.
	 * @param logger Java logger.
	 */
	public Api(XMLConfiguration config, Logger logger){
		this.config = config;
		this.logger = logger;
		
		// this will initialize the XMPP controller
		xmppController = new XmppController(config, logger);
		
		// insert stuff into context
		applicationContext = new Context();
		
		applicationContext.getAttributes().put(CONTEXT_CONFIG, config);
		applicationContext.getAttributes().put(CONTEXT_LOGGER, logger);
		applicationContext.getAttributes().put(CONTEXT_XMPPCONTROLLER, xmppController);
		
		setContext(applicationContext);
		
		// TODO - remove this after testing and move it to authentication call
		xmppController.establishConnection("user0", "user0");
	}
	
	
	/**
	 * Creates a root RESTLET that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		
		
		// create a router Restlet that routes each call to a new instance of 
		Router router = new Router(getContext());
		
		// authenticator
		ChallengeAuthenticator authenticator = createAuthenticator();
		
		// define routes
		// see https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/
		
		
		router.attach("/objects/login", ResourceObjectsLogin.class);
		
		// registry
		router.attach("/adapters", ResourceAdapters.class);
		router.attach("/adapters/{adid}", ResourceAdapters.class);
		router.attach("/adapters/{adid}/objects", ResourceAdaptersObjects.class);
		router.attach("/adapters/{adid}/subscriptions", ResourceAdaptersSubscriptions.class);
		
		router.attach("/objects", ResourceObjects.class);
		router.attach("/objects/{oid}", ResourceObjects.class);
		router.attach("/objects/{oid}/subscriptions", ResourceObjectsSubscriptions.class);
		
		router.attach("/subscriptions", ResourceSubscriptions.class);
		router.attach("/subscriptions/{sid}", ResourceSubscriptions.class);
		
		// discovery
		router.attach("/feeds", ResourceFeeds.class);
		router.attach("/feeds/{fid}", ResourceFeeds.class);
		
		// consumption
		router.attach("/objects/{oid}/properties/{pid}", ResourceObjectsProperties.class);
		router.attach("/objects/{oid}/actions/{aid}", ResourceObjectsActions.class);
		router.attach("/objects/{oid}/actions/{aid}/tasks/{tid}", ResourceObjectsActionsTasks.class);
		
		// query
		router.attach("/sparql", ResourceSparql.class);
		
		// TODO uncomment to enable authentication
		// authenticator.setNext(router);
		// return authenticator;
		
		return router;
	}

	
	/* === PRIVATE METHODS === */
	
	// TODO
	private ChallengeAuthenticator createAuthenticator() {
		Context context = getContext();
		final boolean optional = false;
		ChallengeScheme challengeScheme = ChallengeScheme.HTTP_OAUTH_BEARER;
		String realm = config.getString(CONF_PARAM_AUTHREALM, CONF_DEF_AUTHREALM);

		///////////////////
		MapVerifier verifier = new MapVerifier();
		//verifier.getLocalSecrets().put("scott", "tiger".toCharArray());
		verifier.getLocalSecrets().put("user0", "user0".toCharArray());
		
		RestletJwtVerifier jwtVerifier = new RestletJwtVerifier();

		ChallengeAuthenticator auth = new ChallengeAuthenticator(
								context, optional, challengeScheme, realm, jwtVerifier);
		
		return auth;
    }
	
}
