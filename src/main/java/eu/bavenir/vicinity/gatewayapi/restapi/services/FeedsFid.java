package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jivesoftware.smack.roster.RosterEntry;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import eu.bavenir.vicinity.gatewayapi.App;
import eu.bavenir.vicinity.gatewayapi.restapi.Api;
import eu.bavenir.vicinity.gatewayapi.xmpp.CommunicationNode;


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
 *   URL: 				[server]:[port]/api/feeds/{fid}
 *   METHODS: 			GET, DELETE
 *   SPECIFICATION:		@see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   ATTRIBUTES:		fid - VICINITY Identifier of the feed (e.g. 66348b54-1609-11e7-93ae-92361f002671)
 *   
 * @author sulfo
 *
 */
public class FeedsFid extends ServerResource {

	// === CONSTANTS ===
	
	/**
	 * Name of the Subscription ID attribute.
	 */
	private static final String ATTR_FID = "fid";
	
	/**
	 * Name of the Object ID attribute.
	 */
	private static final String ATTR_OIDS = "oids";
	
	
	// === OVERRIDEN HTTP METHODS ===
	
	/**
	 * Returns the given feed.
	 * 
	 * @return Information about the feed.
	 */
	@Get
	public String represent() {
		
		String attrFid = getAttribute(ATTR_FID);
		
		if (attrFid != null){
			return getFeed(attrFid);
		} else {			
			return null;
		}
	}
	
	
	/**
	 * Deletes the given feed (all exclusive discovered objects by this feed won’t be available).
	 */
	@Delete
	public void remove() {
		String attrFid = getAttribute(ATTR_FID);
		
		if (attrFid != null){
			deleteFeed(attrFid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
	
	
	// === PRIVATE METHODS ===
	// TODO documentation
	private String getFeed(String sid){
		
		Logger logger = (Logger) getContext().getAttributes().get(Api.CONTEXT_LOGGER);
		
		CommunicationNode communicationNode 
				= (CommunicationNode) getContext().getAttributes().get(Api.CONTEXT_COMMNODE);
		
		XMLConfiguration config = (XMLConfiguration) getContext().getAttributes().get(Api.CONTEXT_CONFIG);
		String xmppDomain = config.getString(App.CONFIG_PARAM_XMPPDOMAIN, App.CONFIG_DEF_XMPPDOMAIN);
		
		Collection<RosterEntry> rosterObjects = communicationNode.getRosterEntriesForUser(
				getRequest().getChallengeResponse().getIdentifier());
		
		// create an array list of the oids - and remove the domain from each oid
		JsonArrayBuilder oidsArrayBuilder = Json.createArrayBuilder();
		
		for (RosterEntry entry : rosterObjects) {
			oidsArrayBuilder.add(entry.getJid().toString().replace("@" + xmppDomain, ""));
		}
		
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		mainObjectBuilder.add(ATTR_OIDS, oidsArrayBuilder.build());
		JsonObject object = mainObjectBuilder.build();
		
		// send the thing
		ClientResource clientResource = new ClientResource("http://vicinity.bavenir.eu:3000/commServer/search");
		Writer writer = new StringWriter();
		
		Representation responseRepresentation = clientResource.post(new JsonRepresentation(object.toString()), 
					MediaType.APPLICATION_JSON);
		
		try {
			responseRepresentation.write(writer);
		} catch (IOException e) {
			logger.warning(e.getMessage());
		}
		
		return writer.toString();
		
	}
	
	// TODO documentation
	private void deleteFeed(String fid){
		if (fid.equals("66348b54-1609-11e7-93ae-92361f002671")){
			//return "Object deleted.";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
	}
}
