package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

public class ResourceObjectsActions extends ServerResource {
	@Get
	public String represent() {

		return "ok";
	}
	
	
	@Put
	public void store() {

	}
}
