package eu.bavenir.ogwapi.commons.search;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import client.VicinityAgoraClient;
import client.VicinityClient;
import eu.bavenir.ogwapi.commons.ConnectionDescriptor;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;

public class SparqlQuery {

	/**
	 * Name of the configuration parameter for GW API services URL.
	 */
	private static final String CONFIG_PARAM_GWAPISERVICESURL = "search.sparql.gwapiservicesurl";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_GWAPISERVICESURL CONFIG_PARAM_GWAPISERVICESURL} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_GWAPISERVICESURL = "http://gateway-services.vicinity.linkeddata.es/discovery";
	
	
	private static final int ARRAYINDEX_OID = 2;
	
	private static final int ARRAYINDEX_PID = 4;
	

	// this is necessary to send requests to all neighbours
	private ConnectionDescriptor descriptor;
	
	private Logger logger;
	
	private JsonBuilderFactory jsonBuilderFactory;
	
	private String gwapiServicesUrl;
	
	
	public SparqlQuery(XMLConfiguration config, ConnectionDescriptor descriptor, Logger logger) {
		this.descriptor = descriptor;
		this.logger = logger;
		
		gwapiServicesUrl = config.getString(CONFIG_PARAM_GWAPISERVICESURL, CONFIG_DEF_GWAPISERVICESURL);
		
		jsonBuilderFactory = Json.createBuilderFactory(null);
		
		
	}
	
	
	public String performQuery(String query, Map<String, String> parameters) {
		
		// TODO remove after test
		System.out.println("QUERY: \n" + query
				+ "\nADDITIONAL PARAMETERS: \n" + parameters.toString()
				);
		
		
		
		Set<String> neighbours = descriptor.getRoster();

		// TODO remove after test
		System.out.println("NEIGHBOURS: \n" + neighbours.toString()); 
		
		
		// Retrieve a JSON-LD with a relevant TED for the query from the Gateway API Services
		String jsonTED = retrieveTED(query);
		
		
		// TODO remove after test
		System.out.println("RETRIEVED TED: \n" + jsonTED);
		
		
		// init the client
		VicinityClient client = new VicinityAgoraClient(jsonTED, neighbours, query);

		List<Entry<String,String>> remoteEndpoints = client.getRelevantGatewayAPIAddresses();

		
		// TODO remove after test
		System.out.println("===== STARTING DATA RETRIEVAL =======");
		
		// distributed access through secured channel
		for(int i = 0; i < remoteEndpoints.size(); i++) {
			String remoteGatewayAddress = remoteEndpoints.get(i).getValue(); 
			
			// TODO remove after test
			System.out.println("REFERENCE STRING:  " + remoteGatewayAddress);
			
			// we need to split the /adapter-endpoint/objects/{oid}/property/{pid} 
			
			String[] splitArray = remoteGatewayAddress.split("/");
						
			String objectId = splitArray[ARRAYINDEX_OID];
			String propertyId = splitArray[ARRAYINDEX_PID];
			
			
			// TODO remove after test
			System.out.println("OID: " + objectId + " PROPERTY ID: " + propertyId);
			
			// retrieve the JSON property
			String jsonData = getPropertyOfRemoteObject(objectId, propertyId, parameters);
			
			// TODO remove after test
			System.out.println("RETRIEVED DATA: \n" + jsonData);
			
			
			remoteEndpoints.get(i).setValue(jsonData);
		}
		
		// TODO remove after test
		System.out.println("===== DATA RETRIEVAL FINISHED =======");
		

		// -- Solve query
		List<Map<String,String>> queryResults = client.solveQuery(remoteEndpoints);

		
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = jsonBuilderFactory.createArrayBuilder();
		
		// TODO remove
		System.out.println("RESULT:\n");
		
		for (Map<String, String> map : queryResults) {
			
			JsonObjectBuilder innerBuilder = jsonBuilderFactory.createObjectBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				innerBuilder.add(entry.getKey(), entry.getValue());
				
				// TODO remove
				System.out.println(entry.getKey() + " " + entry.getValue());
			}
			arrayBuilder.add(innerBuilder);
		}
		mainBuilder.add(StatusMessage.ATTR_MESSAGE, arrayBuilder);
		
		// TODO remove after test
		String returnValue = mainBuilder.build().toString();
		System.out.println("RETURN VALUE: \n" + returnValue);
		
		return returnValue;

	}
	
	/*
	private String retrieveTED(String query) {
		
		Client client = new Client(new Context(), Protocol.HTTP);
		client.getContext().getParameters().add ( "idleTimeout", "6000000" );
		client.getContext().getParameters().add ( "socketTimeout", "6000000" );
		client.getContext().getParameters().add ( "stopIdleTimeout", "6000000" );
		
		ClientResource clientResource = new ClientResource(gwapiServicesUrl);
		clientResource.setNext(client);
		
		
		Writer writer = new StringWriter();
		Representation responseRepresentation = clientResource.post(query, MediaType.APPLICATION_ALL_JSON); 
		
		if (responseRepresentation == null){
			return null;
		} 
		
		try {
			responseRepresentation.write(writer);
		} catch (IOException e) {
			e.printStackTrace();
			logger.info(e.getMessage());
		}
		
		return writer.toString();
	}*/
	
	private String retrieveTED(String query) {	

		Unirest.setTimeouts(1800000, 1800000);

		Map<String, String> headers = new HashMap<String, String>();

		headers.put("Accept", "application/ld+json");

		headers.put("Content-Type", "application/ld+json");

		String jsonTED= "{}";

		try {

			jsonTED = Unirest.post(gwapiServicesUrl).headers(headers).body(query).asString().getBody();

			

		} catch (UnirestException e) {

			e.printStackTrace();

		}

		return jsonTED;

	}
	
	
	/*
	private String retrievePrefixes() {
		
		ClientResource clientResource = new ClientResource(GWAPI_SERVICES_URL_PREFIXES);
		
		Writer writer = new StringWriter();
		Representation responseRepresentation = clientResource.get();
		
		if (responseRepresentation == null){
			return null;
		} 
		
		try {
			responseRepresentation.write(writer);
		} catch (IOException e) {
			e.printStackTrace();
			logger.info(e.getMessage());
		}
		
		return writer.toString();
	}*/
	
	
	/*
	private String retrieveRDF(String neighboursThingIRI) {
		
		
		ClientResource clientResource = new ClientResource(GWAPI_SERVICES_URL_RESOURCE);
		
		
		JsonObjectBuilder jsonObjectBuilder = jsonBuilderFactory.createObjectBuilder();
		
		jsonObjectBuilder.add(ATTR_RESOURCE, neighboursThingIRI);
		
		
		Writer writer = new StringWriter();
		Representation responseRepresentation = clientResource.post(new JsonRepresentation(jsonObjectBuilder.build()), 
				MediaType.APPLICATION_JSON);
		
		if (responseRepresentation == null){
			return null;
		} 
		
		try {
			responseRepresentation.write(writer);
		} catch (IOException e) {
			e.printStackTrace();
			logger.info(e.getMessage());
		}
		
		return writer.toString();
		
	}*/
	
	
	private String getPropertyOfRemoteObject(String remoteObjectID, String propertyName, 
			Map<String, String> parameters) {
		
		StatusMessage statusMessage = 
				descriptor.getPropertyOfRemoteObject(remoteObjectID, propertyName, parameters, null);
		
		if (statusMessage.isError()) {
			
			// TODO delete after test
			System.out.println("ERROR MESSAGE RETURNED: " + statusMessage.buildMessage().toString());
			
			return null;
		} 
		
		JsonObject jsonObject  = statusMessage.buildMessage();
		
		JsonArray jsonArray = jsonObject.getJsonArray(StatusMessage.ATTR_MESSAGE);
		
		return jsonArray.get(0).toString();
	}
}
