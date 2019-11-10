package eu.bavenir.ogwapi.restapi.security;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.IOUtils;

import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;

/*
 * STRUCTURE
 * - constants
 * - fields
 * - public methods 
 * - private methods
 */


/**
 * Provides methods for signing and validating messages. 
 * It uses JWT token to give proof of the identity of the gateway against the Platform.
 * 
 * @author jorge
 *
 */

public class SecureServerComms {
	
	// === CONSTANTS ===

	/**
	 * Name of the configuration parameter for Gateway platform identity.
	 */
	private static final String CONFIG_PARAM_PLATFORMIDENTITY = "platformSecurity.identity";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_PLATFORMIDENTITY } parameter. 
	 */
	private static final String CONFIG_DEF_PLATFORMIDENTITY = "ANONYMOUS";
	
	/**
	 * Name of the configuration parameter for setting the token expiration.
	 */
	private static final String CONFIG_PARAM_EXPIRE = "platformSecurity.ttl";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_EXPIRE } parameter. 
	 */
	private static final int CONFIG_DEF_EXPIRE = 604800000;
	
	/**
	 * Name of the configuration parameter for keys path.
	 */
	private static final String CONFIG_PARAM_PATH = "platformSecurity.path";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_PATH } parameter. 
	 */
	private static final String CONFIG_DEF_PATH = "keystore/";
	
	/**
	 * Default value for ogwapi private key. 
	 */
	private static final String CONFIG_PARAM_OGWAPI_PRIV_KEY = "platformSecurity.privatekey";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_OGWAPI_PRIV_KEY } parameter. 
	 */
	private static final String CONFIG_DEF_OGWAPI_PRIV_KEY = "platform-key.der";

	/**
	 * Default value for ogwapi public key. 
	 */
	private static final String CONFIG_PARAM_OGWAPI_PUB_KEY = "platformSecurity.publickey";
	
	/**
	 * Default value for{@link #CONFIG_PARAM_OGWAPI_PUB_KEY } parameter. 
	 */
	private static final String CONFIG_DEF_OGWAPI_PUB_KEY = "platform-pubkey.der";
	
	
	/**
	 * Default value for jwt token. 
	 */
	private static final String CONFIG_DEF_TOKEN = "ogwapi-token";
	
	// === FIELDS ===
	
	/**
	 * Gateway identity. 
	 */
	private String agid;
	
	/**
	 * Define token time to live.
	 */
	private int ttl;
	
	/**
	 * Define private key name.
	 */
	private String privKey;
	
	/**
	 * Define public key name.
	 */
	private String pubKey;
	
	/**
	 * Platform token and expiration.
	 */
	private String platform_token ;
	private Long platform_token_expiration;
	
	/**
	 * Define path to keys folder.
	 */
	private String path;
	
	/**
	 * {@link Logger Logger} used for logging.
	 */
	private Logger logger;
	
	
	// === PUBLIC METHODS ===
	
	/**
	 * Constructor.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public SecureServerComms(XMLConfiguration config, Logger logger) {
		this.logger = logger;
		agid = config.getString(CONFIG_PARAM_PLATFORMIDENTITY, CONFIG_DEF_PLATFORMIDENTITY);
		ttl = config.getInt(CONFIG_PARAM_EXPIRE, CONFIG_DEF_EXPIRE);
		path = config.getString(CONFIG_PARAM_PATH, CONFIG_DEF_PATH);
		privKey = config.getString(CONFIG_PARAM_OGWAPI_PUB_KEY, CONFIG_DEF_OGWAPI_PUB_KEY);
		pubKey = config.getString(CONFIG_PARAM_OGWAPI_PRIV_KEY, CONFIG_DEF_OGWAPI_PRIV_KEY);
		platform_token_expiration = System.currentTimeMillis() + ttl;
	}
	
	// Checks if token exists in memory, in file or needs to be generated
	// Afterwards returns token
	public String getToken() {
		File file = new File(path + CONFIG_DEF_TOKEN);
		if(platform_token_expiration < System.currentTimeMillis()) {
			// If exists and it is expired, regenerate it
			String token = generateToken();
		    logger.fine("Token expired, new token created and stored");
			return token;
		} else if(platform_token != null) {
		// Check if exists in class
		    logger.fine("Loading token from memory");
			return platform_token;
		} else if(file.exists()) { 
		// Check if in file
		    logger.fine("Loading token from file");
			return loadToken(file);
		} else {
			// Otherwise regenerate
			String token = generateToken();
			logger.fine("Token generated and stored");
			return token;
		}		
	}
	
	// === PRIVATE METHODS ===
	
	// Generate token
	private String generateToken() {
		String token = "";
		String file = path + privKey;
		try {
			
			RSAPrivateKey privateKey = readPrivateKey(file);
		    Algorithm algorithm = Algorithm.RSA256(null, privateKey);
		    
			// Current time in milliseconds converted to date
			long nowMillis = System.currentTimeMillis();
		    Date now = new Date(nowMillis);
		    
		    // Set expiration date
		    long expMillis = nowMillis + ttl;
		    Date expires = new Date(expMillis);
		    
		    // Set headers
		    Map<String, Object> headerClaims = new HashMap();
		    headerClaims.put("alg", "RS256");
		    headerClaims.put("typ", "JWT");
		    
		    token = JWT.create()
		    	.withHeader(headerClaims)
		        .withIssuer(agid)
		        .withAudience("NM")
		        .withExpiresAt(expires)
		        .withIssuedAt(now)
		        .sign(algorithm);
		    
		    storeToken(token);
		    
		    // Store token and expiration in memory
		    platform_token = token;
		    platform_token_expiration = expMillis;
		    		   		    	    
	    } catch (JWTCreationException jwte){
	        //Invalid Signing configuration / Couldn't convert Claims.
	    	logger.warning("Token could not be generated...");
	    	jwte.printStackTrace();
	    } catch (IOException ioe) {
	    	logger.warning("Token could not be stored...");
	    	ioe.printStackTrace();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		return token;
	}
	
	// Validate token
	private void verifyToken(String token) throws JWTVerificationException, IOException{
		String file = path + pubKey;
		try {
			RSAPublicKey publicKey = readPublicKey(file); //Get the key instance
		    Algorithm algorithm = Algorithm.RSA256(publicKey, null);
		    JWTVerifier verifier = JWT.require(algorithm)
		        .withIssuer(agid)
		        .build(); //Reusable verifier instance
		    DecodedJWT jwt = verifier.verify(token);
		    logger.fine("Token expires at: " + jwt.getExpiresAt().toString());
		} catch (Exception e) {
	    	e.printStackTrace();
		}
	}
	
	//	Reads public key from the server
	private RSAPublicKey readPublicKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
	    X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(readFileBytes(filename));
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    return (RSAPublicKey) keyFactory.generatePublic(publicSpec);       
	}
	
	//	Generate private key in the keystore from DER file
	private RSAPrivateKey readPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
	    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(readFileBytes(filename));
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);     
	}
	
	// Load DER file contents
	private byte[] readFileBytes(String filename) throws IOException
	{
	    Path path = Paths.get(filename);
	    return Files.readAllBytes(path);        
	}
	
	// Store token in file
	private void storeToken(String token) throws IOException {
		String file = path + CONFIG_DEF_TOKEN;
		OutputStream os = new FileOutputStream(file);
        os.write(token.toString().getBytes());
        os.close();
	}
	
	// Load token string from file
	public String loadToken(File file) {
		// loaded data
		String token;
    	try {
    		InputStream is = new FileInputStream(file);
            token = IOUtils.toString(is, "UTF-8");
            is.close();
            verifyToken(token);
    	} catch (IOException i) {
	    	logger.warning("Token could not be loaded from file, creating new one...");
	    	i.printStackTrace();
	    	token = generateToken();
	        return token;
		} catch (JWTVerificationException jwte){
		    //Invalid signature/claims
	    	logger.warning("Error verifying file token, creating new one...");
	    	jwte.printStackTrace();
	    	token = generateToken();
	        return token;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
    	return token;
	}
	
	
}