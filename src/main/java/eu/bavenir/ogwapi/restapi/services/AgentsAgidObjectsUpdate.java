package eu.bavenir.ogwapi.restapi.services;

import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.ogwapi.restapi.Api;

public class AgentsAgidObjectsUpdate extends ServerResource {

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
	 * Update service string.
	 */
	private static final String LIGHTWEIGHTUPDATE_SERVICE = "items/update";
	
	
	
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
		
		return lightweightUpdate(entity, logger, config);
	}
	
	
	private Representation lightweightUpdate(Representation json, Logger logger, XMLConfiguration config){
		
		String xmppServer = config.getString(CONFIG_PARAM_SERVER, CONFIG_DEF_SERVER);
		
		String endpointUrl = SERVER_PROTOCOL + xmppServer + SERVER_PART2 + LIGHTWEIGHTUPDATE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
}
