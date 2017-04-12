package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ResourceObjects extends ServerResource {
	@Get
	public String represent() {
		return "The gateway status is displayed";
	}
	
}
