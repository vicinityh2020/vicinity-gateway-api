package eu.bavenir.vicinity.gatewayapi.xmpp;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.MediaType;
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
 * Class that processes a request received in {@link NetworkMessageRequest NetworkMessageRequest}. The reasoning behind 
 * this is, that in order to invoke the required action on the Agent, a REST service on its side has to be called. By 
 * receiving the pieces of the URL and its parameters in the incoming message, the URL can be assembled without a need 
 * to hard code it. 
 * 
 * @author sulfo
 *
 */
public class AgentCommunicator {
	
	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for whether Agent utilizes HTTPS or HTTP.
	 */
	private static final String CONFIG_PARAM_APIAGENTUSEHTTPS = "api.agentUseHttps";
	
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
	 * Processes the {@link NetworkMessageRequest request} that arrived from the XMPP network. After the URL of the 
	 * required Agent service is assembled, the URL is called with the necessary HTTP method and the result is returned. 
	 * 
	 * @param request {@link NetworkMessageRequest Message} received over XMPP network. 
	 * @return {@link NetworkMessageResponse Response} from the Agent. 
	 */
	public NetworkMessageResponse processRequestMessage(NetworkMessageRequest request){
		
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
				
				// this should always be json string
				String requestBodyPost = request.getRequestBody();
				
				if (requestBodyPost != null){
					resource.post(requestBodyPost, MediaType.APPLICATION_JSON).write(writer);
				} else {
					logger.warning("Request nr. " + request.getRequestId() + " contains no body.");
				}
				
				break;
				
			case NetworkMessageRequest.REQUEST_OPERATION_PUT:
				
				// this should always be json string
				String requestBodyPut = request.getRequestBody();
				
				if (requestBodyPut != null){
					resource.put(requestBodyPut, MediaType.APPLICATION_JSON).write(writer);
				} else {
					logger.warning("Request nr. " + request.getRequestId() + " contains no body.");
				}
				
				break;
				
			case NetworkMessageRequest.REQUEST_OPERATION_DEL:
				
				resource.delete().write(writer);
				
				break;
				
			}
			
			// TODO return code
		} catch (ResourceException | IOException e) {
			// TODO Auto-generated catch block - we HAVE TO solve various return codes!
			e.printStackTrace();
		}
		
		response.setResponseBody(writer.toString());
		
		// TODO return value of the response code somehow
		response.setResponseCode(200);
		
		return response;
	}
	
	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * This method takes incoming {@link NetworkMessageRequest request} and parses it into URL of an Agent service. 
	 * See the main Javadoc section for the {@link NetworkMessageRequest request} class for more details.
	 * 
	 * @param networkMessageRequest Message with action request.
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
