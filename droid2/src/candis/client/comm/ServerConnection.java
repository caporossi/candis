package candis.client.comm;

import candis.common.Message;
import candis.common.QueuedMessageConnection;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Enrico Joerns
 */
public class ServerConnection {

  private final SecureSocket mSecureSocket;
  private Timer mTimer;
  private long mReconnectDelay;
  private boolean mConnectEnabled = false;
  private String mHostname;
  private int mPort;
  private List<Receiver> receivers = new LinkedList<Receiver>();
  private QueuedMessageConnection mQueuedMessageConnection;
  private Thread mQMCThread;
  private Thread mReceiverThread;
  private static final Logger LOGGER = Logger.getLogger(ServerConnection.class.getName());
  // Minimum reconnect interval: 1 seconds
  private static final int RECONNECT_DELAY_MIN = 1000;
  // Maximum reconnect interval: 60 second
  private static final int RECONNECT_DELAY_MAX = 60000;
  // interval factor
  private static final int RECONNECT_DELAY_FACTOR = 2;

  public enum Status {

    CONNECTED,
    DISCONNECTED
  }

  /**
   *
   */
  public ServerConnection(String hostname, int port, X509TrustManager trustmanager) {
    mHostname = hostname;
    mPort = port;
    mSecureSocket = new SecureSocket(trustmanager);
  }

  public void setHost(String hostname, int port) {
    mHostname = hostname;
    mPort = port;
  }

  /**
   * Connects to the Host.
   * An existing connection will not be changed.
   *
   * If connection fails, reconnect attemps are scheduled automatically
   */
  public void connect() {
    // quit if connecting is already enabled
    if (mConnectEnabled) {
      System.out.println("Connect already enabled, nothing will be done...");
      return;
    }

    // init (re)connection scheduler
    mReconnectDelay = RECONNECT_DELAY_MIN;
    mConnectEnabled = true;
    mTimer = new Timer();
    mTimer.schedule(new ConnectTask(), 0);
  }

  /**
   * Reconnects to the Host.
   * If the Host was not connected before, no reconnect is performed.
   * An existing connection will be disconnected.
   */
  public void reconnect() {
    if (!mConnectEnabled) {
      return;
    }
    disconnect();
    connect();
  }

  /**
   * Disconnects from the Host.
   */
  public void disconnect() {
    System.out.println("disconnect() called");
    // quit if connecting is already disabled
    if (!mConnectEnabled) {
      return;
    }

    System.out.println("disconnecting....");
    // stop conenction
    mConnectEnabled = false;
    // stop timer and sender/receiver threads
    mTimer.cancel();
    mReceiverThread.interrupt();
    mQMCThread.interrupt();
    new Thread(new Runnable() {
      public void run() {
        mSecureSocket.close();
        notifyListeners(Status.DISCONNECTED);
      }
    }).start();
  }

  /**
   * Sends Message if connected.
   *
   * @param msg
   */
  public void sendMessage(Message msg) {
    if (mQueuedMessageConnection == null) {
      LOGGER.log(Level.WARNING, "Message " + msg.getRequest() + " could not be send.");
      return;
    }

    try {
      mQueuedMessageConnection.sendMessage(msg);
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Adds a receiver that will receive messages and connection status updates.
   *
   * @param rec Receiver to add
   */
  public void addReceiver(Receiver rec) {
    if (rec != null) {
      System.out.println("Adding receiver... " + rec);
      receivers.add(rec);
    }
  }

  /**
   * Task scheduled peridically to test connection
   */
  private class ConnectTask extends TimerTask {

    @Override
    public void run() {
      System.out.println("ConnectTask.run()");
      if ((mConnectEnabled) && (!mSecureSocket.isConnected())) {
        System.out.println("ready to connect baby");
        // try to connect to host
        try {
          mSecureSocket.connect(mHostname, mPort);
          mSecureSocket.getSocket().setKeepAlive(true);

          mQueuedMessageConnection = new QueuedMessageConnection(mSecureSocket.getSocket());

          // start message queue thread
          mQMCThread = new Thread(mQueuedMessageConnection);
          mQMCThread.setDaemon(true);
          mQMCThread.start();

          // start receiver thread
          mReceiverThread = new Thread(new MessageReceiver());
          mReceiverThread.start();
          // reset reconnect delay to minimum
          mReconnectDelay = RECONNECT_DELAY_MIN;

          notifyListeners(Status.CONNECTED);
        }
        // connect failed
        catch (IOException ex) {
          Logger.getLogger(ServerConnection.class.getName())
                  .log(Level.SEVERE, ex.getMessage());
          notifyListeners(Status.DISCONNECTED);
          if (mReconnectDelay < RECONNECT_DELAY_MAX) {
            mReconnectDelay *= RECONNECT_DELAY_FACTOR;
          }
          mTimer = new Timer();
          mTimer.schedule(new ConnectTask(), mReconnectDelay);
        }
      }
    }
  }

  private class MessageReceiver implements Runnable {

    /**
     * Thread to handle incoming requests.
     */
    @Override
    public void run() {
      // start message queue
      while (!Thread.interrupted()) {
        try {
          // wait for new message
          Message msg = mQueuedMessageConnection.readMessage();
          // notify listeners
          for (Receiver r : receivers) {
            r.OnNewMessage(msg);
          }
        }
        // The thread was interrupted
        catch (InterruptedIOException ex) {
          LOGGER.warning(ex.getMessage());
          reconnect();
          return;
        }
        // The socket was close of any reason
        catch (IOException ex) {
          LOGGER.warning(ex.getMessage());
          reconnect();
          return;
        }
      }
      LOGGER.log(Level.INFO, "[[ServerConnection THREAD done]]");

      disconnect();
    }
  }

  /**
   * Notify listeners about status update
   *
   * @param status Status to promote
   */
  private void notifyListeners(Status status) {
    for (Receiver r : receivers) {
      System.out.println("Notifying... " + r);
      r.OnStatusUpdate(status);
    }
  }

  /*
   * 
   */
  public interface Receiver {

    /**
     * Invoked when new message was received.
     *
     * @param msg
     */
    public abstract void OnNewMessage(Message msg);

    /**
     * Invoked when connection status changes.
     *
     * @param status
     */
    public abstract void OnStatusUpdate(Status status);
  }
}
