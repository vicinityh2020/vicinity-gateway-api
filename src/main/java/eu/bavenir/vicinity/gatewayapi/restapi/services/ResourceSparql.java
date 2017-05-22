package eu.bavenir.vicinity.gatewayapi.restapi.services;

import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ResourceSparql extends ServerResource {
	
	private static final String QUERY_SPARQLQUERY = "query";
	
	@Get
	public String represent() {

		String querySparqlQuery = getQueryValue(QUERY_SPARQLQUERY);
		
		if (querySparqlQuery != null){
			return "SPARQL results";
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					"Invalid query.");
		}
	}
	
	@Post()
	public void accept() {
		//final Form form = new Form(entity);
		
		
		//String type = form.getFirstValue(ATTR_TYPE);
		
		
		//System.out.println(form.toString());
		
		/*
		if (null != getAttribute(ATTR_OID)){
			
		} else {
			
		}*/
	}
	
}
