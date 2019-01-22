package eu.bavenir.ogwapi.commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;


public class PersistenceManager {

	/*
	 * STRUCTURE:
	 * - constants
	 * - fields
	 * - public methods
	 */


	/**
	 * This class serves to work with data files, which are used for ensure persistence.
	 * 
	 * There are two types of data files:
	 * 		1. Data which is using for remembering gateway current state (EventChannels, Subscriptions and Actions)
	 * 		2. JSON file with information called thing description (TD)
	 * For first type of data is used serialization for storing them.
	 * Second type is storing in JSON format and this JSON file is getting from server by Unirest post.
	 * 
	 * Mentioned data exist for each object which is logged in OGWAPI.
	 * Data class {@link u.bavenir.ogwapi.commons.Data Data}. 
	 * 
	 * 
	 * @author Andrej
	 *
	 */
	
	
	/* === CONSTANTS === */

	/**
	 * Name of the configuration parameter for path to data file.
	 */
	private static final String CONFIG_PARAM_PERSISTENCEFILE = "general.persistencePath";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_PERSISTENCEROOTPATH } parameter. 
	 */
	private static final String CONFIG_DEF_PERSISTENCEFILE = "data/%s-data.ser";
	
	/**
	 * Name of the configuration parameter for path to JSON TD file.
	 */
	private static final String CONFIG_PARAM_TDFILE = "general.TDPath";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_TDFILE } parameter. 
	 */
	private static final String CONFIG_DEF_TDFILE = "data/%s-TD.json";
	
	/**
	 * Name of the configuration parameter for URL path to Neighborhood Manager API.
	 */
	private static final String CONFIG_PARAM_NEIGHBORHOODMANAGERAPI = "general.neighborhoodManagerAPI";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_NEIGHBORHOODMANAGERAPI } parameter. 
	 */
	private static final String CONFIG_DEF_NEIGHBORHOODMANAGERAPI = "https://vicinity.bavenir.eu:3000/commServer/items/searchItems";
	
	/* === FIELDS === */
	
	/**
	 * Path to file for storing data
	 */
	private String persistenceFile; 
	
	/**
	 * Path to TD json file 
	 */
	private String thingDescriptionFile; 
	
	/**
	 * URL Path to Neighborhood Manager API
	 */
	private String neighborhoodManagerAPIURL; 
	
	/**
	 * Configuration of the OGWAPI.
	 */
	private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor
	 */
	public PersistenceManager(XMLConfiguration config, Logger logger) {
		
		this.config = config;
		this.logger = logger;
		
		persistenceFile = config.getString(CONFIG_PARAM_PERSISTENCEFILE, CONFIG_DEF_PERSISTENCEFILE);
		thingDescriptionFile = config.getString(CONFIG_PARAM_TDFILE, CONFIG_DEF_TDFILE);
		neighborhoodManagerAPIURL = config.getString(CONFIG_PARAM_NEIGHBORHOODMANAGERAPI, CONFIG_DEF_NEIGHBORHOODMANAGERAPI);
	}
	
	/**
	 * load object's data from file
	 * 
	 * @param objectId - specify object
	 */
	public Object loadData(String objectId) {
		
		// get the file name and create file object
		String objectDataFileName = String.format(persistenceFile, objectId);
		File file = new File(objectDataFileName);
		
		// call method to load file
		return loadData(file);
	}
	
	/**
	 * load object's data from file
	 * 
	 * @param file - specify file
	 */
	public Object loadData(File file) {
		
		// loaded data
		Object data;
		
		// if file exist then try to open file and load data
        if(file.exists()) {
        	
        	try {
    			
    			FileInputStream fileIn = new FileInputStream(file);
    			ObjectInputStream in = new ObjectInputStream(fileIn);
    			data = in.readObject();
    			in.close();
    			fileIn.close();
    			
    			logger.info("Data was loaded from file - " + file.getName() );
    			
    	    } catch (IOException i) {
    	    	
    	    	logger.warning("Data could not be loaded from file - " + file.getName() );
    	    	i.printStackTrace();
    	        return null;
    	        
    	    } catch (ClassNotFoundException c) {
    	    	
    	        logger.severe("Class not found! Possible file corruption.");
    	        c.printStackTrace();
    	        return null;
    	    }
        	
        	
        	return data;
        }
		
        logger.info("File not found!");
		return null;
	}
	
	/**
	 * save object's data to file
	 * 
	 * @param objectId - specify object
	 * @param data - data to save
	 */
	public void saveData(String objectId, Object data) {
		
		// get the file name 
		String objectDataFileName = String.format(persistenceFile, objectId);
		
		// try to write data to file
		try {
			
			FileOutputStream fileOut =
			new FileOutputStream(objectDataFileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(data);
			out.close();
			fileOut.close();
			
			logger.fine("Serialized data for " + objectId + " is saved in " + objectDataFileName );
			
		} catch (IOException i) {
			
			logger.warning("Data for " + objectId + " could not be written to file. " + objectDataFileName );
			i.printStackTrace();
		}
	}
	
	/**
	 * load object's thing description JSON
	 * 
	 * @param objectId - specify object
	 */
	public JsonNode loadThingDescription(String objectId) {
		
		// First, try to load from server
		JsonNode loadedTD = loadThingDescriptionFromServer(objectId);
		if (loadedTD != null) {
			
			if (!loadedTD.getObject().getBoolean("error")) {
				
				saveThingDescription(objectId, loadedTD.toString());
				return loadedTD;
			} else {
				
				logger.warning("TD json for " + objectId + " contains error message! Try to load TD from file.");
			}	
		}
		
		// Try to load from file
		loadedTD = loadThingDescriptionFromFile(objectId);
		if (loadedTD != null) {
			
			return loadedTD;
		} 
		
		logger.warning("TD json for " + objectId + " could not be loaded.");
		return null;
	}
	
	/**
	 * load object's thing description JSON from server
	 * 
	 * @param objectId - specify object
	 */
	public JsonNode loadThingDescriptionFromServer(String objectId) {
		
		// create headers
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		
		// create body (example: {"oids":["d52f2a03-09c7-46a3-b86c-cf885dd81dd7"]})
		JSONObject body = new JSONObject();
		JSONArray array = new JSONArray();
		
		array.put(objectId); 
		// for DEBUG reason 
		// array.put("d52f2a03-09c7-46a3-b86c-cf885dd81dd7"); 
		body.put("oids", array);
		
		JsonNode response;
		
		try {
			response = Unirest.post(neighborhoodManagerAPIURL)
						   	  .headers(headers)
							  .body(body)
							  .asJson()
							  .getBody();
			
			logger.info("TD json for " + objectId + " was loaded from server.");
			
		} catch (UnirestException e) {
			
			response = null;
			e.printStackTrace();
			
			logger.warning("TD json for " + objectId + " could not be loaded from server.");
		}
		
		return response;
	}
	
	/**
	 * load object's thing description JSON from file
	 * 
	 * @param objectId - specify object
	 */
	public JsonNode loadThingDescriptionFromFile(String objectId) {
		
		// get the file name and create file object
		String objectTDFileName = String.format(thingDescriptionFile, objectId);
		File file = new File(objectTDFileName);
		
		// call method to load file
		return loadThingDescriptionFromFile(file);
	}
	
	/**
	 * load object's thing description JSON from file
	 * 
	 * @param file - specify file
	 */
	public JsonNode loadThingDescriptionFromFile(File file) {
		
		// loaded data
		JsonNode data;
		
		// if file exist then try to open file and load data
        if(file.exists()) {
        	
        	try {
    			
        		ObjectMapper mapper = new ObjectMapper();
    			data = new JsonNode(mapper.readValue(file, new TypeReference<String>(){}));
    			
    			logger.info("TD json was loaded from file - " + file.getName() );
    			
    	    } catch (IOException i) {
    	    	
    	    	logger.warning("TD json could not be loaded from file - " + file.getName() );
    	    	i.printStackTrace();
    	        return null;
    	        
    	    } 
        	
        	return data;
        }
		
        logger.info("TD json not found!");
		return null;
	}
	
	/**
	 * save object's thing description to file
	 * 
	 * @param objectId - specify object
	 * @param data - data to save
	 */
	public void saveThingDescription(String objectId, Object data) {
		
		// get the file name 
		String objectTDFileName = String.format(thingDescriptionFile, objectId);
		
		// try to write data to file
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(new File(objectTDFileName), data);
			
			logger.fine("TD json for " + objectId + " is saved in " + objectTDFileName );
			
		} catch (IOException i) {
			
			logger.warning("TD json for " + objectId + " could not be written to file. " + objectTDFileName );
			i.printStackTrace();
		}
	}
}
