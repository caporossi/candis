package candis.client.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import candis.client.MainActivity;
import candis.client.R;
import candis.client.comm.CertAcceptRequestHandler;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 * Tries to connect to master.
 *
 * Returns null if failed.
 *
 * @author Enrico Joerns
 */
class ConnectTask extends AsyncTask<Object, Object, SecureConnection>
        implements CertAcceptRequestHandler {

  private static final String TAG = "ConnectTask";
  private static final int NOTIFCATION_ID = 4711;
  private static final int IOEXCEPTION = 0;
  private static final int TMLOADFAILED = 10;
  private final AtomicBoolean mCertCheckResult;
  private final NotificationManager mNotificationManager;
  private final Context mContext;
  private final File mTSFile;
  private final Messenger mMessenger;

  public ConnectTask(
          final AtomicBoolean bool,
          final Context context,
          final File ts,
          final Messenger messenger) {
    mCertCheckResult = bool;
    mContext = context;
    mTSFile = ts;
    mMessenger = messenger;
    mNotificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  @Override
  protected void onPreExecute() {
    System.out.println("onPreExecute()");
    Toast.makeText(mContext.getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
    System.out.println("onPreExecute() done");
  }

  @Override
  protected SecureConnection doInBackground(Object... params) {
    final X509TrustManager trustmanager;

    // try to load trustmanager
    try {
      trustmanager = new ReloadableX509TrustManager(mTSFile, this);
    }
    catch (Exception ex) {
      Log.e(TAG, ex.toString());
      publishProgress(TMLOADFAILED);
      return null;
    }

    // try to connect
    SecureConnection sc = new SecureConnection(trustmanager);
    try {
      sc.connect((String) params[0], ((Integer) params[1]).intValue());
    }
    catch (IOException ex) {
      publishProgress(IOEXCEPTION, params[0], params[1]);
      Log.e(TAG, ex.toString());
      return null;
    }

    return sc;
  }

  /**
   * Shows Notifications.
   */
  @Override
  protected void onProgressUpdate(Object... arg) {
    // mId allows you to update the notification later on.
    Notification noti;
    PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                                                            new Intent(mContext, MainActivity.class), 0);
//    NotificationCompat.Builder notiBuild = new NotificationCompat.Builder(mContext);
//    notiBuild.
    switch ((Integer) arg[0]) {
      case IOEXCEPTION:
        System.out.println("onProgressUpdate: IOException");
        noti = new NotificationCompat.Builder(mContext)
                .setContentTitle("Server not found")
                .setContentText(String.format("Connection to %s:%d failed", (String) arg[1], ((Integer) arg[2]).intValue()))
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher))
                .setContentIntent(contentIntent)
                .build();
        break;
      case TMLOADFAILED:
        System.out.println("onProgressUpdate: IOException");
        noti = new NotificationCompat.Builder(mContext)
                .setContentTitle("Trustmanager Error")
                .setContentText(String.format("Loading Trustmanager failed", (String) arg[1], ((Integer) arg[2]).intValue()))
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher))
                .build();
        break;
      default:
        System.out.println("onProgressUpdate: default");
        noti = new NotificationCompat.Builder(mContext)
                .setContentTitle("Notification")
                .setContentText("default")
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher))
                .build();
        break;
    }
    mNotificationManager.notify(NOTIFCATION_ID, noti);
  }

  @Override
  protected void onPostExecute(SecureConnection args) {
    if (args != null) {
      Log.i(TAG, "CONNECTED!!!!");
      Toast.makeText(mContext, "connected", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public boolean userCheckAccept(X509Certificate cert) {
    try {
      Bundle myBundle = new Bundle();
      myBundle.putSerializable("cert", cert);
      Message sendMessage = Message.obtain(null, BackgroundService.CHECK_SERVERCERT);
      sendMessage.setData(myBundle);
      Log.e(TAG, "Sending Message: " + sendMessage.toString());
      mMessenger.send(sendMessage);
    }
    catch (RemoteException ex) {
      Log.e(TAG, ex.getMessage());
      return false;
    }

    synchronized (mCertCheckResult) {
      try {
        System.out.println("cert_check_result.wait()");
        mCertCheckResult.wait();
        System.out.println("cert_check_result.wait() done");
      }
      catch (InterruptedException ex) {
        Log.e(TAG, ex.toString());
      }
    }
    return mCertCheckResult.get();
  }
}
