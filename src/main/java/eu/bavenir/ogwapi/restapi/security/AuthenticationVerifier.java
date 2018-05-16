package eu.bavenir.ogwapi.restapi.security;

import java.util.logging.Logger;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.security.Verifier;

import eu.bavenir.ogwapi.commons.CommunicationManager;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;

// !!!!!!!!!!!! 
// http://www.programcreek.com/java-api-examples/index.php?api=org.restlet.security.Verifier
// https://stackoverflow.com/questions/29527809/how-can-i-add-a-token-to-challenge-based-authenticator-in-restlet
// https://templth.wordpress.com/2015/01/05/implementing-authentication-with-tokens-for-restful-applications/
// https://github.com/auth0/java-jwt
// https://restlet.com/open-source/documentation/javadocs/2.3/jse/api/org/restlet/security/Verifier.html#verify(org.restlet.Request,%20org.restlet.Response)
// https://stackoverflow.com/questions/5199554/restful-authentication-resulting-poor-performance-on-high-load

/**
 * Verifier usable in the P2P network. The credentials are verified two ways:
 * 
 *  1. The clients logs in for the first time:
 *  	In such case, the credentials are verified by whether or not the 
 *  	{@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} can be created, i.e.
 *  	whether it is possible to log into the network with provided credentials.
 *  
 *  2. The client is already logged and has a {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor} created:
 *  	ConnectionDescriptor, after successful login, contains the password that was used to connect to the network 
 *  	as one of its fields. Credentials are then compared when this verifier is called.
 *  
 * @author sulfo
 *
 */
public class AuthenticationVerifier implements Verifier {

	/**
	 * {@link CommunicationManager CommunicationManager} used for the authentication.
	 */
	private CommunicationManager communicationManager;
	
	/**
	 * {@link Logger Logger} used for logging.
	 */
	private Logger logger;
	
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * the descriptor will not be able to connect (in the best case scenario, the other being a storm of null pointer 
	 * exceptions).
	 */
	public AuthenticationVerifier(CommunicationManager communicationNode, Logger logger){
		this.communicationManager = communicationNode;
		this.logger = logger;
	}
	
	
	/**
	 * Overridden method from super class.  
	 */
	@Override
	public int verify(Request request, Response response) {
		
		ChallengeResponse cr = request.getChallengeResponse();
		
		if (cr == null){
			logger.info("Missing credentials in request from a client with IP " 
										+ request.getClientInfo().getAddress() + ".");
			return Verifier.RESULT_MISSING;
		}
		
		String objectID = cr.getIdentifier();
		String password = new String(cr.getSecret());
		
		if (communicationManager.isConnected(objectID)){
			// if the client is already connected, just verify the password
			if (!communicationManager.verifyPassword(objectID, password)){
				logger.info("Invalid credentials in request from a client with IP " 
						+ request.getClientInfo().getAddress() + ".");
				return Verifier.RESULT_INVALID;
			}
		} else {
			// if not, establish a connection
			StatusMessage statusMessage = communicationManager.establishConnection(objectID, password); 
			if (statusMessage.isError()){
				logger.info("Invalid credentials in request from a client with IP " 
											+ request.getClientInfo().getAddress() + ".");
				return Verifier.RESULT_INVALID;
			}
		}
		
		logger.fine("Valid credentials received from a client with IP " + request.getClientInfo().getAddress() + ".");
		return Verifier.RESULT_VALID;
	}
}
