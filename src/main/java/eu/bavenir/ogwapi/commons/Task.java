package eu.bavenir.ogwapi.commons;

import java.util.UUID;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;

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
	
	// TODO make it so that the running time will be updated every second and it will be possible to return comprehensive status
	private long runningTime;
	
	private String requestingObjectID;
	
	private String objectID;
	
	private String taskID;
	
	private String actionID;
	
	private String returnValue;
	
	private byte taskStatus;
	
	private String body;
	
	private AgentConnector connector;
	
	
	
	
	/* === PUBLIC METHODS === */
	
	public Task(String objectID, String requestingObjectID, String actionID, String body, AgentConnector connector) {
		initialize(objectID, requestingObjectID, actionID, connector, body);
		
		// default status after creating an instance of task
		taskStatus = TASKSTATUS_PENDING;
	}
	
	
	
	public Task(String objectID, String requestingObjectID, String actionID, String body, byte taskStatus, AgentConnector connector) {
		initialize(objectID, requestingObjectID, actionID, connector, body);
		
		// if wrong status was given, assign the default
		if (!setTaskStatus(taskStatus)) {
			setTaskStatus(TASKSTATUS_PENDING);
		}
	}
	
	
	
	public NetworkMessageResponse start() {
		
		// only pending task can be started
		if (taskStatus != TASKSTATUS_PENDING) {
			return null;
		}
		
		// TODO this is baaad, need to return status message
		NetworkMessageResponse response = connector.startObjectAction(objectID, actionID, body);
		
		if (response == null) {
			return null;
		}
		
		if ((response.getResponseCode() / 200) != 1){
			
			return null;
		}
		
		startTime = System.currentTimeMillis();
		
		taskStatus = TASKSTATUS_RUNNING;
		
		return response;
	}
	
	
	
	public boolean updateRunningTask(String taskStatus, String returnValue) {
		
		// only running task can be updated and it is only possible to either:
		// 		remain in the running state
		//		switch to finished state
		//		switch to failed state
		
		if (taskStatus.equals(TASKSTATUS_STRING_RUNNING)) {
			// this is more or less just progress update
			this.returnValue = returnValue;
			return true;
		}
		
		if (taskStatus.equals(TASKSTATUS_STRING_FINISHED) || taskStatus.equals(TASKSTATUS_STRING_FAILED)) {
			
			// stop the clock
			endTime = System.currentTimeMillis();
			runningTime = runningTime + (endTime - startTime);
			
			// change the status
			this.taskStatus = translateStringStatusToByte(taskStatus);
			this.returnValue = returnValue;	
			
			return true;
		}
		
		// all other alternatives are banned
		return false;
	}
	
	

	public NetworkMessageResponse cancel() {
		
		NetworkMessageResponse response = null;
		
		if (taskStatus == TASKSTATUS_FAILED || taskStatus == TASKSTATUS_FINISHED) {
			return null;
		}
		
		
		// only pending and running task can be canceled
		if (taskStatus == TASKSTATUS_RUNNING) {
			
			// TODO this is baaad, need to return status message - and control the return value
			response = connector.cancelTask(objectID, actionID);
			
			if (response == null) {
				return null;
			}
			
			if ((response.getResponseCode() / 200) != 1){
				return null;
			}
			
			// stop the clock
			endTime = System.currentTimeMillis();
			runningTime = runningTime + (endTime - startTime);	
		} 
		
		taskStatus = TASKSTATUS_FINISHED;
		returnValue = CANCELED_RETURN_VALUE;
		
		return response;
		
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
	
	public String getRequestingObjectID() {
		return requestingObjectID;
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


	private void initialize(String objectID, String requestingObjectID, String actionID, AgentConnector connector, String body) {
		startTime = 0;
		endTime = 0;
		runningTime = 0;
		
		creationTime = System.currentTimeMillis();
		
		taskID = UUID.randomUUID().toString();		
		returnValue = null;
		
		this.objectID = objectID;
		this.body = body;
		this.requestingObjectID = objectID;
		this.actionID = actionID;
		this.connector = connector;
		
		// TODO delete after test
		System.out.println("BODY: " + body);
	}
	
	
	
	private boolean setTaskStatus(byte taskStatus) {
		
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
	
	
	private byte translateStringStatusToByte(String statusString) {
		switch (statusString) {
		
		case(TASKSTATUS_STRING_FAILED):
			return TASKSTATUS_FAILED;
		
		case(TASKSTATUS_STRING_FINISHED):
			return TASKSTATUS_FINISHED;
		
		case(TASKSTATUS_STRING_PENDING):
			return TASKSTATUS_PENDING;
		
		case(TASKSTATUS_STRING_RUNNING):
			return TASKSTATUS_RUNNING;
		
		default:
			return TASKSTATUS_UNKNOWN;
		}
	}
}
