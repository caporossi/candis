package candis.client;

import android.content.Context;
import android.util.Log;
import candis.client.service.BackgroundService;
import candis.common.ClassLoaderWrapper;
import candis.common.ClassloaderObjectInputStream;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
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
import java.io.IOException;
import java.io.OptionalDataException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls the execution of tasks.
 *
 * @author Enrico Joerns
 */
public class JobCenter {

  private static final String TAG = JobCenter.class.getName();
  /// maximum number of tasks held in cache
  private static final int MAX_TASKS = 5;// TODO: currently no implemented
  // ---
  /// context, needed for file storage
  private final Context mContext;
  /// Wrapper to pass ClassLoader
  private final ClassLoaderWrapper mClassLoader;
  // --- Maps that holds all info for tasks
  private final Map<String, TaskContext> mTaskContextMap = new HashMap<String, TaskContext>();
  /// List of all registered handlers
  private final List<JobCenterHandler> mHandlerList = new LinkedList<JobCenterHandler>();
  private FSM mFSM;
  private byte[] lastUnserializedJob;

  public JobCenter(final Context context, final ClassLoaderWrapper cl) {
    mContext = context;
    mClassLoader = cl;
  }

  public void setFSM(FSM fsm) {
    mFSM = fsm;
  }

  public void setLastUnserializedJob(byte[] lusj) {
    lastUnserializedJob = lusj;
  }

  public DistributedJobParameter serializeJob() {
    Object obj = null;
    try {
      obj = new ClassloaderObjectInputStream(new ByteArrayInputStream(lastUnserializedJob), mClassLoader).readObject();
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
    return (DistributedJobParameter) obj;
  }

  /**
   *
   * @param binary
   */
  public void loadBinary(final String runnableID, final byte[] binary) {
    String filename = runnableID.concat(".jar");
    Log.v(TAG, String.format("Saving jar to file %s", filename));
    final File dexInternalStoragePath = new File(mContext.getFilesDir(), filename);

    if (mTaskContextMap.containsKey(runnableID)) {
      Log.w(TAG, String.format("Warning: Task with ID %s already loaded", runnableID));
    }
    // create new task context
    mTaskContextMap.put(runnableID, new TaskContext(filename));// TODO: name
    mTaskContextMap.get(runnableID).jarfile = filename;

    writeByteArrayToFile(binary, dexInternalStoragePath);

    loadClassesFromJar(runnableID, dexInternalStoragePath);
  }

  /**
   * Executes task with given ID.
   *
   * @param runnableID
   * @param param
   * @return
   */
  public void executeTask(final String runnableID, final DistributedJobParameter param) {

    if (!mTaskContextMap.containsKey(runnableID)) {
      Log.e(TAG, String.format("Task with ID %s not found", runnableID));
      return;
    }

    // Check if task can be executed
    if (!checkExecution()) {
      return;
    }

    // notify handlers about start
    for (JobCenterHandler handler : mHandlerList) {
      handler.onJobExecutionStart(runnableID);
    }

    new Thread(new Runnable() {
      public void run() {
        Log.i(TAG, "Running Job for Task " + mTaskContextMap.get(runnableID).name + " with TaskID " + runnableID);
        DistributedJobResult result = null;
        // try to instanciate class
        DistributedRunnable currentTask;
        try {
          currentTask = (DistributedRunnable) mTaskContextMap.get(runnableID).taskClass.newInstance();
          currentTask.setInitialParameter(mTaskContextMap.get(runnableID).initialParam);
          result = currentTask.runJob(param);
        }
        catch (InstantiationException ex) {
          Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex) {
          Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (result == null) {
          Log.e(TAG, "Process returned null");
        }
        try {
          mFSM.process(ClientStateMachine.ClientTrans.JOB_FINISHED, runnableID, result);
        }
        catch (StateMachineException ex) {
          Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
        }

        // notify handlers about end
        for (JobCenterHandler handler : mHandlerList) {
          handler.onJobExecutionDone(runnableID);
        }
      }
    }).start();
  }

  /**
   * Checks whether Task with specified ID is alread known and loaded.
   *
   * @param runnableID
   * @return true if known, otherwise false
   */
  public boolean isTaskAvailable(String runnableID) {
    return mTaskContextMap.containsKey(runnableID);
  }

  /**
   * Checks if the execution is ok.
   *
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
  private void writeByteArrayToFile(final byte[] data, final File filename) {
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

    mTaskContextMap.get(runnableID).taskClasses = new LinkedList<Class>();

    Log.i(TAG,
          "XXX: Calling DexClassLoader with jarfile: " + jarfile.getName());
    final File tmpDir = mContext.getDir("dex", 0);

    mClassLoader.set(
            new DexClassLoader(
            jarfile.getAbsolutePath(),
            tmpDir.getAbsolutePath(),
            null,
            BackgroundService.class
            .getClassLoader()));


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
          final Class<Object> loadedClass = (Class<Object>) mClassLoader.get().loadClass(className);
          Log.i(TAG, String.format("Loaded class: %s", className));
          // add associated classes to task class list
          if (loadedClass == null) {
            Log.e(TAG, "EEEEEE loadedClass is null");
          }
          if (mTaskContextMap.get(runnableID) == null) {
            Log.e(TAG, "EEEEEE no mapentry found");
          }
          if (mTaskContextMap.get(runnableID).taskClasses == null) {
            Log.e(TAG, "EEEEEE taskClasses empty");
          }
          mTaskContextMap.get(runnableID).taskClasses.add(loadedClass);
          // add task class to task list
          if (DistributedRunnable.class.isAssignableFrom(loadedClass)) {
            mTaskContextMap.get(runnableID).taskClass = loadedClass;
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
    mTaskContextMap.get(runnableID).initialParam = dparam;
    Log.i(TAG, "Initial Parameter for ID " + runnableID + " loaded with classloader " + ((DistributedJobParameter) dparam).getClass().getClassLoader());

    for (JobCenterHandler handler : mHandlerList) {
      handler.onInitialParameterReceived(runnableID);
    }

    return true;
  }

  /**
   *
   * @todo: needed?
   * @param o
   */
//  public boolean loadJob(final String runnableID, final Object o) {
//    if (!(o instanceof DistributedJobParameter)) {
//      Log.e(TAG, "Initial Parameter not of class 'DistributedParameter'");
//      return false;
//    }
//    Log.w(TAG, "Job Parameter dummy-loaded");
//    return true;
//  }
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

    public TaskContext(String name) {
      this.name = name;
    }
  }
}
