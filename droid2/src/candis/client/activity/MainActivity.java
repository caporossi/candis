package candis.client.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import candis.client.R;
import candis.client.service.BackgroundService;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final int EDIT_ID = Menu.FIRST + 2;
  private SharedPreferences mSharedPref;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // loader shared preferences
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    // TODO: check for initial call
//    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
//    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences2, false);
    mSharedPref.registerOnSharedPreferenceChangeListener(this);

    if (mSharedPref.getBoolean("pref_key_run_service", false)) {
      Log.i("foo", "Starting service..");
      startService(new Intent(this, BackgroundService.class));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, EDIT_ID, Menu.NONE, "Edit Prefs")
            .setIcon(R.drawable.action_settings)
            .setAlphabeticShortcut('e');

    return (super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case EDIT_ID:
        // use fallback version of preference activity if OS is too old
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
          startActivity(new Intent(this, EditPreferences.class));
        }
        else {
          startActivity(new Intent(this, EditPreferencesHC.class));
        }

        return (true);
    }

    return (super.onOptionsItemSelected(item));
  }

  /**
   * Tests if the background service ist running.
   *
   * @return true if it is running, false otherwise
   */
  private boolean isBackgroundServiceRunning() {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (BackgroundService.class.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals("pref_key_run_service")) {
      if (sharedPreferences.getBoolean("pref_key_run_service", false)) {
        startService(new Intent(this, BackgroundService.class));
//        doBindService();        // start service
      }
      else {
        // stop service
//        doUnbindService();
        stopService(new Intent(this, BackgroundService.class));
      }
    }
  }
}
