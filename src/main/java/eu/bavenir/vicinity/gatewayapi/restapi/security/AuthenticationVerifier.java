package eu.bavenir.vicinity.gatewayapi.restapi.security;

import java.util.logging.Logger;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.security.Verifier;

import eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode;

// !!!!!!!!!!!! 
// http://www.programcreek.com/java-api-examples/index.php?api=org.restlet.security.Verifier
// https://stackoverflow.com/questions/29527809/how-can-i-add-a-token-to-challenge-based-authenticator-in-restlet
// https://templth.wordpress.com/2015/01/05/implementing-authentication-with-tokens-for-restful-applications/
// https://github.com/auth0/java-jwt
// https://restlet.com/open-source/documentation/javadocs/2.3/jse/api/org/restlet/security/Verifier.html#verify(org.restlet.Request,%20org.restlet.Response)
// https://stackoverflow.com/questions/5199554/restful-authentication-resulting-poor-performance-on-high-load

// TODO javadoc
public class AuthenticationVerifier implements Verifier {

	private CommunicationNode communicationNode;
	private Logger logger;
	
	
	public AuthenticationVerifier(CommunicationNode communicationNode, Logger logger){
		this.communicationNode = communicationNode;
		this.logger = logger;
	}
	
	
	@Override
	public int verify(Request request, Response response) {
		
		ChallengeResponse cr = request.getChallengeResponse();
		
		if (cr == null){
			logger.info("Missing credentials in request from a client with IP " 
										+ request.getClientInfo().getAddress() + ".");
			return Verifier.RESULT_MISSING;
		}
		
		String username = cr.getIdentifier();
		String password = new String(cr.getSecret());
		
		if (communicationNode.isConnected(username)){
			// if the client is already connected to XMPP, just verify the password
			if (!communicationNode.verifyConnection(username, password)){
				logger.info("Invalid credentials in request from a client with IP " 
						+ request.getClientInfo().getAddress() + ".");
				return Verifier.RESULT_INVALID;
			}
		} else {
			// if not, establish a connection
			if (communicationNode.establishConnection(username, password) == null){
				logger.info("Invalid credentials in request from a client with IP " 
											+ request.getClientInfo().getAddress() + ".");
				return Verifier.RESULT_INVALID;
			}
		}
		
		logger.fine("Valid credentials received from a client with IP " + request.getClientInfo().getAddress() + ".");
		return Verifier.RESULT_VALID;
	}

	
	
	/*
	 * 	@Override
	public int verify(Request request, Response response) {
		
		ChallengeResponse cr = request.getChallengeResponse();
		
		
		String token = cr.getRawValue();
		
		//checkToken(token);
		
		return 0;
		
	}
	 */
	
	//HMAC
	//private Algorithm algorithmHS;

	// TODO - not a good way doing it in ctor. something like 'if it exist, use it, if not, create it and throw exc if
	// not in a mood'...
	
	/*
	public RestletJwtVerifier(){
		 try {
			algorithmHS = Algorithm.HMAC256("secret");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	//RSA
	//RSAPublicKey publicKey = //Get the key instance
	//RSAPrivateKey privateKey = //Get the key instance
	//Algorithm algorithmRS = Algorithm.RSA256(publicKey, privateKey);
	
	
	
	
	
	
	
	/* 
	 * (non-Javadoc)
	 * @see org.restlet.security.Verifier#verify(org.restlet.Request, org.restlet.Response)
	 * 
	 * 
	 * toto bolo v objectslogin
	 * 
	 * 	@Get
	public String represent() {

		String testBefore = (String) this.getContext().getAttributes().get("TEST_STRING");
		
		String testAfter = testBefore + " and again";
		
		this.getContext().getAttributes().put("TEST_STRING", testAfter);
		
		return testBefore;
	}
	 * 
	 * 
	 */
	
	
	

	
}
