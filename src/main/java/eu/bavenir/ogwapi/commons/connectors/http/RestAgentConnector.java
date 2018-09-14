package eu.bavenir.ogwapi.commons.connectors.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

// TODO documentation
// TODO the creation of the response message (and tinkering with request messages) should be done one
// level higher

/**
 * Class that processes a request received in {@link NetworkMessageRequest NetworkMessageRequest}. The reasoning behind 
 * this is, that in order to invoke the required action on the Agent, a REST service on its side has to be called. By 
 * receiving the pieces of the URL and its parameters in the incoming message, the URL can be assembled without a need 
 * to hard code it. 
 * 
 * @author sulfo
 *
 */
public class RestAgentConnector extends AgentConnector {
	
	/* === CONSTANTS === */
	
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
	 * Name of the configuration parameter for Agent IP.
	 */
	private static final String CONFIG_PARAM_CONNECTORRESTIP = "connector.restAgentConnector.agentIP";
	
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
	
	
	private static final String ATTR_URL_OBJECTS = "/objects";
	
	private static final String ATTR_URL_PROPERTIES = "/properties";
	
	private static final String ATTR_URL_EVENTS = "/events";
	
	private static final String ATTR_URL_ACTIONS = "/actions";
	
	
	private static final byte OPERATION_GET = 0x00;
	
	private static final byte OPERATION_POST = 0x01;
	
	private static final byte OPERATION_PUT = 0x02;
	
	private static final byte OPERATION_DELETE = 0x03;
	
	
	
	/* === FIELDS === */
	
	// for the sake of making it easier to send the info about HTTPS into the logs
	private boolean useHttps;
	
	// REST agent will be using dummy calls
	private boolean dummyCalls;
	
	// this is the agent service URL, without attributes (basically something like http://ip:port/apiname) 
	private String agentServiceUrl;
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * a storm of null pointer exceptions is imminent.
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
	public NetworkMessageResponse forwardEventToObject(String objectID, String eventID, String eventBody) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + objectID + ATTR_URL_EVENTS + "/" + eventID;
		
		return performOperation(OPERATION_PUT, fullEndpointUrl, eventBody);
	}

	

	@Override
	public NetworkMessageResponse getObjectProperty(NetworkMessageRequest requestMessage) {
		
		NetworkMessageResponse response;
		String fullEndpointUrl = new String(agentServiceUrl);
		
		// finalise the URL
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		
		if (!attributesMap.isEmpty()){
			
			// get the object ID
			fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS;
			String objectID = attributesMap.get(NetworkMessageRequest.ATTR_OID);
			
			if (objectID != null) {
				fullEndpointUrl = fullEndpointUrl + "/" + objectID;
			}
			
			// get the property ID
			fullEndpointUrl = fullEndpointUrl + ATTR_URL_PROPERTIES;
			String propertyID = attributesMap.get(NetworkMessageRequest.ATTR_PID);
			
			if (propertyID != null) {
				fullEndpointUrl = fullEndpointUrl + "/" + propertyID;
			}
			
		}
		
		response = performOperation(OPERATION_GET, fullEndpointUrl, null);
		
		// set the correlation ID 
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}


	@Override
	public NetworkMessageResponse setObjectProperty(NetworkMessageRequest requestMessage) {

		NetworkMessageResponse response;
		String fullEndpointUrl = new String(agentServiceUrl);
		
	
		// finalise the URL
		LinkedHashMap<String, String> attributesMap = requestMessage.getAttributes();
		
		if (!attributesMap.isEmpty()){
			
			// get the object ID
			fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS;
			String objectID = attributesMap.get(NetworkMessageRequest.ATTR_OID);
			
			if (objectID != null) {
				fullEndpointUrl = fullEndpointUrl + "/" + objectID;
			}
			
			// get the property ID
			fullEndpointUrl = fullEndpointUrl + ATTR_URL_PROPERTIES;
			String propertyID = attributesMap.get(NetworkMessageRequest.ATTR_PID);
			
			if (propertyID != null) {
				fullEndpointUrl = fullEndpointUrl + "/" + propertyID;
			}
		}
		
		response = performOperation(OPERATION_PUT, fullEndpointUrl, requestMessage.getRequestBody());
		
		// set the correlation ID 
		response.setRequestId(requestMessage.getRequestId());
		
		return response;
	}


	@Override
	public NetworkMessageResponse startObjectAction(String objectID, String actionID, String requestBody) {
		
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + objectID + ATTR_URL_ACTIONS + "/" + actionID;

		return performOperation(OPERATION_POST, fullEndpointUrl, requestBody);
	}



	@Override
	public NetworkMessageResponse cancelTask(String objectID, String actionID) {
		String fullEndpointUrl = new String(agentServiceUrl);
		
		fullEndpointUrl = fullEndpointUrl + ATTR_URL_OBJECTS + "/" + objectID + ATTR_URL_ACTIONS + "/" + actionID;
		
		return performOperation(OPERATION_DELETE, fullEndpointUrl, null);
	}
	
	
	
	
	
	
	/* === PRIVATE METHODS === */
	
	// TODO documentation
	/**
	 * This method takes incoming {@link NetworkMessageRequest request} and parses it into URL of an Agent service. 
	 * See the main Javadoc section for the {@link NetworkMessageRequest request} class for more details.
	 * 
	 * @param networkMessageRequest Message with action request.
	 * @return URL on the Agent side that is to be called.
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
	
	
	
	// TODO documentation
	/**
	 * Processes the {@link NetworkMessageRequest request} that arrived from the XMPP network. After the URL of the 
	 * required Agent service is assembled, the URL is called with the necessary HTTP method and the result is returned. 
	 * 
	 * @param request {@link NetworkMessageRequest Message} received over XMPP network. 
	 * @return {@link NetworkMessageResponse Response} from the Agent. 
	 */
	private NetworkMessageResponse performOperation(byte operationCode, String fullUrl, String body){
		
		if (dummyCalls) {
			return performDummyOperation(operationCode, fullUrl, body);
		}
		
		logger.finest("REST Agent Connector: Operation code: " + operationCode);
		logger.finest("REST Agent Connector: Assembled URL: " + fullUrl);
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		ClientResource clientResource = new ClientResource(fullUrl);
		
		Writer writer = new StringWriter();
		Representation responseRepresentation = null;
		
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
					logger.warning("REST Agent Connector: PUT request contains no body.");
					responseRepresentation = clientResource.put(null);
				}
				
				break;
				
			case OPERATION_DELETE:
				responseRepresentation = clientResource.delete();
				break;
				
			}
			
			if (responseRepresentation != null){
				
				responseRepresentation.write(writer);
				response.setResponseBody(writer.toString());

			} 
			
		} catch (ResourceException | IOException e) {
			
			logger.warning(e.getMessage());

		} finally {
			
			response.setResponseCode(clientResource.getStatus().getCode());
			response.setResponseCodeReason(clientResource.getStatus().getReasonPhrase());
		}

		return response;
	}
	
	
	/*
	 * Very handy testing method that can be used instead of performOperation. This one does not rely on 
	 * functional agent and always returns positive results.
	*/
	private NetworkMessageResponse performDummyOperation (byte operationCode, String fullUrl, String body) {
		
		NetworkMessageResponse response = new NetworkMessageResponse(config, logger);
		
		response.setResponseCode(200);
		response.setResponseCodeReason("OK");
		response.setResponseBody("REST Agent Connector: Dummy reply from REST Agent Connector. URL: " + fullUrl);
		
		return response;
	}
	
	
	
}
