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


/**
 * A task is a particular instance of an {@link eu.bavenir.ogwapi.commons.Action action} execution, a job. It belongs to 
 * the executing object, was ordered by requesting object, it can be in certain states (only one state at a time) has a 
 * record of how long the execution takes and there is a return value associated with it. For more information about how
 * OGWAPI handles the incoming requests for job execution please have look at {@link eu.bavenir.ogwapi.commons.Action action}.
 * 
 * After a task is created a UUID style task ID is automatically generated and serves as a reference for the ordering
 * object to poll for the task's status.
 * 
 * The valid states for each task are following:
 * 
 *     pending --> running --> finished
 *           \        |
 *            \       v
 *             \-> failed
 * 
 * 
 * To easier grasp the relationship between actions and tasks, consider the following example: 
 * Imagine a door (object) that is capable of opening (action) and closing (action). When Bob steps up to the closed door 
 * he invokes an action (opening) and this particular execution (Bob's opening of the door) is referenced to as a task.
 * This task has its own ID and can be polled for status at any time, to see whether or not it is already done. 
 * When Alice also wants to interact with door's actions, but her tasks need to line up behind Bob's, pending their 
 * executions until the former tasks are finished. 
 * 
 * One thing that needs to be realized in the mind of an integrator is, that the particular object only needs to be able
 * to respond correctly to start and stop a task and must be able to call an update end point. The rest is done by the 
 * OGWAPI.  
 * 
 * @author sulfo
 *
 */
public class Task {

	/* === CONSTANTS === */
	
	/**
	 * Byte value for pending status.
	 */
	public static final byte TASKSTATUS_PENDING = 0x00;
	
	/**
	 * Byte value for running status.
	 */
	public static final byte TASKSTATUS_RUNNING = 0x01;
	
	/**
	 * Byte value for failed status.
	 */
	public static final byte TASKSTATUS_FAILED = 0x02;
	
	/**
	 * Byte value for finished status.
	 */
	public static final byte TASKSTATUS_FINISHED = 0x03;
	
	/**
	 * Byte value for unknown status. This status should not appear during normal operation and indicates an error.
	 */
	public static final byte TASKSTATUS_UNKNOWN = 0x04;
	
	/**
	 * String value for pending status.
	 */
	public static final String TASKSTATUS_STRING_PENDING = "pending";
	
	/**
	 * String value for running status.
	 */
	public static final String TASKSTATUS_STRING_RUNNING = "running";
	
	/**
	 * String value for failed status.
	 */
	public static final String TASKSTATUS_STRING_FAILED = "failed";
	
	/**
	 * String value for finished status.
	 */
	public static final String TASKSTATUS_STRING_FINISHED = "finished";
	
	/**
	 * String value for unknown status. This can happen when the task polled for status does not exist - it is not a 
	 * valid status of a task, more likely it indicates an error.
	 */
	public static final String TASKSTATUS_STRING_UNKNOWN = "unknown";
	
	/**
	 * When a task gets canceled, this is its return value.
	 */
	public static final String CANCELED_RETURN_VALUE = "canceled";
	
	
	
	
	/* === FIELDS === */
	
	/**
	 * The Unix time of the moment when the task was created.
	 */
	private long creationTime;
	
	/**
	 * The Unix time of the moment when the task was started, i.e. when it left the pending queue and was given running state.
	 */
	private long startTime;
	
	/**
	 * The Unix time of the moment when the task finished its execution - with either success or not.
	 */
	private long endTime;
	
	/**
	 * The total time the task spent in the running state.
	 */
	private long runningTime;
	
	/**
	 * ID of the object that ordered the job.
	 */
	private String sourceOid;
	
	/**
	 * ID of the owner.
	 */
	private String destinationOid;
	
	/**
	 * ID of the task - gets generated on creation.
	 */
	private String taskId;
	
	/**
	 * The ID of the {@link eu.bavenir.ogwapi.commons.Action action}.
	 */
	private String actionId;
	
	/**
	 * Stored return value. 
	 */
	private String returnValue;
	
	/**
	 * Status of this task.
	 */
	private byte taskStatus;
	
	/**
	 * Body of the request that was used for creation of this task.
	 */
	private String body;
	
	/**
	 * The {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector} is used to issue the start/stop 
	 * orders to the physical object.
	 */
	private AgentConnector connector;
	
	/**
	 * Parameters used when making a start/stop call through the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 */
	private Map<String, String> parameters;
	
	/**
	 * Configuration of the OGWAPI.
	 */
	private XMLConfiguration config;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initialises fields.  
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 * @param connector The {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector} instance to be used.
	 * @param sourceOid ID of the object that ordered the job.
	 * @param destinationOid ID of the owner.
	 * @param actionId The ID of the {@link eu.bavenir.ogwapi.commons.Action action}.
	 * @param body Body of the request that was used for creation of this task.
	 * @param parameters Parameters used when making a start/stop call through the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 */
	public Task(XMLConfiguration config, Logger logger, AgentConnector connector, String sourceOid, String destinationOid, 
			String actionId, String body, Map<String, String> parameters) {
		
		initialize(config, logger, connector, sourceOid, destinationOid, actionId, body, parameters);
		
		// default status after creating an instance of task
		taskStatus = TASKSTATUS_PENDING;
	}	
	
	
	/**
	 * Starts the execution of this task. 
	 * 
	 * @return True if the response from the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}
	 * was positive, or false if either the task is not in a state that permits starting, or the Agent returned an error. 
	 */
	public boolean start() {
		
		// only pending task can be started
		if (taskStatus != TASKSTATUS_PENDING) {
			return false;
		}
		
		NetworkMessageResponse response = connector.startObjectAction(sourceOid, destinationOid, actionId, body, 
							parameters);
		
		startTime = System.currentTimeMillis();
		
		if (response == null || (response.getResponseCode() / 200) != 1) {
			
			logger.warning("Task " + taskId + " could not be executed - AgentConnector returned error or null repsponse.");
			
			endTime = System.currentTimeMillis();
			
			taskStatus = TASKSTATUS_FAILED;
			
			return false;
		}
		
		
		logger.fine("Task " + taskId + " was send to Agent for execution.");
		
		taskStatus = TASKSTATUS_RUNNING;
		
		return true;
	}
	
	
	/**
	 * Method for updating a running task. Can change a status or a return value of the task.
	 * Only running task can be updated and it is only possible to either:
	 *     remain in the {@link #TASKSTATUS_STRING_RUNNING running} state
	 *     switch to {@link #TASKSTATUS_STRING_FINISHED finished} state
	 *     switch to {@link #TASKSTATUS_STRING_FAILED failed} state
	 *     
	 * @param taskStatus New status of the task - this method uses string value.
	 * @param returnValue New return value. This will overwrite the old one if there is any.
	 * @return True if the task was updated successfully, false otherwise (this happens when invalid state was 
	 * provided.
	 */
	public boolean updateRunningTask(String taskStatus, String returnValue) {
		
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
	
	
	/**
	 * Method for updating a running task. Can change a status or a return value of the task.
	 * Only running task can be updated and it is only possible to either:
	 *     remain in the {@link #TASKSTATUS_RUNNING running} state
	 *     switch to {@link #TASKSTATUS_FINISHED finished} state
	 *     switch to {@link #TASKSTATUS_FAILED failed} state
	 *     
	 * @param taskStatus New status of the task - this method uses byte value.
	 * @param returnValue New return value. This will overwrite the old one if there is any.
	 * @return True if the task was updated successfully, false otherwise (this happens when invalid state was 
	 * provided.
	 */
	public boolean updateRunningTask(byte taskStatus, String returnValue) {
		
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
	

	/**
	 * Cancels a running or pending task.
	 * 
	 * @param body Body to be sent to the object when cancelling.
	 * @param parameters Parameters to be sent along with the body.
	 * @return Response from the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector} or 
	 * null if the task is not in a state that permits cancelling. 
	 */
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
			
			// stop the clock - this has to be done in order not to get purged in the next check for outdated return values
			endTime = System.currentTimeMillis();
			startTime = System.currentTimeMillis();
			
			taskStatus = TASKSTATUS_FINISHED;
			returnValue = CANCELED_RETURN_VALUE;
			
			response = new NetworkMessageResponse(config, logger, 
					false, 
					CodesAndReasons.CODE_200_OK,
					CodesAndReasons.REASON_200_OK + "Canceled pending task",
					"application/json",
					null);
		}
		
		// tasks that are in failed, finished or unknown state will always return null
		
		return response;
	}
	
	
	/**
	 * Returns the time in milliseconds that the task spent in the running state.
	 *   
	 * @return Total time spent in the running state.
	 */
	public long getRunningTime() {
		
		if (taskStatus == TASKSTATUS_RUNNING) {
			return runningTime + (System.currentTimeMillis() - startTime);
		} else {
			return runningTime; 
		}
	}
	
	
	/**
	 * Retrieves a Unix time of the moment when the task was created.
	 * 
	 * @return Unix time of task creation in milliseconds. 
	 */
	public long getCreationTime() {
		return creationTime;
	}
	
	
	/**
	 * Retrieves the Unix time of the moment when the task was moved into failed/finished state.
	 * 
	 * @return Unix time of the task's change to finished / pending state in milliseconds.
	 */
	public long getEndTime() {
		return endTime;
	}

	
	/**
	 * Retrieves the Unix time of the moment when the task was put into running time.
	 * 
	 * @return Unix time of the task's change to running state in milliseconds.
	 */
	public long getStartTime() {
		return startTime;
	}
	
	
	/**
	 * Returns the ID of the object that ordered the job.
	 * 
	 * @return ID of the object that ordered the job.
	 */
	public String getRequestingObjectId() {
		return sourceOid;
	}
	
	
	/**
	 * Getter for the object ID of the owner. 
	 * 
	 * @return ID of the object that owns this action-task pair.
	 */
	public String getObjectId() {
		return destinationOid;
	}


	/**
	 * Getter for the task ID.
	 * 
	 * @return ID of the task.
	 */
	public String getTaskId() {
		return taskId;
	}


	/**
	 * Getter for the return value.
	 * 
	 * @return The last return value or null if there was no update of the return value yet. 
	 */
	public String getReturnValue() {
		return returnValue;
	}


	/**
	 * Getter for the byte representation of task's status. The valid values are:
	 * {@link #TASKSTATUS_FAILED failed}, {@link #TASKSTATUS_FINISHED finished}, {@link #TASKSTATUS_PENDING pending},
	 * {@link #TASKSTATUS_RUNNING running}.     
	 * 
	 * @return Byte representation of the task's status.
	 */
	public byte getTaskStatus() {
		return taskStatus;
	}
	
	
	/**
	 * Getter for the string representation of task's status. The valid values are:
	 * {@link #TASKSTATUS_STRING_FAILED failed}, {@link #TASKSTATUS_STRING_FINISHED finished}, {@link #TASKSTATUS_STRING_PENDING pending},
	 * {@link #TASKSTATUS_STRING_RUNNING running}.     
	 * 
	 * @return String representation of the task's status.
	 */
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

	/**
	 * Initialisation method, giving baseline values for fields and generates the task ID.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param logger Logger of the OGWAPI.
	 * @param connector The {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector} instance to be used.
	 * @param sourceOid ID of the object that ordered the job.
	 * @param destinationOid ID of the owner.
	 * @param actionId The ID of the {@link eu.bavenir.ogwapi.commons.Action action}.
	 * @param body Body of the request that was used for creation of this task.
	 * @param parameters Parameters used when making a start/stop call through the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}.
	 */
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
	
	
	/**
	 * Verifies whether or not the given task is valid.
	 * 
	 * @param taskStatus A status to be verified.
	 * @return True if the state is valid, false otherwise.
	 */
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
	
	
	/**
	 * Translates the status string to byte value.
	 * 
	 * @param statusString String representation of the status to be translated.
	 * @return Byte representation of the status. If the status is not valid, returns {@link #TASKSTATUS_UNKNOWN unknown}. 
	 */
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
