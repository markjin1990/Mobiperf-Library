package com.mobiperf_library.mobiperf;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.myjson.reflect.TypeToken;
import com.mobiperf_library.util.MeasurementJsonConvertor;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Console {
	
	private ArrayList<String> userResults;
	private ArrayList<String> systemResults;
	private ArrayList<String> systemConsole;
	private NotificationManager notificationManager;
	private int completedMeasurementCnt = 0;
	private int failedMeasurementCnt = 0;


	/**
	   * Broadcast an intent to update the system status.
	   */
	  public void updateStatus() {//TODO called by updateFromPreference() , checkin,  call(), pause(), resume(): prints completedMeasurementCnt + " completed, " + failedMeasurementCnt ...
	    Intent intent = new Intent();
	    intent.setAction(MobiperfConfig.SYSTEM_STATUS_UPDATE_ACTION);
	    String statsMsg =
	        completedMeasurementCnt + " completed, " + failedMeasurementCnt
	            + " failed";
	    intent.putExtra(MobiperfConfig.STATS_MSG_PAYLOAD, statsMsg);
	    sendBroadcast(intent);
	  }
	
	/**
	 * Persist service state to prefs.
	 */
	private synchronized void persistState() {//TODO called by handleMeasurement(), cleanUp(),  checkin(),  call()
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
//				Logger.d("Read " + items.size() + " items from prefkey " + prefKey);
				for (String item : items) {
					insertStringToConsole(consoleContent, item);
				}
//				Logger.d("Restored " + consoleContent.size() + " entries to console "+ prefKey);
			}
		}
	}

	/**
	 * Adds a string to the corresponding console depending on whether the result is a user
	 * measurement or a system measurement
	 */
	private void updateResultsConsole(Intent intent) {//TODO, when a measurement is finished, this func should be called
		int priority =
				intent.getIntExtra(UpdateIntent.TASK_PRIORITY_PAYLOAD,MeasurementTask.INVALID_PRIORITY);
		String msg = intent.getStringExtra(UpdateIntent.STRING_PAYLOAD);
		if (msg == null) {
			// Pull out error string instead
			msg = intent.getStringExtra(UpdateIntent.ERROR_STRING_PAYLOAD);
		}
		if (msg != null) {
			if (priority == MeasurementTask.USER_PRIORITY) {
				insertStringToConsole(userResults, msg);
			} else if (priority != MeasurementTask.INVALID_PRIORITY) {
				insertStringToConsole(systemResults, msg);
			}
		}
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
	
	/**
	   * Broadcast an intent to update the system status.
	 */
	public void updateStatus() {//TODO  called by checkin,  call(), pause(), resume(), ...
	    Intent intent = new Intent();
	    intent.setAction(MobiperfConfig.SYSTEM_STATUS_UPDATE_ACTION);
	    String statsMsg =
	        completedMeasurementCnt + " completed, " + failedMeasurementCnt
	            + " failed";
	    intent.putExtra(MobiperfConfig.STATS_MSG_PAYLOAD, statsMsg);
	    sendBroadcast(intent);
	  }
	

}
