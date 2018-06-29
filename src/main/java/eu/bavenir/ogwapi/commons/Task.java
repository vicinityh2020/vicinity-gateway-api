package eu.bavenir.ogwapi.commons;

import java.util.UUID;

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
	
	public static final byte TASKSTATUS_UNKNOWN = 0x04;
	
	public static final String TASKSTATUS_STRING_PENDING = "pending";
	
	public static final String TASKSTATUS_STRING_RUNNING = "running";
	
	public static final String TASKSTATUS_STRING_FAILED = "failed";
	
	public static final String TASKSTATUS_STRING_FINISHED = "finished";
	
	// this can happen when the task polled for status does not exist - it is not a valid status of a task, more likely
	// it indicates an error.
	public static final String TASKSTATUS_STRING_UNKNOWN = "unknown";
	
	
	private static final String CANCELED_RETURN_VALUE = "canceled";
	
	
	
	/* === FIELDS === */
	
	private long creationTime;
	
	private long startTime;
	
	private long endTime;
	
	private long runningTime;
	
	private String objectID;
	
	private String taskID;
	
	private String returnValue;
	
	private byte taskStatus;
	
	private String body;
	
	
	
	
	/* === PUBLIC METHODS === */
	
	public Task(String objectID, String body) {
		initialize(objectID, body);
		
		// default status after creating an instance of task
		setTaskStatus(TASKSTATUS_PENDING);
	}
	
	
	
	public Task(String objectID, String body, byte taskStatus) {
		initialize(objectID, body);
		
		// if wrong status was given, assign the default
		if (!setTaskStatus(taskStatus)) {
			setTaskStatus(TASKSTATUS_PENDING);
		}
	}
	
	
	
	public boolean setTaskStatus(byte taskStatus) {
		
		// check whether the status is in the set of allowed states
		if (taskStatus == TASKSTATUS_FAILED 
				|| taskStatus == TASKSTATUS_FINISHED
				|| taskStatus == TASKSTATUS_PENDING
				|| taskStatus == TASKSTATUS_RUNNING) {
			
			this.taskStatus = taskStatus;
			
		} else {
			return false;
		}
		
		return true;
	}
	
	
	
	public void start() {
		
		startTime = System.currentTimeMillis();
		
		taskStatus = TASKSTATUS_RUNNING;
	}
	
	

	public void stop(String returnValue, boolean finishedWithError) {
		
		if (finishedWithError) {
			taskStatus = TASKSTATUS_FAILED;
		} else {
			taskStatus = TASKSTATUS_FINISHED;
		}
		
		endTime = System.currentTimeMillis();
		
		runningTime = runningTime + (endTime - startTime);
		
		this.returnValue = returnValue;
	}
	
	
	public boolean cancel() {
		
		
		
		taskStatus = TASKSTATUS_FINISHED;
		
		returnValue = CANCELED_RETURN_VALUE;
		
		return true;
	}
	
	
	public long getRunningTime() {
		
		if (taskStatus == TASKSTATUS_RUNNING) {
			return runningTime + (System.currentTimeMillis() - startTime);
		} else {
			return runningTime; 
		}
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
	public long getEndTime() {
		return endTime;
	}

	
	public long getStartTime() {
		return startTime;
	}
	
	
	public String getObjectID() {
		return objectID;
	}


	
	public String getTaskID() {
		return taskID;
	}


	public String getReturnValue() {
		return returnValue;
	}


	
	public void setReturnValue(String returnValue) {
		this.returnValue = returnValue;
	}


	
	public byte getTaskStatus() {
		return taskStatus;
	}
	
	
	public String getTaskStatusString() {
		
		switch (taskStatus) {
		
		case(TASKSTATUS_FAILED):
			return TASKSTATUS_STRING_FAILED;
		
		case(TASKSTATUS_FINISHED):
			return TASKSTATUS_STRING_FINISHED;
		
		case(TASKSTATUS_PENDING):
			return TASKSTATUS_STRING_PENDING;
		
		case(TASKSTATUS_RUNNING):
			return TASKSTATUS_STRING_RUNNING;
		
		default:
			return null;
		}
	}
	
	
	/* === PRIVATE METHODS === */
	



	


	private void initialize(String objectID, String body) {
		startTime = 0;
		endTime = 0;
		runningTime = 0;
		
		creationTime = System.currentTimeMillis();
		
		taskID = UUID.randomUUID().toString();		
		returnValue = null;
		
		this.body = body;
		this.objectID = objectID;
	}
}
