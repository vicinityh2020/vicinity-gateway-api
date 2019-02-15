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
	 * Name of the configuration parameter for URL path to Neighbourhood Manager server.
	 */
	private static final String CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER = "general.neighbourhoodManagerServer";
	
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER } parameter. 
	 */
	private static final String CONFIG_DEF_NEIGHBORHOODMANAGERSERVER = "vicinity.bavenir.eu";
	
	
	/**
	 * Name of the configuration parameter for Neighbourhood Manager port.
	 */
	private static final String CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT = "general.neighourhoodManagerPort";
	
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT } parameter.
	 */
	private static final int CONFIG_DEF_NEIGHBOURHOODMANAGERPORT = 3000;
	
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
		
		String neighbourhoodManagerServer = config.getString(CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER, CONFIG_DEF_NEIGHBORHOODMANAGERSERVER);
		
		int port = config.getInt(CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT, CONFIG_DEF_NEIGHBOURHOODMANAGERPORT);
		
		String endpointUrl = SERVER_PROTOCOL + neighbourhoodManagerServer + ":" + port + API_PATH + LIGHTWEIGHTUPDATE_SERVICE;
		
		ClientResource clientResource = new ClientResource(endpointUrl);

		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);
		
		return responseRepresentation;
	}
}
