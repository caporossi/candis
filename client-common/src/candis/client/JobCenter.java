package candis.client;

import android.util.Log;
import candis.common.ClassloaderObjectInputStream;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
//import dalvik.system.DexClassLoader;
//import dalvik.system.DexFile;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls the execution of tasks.
 *
 * @author Enrico Joerns
 */
public class JobCenter {

  private static final String TAG = JobCenter.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private static final int MAX_TASK_CACHE = 5;
  private static final String TASKE_NAME_POSTFIX = ".jar";
  private static final String IPARAM_NAME_POSTFIX = ".par";
  /// maximum number of tasks held in cache
  private final Set<String> mTaskCache = Collections.newSetFromMap(new HashMap<String, Boolean>());
  private final TaskFileManipulator mTaskFileManiuplator;
  private final TaskProvider mTaskProvider;
  /// List of all registered handlers
  private final List<JobCenterHandler> mHandlerList = new LinkedList<JobCenterHandler>();
  /// Holds mUsableCores threads for jobs processing.
  private final ExecutorService mThreadPool;
  private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
  private final Queue<DistributedJobParameter> parameters = new ConcurrentLinkedQueue<DistributedJobParameter>();
  private int mUsableCores;
  private final File mFilesDir;
  private final File mCacheDir;
  private Thread mJobThread;
  private final List<ExecutionChecker> mExecCheckers = new LinkedList<ExecutionChecker>();

  public JobCenter(File filesDir, File cacheDir, TaskProvider tprovider) {
    mFilesDir = filesDir;
    mCacheDir = cacheDir;
    mUsableCores = 1;
    mThreadPool = Executors.newFixedThreadPool(mUsableCores);
    mTaskFileManiuplator = new TaskFileManipulator(mFilesDir);
    mTaskProvider = tprovider;
    populateTaskCache();
  }

  /**
   * Enables multicore support.
   *
   * I.e. Number-of-processors threads are started to perform job processing.
   *
   * @param enabled
   */
  public void setMulticore(boolean enabled) {
    if (enabled) {
      mUsableCores = DroidContext.getInstance().getProfile().processors;
    }
    else {
      mUsableCores = 1;
    }
    LOGGER.info(String.format("System will use %d Threads for processing", mUsableCores));
  }

  /**
   * Loads runnables from disk.
   *
   * Note: If more than MAX_TASK_CACHE runnables were found, all
   * remaining runnables will be deleted.
   */
  private void populateTaskCache() {
    // load all available jobs.
    int fileCount = 0;
    for (File taskFile : jarFinder(mFilesDir.getAbsolutePath())) {
      fileCount++;
      String taskID = taskFile.getName().substring(0, taskFile.getName().lastIndexOf('.'));
      LOGGER.info("Found cached task: " + taskID);
      // limit maximum cache files
      if (fileCount > MAX_TASK_CACHE) {
        taskFile.delete();
        LOGGER.info(String.format("Deleted runnable in cache: %s", taskFile.getName()));
      }
      // check for initial parameters
      else if (!new File(taskID.concat(IPARAM_NAME_POSTFIX)).exists()) {
        LOGGER.warning("Saved initial param could not be loaded");
      }
      // if everything is okay, add to current cache map
      else {
        mTaskCache.add(taskID);
      }
    }
  }

  /**
   * Looks up all jar files in specified dir.
   *
   * @param dirName Array of File Objects for found jars.
   * @return
   */
  private File[] jarFinder(String dirName) {
    File dir = new File(dirName);

    return dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String filename) {
        return filename.endsWith(".jar");
      }
    });
  }

  /**
   * Adds new runnable (with initial parameter) to runnable cache.
   *
   * @param binary
   */
  public void addRunnable(final String taskID, final byte[] binary, final byte[] iparam) {

    // If runnable is already in cache, skip
    if (mTaskCache.contains(taskID)) {
      Log.w(TAG, String.format("Warning: Task with ID %s already loaded", taskID));
      return;
    }

    // store in file and add to cache map
    mTaskFileManiuplator.store(taskID, binary, iparam);

    if (mTaskCache.size() >= MAX_TASK_CACHE) {
      // TODO: delte oldest one
    }

    mTaskCache.add(taskID);

    // notify listeners
    for (JobCenterHandler handler : mHandlerList) {
      handler.onBinaryReceived(taskID);
    }
  }

  /**
   * Processes job...
   *
   * May start multiple threads.
   * Calls onJobExecutionStart() at start.
   * Calls onJobExecutionDone() when processing finished.
   * Assures that order of results matches order of parameters.
   *
   * @param taskID
   * @param jobID
   * @param param Return true if task available, otherwise false
   */
  public void processJob(final String taskID, final String jobID, byte[] param) {

    if (!mTaskCache.contains(taskID)) {
      for (JobCenterHandler handler : mHandlerList) {
        handler.onBinaryRequired(taskID);
      }
      return;
    }

    final DistributedJobParameter[] params;
    try {
      params = mTaskProvider.deserializeJobParameters(taskID, param);
    }
    catch (TaskNotFoundException ex) {
      LOGGER.severe("Failed loading Task " + taskID);
      for (JobCenterHandler handler : mHandlerList) {
        handler.onJobRejected(taskID, jobID);
      }
      return;
    }

    // Check if task can be  onJobRejected(taskID);executed
    if (!checkExecution()) {
      return;
    }

    // notify handlers about start
    for (JobCenterHandler handler : mHandlerList) {
      handler.onJobExecutionStart(taskID, jobID);
    }

    parameters.clear();
    parameters.addAll(Arrays.asList(params));
    final DistributedJobResult[] results = new DistributedJobResult[parameters.size()];

    mJobThread = new Thread(new Runnable() {
      public void run() {
        long startTime, endTime;
        startTime = System.currentTimeMillis();

        LOGGER.info(String.format("parameters: %d", parameters.size()));
        // start threads
        final int usedThreads = Math.min(mUsableCores, parameters.size());
        // might happen?
        if (usedThreads == 0) {
          LOGGER.warning("Scheduling on 0 Threads, thus canceled");
          for (JobCenterHandler handler : mHandlerList) {
            handler.onJobExecutionDone(taskID, jobID, null, 0);
          }
          return;
        }

        LOGGER.info(String.format("usedThreads: %d", usedThreads));
        final int paramsPerThread = (parameters.size() + (usedThreads - 1)) / usedThreads;
        LOGGER.info(String.format("paramsPerThread: %d", paramsPerThread));

        final CountDownLatch latch = new CountDownLatch(usedThreads);

        // start as many threads as allowed
        for (int i = 0; i < usedThreads; i++) {

          mThreadPool.execute(new Runnable() {
            public void run() {
              Log.i(TAG, "Running Thread for Job for Task " + taskID + " with TaskID " + taskID);

              DistributedRunnable currentTask;
              try {
                currentTask = mTaskProvider.newRunnableInstance(taskID);
                currentTask.setInitialParameter(mTaskProvider.getInitialParam(taskID));
                // execute job for all parameters, assure order
                for (int j = 0; j < paramsPerThread && !parameters.isEmpty(); j++) {
                  DistributedJobParameter runParam;
                  int runID;// needed to assure correct order
                  synchronized (parameters) {
                    runParam = parameters.poll();
                    runID = parameters.size();
                  }
                  DistributedJobResult result = currentTask.execute(runParam);
                  results[results.length - 1 - runID] = result;
                }
                // notify receivers if done
                latch.countDown();
              }
              catch (InstantiationException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
              }
              catch (IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
              }
              catch (TaskNotFoundException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
              }
            }
          });
        }

        System.out.print("Awaiting Threads to finish...");
        try {
          latch.await();
          endTime = System.currentTimeMillis();
          for (JobCenterHandler handler : mHandlerList) {
            handler.onJobExecutionDone(
                    taskID,
                    jobID,
                    results,
                    endTime - startTime);
          }
        }
        catch (InterruptedException ex) {
          LOGGER.log(Level.INFO, null, ex);
        }
        System.out.print("Yeah, they finished!...");
      }
    });
    mJobThread.start();
  }

  /**
   * Stops currently running job.
   */
  public void stopCurrentJob() {
    mThreadPool.shutdownNow();
    if (mJobThread != null) {
      mJobThread.interrupt();
    }
  }

  /**
   * Checks whether Task with specified ID is alread known and loaded.
   *
   * @param runnableID
   * @return true if known, otherwise false
   */
  public boolean isTaskAvailable(String runnableID) {
    return mTaskCache.contains(runnableID);
  }

  /**
   *
   * @param dparam
   */
  public boolean setInitialParameter(final String runnableID, final DistributedJobParameter dparam) {
//    Log.i(TAG, "runnableID: " + runnableID);
//    Log.i(TAG, "Param: " + dparam);
//    mTaskProvider.set(runnableID).initialParam = dparam;
//    Log.i(TAG, "Initial Parameter for ID " + runnableID + " loaded with classloader " + ((DistributedJobParameter) dparam).getClass().getClassLoader());
//
//    for (JobCenterHandler handler : mHandlerList) {
//      handler.onInitialParameterReceived(runnableID);
//    }

    return true;
  }

  /**
   *
   * @param handler
   */
  public void addHandler(final JobCenterHandler handler) {
    mHandlerList.add(handler);
  }

  /**
   * Checks if the execution is ok.
   *
   * @todo Move to FSM level
   * @return
   */
  private boolean checkExecution() {
    for (ExecutionChecker checker : mExecCheckers) {
      if (checker.checkExecution() == false) {
        return false;
      }
    }
    return true;
  }

  public void addExecutionChecker(ExecutionChecker checker) {
    mExecCheckers.add(checker);
  }

  public interface ExecutionChecker {

    /**
     *
     * @return Must return true if check is ok, otherwise false
     */
    public boolean checkExecution();
  }

  /**
   * Allows to save and modify tasks.
   */
  public static class TaskFileManipulator {

    private File mCacheDir;

    public TaskFileManipulator(File cacheDir) {
      mCacheDir = cacheDir;
    }

    public File getRunnableFile(String taskID) {
      return new File(mCacheDir, taskID.concat(TASKE_NAME_POSTFIX));
    }

    public File getInitParamFile(String taskID) {
      return new File(mCacheDir, taskID.concat(IPARAM_NAME_POSTFIX));
    }

    private void store(String taskID, byte[] binary, byte[] iparam) {
      writeByteArrayToFile(binary, getRunnableFile(taskID));
      writeByteArrayToFile(iparam, getInitParamFile(taskID));
    }

    private void storeInitParam(String taskID, byte[] iparam) {
      writeByteArrayToFile(iparam, getInitParamFile(taskID));
    }

    private static void writeByteArrayToFile(final byte[] data, final File filename) {
      BufferedOutputStream bos = null;

      try {
        //create an object of BufferedOutputStream
        bos = new BufferedOutputStream(new FileOutputStream(filename));
        bos.write(data);
        System.out.println(String.format("File '%s' written", filename));
      }
      catch (FileNotFoundException fnfe) {
        System.out.println("Specified file not found" + fnfe);
      }
      catch (IOException ioe) {
        System.out.println("Error while writing file" + ioe);
      }
      finally {
        if (bos != null) {
          try {
            bos.flush();
            bos.close();
          }
          catch (Exception e) {
          }
        }
      }
    }
  }

  public static class TaskNotFoundException extends FileNotFoundException {
  }
}