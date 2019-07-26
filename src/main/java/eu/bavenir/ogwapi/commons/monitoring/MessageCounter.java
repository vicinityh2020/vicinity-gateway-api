package eu.bavenir.ogwapi.commons.monitoring;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.IOUtils;

import com.auth0.jwt.algorithms.Algorithm;
import java.security.interfaces.ECKey;

import eu.bavenir.ogwapi.commons.messages.NetworkMessageRequest;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;

/**
 * This class serves for monitoring count of messages
 */
public class MessageCounter {

	
	/* === CONSTANTS === */
	
	
	/**
	 * Name of the configuration parameter for the number of records that are sent to NM
	 */
	private static final String CONFIG_PARAM_MAXRECORDS = "messageCounter.countOfRecords";
	
	/**
	 * Default value of {@link #CONFIG_PARAM_MAXRECORDS} configuration parameter. 
	 * This value is taken into account when no suitable value is found in the configuration file.
	 */
	private static final int CONFIG_DEF_MAXRECORDS = 1000;

	/**
	 * Record type - not possible to send
	 */
	public static final int RECORDTYPE_INT_NOT_POSSIBLE_TO_SEND = 1;
	
	/**
	 * Record type - not possible to send
	 */
	public static final String RECORDTYPE_STRING_NOT_POSSIBLE_TO_SEND = "Request message was not possible to send";
	
	/**
	 * Record type - no response message received
	 */
	public static final int RECORDTYPE_INT_NO_RESPONSE_MESSAGE_RECEIVED = 2;

	/**
	 * Record type - no response message received
	 */
	public static final String RECORDTYPE_STRING_NO_RESPONSE_MESSAGE_RECEIVED = "No response message received";

	/**
	 * Record type - OK 
	 */
	public static final int RECORDTYPE_INT_OK = 3;
	
	/**
	 * Record type - OK 
	 */
	public static final String RECORDTYPE_STRING_OK = "OK";
	
	
	public static final String PRIVATE_KEY_FILE_PATH = "keystore/private.key.pem";
	public static final String PUBLIC_KEY_FILE_PATH = "keystore/public.key.pem";
	
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
	 * List of messages
	 */
	private List<JsonObject> records;
	
	/**
	 * Number of records that are sent to NM
	 */
	private int countOfSendingRecords;
	
	/**
	 * Current number of saved messages
	 */
	private int count;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Connector
	 */
	public MessageCounter(XMLConfiguration config, Logger logger) {
		
		this.config = config;
		this.logger = logger;
		
		records = new ArrayList<JsonObject>();
		
		count = 0;
		countOfSendingRecords = config.getInt(CONFIG_PARAM_MAXRECORDS, CONFIG_DEF_MAXRECORDS);
	}
	
	/**
	 * add message
	 */
	public void addMessage(NetworkMessageRequest request, NetworkMessageResponse response, int recordType) {
		
		// record JsonObject
		JsonObjectBuilder recordObjectBuilder = Json.createObjectBuilder();
		
		if (recordType == RECORDTYPE_INT_NOT_POSSIBLE_TO_SEND) {
			
			// message status
			recordObjectBuilder.add("messageStatus", RECORDTYPE_STRING_NOT_POSSIBLE_TO_SEND);
			
		} else if (recordType == RECORDTYPE_INT_NO_RESPONSE_MESSAGE_RECEIVED) {
			
			// message status
			recordObjectBuilder.add("messageStatus", RECORDTYPE_STRING_NO_RESPONSE_MESSAGE_RECEIVED);
			
		} else if (recordType == RECORDTYPE_INT_OK) {
			
			// message status
			recordObjectBuilder.add("messageStatus", RECORDTYPE_STRING_OK);
		}
		
		request.buildMessageString();
		recordObjectBuilder.add("request", request.getJsonRepresentation());
		
		if (response != null) {
			response.buildMessageString();
			recordObjectBuilder.add("response", response.getJsonRepresentation());
		} else {
			recordObjectBuilder.add("response", "null");
		}
		
		records.add(recordObjectBuilder.build());
		
		//for testing
		sendToNeighborhoodManager();
		
		
		if (++count > countOfSendingRecords) {
			
			sendToNeighborhoodManager();
			
			records.clear();
			count = 0;
		}
	}
	
	/**
	 * send JsonObject to the Neighborhood Manager
	 */
	private void sendToNeighborhoodManager() {
		
		JsonObject json = createJsonFromRecords();
		
		// send this json to NM
		//TODO
	}
	
	/**
	 * create JsonObject from records
	 */
	private JsonObject createJsonFromRecords() {
		
		// JsonArray outgoing message
		JsonArrayBuilder recordsArrayBuilder = Json.createArrayBuilder();
		// fill this array
		records.stream().forEach(x -> recordsArrayBuilder.add(x));
		
		// main JsonObject
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		mainObjectBuilder.add("timeStamp", System.currentTimeMillis());
		mainObjectBuilder.add("records", recordsArrayBuilder);
		
		return mainObjectBuilder.build();
	}
	
}
