package eu.bavenir.vicinity.gatewayapi.restapi.security;

import java.io.UnsupportedEncodingException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.security.Verifier;

import com.auth0.jwt.algorithms.Algorithm;

public class RestletJwtVerifier implements Verifier {

	//HMAC
	private Algorithm algorithmHS;

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
	
	
	
	// https://restlet.com/open-source/documentation/javadocs/2.3/jse/api/org/restlet/security/Verifier.html#verify(org.restlet.Request,%20org.restlet.Response)
	
	
	
	
	@Override
	public int verify(Request request, Response response) {
		
		ChallengeResponse cr = request.getChallengeResponse();
		String token = cr.getRawValue();
		
		//checkToken(token);
		
		return 0;
		
	}
	
}
