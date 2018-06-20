package eu.bavenir.ogwapi.commons;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */


// TODO documentation
public class Task {

	/* === CONSTANTS === */
	
	public static final byte TASKSTATUS_PENDING = 0x00;
	
	public static final byte TASKSTATUS_RUNNING = 0x01;
	
	public static final byte TASKSTATUS_FAILED = 0x02;
	
	public static final byte TASKSTATUS_FINISHED = 0x03;
	
	
	/* === FIELDS === */
	
	private String objectID;
	
	private String returnValue;
	
	private byte taskStatus;
	
	
	
	
	/* === PUBLIC METHODS === */
	
	
	
	/* === PRIVATE METHODS === */
	
}
