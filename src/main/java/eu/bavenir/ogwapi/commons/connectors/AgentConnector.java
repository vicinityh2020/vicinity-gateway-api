package eu.bavenir.ogwapi.commons.connectors;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;

/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * AgentConnector is the component of OGWAPI that represents an interface between the message oriented world of the OGWAPI 
 * and a protocol of your local infrastructure. This abstract class provides you with a set of methods that you need to 
 * override in your own implementation of AgentConnector. All of them will get executed when a respective request will arrive
 * from some external object. 
 * 
 * It is recommended to create a separate package for your new AgentConnector in eu.bavenir.ogwapi.commons.connectors.{protocol} and
 * put there classes that you need to support the connector.  
 * 
 * In contrast with {@link eu.bavenir.ogwapi.commons.engines.CommunicationEngine CommunicationEngine} there is only single 
 * instance of AgentConnector running for all the objects logged into the OGWAPI. This is a design decision taken early
 * during the VICINITY project and any Agent implementation needs to make the final routing of incoming requests. Also  
 * keep in mind that your implementation needs therefore to be thread safe!
 * 
 * Your AgentConnector implementation can serve multiple objects that are logged into your local instance of OGWAPI.
 * An AgentConnector implementation should always return 
 * {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse NetworkMessageResponse}. See its definition to 
 * get more information about how it works, however for quick understanding here are all the methods you need to get started:
 * 
 *   1. Create a new {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse NetworkMessageResponse}.
 *   2. Use {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse#setError(boolean) setError} to indicate whether 
 *      an error happened during execution.
 *   3. Set a response code with {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse#setResponseCode(int) setResponseCode}
 *      and a response code reason with {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse#setResponseCodeReason(String) setResponseCodeReason}.
 *      Consult the {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons} and Error propagation chapter
 *      in the OGWAPI documentation to get a clue on values to be set.
 *   4. Set the content type of the response with {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse#setContentType(String) setContentType}. 
 *      Use of HTTP compliant content types is encouraged, but using any other string should not cause a crash. 
 *   5. Finally set response body with {@link eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse#setResponseBody(String) setResponseBody}
 *      and return it. 
 *   
 * It is highly recommended to read Error propagation chapter in the OGWAPI documentation to see how the error gets propagated
 * across the network. Also a peek into {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons} can 
 * give you some insight about valid error codes and the reasons for them. In the reference implementation of the  
 * {@link eu.bavenir.ogwapi.commons.connectors.http.RestAgentConnector RestAgentConnector} you'll find out that the code
 * and reason that gets transported to the other side of the network will usually be taken directly from the REST client
 * that communicates with your local infrastructure. As it was the first implementation of the OGWAPI ever (as of 2018)
 * it is likely that the content of the {@link eu.bavenir.ogwapi.commons.messages.CodesAndReasons CodesAndReasons}
 * will remain similarly HTTP oriented in the near future. Setting the values other way than directly grabbing them from 
 * the REST client is demonstrated in the {@link eu.bavenir.ogwapi.commons.connectors.http.RestAgentConnector#performDummyOperation performDummyOperation}
 * of the {@link eu.bavenir.ogwapi.commons.connectors.http.RestAgentConnector RestAgentConnector}.
 *   
 * @author sulfo
 *
 */
public abstract class AgentConnector {
	
	/* === CONSTANTS === */
	
	/* === FIELDS === */

	/**
	 * Configuration of the OGWAPI.
	 */
	protected XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	protected Logger logger;

	
	
	/* === PUBLIC METHODS === */

	/**
	 * Constructor for field initialisation. Your implementation needs to call the super(config, logger)
	 * method of this class as the first thing it ever does.  
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 */
	public AgentConnector(XMLConfiguration config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}

	
	/**
	 * This method gets executed when an event belonging to local object ID is received and needs to be forwarded to 
	 * its respective device.
	 * 
	 * @param sourceOid The object ID that published the event.
	 * @param destinationOid Subscriber object ID.
	 * @param eventId ID of the event channel.
	 * @param body Body of the event.
	 * @param parameters Any parameters that were inserted into the event.
	 * @return Response. See the main documentation for this class on how to create and fill the object.
	 */
	public abstract NetworkMessageResponse forwardEventToObject(String sourceOid, String destinationOid, String eventId, 
			String body, Map<String, String> parameters);
	
	
	/**
	 * This method is executed when a request to retrieve a property value is received.
	 * 
	 * @param sourceOid The object ID that issued the request.
	 * @param destinationOid Destination object ID.
	 * @param propertyId ID of the property.
	 * @param body Body of the request.
	 * @param parameters Any parameters that were inserted into the request.
	 * @return Response. See the main documentation for this class on how to create and fill the object.
	 */
	public abstract NetworkMessageResponse getObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters);
	
	
	/**
	 * This method is executed when a request to set a property value is received.
	 * 
	 * @param sourceOid The object ID that issued the request.
	 * @param destinationOid Destination object ID.
	 * @param propertyId ID of the property.
	 * @param body Body of the request.
	 * @param parameters Any parameters that were inserted into the request.
	 * @return Response. See the main documentation for this class on how to create and fill the object.
	 */
	public abstract NetworkMessageResponse setObjectProperty(String sourceOid, String destinationOid, String propertyId, 
			String body, Map<String, String> parameters);
	
	
	/**
	 * This method is executed when a request to start an action is received.
	 * 
	 * @param sourceOid The object ID that issued the request.
	 * @param destinationOid Destination object ID.
	 * @param actionId ID of the action.
	 * @param body Body of the request.
	 * @param parameters Any parameters that were inserted into the request.
	 * @return Response. See the main documentation for this class on how to create and fill the object.
	 */
	public abstract NetworkMessageResponse startObjectAction(String sourceOid, String destinationOid, String actionId, 
			String body, Map<String, String> parameters);
	
	
	/**
	 * This method is executed when a request to stop a running action is received.
	 * 
	 * @param sourceOid The object ID that issued the request.
	 * @param destinationOid Destination object ID.
	 * @param actionId ID of the action.
	 * @param body Body of the request.
	 * @param parameters Any parameters that were inserted into the request.
	 * @return Response. See the main documentation for this class on how to create and fill the object.
	 */
	public abstract NetworkMessageResponse stopObjectAction(String sourceOid, String destinationOid, String actionId, 
			String body, Map<String, String> parameters);
	
		
	
	/* === PRIVATE METHODS === */
		

}
