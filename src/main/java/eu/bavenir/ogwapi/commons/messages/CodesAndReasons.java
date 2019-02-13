package eu.bavenir.ogwapi.commons.messages;

/**
 * Utility class with constants for status codes and status code reasons. Most of them are identical with standard
 * HTTP codes and reasons (@see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">wiki article</a>).
 * 
 * 
 * @author sulfo
 *
 */
public class CodesAndReasons {

	/**
	 * Integer value for "OK" code.
	 */
	public static final int CODE_200_OK = 200;
	
	/**
	 * String for "OK" code reason.
	 */
	public static final String REASON_200_OK = "OK. ";
	
	/**
	 * Integer value for "Created" code.
	 */
	public static final int CODE_201_CREATED = 201;
	
	/**
	 * String for "Created" code reason.
	 */
	public static final String REASON_201_CREATED = "Created. ";
	
	/**
	 * Integer value for "Accepted" code.
	 */
	public static final int CODE_202_ACCEPTED = 202;
	
	/**
	 * String for "Accepted" code reason.
	 */
	public static final String REASON_202_ACCEPTED = "Accepted. ";
	
	/**
	 * Integer value for "Bad request" code.
	 */
	public static final int CODE_400_BADREQUEST = 400;
	
	/**
	 * String for "Bad request" code reason.
	 */
	public static final String REASON_400_BADREQUEST = "Bad request. ";
	
	/**
	 * Integer value for "Unauthorised" code.
	 */
	public static final int CODE_401_UNAUTHORIZED = 401;

	/**
	 * String for "Unauthorised" code reason.
	 */
	public static final String REASON_401_UNAUTHORIZED = "Unauthorized. ";
	
	/**
	 * Integer value for "Not found" code.
	 */
	public static final int CODE_404_NOTFOUND = 404;
	
	/**
	 * String for "Not found" code reason.
	 */
	public static final String REASON_404_NOTFOUND = "Not found. ";
	
	/**
	 * Integer value for "Request timeout" code.
	 */
	public static final int CODE_408_REQUESTTIMEOUT = 408;
	
	/**
	 * String for "Request timeout" code reason.
	 */
	public static final String REASON_408_REQUESTTIMEOUT = "Request timeout. ";
	
	/**
	 * Integer value for "Service unavailable" code.
	 */
	public static final int CODE_503_SERVICEUNAVAILABLE = 503;
	
	/**
	 * String for "Service unavailable" code reason.
	 */
	public static final String REASON_503_SERVICENAVAILABLE = "Service unavailable. ";
	
}
