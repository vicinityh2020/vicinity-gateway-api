package eu.bavenir.ogwapi.commons.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.query.QueryFactory;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import client.VicinityAgoraClient;
import client.VicinityClient;
import eu.bavenir.ogwapi.commons.ConnectionDescriptor;
import eu.bavenir.ogwapi.commons.messages.StatusMessage;


/**
 * 
 * @author Andrea Cimmino (cimmino@fi.upm.es)
 *
 */
public class SparqlQuery {

	/**
	 * Name of the configuration parameter for GW API services URL.
	 */
	private static final String CONFIG_PARAM_GWAPISERVICESURL = "search.sparql.gwApiServicesUrl";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_GWAPISERVICESURL CONFIG_PARAM_GWAPISERVICESURL} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final String CONFIG_DEF_GWAPISERVICESURL = "http://vicinity-gateway-services.vicinity.linkeddata.es/advanced-discovery";
	
	
	private static final int ARRAYINDEX_OID = 2;
	
	private static final int ARRAYINDEX_PID = 4;
	
	private static final int MAX_PARALLEL_REQUESTS = 300;

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
		// 1. Retrieve TED
		
		Set<String> neighbours = descriptor.getRoster();
		logger.fine("Neighbours: "+neighbours.toString());
		logger.fine("Requesting TED");
		String jsonTED = retrieveTED(query, neighbours);
		logger.fine("TED retrieved, length " + jsonTED.length());
		
		// 2. Init the client
		VicinityClient client = new VicinityAgoraClient(jsonTED, neighbours, query);

		// 3. Retrieve remote data
		List<Entry<String,String>> remoteEndpoints = client.getRelevantGatewayAPIAddresses();
		logger.fine("Remote endpoints to retrieve data from "+remoteEndpoints.size());
		// 3.1 Prepare Futures
		ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_REQUESTS);
        List<Future<String>> list = new ArrayList<Future<String>>();
        for(int i = 0; i < remoteEndpoints.size(); i++) {
			String remoteGatewayAddress = remoteEndpoints.get(i).getValue(); 
			String accessIRI = remoteEndpoints.get(i).getKey();
			// Transform into right request values
			String[] splitArray = remoteGatewayAddress.split("/");		
			String objectId = splitArray[ARRAYINDEX_OID];
			String propertyId = splitArray[ARRAYINDEX_PID];
        		Callable<String> callable = new Callable<String>() {
                public String call() {
                    return getPropertyOfRemoteObject(objectId, propertyId, parameters, accessIRI);
                }
            };
            Future<String> future = executor.submit(callable);
            list.add(future);
        }
        // 3.2 Execute Futures
        for(Future<String> fut : list){
            try {
            		String jsonData = fut.get();
            		if(jsonData!=null && !jsonData.contains("\"error\":") && isCorrectJSON(jsonData)) {
            			JSONObject jsonDocument = new JSONObject(jsonData);
            			if(jsonDocument.has("data")) {
            				remoteEndpoints.get(list.indexOf(fut)).setValue(jsonDocument.getJSONObject("data").toString());
            			}else {
            				remoteEndpoints.get(list.indexOf(fut)).setValue(jsonData);
            			}
        			}
            } catch (Exception  e) {
                e.printStackTrace();
            }
        }
        // 3.3 shut down the executor service now
        executor.shutdown();
        // 3.4 Filter those remote enpoints without json
        remoteEndpoints = remoteEndpoints.stream().filter(entry -> isCorrectJSON(entry.getValue())).collect(Collectors.toList());
        
        logger.fine("Data retrieval finished");
        for(Entry<String, String> entry:remoteEndpoints) {
        		logger.fine("Remote JSON documents retrieved: "+entry.getValue());
        }
        
        logger.fine("Solving query");		
		List<Map<String,String>> queryResults = new ArrayList<>();
		try {
			QueryFactory.create(query) ;
			queryResults = client.solveQuery(remoteEndpoints);
		}catch(ResourceException e) {
			logger.severe("Provided query has errors!");
		}catch(Exception e) {
			logger.severe("Something when wrong processing the query");
		}
		
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
		
		String returnValue = mainBuilder.build().toString();
		logger.fine("RETURN VALUE: \n" + returnValue);
		
		return returnValue;
		
	}
	
	private Boolean isCorrectJSON(String object) {
		Boolean condition1 = object.startsWith("{") && object.endsWith("}");
		Boolean condition2 = object.startsWith("[") && object.endsWith("]");
		Boolean isCorrect = condition1 || condition2 ;
		if(!isCorrect)
			logger.severe("Retrieve something that is not a JSON: "+object);
		return isCorrect; 
	}	
	
	private String retrieveTED(String query, Set<String> neighbours) {	
		Unirest.setTimeouts(1800000, 1800000);
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/ld+json");
		headers.put("Content-Type", "application/ld+json");
		String jsonTED= "{}";
		try {
			jsonTED = Unirest.post(gwapiServicesUrl+"?neighbors="+neighbours.toString().replace("[", "").replace("]","").replace(" ", "")).headers(headers).body(query).asString().getBody();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return jsonTED;
	}
	
		
	private String getPropertyOfRemoteObject(String remoteObjectID, String propertyName, Map<String, String> parameters, String key) {
		
		StatusMessage statusMessage = descriptor.getPropertyOfRemoteObject(remoteObjectID, propertyName, parameters, null);
		
		if (statusMessage.isError()) {
			return null;
		}
		
		JsonObject jsonObject  = statusMessage.buildMessage();
		JsonArray jsonArray = jsonObject.getJsonArray(StatusMessage.ATTR_MESSAGE);
		
		logger.fine("JSON String: " + jsonArray.get(0).toString());
		
		return jsonArray.get(0).toString();
	}
}