package eu.bavenir.ogwapi.commons;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.configuration2.XMLConfiguration;

import eu.bavenir.ogwapi.commons.connectors.AgentConnector;
import eu.bavenir.ogwapi.commons.messages.NetworkMessageResponse;


/*
 * STRUCTURE:
 * - constants
 * - fields
 * - public methods
 * - private methods
 */

/**
 * This class represents an action a local infrastructure can execute. It keeps a list of {@link eu.bavenir.ogwapi.commons.Task tasks}
 * that are waiting to be executed (pending), is currently being executed (running) and a set of tasks and their return 
 * values that are done (either finished successfully or unsuccessfully).
 * 
 * An instance of this class is created for each action an object can perform according to its TD. Each request for action
 * that arrives has its corresponding {@link eu.bavenir.ogwapi.commons.Task task} created and is queued into a linked list
 * with pending state. Every second this class checks whether there is a task still running and if not, the next task in
 * the queue is being executed. Tasks that will not make it into running state in a time frame configured by 
 * {@link #CONF_PARAM_PENDINGTASKTIMEOUT CONF_PARAM_PENDINGTASKTIMEOUT} are periodically looked for and moved into finished
 * tasks set, where they are flagged as timed out. The number of tasks in the pending queue can be limited by 
 * {@link #CONF_PARAM_MAXNUMBEROFPENDINGTASKS CONF_PARAM_MAXNUMBEROFPENDINGTASKS} configuration parameter. 
 * 
 * Running task can be updated by the executing object by calling {@link #updateTask(String, String, Map) updateTask} 
 * method. During the update a new status of the task can be set and a return value can be stored. This can be also 
 * used to periodically store preliminary results, see the method documentation. 
 * 
 * After the running task is updated to finished/failed status, it is stored in the set of tasks that are done and a new
 * task is taken from the pending list. The finished / failed task is retained for the period defined by 
 * {@link #CONF_DEF_TIMETOKEEPRETURNVALUES CONF_DEF_TIMETOKEEPRETURNVALUES}.
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
 * 
 * @author sulfo
 *
 */
public class Action {

	/* === CONSTANTS === */
	
	/**
	 * This parameter sets how long (in minutes) after successful or failed 
	 * execution a task's return value should be retained. In other words, if a 
	 * task is finished or failed, its return value will be deleted from the 
	 * OGWAPI after this number of minutes. This is to prevent the return
	 * values from piling up in the device's memory.
	 * 
	 * If not set, it defaults to 1440 minutes (24 hours).
	 */
	private static final String CONF_PARAM_TIMETOKEEPRETURNVALUES = "actions.timeToKeepReturnValues";
	
	/**
	 * Default value for {@link #CONF_PARAM_TIMETOKEEPRETURNVALUES CONF_PARAM_TIMETOKEEPRETURNVALUES} configuration parameter.
	 */
	private static final int CONF_DEF_TIMETOKEEPRETURNVALUES = 1440;
	
	/**
	 * If a task is pending to be run, how long (in minutes) it should remain in the 
	 * queue before being tagged as failed by timing out. This is infrastructure
	 * specific - if a task usually takes hours to complete, this value should 
	 * be set to higher number. If it takes only a few seconds,
	 * it usually makes no sense to wait for more than an hour. Again, it 
	 * highly depends on what the action is about and integrator's common sense.  
	 * Default value is 120 minutes (2 hours).
	 */
	private static final String CONF_PARAM_PENDINGTASKTIMEOUT = "actions.pendingTaskTimeout";
	
	/**
	 * Default value for {@link #CONF_PARAM_PENDINGTASKTIMEOUT CONF_PARAM_PENDINGTASKTIMEOUT} configuration parameter.
	 */
	private static final int CONF_DEF_PENDINGTASKTIMEOUT = 120;
	
	/**
	 * Maximum number of tasks being queued in pending status, waiting to be
	 * run. This depends on number of objects that are connecting via this 
	 * gateway, and the memory size of the device it runs on. Setting a 
	 * limit prevents a malicious object to fill the memory with pending 
	 * requests. Note that is a limit per action, so if you have two
	 * actions that can be executed on your local object, 
	 * maximum number of pending tasks in memory will be twice this number.  
	 * 
	 * Default is 128.  
	 */
	private static final String CONF_PARAM_MAXNUMBEROFPENDINGTASKS = "actions.maxNumberOfPendingTasks";
	
	/**
	 * Default value for {@link #CONF_PARAM_PENDINGTASKTIMEOUT CONF_PARAM_PENDINGTASKTIMEOUT} configuration parameter.
	 */
	private static final int CONF_DEF_MAXNUMBEROFPENDINGTASKS = 128;
	
	/**
	 * Defines when should a timer start its count.
	 */
	private static final int TIMER1_START = 1000;
	
	/**
	 * Number of milliseconds in a minute.
	 */
	private static final int MINUTE = 60000;
	
	/**
	 * Number of milliseconds in a second.
	 */
	private static final int SECOND = 1000;
	
	/**
	 * JSON attribute name for task ID.
	 */
	public static final String ATTR_TASKID = "taskId";
	
	/**
	 * JSON attribute name for status.
	 */
	public static final String ATTR_STATUS = "status";
	
	/**
	 * JSON attribute name for creation time.
	 */
	public static final String ATTR_CREATIONTIME = "createdAt";
	
	/**
	 * JSON attribute name for start time.
	 */
	public static final String ATTR_STARTTIME = "startTime";
	
	/**
	 * JSON attribute name for end time.
	 */
	public static final String ATTR_ENDTIME = "endTime";
	
	/**
	 * JSON attribute name for total time.
	 */
	public static final String ATTR_TOTALTIME = "totalTime";
	
	/**
	 * JSON attribute name for return value.
	 */
	public static final String ATTR_RETURNVALUE = "returnValue";
	
	
	/* === FIELDS === */
	
	/**
	 * This is the object ID of the action owner (usually the local object, represented by its 
	 * {@link eu.bavenir.ogwapi.commons.ConnectionDescriptor ConnectionDescriptor}). 
	 * 
	 */
	private String objectId;
	
	/**
	 * The ID of the action. 
	 * 
	 */
	private String actionId;
	
	/**
	 * List of pending {@link eu.bavenir.ogwapi.commons.Task tasks}.
	 */
	private List<Task> pendingTasks;
	
	/**
	 * List of finished {@link eu.bavenir.ogwapi.commons.Task tasks} (also a place for failed tasks).
	 */
	private Set<Task> finishedTasks;
	
	/**
	 * The running {@link eu.bavenir.ogwapi.commons.Task task}.
	 */
	private Task runningTask;
	
	/**
	 * The number obtained from the {@link #CONF_PARAM_TIMETOKEEPRETURNVALUES CONF_PARAM_TIMETOKEEPRETURNVALUES} 
	 * configuration parameter.
	 */
	private long timeToKeepReturnValues;
	
	/**
	 * Number obtained from the {@link #CONF_PARAM_PENDINGTASKTIMEOUT CONF_PARAM_PENDINGTASKTIMEOUT} configuration 
	 * parameter. 
	 */
	private long pendingTaskTimeout;
	
	/**
	 * Number obtained from the {@link #CONF_PARAM_MAXNUMBEROFPENDINGTASKS CONF_PARAM_MAXNUMBEROFPENDINGTASKS} configuration 
	 * parameter. 
	 */
	private int maxNumberOfPendingTasks;
	
	/**
	 * Instance of the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}. 
	 */
	private AgentConnector connector;
	
	/**
	 * Logger of the OGWAPI.
	 */
	private Logger logger;
	
	/**
	 * Configuration of the OGWAPI. 
	 */
	private XMLConfiguration config;

	
	
	/* === PUBLIC METHODS === */
	
	/**
	 * Constructor, initialises field, loads configuration and starts timers.
	 * 
	 * @param config Configuration of the OGWAPI.
	 * @param objectId Object ID of the object that owns this instance of the action class.
	 * @param actionId Action ID.
	 * @param connector Instance of the {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}. 
	 * @param logger Logger of the OGWAPI.
	 */
	public Action(XMLConfiguration config, String objectId, String actionId, AgentConnector connector, Logger logger) {
		
		this.objectId = objectId;
		this.actionId = actionId;
		this.connector = connector;
		this.logger = logger;
		this.config = config;
		
		pendingTasks = Collections.synchronizedList(new LinkedList<Task>());
		
		finishedTasks = Collections.synchronizedSet(new HashSet<Task>());
		
		runningTask = null;
		
		// load configuration parameters
		timeToKeepReturnValues = // turn into ms
				config.getInt(CONF_PARAM_TIMETOKEEPRETURNVALUES, CONF_DEF_TIMETOKEEPRETURNVALUES) * MINUTE;
		
		logger.config("Action " + actionId + " time to keep return values set to (ms): " + timeToKeepReturnValues);
		
		pendingTaskTimeout = // turn into ms
				config.getInt(CONF_PARAM_PENDINGTASKTIMEOUT, CONF_DEF_PENDINGTASKTIMEOUT) * MINUTE;
		
		logger.config("Action " + actionId + " pending tasks timeout set to (ms): " + pendingTaskTimeout);
		
		maxNumberOfPendingTasks = config.getInt(CONF_PARAM_MAXNUMBEROFPENDINGTASKS, CONF_DEF_MAXNUMBEROFPENDINGTASKS);
		
		logger.config("Action " + actionId + " max number of pending tasks set to: " + maxNumberOfPendingTasks);
		
		// TODO if thought a little deeper, there can be just one such timer for all actions in the connection
		// descriptor... or one timer in the whole comm manager for everything (the best thing achievable) - it will 
		// save a number of threads 
		
		// schedule a timer for running tasks that are queueing
		Timer timerForTaskScheduling = new Timer();
		
		timerForTaskScheduling.schedule(new TimerTask() {
			@Override
			public void run() {
				workThroughTasks();
				purgeOutdatedReturnValues();
				purgeTimedOutPendingTasks();
			}
		}, TIMER1_START, SECOND);
		
	}
	
	
	/**
	 * Getter for object ID.
	 * @return
	 */
	public String getObjectId() {
		return objectId;
	}


	/**
	 * Getter for action ID.
	 * @return
	 */
	public String getActionId() {
		return actionId;
	}


	/**
	 * Returns the number of {@link eu.bavenir.ogwapi.commons.Task tasks} that are in a status defined by the parameter. 
	 * 
	 * Valid values are:
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_FAILED failed},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_FINISHED finished},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_PENDING pending},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_RUNNING running} (this will return either 0 or 1).
	 * 
	 * @param status The status of tasks in question.
	 * @return Number of tasks in a particular state.
	 */
	public int getNumberOfTasksInCertainStatus(byte status) {
		
		int number = 0;
		for (Task task : pendingTasks) {
			
			if (task.getTaskStatus() == status) {
				number++;
			}
		}
		
		logger.fine("Object ID " + this.objectId + " Action ID " + this.actionId + " Number of tasks in status " 
					+ status + " is "+ number + ".");
		
		return number;
	}
	
	
	/**
	 * Returns a status of a {@link eu.bavenir.ogwapi.commons.Task task} identified by the parameter. 
	 * 
	 * @param taskId ID of the {@link eu.bavenir.ogwapi.commons.Task task}.
	 * @return A status of the task. Valid return values are:
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_FAILED failed},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_FINISHED finished},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_PENDING pending},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_RUNNING running}.
	 */
	public byte getTaskStatus(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			logger.fine(this.actionId + ": Task " + taskId + " not found.");
			
			// if the task is not there, return unknown status
			return Task.TASKSTATUS_UNKNOWN;
		}
		
		byte status = task.getTaskStatus();
		
		logger.fine(this.actionId + ": Task " + taskId + " status is " + status);
		
		return status;
	}
	
	
	/**
	 * Same as the {@link #getTaskStatus(String) getTaskStatus, but returns string instead of byte.
	 * 
	 * @param taskId ID of the {@link eu.bavenir.ogwapi.commons.Task task}.
	 * @return A status of the task. Valid return values are:
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_FAILED failed},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_FINISHED finished},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_PENDING pending},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_RUNNING running}.
	 */
	public String getTaskStatusString(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			logger.fine(this.actionId + ": Task " + taskId + " not found.");
			
			// if the task is not there, return unknown status
			return Task.TASKSTATUS_STRING_UNKNOWN;
		}
		
		String statusString = task.getTaskStatusString();
		
		logger.fine(this.actionId + ": Task " + taskId + " status string: " + statusString);
		
		return statusString;
	}
	
	
	/**
	 * Retrieves a return value of a {@link eu.bavenir.ogwapi.commons.Task task}. The return value gets uploaded by using the 
	 * {@link #updateTask(String, String, Map) updateTask} method.
	 * 
	 * @param taskId ID of the task.
	 * @return Return value previously uploaded by executing object.
	 */
	public String getTaskReturnValue(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			logger.fine(this.actionId + ": Task " + taskId + " not found.");
			
			// if the task is not there, return null
			return null;
		}
		
		String returnValue = task.getReturnValue();
		
		logger.finest(this.actionId + ": Task " + taskId + " return value: " + returnValue);
		
		return returnValue;
	}
	
	
	
	/**
	 * Creates a new {@link eu.bavenir.ogwapi.commons.Task task} in a pending state.
	 * 
	 * @param sourceOid Object ID of the requesting object.
	 * @param body Any JSON that is necessary to forward to the object once it is time to start execution.
	 * @param parameters Any parameters necessary to be supplied with the body.
	 * @return A task ID.
	 */
	public String createNewTask(String sourceOid, String body, Map<String, String> parameters) {
		
		
		if (pendingTasks.size() >= maxNumberOfPendingTasks) {
			
			logger.finest(this.actionId + ": Too many tasks in the queue.");
			
			return null;
		}
		
		// start a task in a default pending status
		Task task = new Task(config, logger, connector, sourceOid, this.objectId, actionId, body, parameters);
		
		pendingTasks.add(task);
		
		logger.finest(this.actionId + ": Task created.");
		
		return task.getTaskId();
	}
	
	
	/**
	 * Method for updating any {@link eu.bavenir.ogwapi.commons.Task task} that is {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_RUNNING running}
	 * state. This is called by the object that is currently executing the task. If {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_RUNNING running}
	 * state is set a new state, the task representation will continue in execution and a return value is overwritten by 
	 * the new value (if provided). This way preliminary results can be stored. 
	 * 
	 * When status different from running is provided, the task will stop the timers and is moved into set of finished/failed
	 * tasks, waiting for the return value to be retrieved.
	 * 
	 * @param taskStatus New task status. Following are the accepted values: {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_RUNNING running},
	 * {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_FINISHED finished} and {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_FAILED failed}.
	 * @param returnValue Either final or preliminary return value. This will be returned to requesting object when it asks for status.
	 * @param parameters Anything that needs to be sent along with the return value. 
	 * @return True if the update was successful. If there is no such task or it can't be put into desired state, returns false. 
	 */
	public boolean updateTask(String taskStatus, String returnValue, Map<String, String> parameters) {
		
		// only running task can be updated
		if (runningTask == null) {
			
			logger.finest(this.actionId + ": No running task.");
			return false;
		}
		
		if (!runningTask.updateRunningTask(taskStatus, returnValue)) {
			
			logger.finest(this.actionId + ": The runnning task can't be put into desired state.");
			return false;
		}
		
		// if the new desired state is failed or finished, the task is to be moved into the set of finished tasks
		if (!taskStatus.equals(Task.TASKSTATUS_STRING_RUNNING)) {
			
			finishedTasks.add(runningTask);
			
			// clear the place for the next task to be run
			runningTask = null;
			
			logger.finest(this.actionId + ": New task status is either failed or finished, moving task to the set"
					+ "of finished tasks.");
			
		}
		
		return true;
		
	}
	
	
	/**
	 * This method cancels {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_RUNNING running} or {@link eu.bavenir.ogwapi.commons.Task#TASKSTATUS_STRING_PENDING pending}
	 * {@link eu.bavenir.ogwapi.commons.Task task}.
	 * 
	 * @param taskId The ID of the task.
	 * @param body If there is a body that needs to be sent along with request.
	 * @param parameters If there are parameters needed to be sent along with the body.
	 * @return Response message with values from {@link eu.bavenir.ogwapi.commons.connectors.AgentConnector AgentConnector}, or
	 * or null if there is no such task / the task status does not permit attempts to cancel it. 
	 */
	public NetworkMessageResponse cancelTask(String taskId, String body, Map<String, String> parameters) {
		
		// this will ensure that the task is neither finished or cancelled (or non existing for that matter)
		Task task = searchForTask(taskId, false);
		
		if (task == null) {
			
			logger.finest(this.actionId + ": Task " + taskId + " not found.");
			
			return null;
		}
		
		NetworkMessageResponse response = task.cancel(body, parameters); 
	
		if (response == null) {

			logger.finest(this.actionId + ": Task " + taskId + " is in a state that forbids cancelling.");
			
			// the task is in a state that forbids cancelling 
			return null;
		}
		
		
		if (response.isError()) {

			logger.finest(this.actionId + ": Something happened when the Agent was asked to abort the task " 
								+ taskId + " not found.");
			// something happened when the agent was asked to abort the action
			return response;
		}
		
		// now move it to finished task pool
		if (runningTask.equals(task)) {
			runningTask = null;
			
		} else {
			pendingTasks.remove(task);
		}
		finishedTasks.add(task);
		
		return response;
	}
	
	
	/**
	 * This method retrieves the status of a {@link eu.bavenir.ogwapi.commons.Task task} in a form of JSON.
	 * 
	 * @param taskId ID of a task to be polled for status.
	 * @return Status, timer values, return values, ... or null if there is no such task. 
	 */
	public JsonObject createTaskStatusJson(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			logger.finest(this.actionId + ": Task " + taskId + " not found.");
			
			return null;
		}
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		// create the factory
		JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
		JsonObjectBuilder mainBuilder = jsonBuilderFactory.createObjectBuilder();
		
		mainBuilder.add(ATTR_TASKID, taskId);
		mainBuilder.add(ATTR_STATUS, task.getTaskStatusString());
		
		Date taskCreationTime = new Date(task.getCreationTime());
		mainBuilder.add(ATTR_CREATIONTIME, df.format(taskCreationTime).toString());
		
		if (task.getStartTime() > 0) {
			Date taskStartTime = new Date(task.getStartTime());
			mainBuilder.add(ATTR_STARTTIME, df.format(taskStartTime).toString());
		}
		
		if (task.getEndTime() > 0) {
			Date taskEndTime = new Date(task.getEndTime());
			mainBuilder.add(ATTR_ENDTIME, df.format(taskEndTime).toString());
		}
		
		mainBuilder.add(ATTR_TOTALTIME, task.getRunningTime());
		
		
	
		if (task.getReturnValue() == null){
			mainBuilder.addNull(ATTR_RETURNVALUE);
		} else {
			mainBuilder.add(ATTR_RETURNVALUE, task.getReturnValue());
		}
	 
		return mainBuilder.build();
		
	}
	

	
	
	/* === PRIVATE METHODS === */
	
	/**
	 * This method is called by a timer every second, checking whether or not there is {@link eu.bavenir.ogwapi.commons.Task task}
	 * running and if there is none, it takes the first one from the pending queue. 
	 */
	private void workThroughTasks() {
		
		printStatusOfAllTasks();
		
		// check whether or not a task is already running and if not, check if there are some tasks pending
		if (runningTask == null && !pendingTasks.isEmpty()) {
			
			logger.fine("AID " + this.actionId + ": There is no task running, yet there are tasks pending. Taking the next task.");
			
			// take one pending task from the queue			
			int i = 0;
			do {
				if (pendingTasks.get(i).getTaskStatus() == Task.TASKSTATUS_PENDING) {
					runningTask = pendingTasks.remove(i);
				}
				
				i++;
			} while (runningTask == null && i < pendingTasks.size());
			
			if (runningTask != null) {
				
				
				// if it failed to start, put it into a set of failed tasks
				if (!runningTask.start()) {
					
					logger.warning("AID " + this.actionId + ": Task " + runningTask.getTaskId() + " failed to start.");
					
					finishedTasks.add(runningTask);
					
					runningTask = null;
				}
			}
			
		}
	}
	
	
	/**
	 * This method checks the pool of finished / failed {@link eu.bavenir.ogwapi.commons.Task tasks} and removes those after expiration.
	 */
	private void purgeOutdatedReturnValues() {
		
		for (Task task : finishedTasks) {	
			if ((System.currentTimeMillis() - task.getEndTime()) > timeToKeepReturnValues) {
				
				logger.finest(this.actionId + ": Finished/failed task " + task.getTaskId() 
														+ " was removed from the pool of finished tasks.");
				finishedTasks.remove(task);
			}
		}
	}
	
	
	/**
	 * This method checks the queue of pending {@link eu.bavenir.ogwapi.commons.Task tasks} and removes those after expiration.
	 */
	private void purgeTimedOutPendingTasks() {
		
		for (Task task : pendingTasks) {
			if ((System.currentTimeMillis() - task.getCreationTime()) > pendingTaskTimeout) {
				
				logger.finest(this.actionId + ": Pending task " + task.getTaskId() 
												+ " was removed from the pool of pending tasks.");
				
				pendingTasks.remove(task);
			}
		}
	}
	
	
	/**
	 * Searches for a {@link eu.bavenir.ogwapi.commons.Task task} identified by the parameter. A boolean value can be 
	 * added to search also among the failed / finished tasks (there are occasions when there is no need for it).
	 *  
	 * @param taskId ID of the task to be sought for.
	 * @param searchAlsoAmongFinishedTasks Set this to true if there is a valid assumption that the task can be among 
	 * finished / failed. 
	 * @return Found task or null.
	 */
	private Task searchForTask(String taskId, boolean searchAlsoAmongFinishedTasks) {
		
		// is it the running task?
		if (runningTask != null && runningTask.getTaskId().equals(taskId)) {
			
			logger.finest(this.actionId + ": Searching for task " + taskId + ", found it as running.");
			return runningTask;
		}
		
		// or among pending tasks?
		for (Task task : pendingTasks) {	
			if (task.getTaskId().equals(taskId)) {
				logger.finest(this.actionId + ": Searching for task " + taskId + ", found it as pending.");
				return task;
			}
		}
		
		if (searchAlsoAmongFinishedTasks) {
			// or among finished tasks?
			for (Task task : finishedTasks) {
				if (task.getTaskId().equals(taskId)) {
					logger.finest(this.actionId + ": Searching for task " + taskId + ", found it as finished.");
					return task;
				}
			}
		}
		
		logger.finest(this.actionId + ": Searching for task " + taskId + ", but was out of luck.");
		// it is gone
		return null;
	}

	
	/**
	 * Debugging method for convenient printout of all tasks for this particular action. In order to be useful,
	 * the debugging level has to be set to finest.
	 */
	private void printStatusOfAllTasks() {
		
		String logMessage = new String();
		
		logMessage = "Tasks of object " + objectId + " / action " + actionId + "\n";
		
		if (runningTask == null) {
			logMessage += "Running task: null\n";
		} else {
			logMessage += "Running task: " + runningTask.getTaskId() + " status " + runningTask.getTaskStatus() + " " 
						+ runningTask.getTaskStatusString() + "\n";
		}
		
		logMessage += "Pending tasks:\n";
		for (Task task : pendingTasks) {	
			logMessage += "ID " + task.getTaskId() + " status " + task.getTaskStatus() + " " + task.getTaskStatusString() + "\n";

		}
		
		logMessage += "Finished tasks:\n";
		for (Task task : finishedTasks) {
			logMessage += "ID " + task.getTaskId() + " status " + task.getTaskStatus() + " " + task.getTaskStatusString() + "\n";
		}
		
		logMessage += "Done";
		
		logger.finest(logMessage);
	}
	
}
