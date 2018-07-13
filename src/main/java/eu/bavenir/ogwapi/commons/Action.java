package eu.bavenir.ogwapi.commons;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
	
	
	/* === FIELDS === */
	
	/**
	 * This is the object ID of the action owner (usually the local object, represented by its 
	 * {@link ConnectionDescriptor ConnectionDescriptor}). 
	 */
	private String objectID;
	
	/**
	 * The ID of the action. 
	 */
	private String actionID;
	
	
	private List<Task> pendingTasks;
	
	private Set<Task> finishedTasks;
	
	private Task runningTask;
	
	private long timeToKeepReturnValues;
	
	private long pendingTaskTimeout;
	
	private int maxNumberOfPendingTasks;
	
	private AgentConnector connector;
	
	
	
	/* === PUBLIC METHODS === */
	
	public Action(XMLConfiguration config, String objectID, String actionID, AgentConnector connector) {
		
		this.objectID = objectID;
		this.actionID = actionID;
		this.connector = connector;
		
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
	
	
	
	public String getObjectID() {
		return objectID;
	}



	public String getActionID() {
		return actionID;
	}


	
	public int getNumberOfTasksInCertainStatus(byte status) {
		
		int number = 0;
		for (Task task : pendingTasks) {
			
			if (task.getTaskStatus() == status) {
				number++;
			}
		}
		
		// TODO delete after test
		System.out.println("Number of tasks is " + number);
		
		return number;
	}
	
	
	
	public byte getTaskStatus(String taskID) {
		
		Task task = searchForTask(taskID, true);
		
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
	
	
	
	public String getTaskStatusString(String taskID) {
		
		Task task = searchForTask(taskID, true);
		
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
	
	
	
	public String getTaskReturnValue(String taskID) {
		
		Task task = searchForTask(taskID, true);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			// if the task is not there, return null
			return null;
		}
		
		return task.getReturnValue();
	}
	
	
	
	// start
	public String createNewTask(String requestingObjectID, String body) {
		
		if (pendingTasks.size() >= maxNumberOfPendingTasks) {
			
			// TODO delete after test
			System.out.println("Too many pending tasks.");
			
			return null;
		}
		
		// start a task in a default pending status
		Task task = new Task(this.objectID, requestingObjectID, actionID, body, connector);
		
		pendingTasks.add(task);
		
		// TODO delete after test
		System.out.println("Task created.");
		
		return task.getTaskID();
	}
	
	
	// update 
	public boolean updateTask(String taskStatus, String returnValue) {
		
		// only running task can be updated
		if (runningTask == null) {
			// TODO delete after test
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
			if (!finishedTasks.add(runningTask)) {
				// TODO delete after test
				System.out.println("Unknown error here.");
				return false;
			} 
			
			// clear the place for the next task to be run
			runningTask = null;
			
		}
		
		// TODO delete after test
		System.out.println("Task updated.");
		
		return true;
		
	}
	
	
	// cancel
	public NetworkMessageResponse cancelTask(String taskID) {
		
		Task task = searchForTask(taskID, false);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Canceling finished/failed task is not possible.");
			
			return null;
		}
		
		// we can't cancel finished or failed job
		if (task.getTaskStatus() == Task.TASKSTATUS_FAILED || task.getTaskStatus() == Task.TASKSTATUS_FINISHED) {
			
			// TODO delete after test
			System.out.println("Canceling finished/failed task is not possible.");
			return null;
		}
		
		
		// TODO delete after test
		System.out.println("Canceling task.");
		
		NetworkMessageResponse response = task.cancel();
		
		if (response == null ) { 
			// TODO delete after test
			System.out.println("Something went wrong during the task cancelling.");
			
			return null;
		} 
		
		// now move it to finished task pool
		finishedTasks.add(task);
		if (runningTask.equals(task)) {
			// TODO delete after test
			System.out.println("Running task set to null.");
			
			runningTask = null;
			
		} else {
			// TODO delete after test
			System.out.println("Removed from pending tasks.");
			pendingTasks.remove(task);
		}
		
		return response;
	}
	
	
	

	
	
	/* === PRIVATE METHODS === */
	
	private void workThroughTasks() {
		
		// check whether or not a task is already running and if not, check if there are some tasks pending
		if (runningTask == null && !pendingTasks.isEmpty()) {
			
			// take one non paused task from the queue
			for (int i = 0; i < pendingTasks.size(); i++) {
				
				if (pendingTasks.get(i).getTaskStatus() == Task.TASKSTATUS_PENDING) {
					runningTask = pendingTasks.remove(i);
				}
			}
			
			// TODO remove after testing
			System.out.println("There is no task running, yet there are tasks pending. Taking the next task: " 
						+ runningTask.getTaskID());
			
			runningTask.start();
		}
	}
	
	
	private void purgeOutdatedReturnValues() {
		
		for (Task task : finishedTasks) {	
			if ((System.currentTimeMillis() - task.getEndTime()) > timeToKeepReturnValues) {
				
				//TODO delete after test
				System.out.println("Finished/failed task " + task.getTaskID() + " was removed from the pool of finished tasks.");
				finishedTasks.remove(task);
			}
		}
	}
	
	
	private void purgeTimedOutPendingTasks() {
		
		for (Task task : pendingTasks) {
			if ((System.currentTimeMillis() - task.getCreationTime()) > pendingTaskTimeout) {
				
				//TODO delete after test
				System.out.println("Pending task " + task.getTaskID() + " was removed from the pool of pending tasks.");
				
				pendingTasks.remove(task);
			}
		}
	}
	
	
	private Task searchForTask(String taskID, boolean searchAlsoAmongFinishedTasks) {
		
		// is it the running task?
		if (runningTask != null && runningTask.getTaskID().equals(taskID)) {
			return runningTask;
		}
		
		// or among pending tasks?
		for (Task task : pendingTasks) {	
			if (task.getTaskID().equals(taskID)) {
				return task;
			}
		}
		
		if (searchAlsoAmongFinishedTasks) {
			// or among finished tasks?
			for (Task task : finishedTasks) {
				if (task.getTaskID().equals(taskID)) {
					return task;
				}
			}
		}
		
		// it is gone
		return null;
	}
	
}
