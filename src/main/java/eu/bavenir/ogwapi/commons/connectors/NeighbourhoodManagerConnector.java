package eu.bavenir.ogwapi.commons.connectors;

import java.io.IOException;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;
import org.json.JSONObject;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import eu.bavenir.ogwapi.commons.ConnectionDescriptor;
import eu.bavenir.ogwapi.restapi.security.SecureServerComms;

/*
 * STRUCTURE
 * - constants
 * - fields
 * - public methods 
 * - private methods
 */

/**
 * Provides methods for communication with the Neighbourhood Manager. The
 * methods and fields are mostly static in nature, as this object is supposed to
 * be shared among the {@link ConnectionDescriptor ConnectionDescriptors } to
 * save resources. All methods are thread safe.
 * 
 * @author sulfo
 *
 */
public class NeighbourhoodManagerConnector {

	// === CONSTANTS ===

	/**
	 * Name of the configuration parameter for URL path to Neighbourhood Manager
	 * server.
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
	 * Name of the configuration parameter for Neighbourhood Manager base URI.
	 */
	private static final String CONFIG_PARAM_NEIGHBOURHOODMANAGERBASEURI = "general.neighourhoodManagerBaseUri";

	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBOURHOODMANAGERBASEURI } parameter.
	 */
	private static final String CONFIG_DEF_NEIGHBOURHOODMANAGERBASEURI = "/commserver/";

	/**
	 * Server URL/IP with port and API name. The final end point is then obtained by
	 * doing: SERVER_URL + SOME_SERVICE_1 + someAttributeLikeID + SOME_SERVICE_2 +
	 * someOtherAttribute + etc...
	 * 
	 * Example for discovery service: SERVER_PROTOCOL + SERVER + DISCOVERY_SERVICE_1
	 * + agid + DISCOVERY_SERVICE_2
	 * 
	 */

	/**
	 * Default value for {@link #CONFIG_PARAM_SERVER_PROTOCOL } parameter.
	 */
	private static final String CONFIG_PARAM_SERVER_PROTOCOL = "general.testProtocol";
	private static final String CONFIG_DEF_SERVER_PROTOCOL = "https://";

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

	/**
	 * Get thing descriptions string
	 */
	private static final String TD_SERVICE = "items/td";

	/**
	 * Send counters
	 */
	private static final String SEND_COUNTERS = "counters";

	/**
	 * Perform handshake with the platform
	 */
	private static final String HANDSHAKE = "handshake";

	/**
	 * Name of the configuration parameter for Gateway platform identity.
	 */
	private static final String CONFIG_PARAM_PLATFORMIDENTITY = "platformSecurity.identity";

	/**
	 * Default value for {@link #CONFIG_PARAM_PLATFORMIDENTITY } parameter.
	 */
	private static final String CONFIG_DEF_PLATFORMIDENTITY = "ANONYMOUS";

	/**
	 * Name of the configuration parameter for setting encryption of the payload.
	 */
	private static final String CONFIG_PARAM_PLATFORMSECURITY = "platformSecurity.enable";

	/**
	 * Default value for {@link #CONFIG_PARAM_PLATFORMENCRYPTION } parameter.
	 */
	private static final Boolean CONFIG_DEF_PLATFORMSECURITY = false;

	// === FIELDS ===

	/**
	 * Protocol used to connect the server
	 */
	private String server_protocol;

	/**
	 * Class for secure comms with platform
	 */
	private static SecureServerComms secureComms;

	/**
	 * The part of the URL containing the API base URI.
	 */
	private String api_base_uri;

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

	/**
	 * Logger.
	 */
	private Logger logger;

	/**
	 * Gateway identity.
	 */
	private String agid;

	/**
	 * Send authorization token.
	 */
	private Boolean securityEnabled;

	// === PUBLIC METHODS ===

	/**
	 * Constructor.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public NeighbourhoodManagerConnector(XMLConfiguration config, Logger logger) {

		this.logger = logger;

		agid = config.getString(CONFIG_PARAM_PLATFORMIDENTITY, CONFIG_DEF_PLATFORMIDENTITY);

		neighbourhoodManagerServer = config.getString(CONFIG_PARAM_NEIGHBORHOODMANAGERSERVER,
				CONFIG_DEF_NEIGHBORHOODMANAGERSERVER);

		server_protocol = config.getString(CONFIG_PARAM_SERVER_PROTOCOL, CONFIG_DEF_SERVER_PROTOCOL);

		port = config.getInt(CONFIG_PARAM_NEIGHBOURHOODMANAGERPORT, CONFIG_DEF_NEIGHBOURHOODMANAGERPORT);

		securityEnabled = config.getBoolean(CONFIG_PARAM_PLATFORMSECURITY, CONFIG_DEF_PLATFORMSECURITY);

		api_base_uri = config.getString(CONFIG_PARAM_NEIGHBOURHOODMANAGERBASEURI, CONFIG_DEF_NEIGHBOURHOODMANAGERBASEURI);

		secureComms = new SecureServerComms(config, logger);
	}

	/**
	 * Retrieves the list of IoT objects registered under given Agent from the
	 * Neighbourhood Manager.
	 * 
	 * @param agid The ID of the Agent in question.
	 * @return All VICINITY identifiers of objects registered under specified agent.
	 */
	public synchronized Representation getAgentObjects(String agid) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri + DISCOVERY_SERVICE_1
				+ agid + DISCOVERY_SERVICE_2;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation representation = clientResource.get(MediaType.APPLICATION_JSON);

		return representation;

	}

	/**
	 * Register the IoT object(s) of the underlying eco system e.g. devices, VA
	 * service.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing
	 *             descriptions that are to be registered (from request).
	 * @return All VICINITY identifiers of objects registered the Agent by this
	 *         call.
	 */
	public synchronized Representation storeObjects(Representation json) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri
				+ REGISTRATION_SERVICE;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation responseRepresentation = clientResource.post(json, MediaType.APPLICATION_JSON);

		return responseRepresentation;
	}

	/**
	 * Update the thing descriptions of objects registered under the Agent. This
	 * will delete the old records and replace them with new ones.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing
	 *             descriptions that are to be updated (from request).
	 * @return The list of approved devices to be registered in agent configuration.
	 *         Approved devices means only devices, that passed the validation in
	 *         semantic repository and their instances were created.
	 */
	public synchronized Representation heavyweightUpdate(Representation json) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri
				+ HEAVYWEIGHTUPDATE_SERVICE;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);

		return responseRepresentation;
	}

	/**
	 * Update the thing descriptions of objects registered under the Agent. This
	 * will only change the required fields.
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing
	 *             descriptions that are to be updated (from request).
	 * @return The list of approved devices to be registered in agent configuration.
	 *         Approved devices means only devices, that passed the validation in
	 *         semantic repository and their instances were created.
	 */
	public synchronized Representation lightweightUpdate(Representation json) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri
				+ LIGHTWEIGHTUPDATE_SERVICE;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation responseRepresentation = clientResource.put(json, MediaType.APPLICATION_JSON);

		return responseRepresentation;
	}

	/**
	 * Deletes - unregisters the IoT object(s).
	 * 
	 * @param json Representation of the incoming JSON. List of IoT thing
	 *             descriptions that are to be removed (taken from request).
	 * @return Notification of success or failure.
	 */
	public synchronized Representation deleteObjects(Representation json) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri + DELETE_SERVICE;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation responseRepresentation = clientResource.post(json, MediaType.APPLICATION_JSON);

		return responseRepresentation;
	}

	/**
	 * Retrieves the thing descriptions of list IoT objects from the Neighborhood
	 * Manager.
	 * 
	 * @param Representation of the incoming JSON. List of OIDs
	 * @return Thing descriptions of objects specified in payload.
	 */
	public synchronized Representation getThingDescriptions(Representation json) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri + TD_SERVICE;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation responseRepresentation = clientResource.post(json, MediaType.APPLICATION_JSON);

		return responseRepresentation;

		/*
		 * String ret;
		 * 
		 * try { ret = representation.getText(); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); ret = null; }
		 * 
		 * return ret;
		 */
	}

	/**
	 * Retrieves the thing descriptions of list IoT objects from the Neighborhood
	 * Manager.
	 * 
	 * @param Representation of the incoming JSON. List of OIDs
	 * @return Thing descriptions of objects specified in payload.
	 */
	public synchronized Representation getThingDescription(String objectId) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri + TD_SERVICE;

		ClientResource clientResource = createRequest(endpointUrl);

		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();

		mainArrayBuilder.add(Json.createObjectBuilder().add("oid", objectId));

		mainObjectBuilder.add("objects", mainArrayBuilder);

		JsonObject payload = mainObjectBuilder.build();

		Representation responseRepresentation = clientResource.post(new JsonRepresentation(payload.toString()),
				MediaType.APPLICATION_JSON);

		return responseRepresentation;

	}

	/**
	 * Sends count of messages sent by the gateway.
	 * 
	 * @param JSON containing array records with all the messages
	 * @return Server acknowledgment
	 */
	public synchronized Representation sendCounters(JsonObject payload) {

		String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri + SEND_COUNTERS;

		ClientResource clientResource = createRequest(endpointUrl);

		Representation responseRepresentation = clientResource.post(new JsonRepresentation(payload.toString()),
				MediaType.APPLICATION_JSON);

		return responseRepresentation;

	}

	/**
	 * Perform handshake and expects NM to validate identity.
	 * 
	 * @param JSON containing array records with all the messages
	 * @return Server acknowledgment
	 */
	public synchronized void handshake() {
		try {
			logger.info("Connecting to NM in: " + neighbourhoodManagerServer + port + api_base_uri);
			String endpointUrl = server_protocol + neighbourhoodManagerServer + ":" + port + api_base_uri + HANDSHAKE;
			ClientResource clientResource = createRequest(endpointUrl);
			Representation responseRepresentation = clientResource.get(MediaType.APPLICATION_JSON);
			JSONObject jsonDocument = new JSONObject(responseRepresentation.getText());
			logger.info(jsonDocument.getString("message"));
		} catch (IOException i) {
			i.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			logger.warning(
					"There might be a problem authenticating your signature, please check that you uploaded the right public key to the server.");
		}

	}

	// === PRIVATE METHODS ===

	private ClientResource createRequest(String endpointUrl) {
		ClientResource clientResource = new ClientResource(endpointUrl);
		// Add auth token if security enabled
		if (securityEnabled) {
			String token = secureComms.getToken();
			ChallengeResponse cr = new ChallengeResponse(ChallengeScheme.HTTP_OAUTH_BEARER);
			cr.setRawValue(token);
			clientResource.setChallengeResponse(cr);
		}
		return clientResource;
	}

}
