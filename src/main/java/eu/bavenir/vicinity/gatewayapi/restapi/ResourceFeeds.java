package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class ResourceFeeds extends ServerResource {
	@Get
	public String represent() {
		if (null != getAttribute("fid")){
			return getAttribute("fid");
		} else {
			return "yet another test method";
		}
		
	}
	
	
	@Post
	public void accept() {

	}
	
	
	@Delete
	public void removeAll() {
		
	}
}
