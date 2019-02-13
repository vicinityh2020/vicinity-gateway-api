package eu.bavenir.ogwapi.commons.connectors.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

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
	
	/*
	private static final String CONFIG_PARAM_HTTPSACCEPTSALLCERTIFICATES = 
														"connector.restAgentConnector.acceptAllCertificates";
	
	private static final String CONFIG_PARAM_AUTHENTICATIONSCHEMA = "connector.restAgentConnector.authenticationSchema";
	
	private static final String CONFIG_PARAM_LOGIN = "connector.restAgentConnector.login";
	
	private static final String CONFIG_PARAM_PASSWORD = "connector.restAgentConnector.password";
	
	*/
	/**
	 * Name of the configuration parameter for Agent IP.
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTIP = "connector.restAgentConnector.agentIp";
	
	/**
	 * Name of the configuration parameter for Agent port.
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTPORT = "connector.restAgentConnector.agentPort";
	
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
	
	
	
	/* === FIELDS === */
	
	/**
	 * Configuration flag for HTTPS.
	 */
	private boolean useHttps;
	
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
		
		if (config.getBoolean(CONFIG_PARAM_CONNECTORRESTDUMMY, CONFIG_DEF_APIRESTAGENTDUMMY)) {
			logger.config("REST Agent Connector: Dummy payloads enabled, all calls to an Agent via this connector "
					+ "will be simulated.");
			dummyCalls = true;
		} else {
			logger.config("REST Agent Connector: Dummy payloads disabled, all calls to an Agent via this connector "
					+ "will be real.");
		}
		
		if (config.getBoolean(CONFIG_PARAM_CONNECTORRESTUSEHTTPS, CONFIG_DEF_APIAGENTUSEHTTPS)){
			logger.config("REST Agent Connector: HTTPS protocol enabled for Agent communication.");
			useHttps = true;
		} else {
			logger.config("REST Agent Connector: HTTPS protocol disabled for Agent communication.");
			useHttps = false;
		}
		
		agentServiceUrl = assembleAgentServiceUrl();
		
	}
	

	@Override
	public NetworkMessageResponse forwardEventToObject(String sourceOid, String destinationOid, String eventId, 
			String body, Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_EVENTS + "/" + eventId;
		
		return performOperation(OPERATION_PUT, sourceOid, fullEndpointUrl, body, parameters);
	}

	

	@Override
	public NetworkMessageResponse getObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = 
				fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_PROPERTIES + "/" + propertyId;

		
		return performOperation(OPERATION_GET, sourceOid, fullEndpointUrl, body, parameters);
	}


	@Override
	public NetworkMessageResponse setObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters) {

		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = 
				fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_PROPERTIES + "/" + propertyId;
		
		return performOperation(OPERATION_PUT, sourceOid, fullEndpointUrl, body, parameters);
	}


	@Override
	public NetworkMessageResponse startObjectAction(String sourceOid, String destinationOid, String actionId, 
			String body, Map<String, String> parameters) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + destinationOid + ATTR_URL_ACTIONS + "/" + actionId;

		return performOperation(OPERATION_POST, sourceOid, fullEndpointUrl, body, parameters);
	}



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
	 * @param fullUrl Full URL of the Agent's endpoint to be reached. 
	 * @param body Body of the request.
	 * @param parameters Parameters passed in the request.
	 * @return Response message with the results.
	 */
	private NetworkMessageResponse performOperation(byte operationCode, String sourceOid, String fullUrl, String body, 
			Map<String, String> parameters){
		
		if (dummyCalls) {
			return performDummyOperation(operationCode, sourceOid, fullUrl, body, parameters);
		}
		
		// don't forget to put source OID as one of the parameters
		parameters.put(PARAM_SOURCEOID, sourceOid);
		
		logger.finest("REST Agent Connector:\nOperation code: " + operationCode
				+ "\nAssembled full URL: " + fullUrl
				+ "\nParameters: " + parameters.toString()
				+ "\nBody: " + body
				);
		
		// create stuff
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);

		Writer writer = new StringWriter();
		Representation responseRepresentation = null;
		
		ClientResource clientResource = new ClientResource(fullUrl);
		
		// fill the parameters
		if (parameters != null) {
			for (String paramName : parameters.keySet()) {
				clientResource.addQueryParameter(paramName, parameters.get(paramName));
			}
		}
	
		try {
			
			switch (operationCode){
			
			case OPERATION_GET:
			
				// parameters
				responseRepresentation = clientResource.get();
			
				break;
				
			case OPERATION_POST:
			
				if (body != null){
					logger.finest("REST Agent Connector: POST request contains following body: " + body);
					
					// this should always be json string
					responseRepresentation = clientResource.post(new JsonRepresentation(body), 
							MediaType.APPLICATION_JSON);
				} else {
					logger.finest("REST Agent Connector: POST request contains no body.");
					responseRepresentation = clientResource.post(null);
				}
				
				break;
				
			case OPERATION_PUT:

				if (body != null){
					logger.finest("REST Agent Connector: PUT request contains following body: " + body);
					
					// this should always be json string
					responseRepresentation = clientResource.put(new JsonRepresentation(body), 
							MediaType.APPLICATION_JSON);
				} else {
					logger.finest("REST Agent Connector: PUT request contains no body.");
					responseRepresentation = clientResource.put(null);
				}
				
				break;
				
			case OPERATION_DELETE:
				responseRepresentation = clientResource.delete();
				break;
				
			}
			
		} catch (ResourceException e) {
			
			// this is what happens when something else than 2xx got returned - in that case we don't have access
			// to response body the regular way...
			
			logger.warning("Exception from the RESTLET client: " + e.getMessage());
			
			responseRepresentation = clientResource.getResponseEntity();

		} finally {
			
			MediaType type = responseRepresentation.getMediaType();
			
			// save the body
			if (responseRepresentation != null){
				try {
					responseRepresentation.write(writer);
					response.setResponseBody(writer.toString());
					
					if (type != null && !type.getName().isEmpty()) {
						response.setContentType(type.getName());
					}
					
				} catch (IOException e) {
					
					logger.warning("Exception during writing the response body: " + e.getMessage());
				}
				
			} 
			
			// save the status code and reason
			if (clientResource.getStatus().getCode() / 200 == 1) {
				response.setError(false);
			} else {
				response.setError(true);
			}
			
			response.setResponseCode(clientResource.getStatus().getCode());
			response.setResponseCodeReason(clientResource.getStatus().getReasonPhrase());
			
			try {
				writer.close();
			} catch (IOException e) {
				logger.severe("IO exception during writer closure: " + e.getMessage());
			}
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
						+ "\nFull URL: " + fullUrl
						+ "\nBody: " + body);
		
		logger.info(dummyResponseMessage);
		
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
