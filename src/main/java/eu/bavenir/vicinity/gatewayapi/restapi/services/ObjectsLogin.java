package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

// TODO
public class ObjectsLogin extends ServerResource{

	
	@Get
	public String represent() {
		
		return "Login successfull.";
	}
}
