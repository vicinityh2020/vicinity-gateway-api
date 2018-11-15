package eu.bavenir.ogwapi.commons;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.messages.CodesAndReasons;
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
	
	public static final String CANCELED_RETURN_VALUE = "canceled";
	
	
	
	
	/* === FIELDS === */
	
	private long creationTime;
	
	private long startTime;
	
	private long endTime;
	
	private long runningTime;
	
	private String sourceOid;
	
	private String destinationOid;
	
	private String taskId;
	
	private String actionId;
	
	private String returnValue;
	
	private byte taskStatus;
	
	private String body;
	
	private AgentConnector connector;
	
	private Map<String, String> parameters;
	
	private XMLConfiguration config;
	
	private Logger logger;
	
	
	
	
	/* === PUBLIC METHODS === */
	
	public Task(XMLConfiguration config, Logger logger, AgentConnector connector, String sourceOid, String destinationOid, 
			String actionId, String body, Map<String, String> parameters) {
		
		initialize(config, logger, connector, sourceOid, destinationOid, actionId, body, parameters);
		
		// default status after creating an instance of task
		taskStatus = TASKSTATUS_PENDING;
	}	
	
	
	public NetworkMessageResponse start() {
		
		// only pending task can be started
		if (taskStatus != TASKSTATUS_PENDING) {
			return null;
		}
		
		NetworkMessageResponse response = connector.startObjectAction(sourceOid, destinationOid, actionId, body, 
							parameters);
		
		
		if (response == null) {
			
			return null;
		}
		
		if ((response.getResponseCode() / 200) == 1){
			
			startTime = System.currentTimeMillis();
			
			taskStatus = TASKSTATUS_RUNNING;
			
		}
		
		return response;
	}
	
	
	
	public boolean updateRunningTask(String taskStatus, String returnValue) {
		
		// only running task can be updated and it is only possible to either:
		// 		remain in the running state
		//		switch to finished state
		//		switch to failed state
		
		byte taskStatusByte = translateStringStatusToByte(taskStatus);
		
		if (!validateTaskStatus(taskStatusByte)) {
			return false;
		}
		
		if (taskStatusByte == TASKSTATUS_RUNNING) {
			// this is more or less just progress update
			this.returnValue = returnValue;
			return true;
		}
		
		if (taskStatusByte == TASKSTATUS_FINISHED || taskStatusByte == TASKSTATUS_FAILED) {
			
			// stop the clock
			endTime = System.currentTimeMillis();
			runningTime = runningTime + (endTime - startTime);
			
			// change the status
			this.taskStatus = taskStatusByte;
			this.returnValue = returnValue;	
			
			return true;
		}
		
		// all other alternatives are banned
		return false;
	}
	
	
	public boolean updateRunningTask(byte taskStatus, String returnValue) {
		
		// only running task can be updated and it is only possible to either:
		// 		remain in the running state
		//		switch to finished state
		//		switch to failed state
		
		if (!validateTaskStatus(taskStatus)) {
			return false;
		}
		
		if (taskStatus == TASKSTATUS_RUNNING) {
			// this is more or less just progress update
			this.returnValue = returnValue;
			return true;
		}
		
		if (taskStatus == TASKSTATUS_FINISHED || taskStatus == TASKSTATUS_FAILED) {
			
			// stop the clock
			endTime = System.currentTimeMillis();
			runningTime = runningTime + (endTime - startTime);
			
			// change the status
			this.taskStatus = taskStatus;
			this.returnValue = returnValue;	
			
			return true;
		}
		
		// all other alternatives are banned
		return false;
	}
	

	public NetworkMessageResponse cancel(String body, Map<String, String> parameters) {
		
		NetworkMessageResponse response = null;
		
		// only pending and running task can be aborted
		if (taskStatus == TASKSTATUS_RUNNING) {
			
			response = connector.stopObjectAction(sourceOid, destinationOid, actionId, body, parameters);
			
			if ((response.getResponseCode() / 200) == 1){
				
				// stop the clock
				endTime = System.currentTimeMillis();
				runningTime = runningTime + (endTime - startTime);
				
				taskStatus = TASKSTATUS_FINISHED;
				
				if (response.getResponseBody() != null) {
					returnValue = response.getResponseBody();
				} else {
					returnValue = CANCELED_RETURN_VALUE;
				}
			}
			
			return response;
		} 
		
		if (taskStatus == TASKSTATUS_PENDING) {
			
			taskStatus = TASKSTATUS_FINISHED;
			returnValue = CANCELED_RETURN_VALUE;
			
			response = new NetworkMessageResponse(config, logger, 
					false, 
					CodesAndReasons.CODE_200_OK,
					null,
					CodesAndReasons.REASON_200_OK + "Canceled pending task", 
					null);
		}
		
		// tasks that are in failed, finished or unknown state will always return null
		
		return response;
	}
	
	
	public long getRunningTime() {
		
		// TODO this is inaccurate
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
	
	public String getRequestingObjectId() {
		return sourceOid;
	}
	
	public String getObjectID() {
		return destinationOid;
	}


	
	public String getTaskId() {
		return taskId;
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


	private void initialize(XMLConfiguration config, Logger logger, AgentConnector connector, String sourceOid, 
			String destinationOid, String actionId, String body, Map<String, String> parameters) {
		startTime = 0;
		endTime = 0;
		runningTime = 0;
		
		creationTime = System.currentTimeMillis();
		
		taskId = UUID.randomUUID().toString();		
		returnValue = null;
		
		this.config = config;
		this.logger = logger;
		this.destinationOid = destinationOid;
		this.body = body;
		this.sourceOid = sourceOid;
		this.actionId = actionId;
		this.connector = connector;
		this.parameters = parameters;
		
	}
	
	
	
	private boolean validateTaskStatus(byte taskStatus) {
		
		// check whether the status is in the set of allowed states
		if (taskStatus == TASKSTATUS_FAILED 
				|| taskStatus == TASKSTATUS_FINISHED
				|| taskStatus == TASKSTATUS_PENDING
				|| taskStatus == TASKSTATUS_RUNNING) {
			return true;
		}
		
		return false;
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
