package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;



public class Api extends Application {
	
	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		// create a router Restlet that routes each call to a new instance of 
		Router router = new Router(getContext());

		// define routes
		router.attach("/objects", ResourceObjects.class);
		router.attach("/feeds", ResourceFeeds.class);
		router.attach("/sparql", ResourceSparql.class);

		return router;
	}

}
