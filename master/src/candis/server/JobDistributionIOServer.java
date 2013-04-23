package candis.server;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.JobDistributionIOHandler;
import candis.distributed.JobHandler;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SchedulerBinder;
import candis.distributed.SchedulerStillRuningException;
import candis.distributed.WorkerQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class JobDistributionIOServer implements JobDistributionIO, SchedulerBinder, JobHandler {

	private static final Logger LOGGER = Logger.getLogger(JobDistributionIOServer.class.getName());
	protected final DroidManager mDroidManager;
	private final CDBLoader mCDBLoader;
	/// The control, scheduler and id for the task currently processed
	protected DistributedControl mDistributedControl;
	protected Scheduler mCurrentScheduler;
	private String mCurrentTaskID = "";
	private int mCurrenJobID = 0;
	/// Thread and list for execution queue
	private WorkerQueue mWorker;
	/// Holds all registered handlers
	private List<JobDistributionIOHandler> mHanderList = new LinkedList<JobDistributionIOHandler>();
	/// Holds all registered receivers
	protected final List<ResultReceiver> mReceivers = new LinkedList<ResultReceiver>();

	public JobDistributionIOServer(final DroidManager manager) {
		mCDBLoader = new CDBLoader();
		mDroidManager = manager;
		mWorker = new WorkerQueue();
		new Thread(mWorker).start();
	}
	/*--------------------------------------------------------------------------*/
	/* Callback functionality                                                   */
	/*--------------------------------------------------------------------------*/

	public void addHandler(JobDistributionIOHandler handler) {
		mHanderList.add(handler);
	}

	private void invoke(JobDistributionIOHandler.Event event) {
		for (JobDistributionIOHandler h : mHanderList) {
			h.onEvent(event);
		}
	}

	/*--------------------------------------------------------------------------*/
	/* Get methods                                                              */
	/*--------------------------------------------------------------------------*/
	public String getCurrentTaskID() {
		return mCurrentTaskID;
	}

	public Scheduler getCurrentScheduler() {
		return mCurrentScheduler;
	}

	public CDBLoader getCDBLoader() {
		return mCDBLoader;
	}

	@Override
	public DistributedControl getControl() {
		if (mControl == null) {
			LOGGER.severe("Control is null");
		}
		return mControl;
	}
	// Called by Scheduler.

	/*--------------------------------------------------------------------------*/
	/* Scheduler callback methods for Droid control                             */
	/*--------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------*/
	/* Execution queue methods                                                  */
	/*--------------------------------------------------------------------------*/

	/*--------------------------------------------------------------------------*/
	/* Scheduler control functions                                              */
	/*--------------------------------------------------------------------------*/
//	public void initScheduler(String taskID) throws SchedulerStillRuningException {
//		if ((mCurrentScheduler == null) || (mCurrentScheduler.isDone())) {
//			mCurrentTaskID = taskID;
//			// init scheduler and set self as callback
//			mDistributedControl = mCDBLoader.getDistributedControl(taskID);
//			mCurrentScheduler = mDistributedControl.initScheduler();
//			mCurrentScheduler.setJobDistributionIO(this);
//			mCurrenJobID = 0;
//		}
//		else {
//			throw new SchedulerStillRuningException("Scheduler still running");
//		}
//	}
	//
	// ------- NEW IMPLEMENATION -------
	//
	/// Max. time [ms] to wait for an ACK
	private long mAckTimeout = 1000;
	/// Max. time [ms] to wait for a Result
	private long mJobTimeout = 60000;
	/// Parameter cache
	private final Queue<DistributedJobParameter> mParamCache = new ConcurrentLinkedQueue<DistributedJobParameter>();
	/// Holds all active AckTimerTasks
	private final Map<String, TimerTask> mAckTimers = new ConcurrentHashMap<String, TimerTask>();
	/// Holds all active AckTimerTasks
	private final Map<String, TimerTask> mJobTimers = new ConcurrentHashMap<String, TimerTask>();
	/// Holds all sent parameters for that no result is available yet
	private final Map<String, DistributedJobParameter[]> mProcessingParams = new ConcurrentHashMap<String, DistributedJobParameter[]>();
	/// Holds ID of all Droids that are available and idle
	private final Set<String> mIdleDroids = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	/// Timer to run TimerTasks
	private final Timer mTimer = new Timer();
	///
	private DistributedControl mControl;

	@Override
	public void startJob(String droidID, DistributedJobParameter[] params) {
		DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(droidID);
		// if no handler is available, put parameters back to cache and exit.
		if (mHandler == null) {
			LOGGER.severe(String.format("startJob() failed, lost handler for droid %s", droidID));
			mParamCache.addAll(Arrays.asList(params));
			return;
		}

		mCurrenJobID++;

		LOGGER.info(String.format(
						"Start Job %d on Droid %s with %d params",
						mCurrenJobID,
						droidID.substring(0, 9),
						params.length));

		// remove droid from list of available
		mIdleDroids.remove(droidID);
		// add parameters to list of unAcked
		System.out.println("params[0]: " + params[0]);
		mProcessingParams.put(droidID, params);
		// start timeout timers
		TimerTask ackTask = new AckTimerTask(droidID);
		TimerTask jobTask = new JobTimerTask(droidID);
		mAckTimers.put(droidID, ackTask);
		mJobTimers.put(droidID, jobTask);
		mTimer.schedule(ackTask, mAckTimeout);
		mTimer.schedule(jobTask, mJobTimeout);
		// actually start job
		mHandler.onSendJob(mCurrentTaskID, String.valueOf(mCurrenJobID), params);
		invoke(JobDistributionIOHandler.Event.JOB_SENT);
	}

	@Override
	public void stopJob(final String droidID) {
		DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(droidID);
		if (mHandler == null) {
			LOGGER.severe(String.format("stopJob() failed, lost handler for droid %s", droidID));
			return;
		}

		mHandler.onStopJob(String.valueOf(mCurrenJobID), mCurrentTaskID);
		// cancel timers
		if (mAckTimers.containsKey(droidID)) {
			mAckTimers.remove(droidID).cancel();
		}
		if (mJobTimers.containsKey(droidID)) {
			mJobTimers.remove(droidID).cancel();
		}
	}

	@Override
	public String[] getAvailableDroids() {
		return mIdleDroids.toArray(new String[mIdleDroids.size()]);
	}

	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	@Override
	public DistributedJobParameter[] getParameters(int n) {
		int count = 0;
		ArrayList<DistributedJobParameter> params = new ArrayList<DistributedJobParameter>();
		// first get from internal cache
		while ((mParamCache.size() > 0) && (count < n)) {
			params.add(mParamCache.poll());
			count++;
		}
		n -= count;
		count = 0;
		// try to load remaining parameters from scheduler
		while (count < n) {
			if (!mControl.hasParametersLeft()) {
				break;
			}
			DistributedJobParameter param = mControl.getParameter();
			params.add(param);
			count++;
		}

		return params.toArray(new DistributedJobParameter[params.size()]);
	}

	@Override
	public void setControl(String taskID) {
		mCurrentTaskID = taskID;
		mControl = mCDBLoader.getDistributedControl(taskID);
		System.out.println("Control loaded: " + mControl.getClass());
		mControl.init();
	}

	/**
	 *
	 * @param droidID
	 * @param p
	 */
	@Override
	public void sendInitialParameter(String droidID, DistributedJobParameter p) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAckTimeout(long millis) {
		mAckTimeout = millis;
	}

	@Override
	public void setJobTimeout(long millis) {
		mJobTimeout = millis;
	}

	@Override
	public void bindScheduler(Scheduler scheduler) {
		mCurrentScheduler = scheduler;
		mCurrentScheduler.setJobDistIO(this);
	}

	@Override
	public void unbindScheduler(Scheduler scheduler) {
		mCurrentScheduler = null;
	}

	// TODO: synchronize
	@Override
	public void onDroidConnected(String droidID) {
		mIdleDroids.add(droidID);
	}

	// TODO: synchronize
	@Override
	public void onJobReceived(String droidID, String jobID) {
		// cancel droids Ack timeout timer
		mAckTimers.remove(droidID).cancel();
	}

	// TODO: synchronize
	@Override
	public void onJobDone(String droidID, String jobID, final DistributedJobResult[] results, long exectime) {
		// (re)add to list of idle droids
		mIdleDroids.add(jobID);
		System.out.println("Notifying current Scheduler");
		mCurrentScheduler.doNotify();
		// 
		if (!mProcessingParams.containsKey(droidID)) {
			LOGGER.severe(String.format(
							"Received unknown result from droid %s",
							droidID.substring(0, 9)));
			return;
		}

		final DistributedJobParameter[] params = mProcessingParams.remove(droidID);
		// notify receivers
		for (int i = 0; i < results.length; i++) {
			releaseResult(params[i], results[i]);
		}
	}

	/**
	 * Adds a listener for Results.
	 *
	 * @param receiver
	 */
	public void addResultReceiver(ResultReceiver receiver) {
		mReceivers.add(receiver);
	}

	/**
	 * Called by Scheduler to publish results.
	 *
	 * @param param
	 * @param result
	 */
	protected void releaseResult(final DistributedJobParameter param, final DistributedJobResult result) {
		mWorker.add(new Runnable() {
			@Override
			public void run() {
				for (ResultReceiver receiver : mReceivers) {
					receiver.onReceiveResult(param, result);
				}
			}
		});
	}

	/**
	 * Invoked if no ACK for Job was received within timeout interval.
	 */
	private class AckTimerTask extends TimerTask {

		private String mDroidID;

		AckTimerTask(String droidID) {
			mDroidID = droidID;
		}

		@Override
		public void run() {
			DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(mDroidID);
			LOGGER.warning(String.format(
							"Timeout while waiting for ACK from Droid %s...",
							mDroidID.substring(0, 9)));
			// tell droid to stop
			if (mHandler != null) {
				mHandler.onStopJob(null, mDroidID);// TODO: add taskID
			}
			// remove parameters from processing list and add back to cache
			if (mProcessingParams.containsKey(mDroidID)) {
				mParamCache.addAll(Arrays.asList(mProcessingParams.remove(mDroidID)));
			}
		}
	}

	/**
	 * Invoked if no Result for Job was received within timeout interval.
	 */
	private class JobTimerTask extends TimerTask {

		private String mDroidID;

		JobTimerTask(String droiID) {
			mDroidID = droiID;
		}

		@Override
		public void run() {
			DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(mDroidID);
			LOGGER.warning(String.format(
							"Timeout while waiting for Result from Droid %s",
							mDroidID.substring(0, 9)));
			// tell droid to stop
			if (mHandler != null) {
				mHandler.onStopJob(null, mDroidID);// TODO: add taskID
			}
			// remove parameters from processing list and add back to cache
			if (mProcessingParams.containsKey(mDroidID)) {
				mParamCache.addAll(Arrays.asList(mProcessingParams.remove(mDroidID)));
			}
		}
	}
}
