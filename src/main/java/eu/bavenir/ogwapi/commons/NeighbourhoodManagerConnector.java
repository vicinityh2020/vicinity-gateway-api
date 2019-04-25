package eu.bavenir.ogwapi.commons;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/*
 * STRUCTURE
 * - constants
 * - fields
 * - public methods 
 * - private methods
 */


/**
 * Provides methods for communication with the Neighbourhood Manager. The methods and fields are mostly static in nature,
 * as this object is supposed to be shared among the {@link ConnectionDescriptor ConnectionDescriptors } to save 
 * resources. All methods are thread safe.
 * 
 * @author sulfo
 *
 */
public class NeighbourhoodManagerConnector {

	// === CONSTANTS ===
	
	/**
	 * Name of the configuration parameter for URL path to Neighbourhood Manager server.
	 */
	private static final String CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER = "general.neighbourhoodManagerServer";
	
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER } parameter. 
	 */
	private static final String CONFIG_DEF_NEIGHBORHOODMANAGERSERVER = "api.vicinity.bavenir.eu";
	
	
	/**
	 * Name of the configuration parameter for Neighbourhood Manager port.
	 */
	private static final String CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT = "general.neighourhoodManagerPort";
	
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT } parameter.
	 */
	private static final int CONFIG_DEF_NEIGHBOURHOODMANAGERPORT = 3000;
	
	
	/**
	 * Name of the configuration parameter for Neighbourhood Manager user name. 
	 */
	private static final String CONFIG_PARAM_NMUSERNAME = "general.neighbourhoodManagerUsername";
	
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NMUSERNAME } parameter.
	 */
	private static final String CONFIG_DEF_NMUSERNAME = "";
	
	
	/**
	 * Name of the configuration parameter for Neighbourhood Manager password.
	 */
	private static final String CONFIG_PARAM_NMPASSWORD = "general.neighbourhoodManagerPassword";
	
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NMPASSWORD } parameter.
	 */
	private static final String CONFIG_DEF_NMPASSWORD = "";
	
	
	/**
	 * Server URL/IP with port and API name. The final end point is then obtained by doing:
	 * SERVER_URL + SOME_SERVICE_1 + someAttributeLikeID + SOME_SERVICE_2 + someOtherAttribute + etc...
	 * 
	 *  Example for discovery service:
	 *  SERVER_PROTOCOL + SERVER + DISCOVERY_SERVICE_1 + agid + DISCOVERY_SERVICE_2
	 *  
	 */
	private static final String SERVER_PROTOCOL = "https://"; 
	
	
	/**
	 * The part of the URL containing the API path.
	 */
	private static final String API_PATH = "/commServer/";
	
	
	/**
	 * Discovery service part 1 of the string.
	 */
	private static final String DISCOVERY_SERVICE_1 = "agent/";
	
	
	/**
	 * Discovery service part 2 of the string.
	 */
	private static final String DISCOVERY_SERVICE_2 = "/items";
	
	
	/**
	 * Registration service string.
	 */
	private static final String REGISTRATION_SERVICE = "items/register";
	
	
	/**
	 * Modify service string.
	 */
	private static final String HEAVYWEIGHTUPDATE_SERVICE = "items/modify";
	
	
	/**
	 * Delete service string.
	 */
	private static final String DELETE_SERVICE = "items/remove";
	
	
	/**
	 * Update service string.
	 */
	private static final String LIGHTWEIGHTUPDATE_SERVICE = "items/update";
	
	
	// === FIELDS ===
	
	/*
	 * Configuration of the OGWAPI.
	 */
	// private XMLConfiguration config;
	
	/*
	 * Logger of the OGWAPI.
	 */
	//private Logger logger;
	
	/**
	 * User name to be used when communicating with NM. 
	 */
	private String username;
	
	/**
	 * Password to be used when communication with NM.
	 */
	private String password;
	
	/**
	 * URL/IP of the NM server.
	 */
	private String neighbourhoodManagerServer;
	
	/**
	 * Port of the NM server.
	 */
	private int port;
	
	
	
	// === PUBLIC METHODS ===
	
	/**
	 * Constructor.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NeighbourhoodManagerConnector(XMLConfiguration config, Logger logger) {
		
		//this.config = config;
		//this.logger = logger;
		
		neighbourhoodManagerServer = 
				config.getString(CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER, CONFIG_DEF_NEIGHBORHOODMANAGERSERVER);
		
		port = config.getInt(CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT, CONFIG_DEF_NEIGHBOURHOODMANAGERPORT);
		
		username = config.getString(CONFIG_PARAM_NMUSERNAME, CONFIG_DEF_NMUSERNAME);
		
		if (!username.equals(CONFIG_DEF_NMUSERNAME)){
			password = config.getString(CONFIG_PARAM_NMPASSWORD, CONFIG_DEF_NMPASSWORD);
		} else {
			password = null;
		}
		
		
	}
	
	
	/**
	 * Retrieves the list of IoT objects registered under given Agent from the Neighbourhood Manager. 
	 * 
	 * @param agid The ID of the Agent in question.
	 * @return All VICINITY identifiers of objects registered under specified agent.
	 */
	public synchronized Representation getAgentObjects(String agid){
		
		String endpointUrl = SERVER_PROTOCOL + neighbourhoodManagerServer + ":" + port + API_PATH + DISCOVERY_SERVICE_1 + agid + DISCOVERY_SERVICE_2;
		
		ClientResource clientResource = new ClientResource(endpointUrl);
		
		if (!username.equals(CONFIG_DEF_NMUSERNAME) && password != null ){
			clientResource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
		}
		
		Representation representation = clientResource.get(MediaType.APPLICATION_JSON);
		
		return representation;
	
	}
	
	
	/**
	 * Register the IoT object(s) of the underlying eco system e.g. devices, VA service.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be registered 
	 * (from request).
	 * @return All VICINITY identifiers of objects registered the Agent by this call.
	 */
	public synchronized Representation storeObjects(Representation json){
		
		String endpointUrl = SERVER_PROTOCOL + neighbourhoodManagerServer + ":" + port + API_PATH + REGISTRATION_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		if (!username.equals(CONFIG_DEF_NMUSERNAME) && password != null ){
			clientResource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
		}
		
		Representation responseRepresentation = clientResource.post(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	/**
	 * Update the thing descriptions of objects registered under the Agent. This will delete the old records and 
	 * replace them with new ones.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be updated 
	 * (from request).
	 * @return The list of approved devices to be registered in agent configuration. Approved devices means only 
	 * devices, that passed the validation in semantic repository and their instances were created. 
	 */
	public synchronized Representation heavyweightUpdate(Representation json){
		
		String endpointUrl = SERVER_PROTOCOL + neighbourhoodManagerServer + ":" + port + API_PATH + HEAVYWEIGHTUPDATE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		if (!username.equals(CONFIG_DEF_NMUSERNAME) && password != null ){
			clientResource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
		}
		
		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	/**
	 * Update the thing descriptions of objects registered under the Agent. This will only change the required fields.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be updated 
	 * (from request).
	 * @return The list of approved devices to be registered in agent configuration. Approved devices means only 
	 * devices, that passed the validation in semantic repository and their instances were created. 
	 */
	public synchronized Representation lightweightUpdate(Representation json){
		
		String endpointUrl = SERVER_PROTOCOL + neighbourhoodManagerServer + ":" + port + API_PATH + LIGHTWEIGHTUPDATE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		if (!username.equals(CONFIG_DEF_NMUSERNAME) && password != null ){
			clientResource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
		}
		
		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	/**
	 * Deletes - unregisters the IoT object(s).
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be removed 
	 * (taken from request).
	 * @return Notification of success or failure.
	 */
	public synchronized Representation deleteObjects(Representation json){
		
		String endpointUrl = SERVER_PROTOCOL + neighbourhoodManagerServer + ":" + port + API_PATH + DELETE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);
		
		if (!username.equals(CONFIG_DEF_NMUSERNAME) && password != null ){
			clientResource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
		}
		
		Representation responseRepresentation = clientResource.post(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	
	// === PRIVATE METHODS ===
}
