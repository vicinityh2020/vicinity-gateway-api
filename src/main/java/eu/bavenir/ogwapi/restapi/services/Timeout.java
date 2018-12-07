package eu.bavenir.ogwapi.restapi.services;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class Timeout extends ServerResource {
	@Get
	public Representation represent() {
			
		JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
		
		mainObjectBuilder.add("kokot", "pica");
		
		try {
			Thread.sleep(60000*2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return new JsonRepresentation(mainObjectBuilder.build().toString());
	}
}
