package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ResourceObjectsLogin extends ServerResource {

	@Get
	public String represent() {

		String testBefore = (String) this.getContext().getAttributes().get("TEST_STRING");
		
		String testAfter = testBefore + " and again";
		
		this.getContext().getAttributes().put("TEST_STRING", testAfter);
		
		return testBefore;
	}
}
