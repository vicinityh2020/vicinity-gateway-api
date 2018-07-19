package eu.bavenir.ogwapi.commons.search;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import client.VicinityClient;
import client.model.Triple;
import eu.bavenir.ogwapi.commons.ConnectionDescriptor;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;

public class SparqlQuery {

	
	private static final String GWAPI_SERVICES_URL = "vic.org";
	

	// this is necessary to send requests to all neighbours
	private ConnectionDescriptor descriptor;
	
	private Logger logger;
	
	
	public SparqlQuery(ConnectionDescriptor descriptor, Logger logger) {
		this.descriptor = descriptor;
		this.logger = logger;
	}
	
	
	public String performQuery(String query) {
		
	
		Set<String> neighbours = descriptor.getRoster();
		
		
		// Retrieve a JSON-LD with a relevant TED for the query from the Gateway API Services
		String jsonTED = retrieveTED();
		
		// Retrieve a JSON document containing VICINITY ontology prefixes from the Gateway API Services
		String jsonPrefixes = retrievePrefixes();
		
		// init the client
		VicinityClient client = new VicinityClient(jsonTED, neighbours, jsonPrefixes);

		// discovery
		while(client.existIterativelyDiscoverableThings()){
			// discover relevant resources in the TED
			List<String> neighboursThingsIRIs = client.discoverRelevantThingIRI();
			// retrieve remote JSON data for each Thing IRI
			for(String neighboursThingIRI:neighboursThingsIRIs){
				// retrieve the JSON-LD exposed by the GATEWAY API SERVICES for this IRI Thing
				String thingsJsonRDF = retrieveRDF(neighboursThingIRI); 
				client.updateDiscovery(thingsJsonRDF);
			}
		}
		
		List<Triple<String,String,String>> relevantGatewayAPIAddresses = client.getRelevantGatewayAPIAddresses();

		// distributed access through secured channel
		for(Triple<String,String,String> neighbourGatewayAPIAddress:relevantGatewayAPIAddresses){ 
			String gatewayApiAddress = neighbourGatewayAPIAddress.getThirdElement();
			
			// retrieve the JSON document exposed by URL in gatewayApiAddress
			String jsonData = getPropertyOfRemoteObject(gatewayApiAddress, null); //!!!
			neighbourGatewayAPIAddress.setThirdElement(jsonData); 
		}

		// -- Solve query
		//List<Map<String,String>> queryResults = client.solveQuery(query, relevantGatewayAPIAddresses);
		client.close();
		
		// !!!
		return null;
		
	}
	
	
	private String retrieveTED() {
		
		ClientResource clientResource = new ClientResource(GWAPI_SERVICES_URL);
		
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
	
	
	
	private String retrievePrefixes() {
		
		ClientResource clientResource = new ClientResource(GWAPI_SERVICES_URL);
		
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
		
		ClientResource clientResource = new ClientResource(GWAPI_SERVICES_URL);
		
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
	
	
	private String getPropertyOfRemoteObject(String remoteObjectID, String propertyName) {
		
		StatusMessage statusMessage = descriptor.getPropertyOfRemoteObject(remoteObjectID, propertyName);
		
		if (statusMessage.isError()) {
			return null;
		}
		
		return statusMessage.getBody();
	}
}
