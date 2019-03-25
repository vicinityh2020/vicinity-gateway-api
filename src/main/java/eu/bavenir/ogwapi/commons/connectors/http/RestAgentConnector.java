package eu.bavenir.ogwapi.commons.connectors.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.messages.CodesAndReasons;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * Implementation of an {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector} based on HTTP
 * REST services. This connector uses Restlet framework to deliver requests to an Agent component specified in the
 * configuration file. 
 * 
 * @author sulfo
 *
 */
public class RestAgentConnector extends AgentConnector {
	
	/* === CONSTANTS === */
	
	/**
	 * Reserved word for source OID parameter. Since it is impossible to incorporate the source object ID into the request
	 * other way than in parameters (without changing the request body), one parameter with this name is reserved and
	 * the source object ID is sent as its value. If there was a parameter with the same name before, it will get
	 * overwritten. 
	 */
	private static final String PARAM_SOURCEOID = "sourceOid";
	
	/**
	 * Name of the configuration parameter for whether the REST Agent Connector uses simulated calls (such configuration
	 * is useful during debugging, when no real REST Agent is available or operational and connectivity of this
	 * OGWAPI needs to be tested). 
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTDUMMY = "connector.restAgentConnector.useDummyCalls";
	
	/**
	 * Name of the configuration parameter for whether Agent utilises HTTPS or HTTP.
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTUSEHTTPS = "connector.restAgentConnector.useHttps";
	
	/**
	 * Name of the configuration parameter for whether Agent should accept self signed certificate.
	 */
	private static final String CONFIG_PARAM_HTTPSACCEPTSELFSIGNED = "connector.restAgentConnector.acceptSelfSignedCertificate";
	
	/**
	 * Name of the configuration parameter for authentication method should be used when connecting to Agent.
	 */
	private static final String CONFIG_PARAM_AUTHMETHOD = "connector.restAgentConnector.agentAuthenticationMethod";
	
	/**
	 * Name of the configuration parameter for user name.
	 */
	private static final String CONFIG_PARAM_AGENTUSERNAME = "connector.restAgentConnector.agentUsername";

	/**
	 * Name of the configuration parameter for password.
	 */
	private static final String CONFIG_PARAM_AGENTPASSWORD = "connector.restAgentConnector.agentPassword";
	
	/**
	 * Name of the configuration parameter for Agent timeout.
	 */
	private static final String CONFIG_PARAM_AGENTTIMEOUT = "connector.restAgentConnector.agentTimeout";
	
	/**
	 * Name of the configuration parameter for Agent IP.
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTIP = "connector.restAgentConnector.agent";
	
	/**
	 * Name of the configuration parameter for Agent port.
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTPORT = "connector.restAgentConnector.agentPort";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_AGENTTIMEOUT CONFIG_PARAM_AGENTTIMEOUT} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final int CONFIG_DEF_AGENTTIMEOUT = 60;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_AGENTUSERNAME CONFIG_PARAM_AGENTUSERNAME} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_AGENTUSERNAME = "";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_AGENTPASSWORD CONFIG_PARAM_AGENTPASSWORD} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_AGENTPASSWORD = "";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_AUTHMETHOD CONFIG_PARAM_AUTHMETHOD} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_AUTHMETHOD = "none";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_HTTPSACCEPTSELFSIGNED CONFIG_PARAM_HTTPSACCEPTSELFSIGNED} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_HTTPSACCEPTSELFSIGNED = true;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_CONNECTORRESTDUMMY CONFIG_PARAM_APIRESTAGENTDUMMY} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_APIRESTAGENTDUMMY = false;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_CONNECTORRESTUSEHTTPS CONFIG_PARAM_APIAGENTUSEHTTPS} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_APIAGENTUSEHTTPS = false;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_CONNECTORRESTIP CONFIG_PARAM_APIAGENTIP} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_APIRESTAGENTPORT = "9997";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_CONNECTORRESTIP CONFIG_PARAM_APIAGENTIP} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_APIRESTAGENTIP = "localhost";
	
	/**
	 * The beginning of the Agent service URL when using HTTP protocol. 
	 */
	private static final String HTTP_PROTOCOL = "http://";
	
	/**
	 * The beginning of the Agent service URL when using HTTPS protocol. 
	 */
	private static final String HTTPS_PROTOCOL = "https://";
	
	/**
	 * The name of the Agent API, this string usually follows the IP and port. Like http://localhost:port/agent/stuff.
	 */
	private static final String AGENT_API_STRING = "/agent";
	
	/**
	 * Name of the 'objects' attribute in the final URL.
	 */
	private static final String ATTR_URL_OBJECTS = "/objects";
	
	/**
	 * Name of the 'properties' attribute in the final URL.
	 */
	private static final String ATTR_URL_PROPERTIES = "/properties";
	
	/**
	 * Name of the 'events' attribute in the final URL.
	 */
	private static final String ATTR_URL_EVENTS = "/events";
	
	/**
	 * Name of the 'actions' attribute in the final URL.
	 */
	private static final String ATTR_URL_ACTIONS = "/actions";
	
	/**
	 * Name of the 'dummy' attribute in the returned JSON.
	 */
	private static final String ATTR_DUMMY = "dummy";
	
	/**
	 * Operation code for GET.
	 */
	private static final byte OPERATION_GET = 0x00;
	
	/**
	 * Operation code for POST.
	 */
	private static final byte OPERATION_POST = 0x01;
	
	/**
	 * Operation code for PUT.
	 */
	private static final byte OPERATION_PUT = 0x02;
	
	/**
	 * Operation code for DELETE.
	 */
	private static final byte OPERATION_DELETE = 0x03;
	
	/**
	 * Status code that will be returned when HTTP/HTTPS client will throw an exception and there is no code available 
	 * to be returned. 
	 */
	private static final int CLIENT_ERR_STATUSCODE = 400;
	
	/**
	 * Status code reason that will be returned when HTTP/HTTPS client will throw an exception and there is no reason available 
	 * to be returned. 
	 */
	private static final String CLIENT_ERR_STATUSREASON = "An error occured when connecting to Agent: ";
	
	
	
	/* === FIELDS === */
	
	/**
	 * Configuration flag for HTTPS.
	 */
	private boolean useHttps;
	
	/**
	 * Configuration flag for accepting self signed certificate.
	 */
	private boolean acceptSelfSigned;
	
	/**
	 * Agent timeout.
	 */
	private int agentTimeout;
	
	/**
	 * Agent authentication method.
	 */
	private String agentAuthMethod;
	
	/**
	 * User name.
	 */
	private String agentUsername;
	
	/**
	 * Password.
	 */
	private String agentPassword;
	
	/**
	 * Configuration flag for using dummy operations instead of the real ones.
	 */
	private boolean dummyCalls;
	
	/**
	 * This is the agent service URL, without attributes (basically something like http://ip:port/apiname) .
	 */
	private String agentServiceUrl;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * a storm of null pointer exceptions is imminent.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public RestAgentConnector(XMLConfiguration config, Logger logger){
		super(config, logger);
		
		// load configuration for dummy connector
		if (config.getBoolean(CONFIG_PARAM_CONNECTORRESTDUMMY, CONFIG_DEF_APIRESTAGENTDUMMY)) {
			logger.config("REST Agent Connector: Dummy payloads enabled, all calls to an Agent via this connector "
					+ "will be simulated.");
			dummyCalls = true;
		} else {
			logger.config("REST Agent Connector: Dummy payloads disabled, all calls to an Agent via this connector "
					+ "will be real.");
		}
		
		// load configuration for https
		if (config.getBoolean(CONFIG_PARAM_CONNECTORRESTUSEHTTPS, CONFIG_DEF_APIAGENTUSEHTTPS)){
			logger.config("REST Agent Connector: HTTPS protocol enabled for Agent communication.");
			useHttps = true;
			
			acceptSelfSigned = config.getBoolean(CONFIG_PARAM_HTTPSACCEPTSELFSIGNED, CONFIG_DEF_HTTPSACCEPTSELFSIGNED);
			
		} else {
			logger.config("REST Agent Connector: HTTPS protocol disabled for Agent communication.");
			useHttps = false;
			acceptSelfSigned = false;
		}
		
		// load authentication method
		agentAuthMethod = config.getString(CONFIG_PARAM_AUTHMETHOD, CONFIG_DEF_AUTHMETHOD);
		if (!agentAuthMethod.equals(CONFIG_DEF_AUTHMETHOD)) {
			// log it
		}
		agentUsername = config.getString(CONFIG_PARAM_AGENTUSERNAME, CONFIG_DEF_AGENTUSERNAME);
		agentPassword = config.getString(CONFIG_PARAM_AGENTPASSWORD, CONFIG_DEF_AGENTPASSWORD);
		
		// load timeout
		agentTimeout = config.getInt(CONFIG_PARAM_AGENTTIMEOUT, CONFIG_DEF_AGENTTIMEOUT);
		
		agentServiceUrl = assembleAgentServiceUrl();
		
	}
	

	/**
	 * This will make a call to PUT http://<agent IP>:<agent port>/agent/objects/<destination OID>/events/<event ID>
	 */
	@Override
	public NetworkMessageResponse forwardEventToObject(String sourceOid, String destinationOid, String eventId, 
			String body, Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_EVENTS + "/" + eventId;
		
		return performOperation(OPERATION_PUT, sourceOid, fullEndpointUrl, body, parameters);
	}

	
	/**
	 * This will make a call to GET http://<agent IP>:<agent port>/agent/objects/<destination OID>/properties/<property ID>
	 */
	@Override
	public NetworkMessageResponse getObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = 
				fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_PROPERTIES + "/" + propertyId;

		
		return performOperation(OPERATION_GET, sourceOid, fullEndpointUrl, body, parameters);
	}


	/**
	 * This will make a call to PUT http://<agent IP>:<agent port>/agent/objects/<destination OID>/properties/<property ID>
	 */
	@Override
	public NetworkMessageResponse setObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters) {

		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = 
				fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_PROPERTIES + "/" + propertyId;
		
		return performOperation(OPERATION_PUT, sourceOid, fullEndpointUrl, body, parameters);
	}


	/**
	 * This will make a call to POST http://<agent IP>:<agent port>/agent/objects/<destination OID>/actions/<action ID>
	 */
	@Override
	public NetworkMessageResponse startObjectAction(String sourceOid, String destinationOid, String actionId, 
			String body, Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_ACTIONS + "/" + actionId;

		return performOperation(OPERATION_POST, sourceOid, fullEndpointUrl, body, parameters);
	}


	/**
	 * This will make a call to DELETE http://<agent IP>:<agent port>/agent/objects/<destination OID>/actions/<action ID>
	 */
	@Override
	public NetworkMessageResponse stopObjectAction(String sourceOid, String destinationOid, String actionId, String body, 
			Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_ACTIONS + "/" + actionId;
		
		return performOperation(OPERATION_DELETE, sourceOid, fullEndpointUrl, body, parameters);
	}
	
	
	
	
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * This method takes incoming {@link NetworkMessageRequest request} and parses it into URL of an Agent service. 
	 * See the main Javadoc section for the {@link NetworkMessageRequest request} class for more details.
	 * 
	 */
	private String assembleAgentServiceUrl(){
		
		// resolve the protocol to use
		String protocol;
		if (useHttps){
			protocol = HTTPS_PROTOCOL;
		} else {
			protocol = HTTP_PROTOCOL;
		}
		
		String agentServiceUrl = new String(
				protocol
				+ config.getString(CONFIG_PARAM_CONNECTORRESTIP, CONFIG_DEF_APIRESTAGENTIP)
				+ ":"
				+ config.getString(CONFIG_PARAM_CONNECTORRESTPORT, CONFIG_DEF_APIRESTAGENTPORT)
				+ AGENT_API_STRING
		);
		
		return agentServiceUrl;
	}
	
	
	/**
	 * Processes the {@link NetworkMessageRequest request} that arrived from the network. After the URL of the 
	 * required Agent service is assembled, the URL is called with the necessary HTTP method and the result is returned.
	 * 
	 * @param operationCode Code of the HTTP operation, see the constants.
	 * @param sourceOid The object ID of the source. 
	 * @param fullUrl Full URL of the Agent's end point to be reached. 
	 * @param body Body of the request.
	 * @param parameters Parameters passed in the request.
	 * @return Response message with the results.
	 */
	private NetworkMessageResponse performOperation(byte operationCode, String sourceOid, String fullUrl, String body, 
			Map<String, String> parameters){
		
		// prepare for error
		boolean clientError = false;
		String exceptionString = null;
		
		// don't forget to put source OID as one of the parameters (this will also overwrite any previous such 
		// parameter that someone maliciously could have thrown in)
		parameters.put(PARAM_SOURCEOID, sourceOid);
		
		logger.fine("REST Agent Connector:\nOperation code: " + operationCode
				+ "\nAssembled full URL: " + fullUrl
				+ "\nParameters: " + parameters.toString()
				+ "\nBody: " + body
				);
		
		// is this for real, or just simulation
		if (dummyCalls) {
			return performDummyOperation(operationCode, sourceOid, fullUrl, body, parameters);
		}
		
		// Unirest needs Map<String, Object>, but all we have is Map<String, String>. We have to do something...
		Map<String, Object> convertedParams = Collections.<String, Object>unmodifiableMap(parameters);
		
		// create stuff
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);

		// accept snake oil
		boolean customClientInUse = false;
		
		if (useHttps && acceptSelfSigned) {
			
			SSLContext sslcontext = null;
			try {
				sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
			} catch (Exception e) {
				logger.warning("Exception during configuration of SSL for Agent Connector. Reverting to HTTP. "
						+ "Exception message: " + e.getMessage());
			} 
			
			if (sslcontext != null) {
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext); 
				CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build(); 
								
				Unirest.setHttpClient(httpClient);
				
				customClientInUse = true;
			} 
		} 
		
		if (!customClientInUse) {
			// set timeouts - we are not using custom client, we have to do it this way
			Unirest.setTimeouts(agentTimeout * 1000, agentTimeout * 1000);
		}
		
		HttpResponse<String> responseNode = null;
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("Content-Type", "application/json");
		
		JSONObject jsonBody = null;
		if (body != null && !body.isEmpty()) {
			jsonBody = new JSONObject(body);
		}
		
		switch (operationCode){
		
		case OPERATION_GET:
		
			GetRequest getRequest = Unirest.get(fullUrl);
			
			// fill the parameters
			getRequest.queryString(convertedParams);
			
			// if authentication is not 'none'
			if (!agentAuthMethod.equals(CONFIG_DEF_AUTHMETHOD)) {
				getRequest.basicAuth(agentUsername, agentPassword);
			}
			
			getRequest.headers(headers);
			
			try {
				responseNode = getRequest.asString();
			} catch (UnirestException e) {
				
				clientError = true;
				exceptionString = e.getMessage();
				
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				
				logger.warning("Exception when connecting to Agent: " + exceptionString + "\nThe whole exception: " + exceptionAsString);
			}
		
			break;
			
		case OPERATION_POST:
		
			HttpRequestWithBody postRequest = Unirest.post(fullUrl);
			
			// fill the parameters
			postRequest.queryString(convertedParams);
				
			// if authentication is not 'none'
			if (!agentAuthMethod.equals(CONFIG_DEF_AUTHMETHOD)) {
				postRequest.basicAuth(agentUsername, agentPassword);
			}
			
			postRequest.headers(headers);
			
			if (jsonBody != null) {
				postRequest.body(jsonBody);
			}
			
			try {
				responseNode = postRequest.asString();
			} catch (UnirestException e) {
				clientError = true;
				exceptionString = e.getMessage();
				
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				
				logger.warning("Exception when connecting to Agent: " + exceptionString + "\nThe whole exception: " + exceptionAsString);
			}
			
			break;
			
		case OPERATION_PUT:

			HttpRequestWithBody putRequest = Unirest.put(fullUrl);
			
			// fill the parameters
			putRequest.queryString(convertedParams);
			
			// if authentication is not 'none'
			if (!agentAuthMethod.equals(CONFIG_DEF_AUTHMETHOD)) {
				putRequest.basicAuth(agentUsername, agentPassword);
			}
			
			putRequest.headers(headers);
			
			if (jsonBody != null) {
				putRequest.body(jsonBody);
			}
			
			try {
				responseNode = putRequest.asString();
			} catch (UnirestException e) {
				clientError = true;
				exceptionString = e.getMessage();
				
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				
				logger.warning("Exception when connecting to Agent: " + exceptionString + "\nThe whole exception: " + exceptionAsString);
			}
			
			break;
			
		case OPERATION_DELETE:
			
			HttpRequestWithBody deleteRequest = Unirest.delete(fullUrl);
			
			// fill the parameters
			deleteRequest.queryString(convertedParams);
			
			// if authentication is not 'none'
			if (!agentAuthMethod.equals(CONFIG_DEF_AUTHMETHOD)) {
				deleteRequest.basicAuth(agentUsername, agentPassword);
			}
			
			deleteRequest.headers(headers);
			
			try {
				responseNode = deleteRequest.asString();
			} catch (UnirestException e) {
				clientError = true;
				exceptionString = e.getMessage();
				
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				
				logger.warning("Exception when connecting to Agent: " + exceptionString + "\nThe whole exception: " + exceptionAsString);
			}
		
			break;
			
		}
		
		if (clientError) {
			
			//the responseNode is null now
			response.setError(true);
			response.setResponseCode(CLIENT_ERR_STATUSCODE);
			response.setResponseCodeReason(CLIENT_ERR_STATUSREASON + exceptionString);
			
			
		} else {
			
			if (responseNode.getBody() != null) {
				response.setResponseBody(responseNode.getBody().toString());
			}
			
			if (responseNode.getHeaders().containsKey("Content-type")) {
				response.setContentType(responseNode.getHeaders().get("Content-type").toString());
			}
			
			// save the status code and reason
			if (responseNode.getStatus() / 200 == 1) {
				response.setError(false);
			} else {
				response.setError(true);
			}
			
			response.setResponseCode(responseNode.getStatus());
			response.setResponseCodeReason(responseNode.getStatusText());
		}
		
		

		return response;
	}
	
	

	/**
	 * Very handy testing method that, if set in the configuration file, can be used instead of performOperation. This one does 
	 * not rely on functional agent and always returns positive results.
	 * 
	 * @param operationCode Code of the HTTP operation, see the constants.
	 * @param sourceOid The object ID of the source. 
	 * @param fullUrl Full URL of the Agent's end point to be reached. 
	 * @param body Body of the request.
	 * @param parameters Parameters passed in the request.
	 * @return Response message with the results.
	 */
	private NetworkMessageResponse performDummyOperation (byte operationCode, String sourceOid, String fullUrl, 
			String body, Map<String, String> parameters) {
		
		// don't forget to put source OID as one of the parameters
		parameters.put(PARAM_SOURCEOID, sourceOid);
		
		String dummyResponseMessage = 
				new String("Dummy REST Agent Connector received following data to perform request:"
						+ "\nOperation code: " + operationCode
						+ "\nParameters: " + parameters.toString()
						+ "\nFull URL: " + fullUrl
						+ "\nBody: " + body);
		
		logger.fine(dummyResponseMessage);
		
		JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
		
		JsonObjectBuilder builder = jsonBuilderFactory.createObjectBuilder();
		
		builder.add(ATTR_DUMMY, dummyResponseMessage);
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		response.setError(false);
		response.setResponseCode(CodesAndReasons.CODE_200_OK);
		response.setResponseCodeReason(CodesAndReasons.REASON_200_OK);
		response.setResponseBody(builder.build().toString());
		
		return response;
	}	
	
}
