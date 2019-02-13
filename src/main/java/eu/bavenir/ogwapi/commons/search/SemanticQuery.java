package eu.bavenir.ogwapi.commons.search;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.configuration2.XMLConfiguration;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;



public class SemanticQuery {

	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for URL path to Neighborhood Manager API.
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
	private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * URL Path to Semantic Search API
	 */
	private String semanticSearchAPIURL; 
	
	public SemanticQuery(XMLConfiguration config, Logger logger) {

		this.config = config;
		this.logger = logger;
		
		semanticSearchAPIURL = config.getString(CONFIG_PARAM_SEMANTICSEARCHAPI, CONFIG_DEF_SEMANTICSEARCHAPI);
	}
	
	public String performQuery(String query, Map<String, String> parameters) {
		
		JsonReader jsonReader = Json.createReader(new StringReader(query));
		JsonObject json;
		
		try {
			json = jsonReader.readObject();
		} catch (Exception e) {
			
			logger.severe("SemanticQuery#performQuery: Exception during reading JSON object: " 
						+ e.getMessage());
			
			return null;
		} finally {
			jsonReader.close();
		}
		
		String searchParam = json.getString("semanticInterface");
		
		// create headers
		Map<String, String> headers = new HashMap<>();
		headers.put("accept", "application/json");
		
		JsonNode response;
		
		try {
			response = Unirest.get(semanticSearchAPIURL)
							  .headers(headers)
						   	  .queryString("s", searchParam)
							  .asJson()
							  .getBody();
			
			logger.info("Semantic Search endpoint successfully reached.");
			
		} catch (UnirestException e) {
			
			response = null;
			e.printStackTrace();
			
			logger.warning("Can't reach Semantic Search endpoint.");
		}
		
		return response.toString();
	}
}