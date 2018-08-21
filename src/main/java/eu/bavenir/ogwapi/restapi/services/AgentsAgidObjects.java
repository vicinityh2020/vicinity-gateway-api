package eu.bavenir.ogwapi.restapi.services;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.Patch;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.restapi.Api;


/*
 * STRUCTURE
 * - constants
 * - public methods overriding HTTP methods 
 * - private methods
 */


/**
 * This class implements a {@link org.restlet.resource.ServerResource ServerResource} interface for following
 * Gateway API calls:
 * 
 *   URL: 				[server]:[port]/api/agents/{agid}/objects
 *   METHODS: 			GET, POST, PUT
 *   ATTRIBUTES:		agid - VICINITY Identifier of the Agent, that is in control of the Adapters 
 *   					(e.g. 1dae4326-44ae-4b98-bb75-15aa82516cc3).
 *   
 * @author sulfo
 *
 */
public class AgentsAgidObjects extends ServerResource {
	
	// === CONSTANTS ===
	
	/**
	 * Name of the Agent ID attribute.
	 */
	private static final String ATTR_AGID = "agid";
	
	/**
	 * Name of the configuration parameter for server URL.
	 */
	private static final String CONFIG_PARAM_SERVER = "general.server";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_SERVER CONFIG_PARAM_SERVER} configuration parameter. This value is
	 * taken into account when no suitable value is found in the configuration file. 
	 */
	private static final String CONFIG_DEF_SERVER = "";

	/**
	 * Server URL/IP with port and API name. The final end point is then obtained by doing:
	 * SERVER_URL + SOME_SERVICE_1 + someAttributeLikeID + SOME_SERVICE_2 + someOtherAttribute + etc...
	 * 
	 *  Example for discovery service:
	 *  SERVER_PROTOCOL + SERVER + DISCOVERY_SERVICE_1 + agid + DISCOVERY_SERVICE_2
	 *  
	 */
	private static final String SERVER_PROTOCOL = "https://"; 
	
	
	private static final String SERVER_PART2 = ":3000/commServer/";
	
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
	 * Update service string.
	 */
	private static final String LIGHTWEIGHTUPDATE_SERVICE = "items/update";
	
	/**
	 * Modify service string.
	 */
	private static final String HEAVYWEIGHTUPDATE_SERVICE = "items/modify";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	
	/**
	 * Returns the list of IoT objects registered under given Agent.
	 * 
	 * @return All VICINITY identifiers of objects registered under specified agent.
	 */
	@Get
	public Representation represent() {
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		XMLConfiguration config = (XMLConfiguration) getContext().getAttributes().get(Api.CONTEXT_CONFIG);
		
		
		String attrAgid = getAttribute(ATTR_AGID);
		
		if (attrAgid == null){
			logger.info("AGID: " + attrAgid + " Invalid Agent ID.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid Agent ID.");
		}
		
		return getAgentObjects(attrAgid, logger, config);
	}
	
	
	/**
	 * Register the IoT object(s) of the underlying eco-system e.g. devices, VA service.
	 * 
	 * @param entity Representation of the incoming JSON. List of IoT thing descriptions that are to be registered 
	 * (from request).
	 * @return All VICINITY identifiers of objects registered under VICINITY Gateway by this call.
	 * 
	 */
	@Post("json")
	public Representation accept(Representation entity) {
		
		String attrAgid = getAttribute(ATTR_AGID);
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		XMLConfiguration config = (XMLConfiguration) getContext().getAttributes().get(Api.CONTEXT_CONFIG);
		
		
		if (attrAgid == null){
			logger.info("AGID: " + attrAgid + " Invalid Agent ID.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid Agent ID.");
		}
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("AGID: " + attrAgid + " Invalid object descriptions.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object descriptions");
		}
		
		return storeObjects(entity, logger, config);
	}
	
	
	
	/**
	 * Update the thing descriptions of objects registered under the Agent.
	 * 
	 * @param entity Representation of the incoming JSON.
	 * @param description New thing descriptions for already registered objects (from request).
	 * 
	 */
	@Put("json")
	public Representation store(Representation entity) {
		
		String attrAgid = getAttribute(ATTR_AGID);
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		XMLConfiguration config = (XMLConfiguration) getContext().getAttributes().get(Api.CONTEXT_CONFIG);
		
		if (attrAgid == null){
			logger.info("AGID: " + attrAgid + " Invalid Agent ID.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid Agent ID.");
		}
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("AGID: " + attrAgid + " Invalid object descriptions.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object descriptions");
		}
		
		return heavyweightUpdate(entity, logger, config);
	}
	
	
	@Patch("json")
	public Representation modify(Representation entity) {
		
		String attrAgid = getAttribute(ATTR_AGID);
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		XMLConfiguration config = (XMLConfiguration) getContext().getAttributes().get(Api.CONTEXT_CONFIG);
		
		if (attrAgid == null){
			logger.info("AGID: " + attrAgid + " Invalid Agent ID.");
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Invalid Agent ID.");
		}
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			logger.info("AGID: " + attrAgid + " Invalid object descriptions.");
			
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid object descriptions");
		}
		
		return lightweightUpdate(entity, logger, config);
	}
	
	
	// === PRIVATE METHODS ===
	
	
	/**
	 * Update the thing descriptions of objects registered under the Agent.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be updated 
	 * (from request).
	 * @param logger Logger to be used. 
	 * @return The list of approved devices to be registered in agent configuration. Approved devices means only 
	 * devices, that passed the validation in semantic repository and their instances were created. 
	 */
	private Representation heavyweightUpdate(Representation json, Logger logger, XMLConfiguration config){
		
		String xmppServer = config.getString(CONFIG_PARAM_SERVER, CONFIG_DEF_SERVER);
		
		String endpointUrl = SERVER_PROTOCOL + xmppServer + SERVER_PART2 + HEAVYWEIGHTUPDATE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	private Representation lightweightUpdate(Representation json, Logger logger, XMLConfiguration config){
		
		String xmppServer = config.getString(CONFIG_PARAM_SERVER, CONFIG_DEF_SERVER);
		
		String endpointUrl = SERVER_PROTOCOL + xmppServer + SERVER_PART2 + LIGHTWEIGHTUPDATE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	
	
	/**
	 * Register the IoT object(s) of the underlying eco system e.g. devices, VA service.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing descriptions that are to be registered 
	 * (from request).
	 * @param logger Logger to be used. 
	 * @return All VICINITY identifiers of objects registered the Agent by this call.
	 */
	private Representation storeObjects(Representation json, Logger logger, XMLConfiguration config){
		
		String xmppServer = config.getString(CONFIG_PARAM_SERVER, CONFIG_DEF_SERVER);
		
		String endpointUrl = SERVER_PROTOCOL + xmppServer + SERVER_PART2 + REGISTRATION_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		Representation responseRepresentation = clientResource.post(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
	
	
	/**
	 * Retrieves the list of IoT objects registered under given Agent from the Neighbourhood Manager. 
	 * 
	 * @param agid The ID of the Agent in question.
	 * @param logger Logger to be used. 
	 * 
	 * @return All VICINITY identifiers of objects registered under specified agent.
	 */
	private Representation getAgentObjects(String agid, Logger logger, XMLConfiguration config){
		
		String xmppServer = config.getString(CONFIG_PARAM_SERVER, CONFIG_DEF_SERVER);
		
		
		String endpointUrl = SERVER_PROTOCOL + xmppServer + SERVER_PART2 + DISCOVERY_SERVICE_1 + agid + DISCOVERY_SERVICE_2;
		
		ClientResource clientResource = new ClientResource(endpointUrl);
		
		Representation representation = clientResource.get(MediaType.APPLICATION_JSON);
		
		return representation;
	
	}
}
