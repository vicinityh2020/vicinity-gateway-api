package eu.bavenir.ogwapi.commons;

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

// TODO documentation
public class Action {

	/* === CONSTANTS === */
	
	
	private static final String CONF_PARAM_TIMETOKEEPRETURNVALUES = "actions.timeToKeepReturnValues";
	
	private static final int CONF_DEF_TIMETOKEEPRETURNVALUES = 1440;
	
	private static final String CONF_PARAM_PENDINGTASKTIMEOUT = "actions.pendingTaskTimeout";
	
	private static final int CONF_DEF_PENDINGTASKTIMEOUT = 120;
	
	private static final String CONF_PARAM_MAXNUMBEROFPENDINGTASKS = "actions.maxNumberOfPendingTasks";
	
	private static final int CONF_DEF_MAXNUMBEROFPENDINGTASKS = 128;
	
	private static final int TIMER1_START = 1000;
	
	private static final int TIMER2_START = 30000;
	
	private static final int TIMER3_START = 60000;
	
	private static final int MINUTE = 60000;
	
	private static final int SECOND = 1000;
	
	public static final String ATTR_TASKID = "taskId";
	
	public static final String ATTR_STATUS = "status";
	
	public static final String ATTR_CREATIONTIME = "createdAt";
	
	public static final String ATTR_STARTTIME = "startTime";
	
	public static final String ATTR_ENDTIME = "endTime";
	
	public static final String ATTR_TOTALTIME = "totalTime";
	
	public static final String ATTR_RETURNVALUE = "returnValue";
	
	/* === FIELDS === */
	
	/**
	 * This is the object ID of the action owner (usually the local object, represented by its 
	 * {@link ConnectionDescriptor ConnectionDescriptor}). 
	 */
	private String objectId;
	
	/**
	 * The ID of the action. 
	 */
	private String actionId;
	
	
	private List<Task> pendingTasks;
	
	private Set<Task> finishedTasks;
	
	private Task runningTask;
	
	private long timeToKeepReturnValues;
	
	private long pendingTaskTimeout;
	
	private int maxNumberOfPendingTasks;
	
	private AgentConnector connector;
	
	private Logger logger;
	
	private XMLConfiguration config;

	
	
	
	/* === PUBLIC METHODS === */
	
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
		
		pendingTaskTimeout = // turn into ms
				config.getInt(CONF_PARAM_PENDINGTASKTIMEOUT, CONF_DEF_PENDINGTASKTIMEOUT) * MINUTE;
		
		maxNumberOfPendingTasks = config.getInt(CONF_PARAM_MAXNUMBEROFPENDINGTASKS, CONF_DEF_MAXNUMBEROFPENDINGTASKS);
		
		
		// schedule a timer for running tasks that are queueing
		Timer timerForTaskScheduling = new Timer();
		
		timerForTaskScheduling.schedule(new TimerTask() {
			@Override
			public void run() {
				workThroughTasks();
			}
		}, TIMER1_START, SECOND);
		
		
		// schedule a timer for keeping the return values of tasks
		Timer timerForReturnValues = new Timer();
		
		timerForReturnValues.schedule(new TimerTask() {
			@Override
			public void run() {
				purgeOutdatedReturnValues();
			}
		}, TIMER2_START, MINUTE);
		
		
		// schedule a timer for timing out the pending tasks
		Timer timerForPendingTasks = new Timer();
		
		timerForPendingTasks.schedule(new TimerTask() {
			@Override
			public void run() {
				purgeTimedOutPendingTasks();
				
			}
		}, TIMER3_START, MINUTE);
		
	}
	
	
	
	public String getObjectId() {
		return objectId;
	}



	public String getActionId() {
		return actionId;
	}


	
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
	
	
	public byte getTaskStatus(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			// if the task is not there, return unknown status
			return Task.TASKSTATUS_UNKNOWN;
		}
		
		// TODO delete after test
		System.out.println("Task status is " + task.getTaskStatus());
		
		return task.getTaskStatus();
	}
	
	
	
	public String getTaskStatusString(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			// if the task is not there, return unknown status
			return Task.TASKSTATUS_STRING_UNKNOWN;
		}
		
		// TODO delete after test
		System.out.println("Task status is " + task.getTaskStatusString());
		
		return task.getTaskStatusString();
	}
	
	
	
	public String getTaskReturnValue(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			// if the task is not there, return null
			return null;
		}
		
		return task.getReturnValue();
	}
	
	
	
	// start
	public String createNewTask(String sourceOid, String body, Map<String, String> parameters) {
		
		if (pendingTasks.size() >= maxNumberOfPendingTasks) {
			
			// TODO delete after test
			System.out.println("Too many pending tasks.");
			
			return null;
		}
		
		// start a task in a default pending status
		Task task = new Task(config, logger, connector, sourceOid, this.objectId, actionId, body, parameters);
		
		pendingTasks.add(task);
		
		// TODO delete after test
		System.out.println("Task created.");
		
		return task.getTaskId();
	}
	
	
	// update 
	public boolean updateTask(String taskStatus, String returnValue, Map<String, String> parameters) {
		
		// only running task can be updated
		if (runningTask == null) {
			// TODO delete after test and use the params
			System.out.println("There is no running task.");
			
			return false;
		}
		
		if (!runningTask.updateRunningTask(taskStatus, returnValue)) {
			
			// TODO delete after test
			System.out.println("The runnning task can't be put into desired state.");
			return false;
		}
		
		// if the new desired state is failed or finished, the task is to be moved into the set of finished tasks
		if (!taskStatus.equals(Task.TASKSTATUS_STRING_RUNNING)) {
			
			finishedTasks.add(runningTask);
			
			// clear the place for the next task to be run
			runningTask = null;
			
		}
		
		return true;
		
	}
	
	
	// cancel
	public NetworkMessageResponse cancelTask(String taskId, String body, Map<String, String> parameters) {
		
		// this will ensure that the task is neither finished or cancelled (or non existing for that matter)
		Task task = searchForTask(taskId, false);
		
		if (task == null) {
			
			return null;
		}
		
		NetworkMessageResponse response = task.cancel(body, parameters); 
	
		if (response == null) {

			// the task is in a state that forbids cancelling 
			return null;
		}
		
		
		if (response.isError()) {

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
	
	
	public JsonObject createTaskStatusJson(String taskId) {
		
		Task task = searchForTask(taskId, true);
		
		if (task == null) {
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
		
		if (task.getRunningTime() > 0) {
			Date taskStartTime = new Date(task.getStartTime());
			mainBuilder.add(ATTR_STARTTIME, df.format(taskStartTime).toString());
			
			if (task.getEndTime() > 0) {
				Date taskEndTime = new Date(task.getEndTime());
				mainBuilder.add(ATTR_ENDTIME, df.format(taskEndTime).toString());
			}
			
			mainBuilder.add(ATTR_TOTALTIME, task.getRunningTime());
		}
		
		
	
		if (task.getReturnValue() == null){
			mainBuilder.addNull(ATTR_RETURNVALUE);
		} else {
			mainBuilder.add(ATTR_RETURNVALUE, task.getReturnValue());
		}
	
		return mainBuilder.build();
		
	}
	

	
	
	/* === PRIVATE METHODS === */
	
	private void workThroughTasks() {
		
		// check whether or not a task is already running and if not, check if there are some tasks pending
		if (runningTask == null && !pendingTasks.isEmpty()) {
			
			// take one non pending task from the queue
			for (int i = 0; i < pendingTasks.size(); i++) {
				
				if (pendingTasks.get(i).getTaskStatus() == Task.TASKSTATUS_PENDING) {
					runningTask = pendingTasks.remove(i);
				}
			}
			
			// TODO put to logger
			System.out.println("There is no task running, yet there are tasks pending. Taking the next task: " 
						+ runningTask.getTaskId());
			
			runningTask.start();
		}
	}
	
	
	private void purgeOutdatedReturnValues() {
		
		for (Task task : finishedTasks) {	
			if ((System.currentTimeMillis() - task.getEndTime()) > timeToKeepReturnValues) {
				
				//TODO delete after test
				System.out.println("Finished/failed task " + task.getTaskId() + " was removed from the pool of finished tasks.");
				finishedTasks.remove(task);
			}
		}
	}
	
	
	private void purgeTimedOutPendingTasks() {
		
		for (Task task : pendingTasks) {
			if ((System.currentTimeMillis() - task.getCreationTime()) > pendingTaskTimeout) {
				
				//TODO delete after test
				System.out.println("Pending task " + task.getTaskId() + " was removed from the pool of pending tasks.");
				
				pendingTasks.remove(task);
			}
		}
	}
	
	
	private Task searchForTask(String taskId, boolean searchAlsoAmongFinishedTasks) {
		
		// is it the running task?
		if (runningTask != null && runningTask.getTaskId().equals(taskId)) {
			return runningTask;
		}
		
		// or among pending tasks?
		for (Task task : pendingTasks) {	
			if (task.getTaskId().equals(taskId)) {
				return task;
			}
		}
		
		if (searchAlsoAmongFinishedTasks) {
			// or among finished tasks?
			for (Task task : finishedTasks) {
				if (task.getTaskId().equals(taskId)) {
					return task;
				}
			}
		}
		
		// it is gone
		return null;
	}
	
}
