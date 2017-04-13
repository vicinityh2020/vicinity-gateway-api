package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class ResourceObjectsSubscriptions extends ServerResource {

	@Get
	public String represent() {

		return "ok";
	}
	
	
	@Post
	public void accept() {

	}
	
	@Delete
	public void removeAll() {
		
	}
}
