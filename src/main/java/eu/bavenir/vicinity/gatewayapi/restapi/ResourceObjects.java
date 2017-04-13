package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;



public class ResourceObjects extends ServerResource {
	
	/**
	 * Returns all available IoT objects if 'oid' is set.
	 * 
	 * @return All UUIDs of the available IoT objects in a JSON.
	 */
	@Get
	public String represent() {
		if (null != getAttribute("oid")){
			return getAttribute("oid");
		} else {
			return "yet another test method";
		}
		
	}
	
	
	@Post
	public void accept() {
		if (null != getAttribute("oid")){
			
		} else {
			
		}
	}
	
	
	@Put
	public void store() {
		if (null != getAttribute("oid")){
			
		} else {
			
		}
	}
	
	@Delete
	public void removeAll() {
		
	}
}
