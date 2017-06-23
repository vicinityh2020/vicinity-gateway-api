package eu.bavenir.vicinity.gatewayapi.xmpp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


/**
 * Class that processes a request received in JSON message. The reasoning behind this is, that in
 * order to invoke the required action on the Agent, a REST service on its side has to be called. By receiving the
 * pieces of the URL in the incoming JSON, the URL can be assembled without a need to hard code it. Consider the 
 * need to assemble following URL:
 * 
 * https://192.168.1.1:9997/agent/objects/0729a580-2240-11e6-9eb5-0002a5d5c51b/properties/PowerConsumption
 * 
 * or
 * 
 * [1]://[2]:[3]/[4]/[5]/[6]/[7]/[8]
 * 
 * [1] - Protocol - from configuration.
 * [2] - IP - from configuration.
 * [3] - Port - from configuration.
 * [4] - API String - hard coded.
 * [5] - Key 1 - from JSON.
 * [6] - Value 1 - from JSON.
 * [7] - Key 2 - from JSON.
 * [8] - Value 2 - from JSON.
 * 
 * This URL was assembled from following JSON:
 * 
 * {
 * 		"requestOperation": "get",
 * 		"objects": "0729a580-2240-11e6-9eb5-0002a5d5c51b",
 * 		"properties": "PowerConsumption"
 * }
 * 
 * 
 * @author sulfo
 *
 */

// TODO
public class AgentCommunicator {
	
	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for whether Agent utilizes HTTPS or HTTP.
	 */
	private static final String CONFIG_PARAM_APIAGENTUSEHTTPS = "api.agentUseHttps";
	
	/**
	 * Name of the configuration parameter for whether the Gateway should ignore self-signed certificate when
	 * when communicating over HTTPS with the Agent.
	 */
	private static final String CONFIG_PARAM_APIAGENTDISABLESSLVALIDATION = "api.agentDisableSSLValidation";
	
	/**
	 * Name of the configuration parameter for Agent IP.
	 */
	private static final String CONFIG_PARAM_APIAGENTIP = "api.agentIP";
	
	/**
	 * Name of the configuration parameter for Agent port.
	 */
	private static final String CONFIG_PARAM_APIAGENTPORT = "api.agentPort";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_APIAGENTUSEHTTPS CONFIG_PARAM_APIAGENTUSEHTTPS} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_APIAGENTUSEHTTPS = false;
	
	/**
	 * Default value of {@link #CONFIG_PARAM_APIAGENTDISABLESSLVALIDATION CONFIG_PARAM_APIAGENTDISABLESSLVALIDATION} 
	 * configuration parameter. This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final boolean CONFIG_DEF_APIAGENTDISABLESSLVALIDATION = false;
	
	/**
	 * Default value of api.agentPort configuration parameter. This value is taken into account when no 
	 * suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_APIAGENTPORT = "80";
	
	/**
	 * Default value of api.agentIP configuration parameter. This value is taken into account when no 
	 * suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_APIAGENTIP = "localhost";
	
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
	
	
	
	/* === FIELDS === */
	
	// logger and configuration
	private XMLConfiguration config;
	private Logger logger;
	
	// for the sake of making it easier to send the info about HTTPS into the logs
	private boolean useHttps;
	
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor. It is necessary to provide all parameters. If null is provided in place of any of them, 
	 * a storm of null pointer exceptions is imminent.
	 */
	public AgentCommunicator(XMLConfiguration config, Logger logger){
		this.config = config;
		this.logger = logger;
		
		if (config.getBoolean(CONFIG_PARAM_APIAGENTUSEHTTPS, CONFIG_DEF_APIAGENTUSEHTTPS)){
			logger.config("HTTPS protocol enabled for Agent communication");
			useHttps = true;
		} else {
			logger.config("HTTPS protocol disabled for Agent communication");
			useHttps = false;
		}
		
	}
	
	
	/**
	 * Processes the request that arrived from the XMPP network. After the URL of the required Agent service is 
	 * assembled, the URL is called with the necessary HTTP method and the result is returned. 
	 * 
	 * @param request Request that was assembled from the incoming JSON. 
	 * @return {@link NetworkMessageResponse Response} from the Agent. 
	 */
	public NetworkMessageResponse processRequestMessage(NetworkMessageRequest request){
		
		
		// TODO make it possible to ignore the SSL certificate problems 
		// https://stackoverflow.com/questions/9001351/how-to-make-restlet-client-ignore-ssl-certificate-problems
		
		NetworkMessageResponse response = new NetworkMessageResponse();
		
		// always set the correlation ID of the request
		response.setRequestId(request.getRequestId());
		
		ClientResource resource = new ClientResource(assembleAgentUrl(request));
		
		
		
		
		Writer writer = new StringWriter();
		try {
			switch (request.getRequestOperation()){
			
			case NetworkMessageRequest.REQUEST_OPERATION_GET:
				// parameters
				resource.get().write(writer);
				break;
				
			case NetworkMessageRequest.REQUEST_OPERATION_POST:
				break;
				
			case NetworkMessageRequest.REQUEST_OPERATION_PUT:
				break;
				
			case NetworkMessageRequest.REQUEST_OPERATION_DEL:
				break;
				
			}
			
			// TODO return code
		} catch (ResourceException | IOException e) {
			// TODO Auto-generated catch block - we HAVE TO solve various return codes!
			e.printStackTrace();
		}
		
		response.setResponseBody(writer.toString());
		
				response.setResponseCode(200);
		// response.setResponseBody("{\"oid\": \"0729a580-2240-11e6-9eb5-0002a5d5c51b\"}");
		
		return response;
	}
	
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * This method takes incoming JSON and parses it into URL of an Agent service. See the main Javadoc section for
	 * this class for more details.
	 * 
	 * @param networkMessageRequest Message with action request.
	 *  
	 * @return URL on the Agent side that is to be called.
	 * 
	 */
	private String assembleAgentUrl(NetworkMessageRequest networkMessageRequest){
		
		// resolve the protocol to use
		String protocol;
		if (useHttps){
			protocol = HTTPS_PROTOCOL;
		} else {
			protocol = HTTP_PROTOCOL;
		}
		
		String agentServiceUrl = new String(
				protocol
				+ config.getString(CONFIG_PARAM_APIAGENTIP, CONFIG_DEF_APIAGENTIP)
				+ ":"
				+ config.getString(CONFIG_PARAM_APIAGENTPORT, CONFIG_DEF_APIAGENTPORT)
				+ AGENT_API_STRING
		);
		
		LinkedHashMap<String, String> attributesMap = networkMessageRequest.getAttributes();
		
		if (!attributesMap.isEmpty()){
			// in /objects/{oid}, the 'objects' part is the key, {oid} is a value - keep that example in mind
			for (Map.Entry<String, String> entry : attributesMap.entrySet()){
				agentServiceUrl = agentServiceUrl + "/" + entry.getKey();
				// watch out for nulls
				if (entry.getValue() != null){
					agentServiceUrl = agentServiceUrl + "/" + entry.getValue();
				} 
			}
		}
		
		logger.finest("Assembled URL: " + agentServiceUrl);
		
		return agentServiceUrl;
	}
	
}
