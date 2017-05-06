package eu.bavenir.vicinity.gatewayapi.restapi;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;


/**
 * RESTLET application that serves incoming calls for the Gateway API. It routes the requests to their respective
 * {@link org.restlet.resource.ServerResource Resources}.
 * 
 * @see <a href="https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/">Gateway API</a>
 *   
 * @author sulfo
 *
 */
public class Api extends Application {
	
	/**
	 * Creates a root RESTLET that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		// create a router Restlet that routes each call to a new instance of 
		Router router = new Router(getContext());
		
	
		// define routes
		// see https://app.swaggerhub.com/apis/fserena/vicinity_gateway_api/
		
		// registry
		router.attach("/adapters", ResourceAdapters.class);
		router.attach("/adapters/{adid}", ResourceAdapters.class);
		router.attach("/adapters/{adid}/objects", ResourceAdaptersObjects.class);
		router.attach("/adapters/{adid}/subscriptions", ResourceAdaptersSubscriptions.class);
		
		router.attach("/objects", ResourceObjects.class);
		router.attach("/objects/{oid}", ResourceObjects.class);
		router.attach("/objects/{oid}/subscriptions", ResourceObjectsSubscriptions.class);
		
		router.attach("/subscriptions", ResourceSubscriptions.class);
		router.attach("/subscriptions/{sid}", ResourceSubscriptions.class);
		
		// discovery
		router.attach("/feeds", ResourceFeeds.class);
		router.attach("/feeds/{fid}", ResourceFeeds.class);
		
		// consumption
		router.attach("/objects/{oid}/properties/{pid}", ResourceObjectsProperties.class);
		router.attach("/objects/{oid}/actions/{aid}", ResourceObjectsActions.class);
		router.attach("/objects/{oid}/actions/{aid}/tasks/{tid}", ResourceObjectsActionsTasks.class);
		
		// query
		router.attach("/sparql", ResourceSparql.class);
		
		return router;
	}

}
