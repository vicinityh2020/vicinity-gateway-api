package eu.bavenir.ogwapi.commons.search;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.json.JSONArray;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;



public class SemanticQuery {

	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for URL path to Neighbourhood Manager API.
	 */
	private static final String CONFIG_PARAM_SEMANTICSEARCHAPI = "search.semantic.semanticSearchAPI";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBORHOODMANAGERAPI } parameter. 
	 */
	private static final String CONFIG_DEF_SEMANTICSEARCHAPI = "http://repo.sharq.se.rwth-aachen.de/search/";
	
	
	/* === FIELDS === */
	
	/**
	 * Configuration of the OGWAPI.
	 */
	//private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * URL Path to Semantic Search API
	 */
	private String semanticSearchAPIURL; 
	
	public SemanticQuery(XMLConfiguration config, Logger logger) {

		this.logger = logger;
		
		semanticSearchAPIURL = config.getString(CONFIG_PARAM_SEMANTICSEARCHAPI, CONFIG_DEF_SEMANTICSEARCHAPI);
	}
	
	public String performQuery(String sourceObjectId, String query, Map<String, String> parameters, JsonArray tds) {
		
		JsonReader jsonReader = Json.createReader(new StringReader(query));
		JsonObject json;
		
		try {
			json = jsonReader.readObject();
		} catch (Exception e) {
			
			logger.warning("Exception during reading JSON object: " + e.getMessage());
			
			return null;
		} finally {
			jsonReader.close();
		}
		
		String searchParam = json.getString("semanticInterface");
		
		// create headers
		Map<String, String> headers = new HashMap<>();
		headers.put("accept", "application/json");
		
		JsonNode response = null;
		
		try {
			response = Unirest.get(semanticSearchAPIURL)
							  .headers(headers)
						   	  .queryString("s", searchParam)
							  .asJson()
							  .getBody();
			
			logger.fine("Semantic Search endpoint successfully reached.");
			
		} catch (UnirestException e) {
			
			response = null;
			
			logger.warning("Can't reach Semantic Search endpoint. Exception: " + e.getMessage());
			
		}
		
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder mainArrayBuilder = Json.createArrayBuilder();
		
		Set<String> arr = new HashSet<>();
		
		response.getArray().forEach(item -> {
			arr.add((String) item);
		});
		
		tds.forEach(item -> {
			
			String currentSemanticInterface = getSemanticInterface(Json.createObjectBuilder().add("td", item).build());
			
			if (currentSemanticInterface != null) {
				
				arr.forEach(item2 -> {
					
					if (currentSemanticInterface.equals(item2.toString())) {
						
						JsonObjectBuilder innerObjectBuilder = Json.createObjectBuilder();
						
						innerObjectBuilder.add("oid", getOid(Json.createObjectBuilder().add("td", item).build()));
						innerObjectBuilder.add("semanticInterface", item2.toString());
						
						mainArrayBuilder.add(innerObjectBuilder);
					}
				});
			}
			
		});

		mainObjectBuilder.add("semanticInterfaces", mainArrayBuilder);

		return mainObjectBuilder.build().toString();
	}
	
	public String getSemanticInterface(JsonObject td) {
		
		if (td == null) {
			logger.warning("thingDescription is null.");
			return null;
		}
		
		JsonObject content = td.getJsonObject("td");
		if (content == null) {
			logger.warning("thingDescription is null.");
			return null;
		}
		
		return content.getString("semanticInterface", null);
	}
	
	public String getOid(JsonObject td) {
		
		if (td == null) {
			logger.warning("thingDescription is null.");
			return null;
		}
		
		JsonObject content = td.getJsonObject("td");
		if (content == null) {
			logger.warning("thingDescription is null.");
			return null;
		}
		
		return content.getString("oid", null);
	}

}