package eu.bavenir.ogwapi.commons.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.IOUtils;

/**
 * Class for persisting counters between restarts.
 * Instance of this class exists in app -> MessageCounter.
 * 
 * On init if counter persisted data exists is loaded.
 *  
 * @author Jorge
 *
 */


public class Counters {
	
	/* === CONSTANTS === */
	
	/**
	 * Name of the configuration parameter for the number of records that are sent to NM
	 */
	private static final String COUNTERS_PERSISTENCE_FILE = "counters.json";
	
	/**
	 * Name of the configuration parameter for path to data files.
	 */
	private static final String CONFIG_PARAM_DATADIR = "general.dataDirectory";
	
	/**
	 * Default value for {@link #CONFIG_PARAM_PERSISTENCEROOTPATH } parameter. 
	 */
	private static final String CONFIG_DEF_PERSISTENCEFILE = "data/";
	
	/* === FIELDS === */
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * List of messages
	 */
	private ArrayList<JsonObject> records;
	
	/**
	 * Path to file for storing data
	 */
	private String countersFile;
	
	
	/* === PUBLIC METHODS === */

	/**
	 * Constructor
	 */	
	public Counters(XMLConfiguration config, Logger logger) {
		this.logger = logger;
		countersFile = config.getString(CONFIG_PARAM_DATADIR, CONFIG_DEF_PERSISTENCEFILE) + COUNTERS_PERSISTENCE_FILE;
		// Load old records and delete file that stored them
		records = loadCounters();
		deleteCounters();
	}
	
	/**
	 * Store counters in file
	 * @param records
	 */
	public void saveCounters(List<JsonObject> records) {
		// get the file name 
		String objectDataFileName = String.format(countersFile, COUNTERS_PERSISTENCE_FILE);
		String toWrite; 
		
		// try to write data to file
		try {
			
			FileOutputStream fileOut =
			new FileOutputStream(objectDataFileName);
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(objectDataFileName), StandardCharsets.UTF_8);
			toWrite = records.toString();
			out.write(toWrite);
			out.close();
			fileOut.close();
			
			logger.fine("Serialized counters data is saved in " + COUNTERS_PERSISTENCE_FILE );
			
		} catch (IOException i) {
			
			logger.warning("Counters data could not be written to file " + COUNTERS_PERSISTENCE_FILE );
			i.printStackTrace();
		}
	}
	
	/**
	 * Get and return number of messages in counters file
	 * @return count
	 */
	public int getCountOfMessages(){
		logger.fine(records.size() + " messages have been loaded.");
		return records.size();
	}
	
	/**
	 * Returns list of messages
	 * @return
	 */
	public ArrayList<JsonObject> getRecords(){
		return records;
	}
	
	/* === PRIVATE METHODS === */
	
	/**
	 * Load messages from counters file
	 */
	public ArrayList<JsonObject> loadCounters() {
		// loaded data
		JsonArray raw;
		ArrayList<JsonObject> data = new ArrayList<JsonObject>();
				
		String objectDataFileName = String.format(countersFile, COUNTERS_PERSISTENCE_FILE);
		File file = new File(objectDataFileName);
		// if file exist then try to open file and load data
        if(file.exists()) {
    		try {
    			
        		InputStream is = new FileInputStream(file);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                is.close();
                
                JsonReader jsonReader = Json.createReader(new StringReader(jsonTxt));
                
                try {
                	raw = jsonReader.readArray();
        		} catch (Exception e) {
        			
        			logger.severe("PersistanceManager#loadThingDescriptionFromFile: Exception during reading JSON object: " 
        						+ e.getMessage());
        			
        			return null;
        		} finally {
        			jsonReader.close();
        		}
                
    			logger.fine("Counters were loaded from file - " + file.getName() );
    			
    	    } catch (IOException i) {
    	    	
    	    	logger.warning("Counters could not be loaded from file - " + file.getName() );
    	    	i.printStackTrace();
    	        return null;
    	        
    	    } 
    		

    		// Convert to ArrayList 
			if (raw != null) { 
			   for (int i=0;i< raw.size();i++){ 
			    data.add(raw.getJsonObject(i));
			   } 
			} 

        	return data;
        }
		
        logger.info("There are no persisted counters, starting new list...");
		return data;
	}
	
	/**
	 * Delete counters file
	 */
	private void deleteCounters() {
		String objectDataFileName = String.format(countersFile, COUNTERS_PERSISTENCE_FILE);
		File file = new File(objectDataFileName);
		file.delete();
	}
	
}
