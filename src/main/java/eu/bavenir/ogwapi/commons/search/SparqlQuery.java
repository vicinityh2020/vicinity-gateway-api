package eu.bavenir.ogwapi.commons.search;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import client.VicinityClient;
import client.model.Triple;
import eu.bavenir.ogwapi.commons.ConnectionDescriptor;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;

public class SparqlQuery {

	private static final int ARRAYINDEX_OID = 3;
	
	private static final int ARRAYINDEX_PID = 5;
	
	private static final String ATTR_RESOURCE = "resource";
	
	private static final String GWAPI_SERVICES_URL_PREFIXES = "http://gateway-services.vicinity.linkeddata.es/prefixes";
	
	private static final String GWAPI_SERVICES_URL_DISCOVERY = "http://gateway-services.vicinity.linkeddata.es/discovery";
	
	private static final String GWAPI_SERVICES_URL_RESOURCE = "http://gateway-services.vicinity.linkeddata.es/resource";

	// this is necessary to send requests to all neighbours
	private ConnectionDescriptor descriptor;
	
	private Logger logger;
	
	private JsonBuilderFactory jsonBuilderFactory;
	
	
	public SparqlQuery(ConnectionDescriptor descriptor, Logger logger) {
		this.descriptor = descriptor;
		this.logger = logger;
		
		jsonBuilderFactory = Json.createBuilderFactory(null);
	}
	
	
	public String performQuery(String query, Map<String, String> parameters) {
		
	
		// TODO remove after test
		System.out.println("QUERY: \n" + query
				+ "\nADDITIONAL PARAMETERS: \n" + parameters.toString()
				);
		
		
		
		Set<String> neighbours = descriptor.getRoster();
		
		neighbours.add("test-agora-1");
		
		// TODO remove after test
		System.out.println("NEIGHBOURS: \n" + neighbours.toString()); 
		
		
		// Retrieve a JSON-LD with a relevant TED for the query from the Gateway API Services
		String jsonTED = retrieveTED(query);
		
		
		// TODO remove after test
		System.out.println("RETRIEVED TED: \n" + jsonTED);
		
		
		// Retrieve a JSON document containing VICINITY ontology prefixes from the Gateway API Services
		String jsonPrefixes = retrievePrefixes();
		
		// TODO remove after test
		System.out.println("RETRIEVED PREFIXES: \n" + jsonPrefixes);
		
		
		// init the client
		VicinityClient client = new VicinityClient(jsonTED, neighbours, jsonPrefixes);

		
		// TODO remove after test
		System.out.println("===== STARTING THE DISCOVERY =======");
		
		// discovery
		while(client.existIterativelyDiscoverableThings()){
			
			// TODO remove after test
			System.out.println("DISCOVERY ITERATION STARTS");
			
			// discover relevant resources in the TED
			List<String> neighboursThingsIRIs = client.discoverRelevantThingIRI();
			// retrieve remote JSON data for each Thing IRI
			for(String neighboursThingIRI:neighboursThingsIRIs){
				
				// TODO remove after test
				System.out.println("DISCOVERED RELEVANT THING IRI: \n" + neighboursThingIRI);
				
				// TODO make this run in parallel
				
				// retrieve the JSON-LD exposed by the GATEWAY API SERVICES for this IRI Thing
				String thingsJsonRDF = retrieveRDF(neighboursThingIRI); 
				
				// TODO remove after test
				System.out.println("RETRIEVED THING RDF: \n" + thingsJsonRDF);
				
				client.updateDiscovery(thingsJsonRDF);
			}
		}
		
		// TODO remove after test
		System.out.println("===== DISCOVERY FINISHED =======");
		
		List<Triple<String,String,String>> relevantGatewayAPIAddresses = client.getRelevantGatewayAPIAddresses();

		
		// TODO remove after test
		System.out.println("===== STARTING DATA RETRIEVAL =======");
		
		// distributed access through secured channel
		for(Triple<String,String,String> neighbourGatewayAPIAddress:relevantGatewayAPIAddresses){ 
			String referenceString = neighbourGatewayAPIAddress.getThirdElement();
			
			// TODO remove after test
			System.out.println("REFERENCE STRING:  " + referenceString);
			
			// we need to split the /adapter-endpoint/objects/{oid}/property/{pid} 
			
			String[] splitArray = referenceString.split("/");
			
			String objectId = splitArray[ARRAYINDEX_OID];
			String propertyId = splitArray[ARRAYINDEX_PID];
			
			// TODO remove after test
			System.out.println("OID: " + objectId + " PROPERTY ID: " + propertyId);
			
			// retrieve the JSON property
			String jsonData = getPropertyOfRemoteObject(objectId, propertyId, parameters);
			
			// TODO remove after test
			System.out.println("RETRIEVED DATA: \n" + jsonData);
			
			neighbourGatewayAPIAddress.setThirdElement(jsonData); 
		}
		
		// TODO remove after test
		System.out.println("===== DATA RETRIEVAL FINISHED =======");
		

		// -- Solve query
		List<Map<String,String>> queryResults = client.solveQuery(query, relevantGatewayAPIAddresses);
		client.close();
		
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = jsonBuilderFactory.createArrayBuilder();
		
		for (Map<String, String> map : queryResults) {
			
			JsonObjectBuilder innerBuilder = jsonBuilderFactory.createObjectBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				innerBuilder.add(entry.getKey(), entry.getValue());
			}
			arrayBuilder.add(innerBuilder);
		}
		mainBuilder.add(StatusMessage.ATTR_MESSAGE, arrayBuilder);
		
		// TODO remove after test
		String returnValue = mainBuilder.build().toString();
		System.out.println("RETURN VALUE: \n" + returnValue);
		
		return returnValue;
		
		//return mainBuilder.build().toString();
		
	}
	
	
	private String retrieveTED(String query) {
		
		ClientResource clientResource = new ClientResource(GWAPI_SERVICES_URL_DISCOVERY);
		
		Writer writer = new StringWriter();
		Representation responseRepresentation = clientResource.post(new JsonRepresentation(query), 
				MediaType.APPLICATION_ALL_JSON);
		
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
	}
	
	
	
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
	}
	
	
	
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
		
	}
	
	
	private String getPropertyOfRemoteObject(String remoteObjectID, String propertyName, 
			Map<String, String> parameters) {
		
		StatusMessage statusMessage = 
				descriptor.getPropertyOfRemoteObject(remoteObjectID, propertyName, parameters, null);
		
		if (statusMessage.isError()) {
			return null;
		}
		
		JsonObject jsonObject  = statusMessage.buildMessage();
		
		JsonArray jsonArray = jsonObject.getJsonArray(StatusMessage.ATTR_MESSAGE);
		
		return jsonArray.get(0).toString();
	}
}
