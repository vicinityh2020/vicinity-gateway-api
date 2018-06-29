package eu.bavenir.ogwapi.commons;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration2.XMLConfiguration;


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
	
	
	
	/* === PUBLIC METHODS === */
	
	public Action(XMLConfiguration config, String objectID, String actionID) {
		
		this.objectID = objectID;
		this.actionID = actionID;
		
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
				
				// TODO delete after test
				System.out.println("Timer1: scheduling tasks.");
				workThroughTasks();
				
			}
		}, TIMER1_START, SECOND);
		
		
		// schedule a timer for keeping the return values of tasks
		Timer timerForReturnValues = new Timer();
		
		timerForReturnValues.schedule(new TimerTask() {
			@Override
			public void run() {
				
				// TODO delete after test
				System.out.println("Timer2: attempting to purge outdated return values.");
				purgeOutdatedReturnValues();
				
			}
		}, TIMER2_START, MINUTE);
		
		
		// schedule a timer for timing out the pending tasks
		Timer timerForPendingTasks = new Timer();
		
		timerForPendingTasks.schedule(new TimerTask() {
			@Override
			public void run() {
				
				// TODO delete after test
				System.out.println("Timer3: attempting to purge timed out pending tasks.");				
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
		
		Task task = searchForTask(taskID);
		
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
		
		Task task = searchForTask(taskID);
		
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
		
		Task task = searchForTask(taskID);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			// if the task is not there, return null
			return null;
		}
		
		return task.getReturnValue();
	}
	
	
	
	// start
	public String createNewTask(String objectID, String body) {
		
		if (pendingTasks.size() >= maxNumberOfPendingTasks) {
			
			// TODO delete after test
			System.out.println("Too many pending tasks.");
			
			return null;
		}
		
		// start a task in a default pending status
		Task task = new Task(objectID, body);
		
		pendingTasks.add(task);
		
		// TODO delete after test
		System.out.println("Task created.");
		
		return task.getTaskID();
	}
	
	
	// update 
	public boolean updateTask(String taskID, String returnValue) {
		
		Task task = searchForTask(taskID);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			return false;
		}
		
		// we can only update a status of running task
		if (task.getTaskStatus() != Task.TASKSTATUS_RUNNING) {
			
			// TODO delete after test
			System.out.println("Task is not running.");
			
			return false;
		}
		
		// TODO delete after test
		System.out.println("Task return value set.");
		
		task.setReturnValue(returnValue);
		
		return true;
		
	}
	
	// stop
	
	
	
	
	// cancel
	public boolean cancelTask(String taskID) {
		
		Task task = searchForTask(taskID);
		
		if (task == null) {
			
			// TODO delete after test
			System.out.println("Task does not exist.");
			
			return false;
		}
		
		// we can't cancel finished or failed job
		if (task.getTaskStatus() == Task.TASKSTATUS_FAILED || task.getTaskStatus() == Task.TASKSTATUS_FINISHED) {
			
			// TODO delete after test
			System.out.println("Canceling finished/failed task is not possible.");
			return false;
		}
		
		
		// TODO delete after test
		System.out.println("Canceling task.");
		
		if (!task.cancel())
		
		
		return task.cancel();
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
	
	
	private Task searchForTask(String taskID) {
		
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
		
		// or among finished tasks?
		for (Task task : finishedTasks) {
			if (task.getTaskID().equals(taskID)) {
				return task;
			}
		}
		
		// it is gone
		return null;
	}
	
}
