package candis.client;

import android.content.Context;
import android.util.Log;
import candis.client.service.BackgroundService;
import candis.common.ClassloaderObjectInputStream;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
  /// maximum number of tasks held in cache
  private static final int MAX_TASK_CACHE = 5;// TODO: currently no implemented
  // ---
  /// context, needed for file storage
  private final Context mContext;
  /// Wrapper to pass ClassLoader
  // --- Maps that holds all info for tasks
  private final Map<String, TaskContext> mTaskCache = new HashMap<String, TaskContext>();
  /// List of all registered handlers
  private final List<JobCenterHandler> mHandlerList = new LinkedList<JobCenterHandler>();
  final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
  /**
   * Holds mUsableCores threads for jobs processing.
   */
  private ExecutorService mThreadPool;
//  final ArrayList<DistributedJobResult> results = new ArrayList<DistributedJobResult>();
  final Queue<DistributedJobParameter> parameters = new ConcurrentLinkedQueue<DistributedJobParameter>();
  final Object execdone = new Object();
  private int mUsableCores;

  public JobCenter(final Context context) {
    mContext = context;
    mUsableCores = DroidContext.getInstance().getProfile().processors;
    mThreadPool = Executors.newFixedThreadPool(mUsableCores);
    loadTaskCache();
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
   *
   */
  public final void loadTaskCache() {
    // load all available jobs.
//    for (File f : jarFinder(mContext.getFilesDir().getAbsolutePath())) {
//      System.out.println("Found jar: " + f.getAbsoluteFile());
//    }
    // TODO:...
  }

  /**
   *
   * @param dirName
   * @return
   */
  private File[] jarFinder(String dirName) {
    File dir = new File(dirName);

    return dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String filename) {
        return filename.endsWith(".jar");
      }
    });

  }

  /**
   * Processes job...
   *
   * @param runnableID
   * @param jobID
   * @param param Return true if task available, otherwise false
   */
  public void processJob(String runnableID, String jobID, byte[] param) {
    if (!mTaskCache.containsKey(runnableID)) {
      // notify handlers about missing binary
      for (JobCenterHandler handler : mHandlerList) {
        System.out.println("Notifying handler... " + handler);
        handler.onBinaryRequired(runnableID);
      }
      return;
    }

    executeJob(runnableID, jobID, deserializeJobParameters(runnableID, param));
  }

  /**
   * Adds new runnable (with initial parameter) to runnable cache.
   *
   * @param binary
   */
  public void addRunnable(final String runnableID, final byte[] binary, final byte[] iparam) {
    String filename = runnableID.concat(".jar");
    Log.v(TAG, String.format("Saving jar to file %s/%s", mContext.getFilesDir(), filename));
    final File dexInternalStoragePath = new File(mContext.getFilesDir(), filename);

    if (mTaskCache.containsKey(runnableID)) {
      Log.w(TAG, String.format("Warning: Task with ID %s already loaded", runnableID));
    }
    // create new task context
    mTaskCache.put(runnableID, new TaskContext(filename));// TODO: name
    mTaskCache.get(runnableID).jarfile = filename;

    // store in file
    writeByteArrayToFile(binary, dexInternalStoragePath);

    // load classes
    loadClassesFromJar(runnableID, dexInternalStoragePath);

    // deserialize initial parameter
    mTaskCache.get(runnableID).initialParam = deserializeJobParameters(runnableID, iparam)[0];
  }

  /**
   * Executes task with given ID.
   *
   * May start multiple threads.
   * Calls onJobExecutionStart() at start.
   * Calls onJobExecutionDone() when processing finished.
   * Assures that order of results matches order of parameters.
   *
   * @param runnableID
   * @param params
   * @return
   */
  private void executeJob(final String runnableID, final String jobID, final DistributedJobParameter[] params) {

    if (!mTaskCache.containsKey(runnableID)) {
      Log.e(TAG, String.format("Task with ID %s not found in cache!", runnableID));
      return;
    }

    // Check if task can be executed
    if (!checkExecution()) {
      return;
    }

    // notify handlers about start
    for (JobCenterHandler handler : mHandlerList) {
      handler.onJobExecutionStart(runnableID, jobID);
    }

    parameters.clear();
    parameters.addAll(Arrays.asList(params));
    final DistributedJobResult[] results = new DistributedJobResult[parameters.size()];

    new Thread(new Runnable() {
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
            handler.onJobExecutionDone(null, null, null, 0);
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
              Log.i(TAG, "Running Thread for Job for Task " + mTaskCache.get(runnableID).name + " with TaskID " + runnableID);
//              Log.i(TAG, "Threads: " + mThreadPool.getActiveCount());
              DistributedRunnable currentTask;
              try {
                currentTask = (DistributedRunnable) mTaskCache.get(runnableID).taskClass.newInstance();
                currentTask.setInitialParameter(mTaskCache.get(runnableID).initialParam);
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
                Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
              }
              catch (IllegalAccessException ex) {
                Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
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
                    runnableID,
                    jobID,
                    results,
                    endTime - startTime);
          }
        }
        catch (InterruptedException ex) {
          Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.print("Yeah, they finished!...");
      }
    }).start();

  }

  /**
   * Deserializes the provided serialized DistributedJobParameter
   *
   * @param runnableID runnable ID to deserialize for
   * @return Loaded DistributedJobParameter
   */
  private DistributedJobParameter[] deserializeJobParameters(String runnableID, byte[] rawdata) {
    //
//    mClassLoader.set(mTaskContextMap.get(mCurrentRunnableID).classLoader);
    ClassLoader cloader = mTaskCache.get(runnableID).classLoader;
    Log.i(TAG, "ClassLoaderWrapper now is: " + cloader.toString());
    //
    ObjectInputStream objInstream;
    Object obj = null;
    try {
      objInstream = new ClassloaderObjectInputStream(
              new ByteArrayInputStream(rawdata),
              cloader);
      obj = objInstream.readObject();
      objInstream.close();
    }
    catch (OptionalDataException ex) {
      Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (ClassNotFoundException ex) {
      Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IOException ex) {
      Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
    }
    return (DistributedJobParameter[]) obj;
  }

  /**
   * Checks whether Task with specified ID is alread known and loaded.
   *
   * @param runnableID
   * @return true if known, otherwise false
   */
  public boolean isTaskAvailable(String runnableID) {
    return mTaskCache.containsKey(runnableID);
  }

  /**
   * Checks if the execution is ok.
   *
   * @todo Move to FSM level
   * @return
   */
  private boolean checkExecution() {
    if (!checkPhoneStatusOK()) {
      return false;
    }
    if (!checkBatteryLevelOK()) {
      return false;
    }
    return true;
  }

  /**
   * Checks if battery level is ok for execution.
   *
   * @return
   */
  private boolean checkBatteryLevelOK() {
    return true;
  }

  /**
   * Checks if phone status is ok for execution.
   *
   * @return
   */
  private boolean checkPhoneStatusOK() {
    return true;
  }

  /**
   *
   * @param data
   * @param filename
   */
  private static void writeByteArrayToFile(final byte[] data, final File filename) {
    BufferedOutputStream bos = null;

    try {
      //create an object of FileOutputStream
      FileOutputStream fos = new FileOutputStream(filename);
      //create an object of BufferedOutputStream
      bos = new BufferedOutputStream(fos);
      bos.write((byte[]) data);
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

  /**
   * As it name claims, it loads classes from the given jar.
   *
   * @param runnableID
   * @param jarfile
   */
  private void loadClassesFromJar(final String runnableID, final File jarfile) {

    mTaskCache.get(runnableID).taskClasses = new LinkedList<Class>();

    Log.i(TAG,
          "XXX: Calling DexClassLoader with jarfile: " + jarfile.getAbsolutePath());
    final File tmpDir = mContext.getDir("dex", 0);

    mTaskCache.get(runnableID).classLoader = new DexClassLoader(
            jarfile.getAbsolutePath(),
            tmpDir.getAbsolutePath(),
            null,
            BackgroundService.class.getClassLoader());
//    mTaskCache.get(mCurrentRunnableID).classLoader = mTaskCache.get(runnableID).classLoader;
//    setRunnableID(runnableID);    

    // load all available classes
    String path = jarfile.getPath();


    try {
      // load dexfile
      DexFile dx = DexFile.loadDex(
              path,
              File.createTempFile("opt", "dex", mContext.getCacheDir()).getPath(),
              0);

      // extract all available classes
      for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
        String className = classNames.nextElement();
        Log.i(TAG, String.format("found class: %s", className));
        try {
          // TODO: do only forName() here?
//          final Class<Object> loadedClass = (Class<Object>) mClassLoaderWrapper.get().loadClass(className);
          final Class<Object> loadedClass = (Class<Object>) mTaskCache.get(runnableID).classLoader.loadClass(className);
          Log.i(TAG, String.format("Loaded class: %s", className));
          // add associated classes to task class list
          if (loadedClass == null) {
            Log.e(TAG, "EEEEEE loadedClass is null");
          }
          if (mTaskCache.get(runnableID) == null) {
            Log.e(TAG, "EEEEEE no mapentry found");
          }
          if (mTaskCache.get(runnableID).taskClasses == null) {
            Log.e(TAG, "EEEEEE taskClasses empty");
          }
          mTaskCache.get(runnableID).taskClasses.add(loadedClass);
          // add task class to task list
          if (DistributedRunnable.class.isAssignableFrom(loadedClass)) {
            mTaskCache.get(runnableID).taskClass = loadedClass;
          }
        }
        catch (ClassNotFoundException ex) {
          Log.getStackTraceString(ex);
        }
      }
    }
    catch (IOException e) {
      System.out.println("Error opening " + path);
    }
    // notify listeners
    for (JobCenterHandler handler : mHandlerList) {
      handler.onBinaryReceived(runnableID);
    }
  }

  /**
   *
   * @param dparam
   */
  public boolean setInitialParameter(final String runnableID, final DistributedJobParameter dparam) {
    Log.i(TAG, "runnableID: " + runnableID);
    Log.i(TAG, "Param: " + dparam);
    mTaskCache.get(runnableID).initialParam = dparam;
    Log.i(TAG, "Initial Parameter for ID " + runnableID + " loaded with classloader " + ((DistributedJobParameter) dparam).getClass().getClassLoader());

    for (JobCenterHandler handler : mHandlerList) {
      handler.onInitialParameterReceived(runnableID);
    }

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
   * Class that can hold all information required for a task.
   */
  private class TaskContext {

    public final String name;
    /// Name of jarfile to be able to delete it
    public String jarfile;
    /// List of Classes for Task
    public List<Class> taskClasses;
    /// Holds the runnable Class for Task
    public Class taskClass;
    /// Holds initial parameter for Task
    public DistributedJobParameter initialParam;
    /// Holds the Taks classloader
    public ClassLoader classLoader;

    public TaskContext(String name) {
      this.name = name;
    }
  }
}
