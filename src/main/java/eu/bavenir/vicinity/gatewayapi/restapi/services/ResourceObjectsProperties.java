package eu.bavenir.vicinity.gatewayapi.restapi.services;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceObjectsProperties extends ServerResource {

	private static final String ATTR_OID = "oid";
	
	private static final String ATTR_PID = "pid";
	
	private static final String ATTR_VALUE = "value";
	
	private static final String ATTR_TIMESTAMP = "timestamp";
	
	@Get
	public String represent() {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		
		if (attrOid != null && attrPid != null){
			return getObjectProperty(attrOid, attrPid);
		} else {			
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
	
	@Put("json")
	public void store(Representation entity) {
		String attrOid = getAttribute(ATTR_OID);
		String attrPid = getAttribute(ATTR_PID);
		
		if (attrOid == null || attrPid == null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Object or property does not exist under given identifier.");
		}
		
		
		if (!entity.getMediaType().equals(MediaType.APPLICATION_JSON)){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		// get the json
		String propertyJsonString = null;
		try {
			propertyJsonString = entity.getText();
		} catch (IOException e) {
			// TODO to logs
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		updateProperty(propertyJsonString);
	}
	
	
	private void updateProperty(String jsonString){
		JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
		JsonObject jsonRequest = jsonReader.readObject();
		
		if (jsonRequest.containsKey(ATTR_VALUE)){
			//TODO do something
		} else {
			jsonReader.close();
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid property description");
		}
		
		jsonReader.close();
	}
	
	
	private String getObjectProperty(String attrOid, String attrPid){
		
		if (attrOid.equals("0729a580-2240-11e6-9eb5-0002a5d5c51b") && attrPid.equals("temp1")){
			JsonObject json = Json.createObjectBuilder()
					.add(ATTR_VALUE, 24.5)
					.add(ATTR_TIMESTAMP, "2017-05-06T15:09:30.204Z")
					.build();
			
			return json.toString();	
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					"Given identifier does not exist.");
		}
		
	}
	
}
