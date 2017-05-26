package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class Kokot extends ServerResource {

	@Get
	public String represent(){
		return "pica kokot";
		
	}
}
