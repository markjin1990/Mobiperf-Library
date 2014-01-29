package com.mobiperf_library.mobiperf;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.myjson.reflect.TypeToken;
import com.mobiperf_library.MeasurementResult;
import com.mobiperf_library.R;
import com.mobiperf_library.UpdateIntent;
import com.mobiperf_library.util.Logger;
import com.mobiperf_library.util.MeasurementJsonConvertor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;

public final class Console extends Service{
  
  // This arbitrary id is private to Speedometer
  private static final int NOTIFICATION_ID = 1234;
  
  private BroadcastReceiver broadcastReceiver;
  private boolean stopRequested = false;
  private boolean isSchedulerStarted = false;
  
  private AlarmManager alarmManager;
  // Binder given to clients
  private final IBinder binder = new ConsoleBinder();
  
  private NotificationManager notificationManager;
  private int completedMeasurementCnt = 0;
  private int failedMeasurementCnt = 0;
  
  private ArrayList<String> userResults;
  private ArrayList<String> systemResults;
  private ArrayList<String> systemConsole;
  /**
   * The Binder class that returns an instance of running scheduler 
   */
  public class ConsoleBinder extends Binder {
    public Console getService() {
      return Console.this;
    }
  }

  /* Returns a IBinder that contains the instance of the Console object
   * @see android.app.Service#onBind(android.content.Intent)
   */
  @Override
  public IBinder onBind(Intent intent) {
    Logger.d("Service onBind called");
    return this.binder;
  }
  
  
  // Service objects are by nature singletons enforced by Android
  @Override
  public void onCreate() {
    Logger.d("Console onCreate called");
    
    this.stopRequested = false;
    this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    this.alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    
    restoreState();
    
    // Register activity specific BroadcastReceiver here    
    IntentFilter filter = new IntentFilter();
    filter.addAction(MobiperfIntent.PREFERENCE_ACTION);
    filter.addAction(MobiperfIntent.MSG_ACTION);
    filter.addAction(UpdateIntent.USER_RESULT_ACTION);
    filter.addAction(UpdateIntent.SERVER_RESULT_ACTION);
    
    broadcastReceiver = new BroadcastReceiver() {
      // Handles various broadcast intents.
      @Override
      public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(MobiperfIntent.PREFERENCE_ACTION)) {
          updateFromPreference();
        } else if (intent.getAction().equals(UpdateIntent.USER_RESULT_ACTION) ||
            intent.getAction().equals(UpdateIntent.SERVER_RESULT_ACTION)) {
          Logger.d("MeasurementIntent update intent received");
          Logger.e("MeasurementIntent update intent received");
          updateResultsConsole(intent);
        } else if (intent.getAction().equals(MobiperfIntent.MSG_ACTION)) {
          String msg = intent.getExtras().getString(MobiperfIntent.STRING_PAYLOAD);
          Date now = Calendar.getInstance().getTime();
          insertStringToConsole(systemConsole, now + "\n\n" + msg);
        }
      }
    };
    this.registerReceiver(broadcastReceiver, filter);
    // TODO(mdw): Make this a user-selectable option
    addIconToStatusBar();
    //startMobiperfInForeground();
  }
  
  /**
   * Add an icon to the device status bar.
   */
  private void addIconToStatusBar() {
    notificationManager.notify(NOTIFICATION_ID, createServiceRunningNotification());
  }

  /**
   * Remove the icon from the device status bar.
   */
  private void removeIconFromStatusBar() {
    notificationManager.cancel(NOTIFICATION_ID);
  }

  /**
   * Create notification that indicates the service is running.
   */ 
  private Notification createServiceRunningNotification() {
    //The intent to launch when the user clicks the expanded notification
    Intent intent = new Intent(this, SpeedometerApp.class);
    PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 
        PendingIntent.FLAG_CANCEL_CURRENT);

    //This constructor is deprecated in 3.x. But most phones still run 2.x systems
    Notification notice = new Notification(R.drawable.icon_statusbar,
        getString(R.string.notificationSchedulerStarted), System.currentTimeMillis());
    notice.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

    //This is deprecated in 3.x. But most phones still run 2.x systems
    notice.setLatestEventInfo(this, getString(R.string.app_name),
        getString(R.string.notificationServiceRunning), pendIntent);
    return notice;
  }
  
  
  @Override 
  public int onStartCommand(Intent intent, int flags, int startId)  {
    Logger.d("Service onStartCommand called, isSchedulerStarted = " + isSchedulerStarted);
    // Start up the thread running the service. Using one single thread for all requests
    Logger.i("starting console");
    sendStringMsg("Scheduler starting");
    if (!isSchedulerStarted) {
      restoreState();
      updateFromPreference();
//      this.resume();
      isSchedulerStarted = true;
    }
    return START_STICKY;
  }
  
  @Override
  public void onDestroy() {
    Logger.d("Service onDestroy called");
    super.onDestroy();
    cleanUp();
  }
  
  /** 
   * Prevents new tasks from being scheduled. Started task will still run to finish. 
   */
//  public synchronized void pause() {
//    Logger.d("Service pause called");
//    sendStringMsg("Scheduler pausing");
//    this.pauseRequested = true;
//    updateStatus();
//  }
  
//  /** Enables new tasks to be scheduled */
//  public synchronized void resume() {
//    Logger.d("Service resume called");
//    sendStringMsg("Scheduler resuming");
//    updateStatus(null); 
//  }
  
  /** Return whether new tasks can be scheduled */
//  public synchronized boolean isPauseRequested() {
//    return this.pauseRequested;
//  }
  
  
  @SuppressWarnings("unused")
  private void updateNotificationBar(String notificationMsg) {
    //The intent to launch when the user clicks the expanded notification
    Intent intent = new Intent(this, SpeedometerApp.class);
    PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 
        PendingIntent.FLAG_CANCEL_CURRENT);
    
    //This constructor is deprecated in 3.x. But most phones still run 2.x systems
    Notification notice = new Notification(R.drawable.icon_statusbar, 
        notificationMsg, System.currentTimeMillis());

    //This is deprecated in 3.x. But most phones still run 2.x systems
    notice.setLatestEventInfo(this, "Speedometer", notificationMsg, pendIntent);

    notificationManager.notify(NOTIFICATION_ID, notice);
  }

  /**
   * Write a string to the system console.
   */
  public void sendStringMsg(String str) {
    MobiperfIntent intent = new MobiperfIntent(str, MobiperfIntent.MSG_ACTION);
    this.sendBroadcast(intent);    
  }

  /** Request the scheduler to stop execution. */
  public synchronized void requestStop() {
    sendStringMsg("Scheduler stop requested");
    this.stopRequested = true;
    this.notifyAll();
    this.stopForeground(true);
    this.removeIconFromStatusBar();
    this.stopSelf();
  }
  
  private synchronized void cleanUp() {
    Logger.d("Service cleanUp called");

    this.isSchedulerStarted = false;
    this.unregisterReceiver(broadcastReceiver);
    persistState();
    this.notifyAll();
    
    removeIconFromStatusBar();

    Logger.i("Shut down all executors and stopping service");
  }
  
  
//  @SuppressWarnings("unused")
//  private synchronized boolean isStopRequested() {
//    return this.stopRequested;
//  }

  /**
   * Return a read-only list of the user results.
   */
  public synchronized List<String> getUserResults() {
    return Collections.unmodifiableList(userResults);
  }
  
  /**
   * Return a read-only list of the system results.
   */
  public synchronized List<String> getSystemResults() {
    return Collections.unmodifiableList(systemResults);
  }

  /**
   * Return a read-only list of the system console messages.
   */
  public synchronized List<String> getSystemConsole() {
    return Collections.unmodifiableList(systemConsole);
  }
  
  public void updateFromPreference() {
    Logger.d("Console updateFromPreference called");
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
        this);
    try {
//      powerManager.setBatteryThresh(Integer.parseInt(
//          prefs.getString(getString(R.string.batteryMinThresPrefKey),
//          String.valueOf(Config.DEFAULT_BATTERY_THRESH_PRECENT))));
//      
//      this.setCheckinInterval(Integer.parseInt(
//          prefs.getString(getString(R.string.checkinIntervalPrefKey),
//          String.valueOf(Config.DEFAULT_CHECKIN_INTERVAL_SEC / 3600))) * 3600);
      
      updateStatus(null);
      
//      Logger.i("Preference set from SharedPreference: " + 
//          "checkinInterval=" + checkinIntervalSec +
//          ", minBatThres= " + powerManager.getBatteryThresh());
    } catch (ClassCastException e) {
      Logger.e("exception when casting preference values", e);
    }
  }
  
  /**
   * Broadcast an intent to update the system status.
   */
  public void updateStatus(String statusMsg) {//TODO called by updateFromPreference() , checkin,  call(), pause(), resume(): prints completedMeasurementCnt + " completed, " + failedMeasurementCnt ...
    Intent intent = new Intent();
    intent.setAction(MobiperfIntent.SYSTEM_STATUS_UPDATE_ACTION);
    intent.putExtra(MobiperfIntent.STATUS_MSG_PAYLOAD, statusMsg);
    String statsMsg =
        completedMeasurementCnt + " completed, " + failedMeasurementCnt
        + " failed";
    intent.putExtra(MobiperfIntent.STATS_MSG_PAYLOAD, statsMsg);
    sendBroadcast(intent);
  }

  /**
   * Persist service state to prefs.
   */
  public synchronized void persistState() {//TODO called by handleMeasurement(), cleanUp(),  checkin(),  call()
    saveConsoleContent(systemResults, MobiperfConfig.PREF_KEY_SYSTEM_RESULTS);
    saveConsoleContent(userResults, MobiperfConfig.PREF_KEY_USER_RESULTS);
    saveConsoleContent(systemConsole, MobiperfConfig.PREF_KEY_SYSTEM_CONSOLE);
    saveStats();
  }

  /**
   * Restore service state from prefs.
   */
  private void restoreState() { //TODO called by onCreate and onStartCommand, so we need to do that when we are going to connect to scheduler
    initializeConsoles();
    restoreStats();
  }

  /**
   * Save measurement statistics to persistent storage.
   */
  private void saveStats() {//TODO Save measurement statistics (completedMeasurementCnt) to pref
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(MobiperfConfig.PREF_KEY_COMPLETED_MEASUREMENTS,
      completedMeasurementCnt);
    editor.putInt(MobiperfConfig.PREF_KEY_FAILED_MEASUREMENTS, failedMeasurementCnt);
    editor.commit();
  }

  /**
   * Restore measurement statistics from persistent storage.
   */
  private void restoreStats() {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    completedMeasurementCnt =
        prefs.getInt(MobiperfConfig.PREF_KEY_COMPLETED_MEASUREMENTS, 0);
    failedMeasurementCnt = prefs.getInt(MobiperfConfig.PREF_KEY_FAILED_MEASUREMENTS, 0);
  }


  /**
   * Persists the content of the console as a JSON string
   */
  private void saveConsoleContent(List<String> consoleContent, String prefKey) {//TODO save console content to pref

    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    SharedPreferences.Editor editor = prefs.edit();

    int length = consoleContent.size();

    ArrayList<String> items = new ArrayList<String>();
    // Since we use insertToConsole later on to restore the content, we have to store them
    // in the reverse order to maintain the same look
    for (int i = length - 1; i >= 0; i--) {
      items.add(consoleContent.get(i));
    }
    Type listType = new TypeToken<ArrayList<String>>() {}.getType();
    editor.putString(prefKey, MeasurementJsonConvertor.getGsonInstance()
      .toJson(items, listType));
    editor.commit();
  }

  /**
   * Restores the console content from the saved JSON string
   */
  private void initializeConsoles() {//TODO Called by restoreState()
    systemResults = new ArrayList<String>();
    restoreConsole(systemResults, MobiperfConfig.PREF_KEY_SYSTEM_RESULTS);
    if (systemResults.size() == 0) {
      insertStringToConsole(systemResults,
        "Automatically-scheduled measurement results will " + "appear here.");
    }

    userResults = new ArrayList<String>();
    restoreConsole(userResults, MobiperfConfig.PREF_KEY_USER_RESULTS);
    if (userResults.size() == 0) {
      insertStringToConsole(userResults,
          "Your measurement results will appear here.");
    }

    systemConsole = new ArrayList<String>();
    restoreConsole(systemConsole, MobiperfConfig.PREF_KEY_SYSTEM_CONSOLE);
  }

  /**
   * Restores content for consoleContent with the key prefKey.
   */
  private void restoreConsole(List<String> consoleContent, String prefKey) {//TODO called by initializeConsoles()
    Logger.d("Service restoreConsole for " + prefKey);
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    String savedConsole = prefs.getString(prefKey, null);
    if (savedConsole != null) {
      Type listType = new TypeToken<ArrayList<String>>() {}.getType();
      ArrayList<String> items =
          MeasurementJsonConvertor.getGsonInstance().fromJson(savedConsole,
            listType);
      if (items != null) {
        Logger.d("Read " + items.size() + " items from prefkey " + prefKey);
        for (String item : items) {
          insertStringToConsole(consoleContent, item);
        }
        Logger.d("Restored " + consoleContent.size() + " entries to console "+ prefKey);
      }
    }
  }

  /**
   * Adds a string to the corresponding console depending on whether the result is a user
   * measurement or a system measurement
   */
  public void updateResultsConsole(Intent intent) {//TODO, when a measurement is finished, this func should be called
    ArrayList<String> resultList = null;
    if ( intent.getAction().equals(UpdateIntent.USER_RESULT_ACTION)) {
      Logger.d("Get user result");
      resultList = userResults;
      Logger.e("Console-> user result is received.");
    }
    else if ( intent.getAction().equals(UpdateIntent.SERVER_RESULT_ACTION)) {
      Logger.d("Get server result");
      Logger.e("Console-> server result is received.");
      resultList = systemResults;
    }
    
    Parcelable[] parcels =
        intent.getParcelableArrayExtra(UpdateIntent.RESULT_PAYLOAD);
    MeasurementResult[] results = null;
    if ( parcels != null ) {
      results = new MeasurementResult[parcels.length];
      for ( int i = 0; i < results.length; i++ ) {
        results[i] = (MeasurementResult) parcels[i];
        if (results[i].isSucceed()) {
          this.completedMeasurementCnt++;
        }
        else {
          this.failedMeasurementCnt++;
        }
        insertStringToConsole(resultList, results[i].toString());
      }
    }
//    if (msg != null) {
//      if (priority == API.USER_PRIORITY) {
//        insertStringToConsole(userResults, msg);
//      } else if (priority != API.INVALID_PRIORITY) {
        /**
         * TODO(Hongyi): invalid priority not stands for server priority?
         *  Strange design...
         */
//        insertStringToConsole(systemResults, msg);
//      }
//    }
  }

  /**
   * Inserts a string into the console with the latest message on top.
   */
  private void insertStringToConsole(List<String> console, String msg) {//TODO called by updateResultsConsole()
    if (msg != null) {
      console.add(0, msg);
      if (console.size() > MobiperfConfig.MAX_LIST_ITEMS) {
        console.remove(console.size() - 1);
      }
    }
  }
}
