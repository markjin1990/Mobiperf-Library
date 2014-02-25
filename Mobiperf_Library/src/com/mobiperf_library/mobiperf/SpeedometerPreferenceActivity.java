/* Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiperf_library.mobiperf;

import com.mobilyzer.AccountSelector;
import com.mobilyzer.MeasurementScheduler.DataUsageProfile;
import com.mobilyzer.UpdateIntent;
import com.mobilyzer.api.API;
import com.mobilyzer.exceptions.MeasurementError;
import com.mobiperf_library.R;
import com.mobiperf_library.util.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Activity that handles user preferences
 */
public class SpeedometerPreferenceActivity extends PreferenceActivity {
  private API api;
  private BroadcastReceiver receiver;
  private EditTextPreference intervalPref;
  private EditTextPreference batteryPref;
  private ListPreference accountPref;
  private ListPreference dataLimitPref;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preference);

    // Hongyi: get API singleton object
    this.api = API.getAPI(this, MobiperfConfig.CLIENT_KEY);

    this.intervalPref = (EditTextPreference)findPreference(getString(R.string.checkinIntervalPrefKey));
    this.batteryPref = (EditTextPreference)findPreference(getString(R.string.batteryMinThresPrefKey));
    this.accountPref = (ListPreference)findPreference(getString(R.string.accountPrefKey));
    this.dataLimitPref = (ListPreference)findPreference(getString(R.string.dataLimitPrefKey));
    /* This should never occur. */
    if (intervalPref == null || batteryPref == null || accountPref == null
        || dataLimitPref == null) {
      Logger.w("Cannot find some of the preferences");
      Toast.makeText(SpeedometerPreferenceActivity.this, 
        getString(R.string.menuInitializationExceptionToast), Toast.LENGTH_LONG).show();
      return;
    }
    
    // Setting data limit
    final String[] dataLimitName = 
      { "PROFILE 1", "PROFILE 2", "PROFILE 3", "PROFILE 4", "UNLIMITED" };
    final String[] dataLimitProfile = 
      { "PROFILE1", "PROFILE2", "PROFILE3", "PROFILE4", "UNLIMITED" };
    dataLimitPref.setEntries(dataLimitName);
    dataLimitPref.setEntryValues(dataLimitProfile);

    
    IntentFilter filter = new IntentFilter();
    filter.addAction(api.batteryThresholdAction);
    filter.addAction(api.checkinIntervalAction);
    filter.addAction(api.dataUsageAction);
    this.receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ( action.equals(api.batteryThresholdAction) ) {
          int threshold = intent.getIntExtra(UpdateIntent.BATTERY_THRESHOLD_PAYLOAD, -1);
          if ( threshold != -1 ) {
            Logger.i("Current battery threshold " + threshold);
            batteryPref.setText(String.valueOf(threshold));
          }
          else {
            Logger.e("No battery threshold found");
          }
        }
        else if (action.equals(api.checkinIntervalAction)) {
          long interval = intent.getLongExtra(UpdateIntent.CHECKIN_INTERVAL_PAYLOAD, -1);
          if ( interval != -1 ) {
            Logger.i("Current checkin interval " + interval);
            intervalPref.setText(String.valueOf(interval / 3600));
          }
          else {
            Logger.e("No checkin interval found");
          }
        }
        else if (action.equals(api.dataUsageAction)) {
          DataUsageProfile profile = (DataUsageProfile)
              intent.getSerializableExtra(UpdateIntent.DATA_USAGE_PAYLOAD);
          if ( profile != null ) {
            Logger.i("Current data usage profile " + profile);
            dataLimitPref.setValue(profile.toString());
          }
          else {
            Logger.e("No data usage profile found");
          }
        }
        else {
          Logger.e("Preference: received unknown action " + action);
        }
      }
    };
    this.registerReceiver(receiver, filter);
    
    // Hongyi: Update parameter values in SharedPreference 
    try {
      api.getBatteryThreshold();
      api.getCheckinInterval();
      api.getDataUsage();
    } catch (MeasurementError e) {
      Logger.e("Error initialize scheduler properties: " + e.getMessage());
    }
    
    /**
     * Setting checkin interval, battery threshold
     */

    OnPreferenceChangeListener prefChangeListener = new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String prefKey = preference.getKey();
        if (prefKey.compareTo(getString(R.string.checkinIntervalPrefKey)) == 0) {
          try {
            Integer val = Integer.parseInt((String) newValue);
            if (val <= 0 || val > 24) {
              Toast.makeText(SpeedometerPreferenceActivity.this,
                  getString(R.string.invalidCheckinIntervalToast), Toast.LENGTH_LONG).show();
              return false;
            }
            else {
              // checkin interval's granularity is second level
              api.setCheckinInterval(val * 3600);  
              // Update data usage profile in SharedPreference 
              api.getCheckinInterval();
              return true;
              
            }
          } catch (ClassCastException e) {
            Logger.e("Cannot cast checkin interval preference value to Integer");
            return false;
          } catch (NumberFormatException e) {
            Logger.e("Cannot cast checkin interval preference value to Integer");
            return false;
          } catch (MeasurementError e) {
            Logger.e("Error in setting checkin interval: " + e.getMessage());
            return false;
          }
        } else if (prefKey.compareTo(getString(R.string.batteryMinThresPrefKey)) == 0) {
          try {
            Integer val = Integer.parseInt((String) newValue);
            if (val < 0 || val > 100) {
              Toast.makeText(SpeedometerPreferenceActivity.this,
                  getString(R.string.invalidBatteryToast), Toast.LENGTH_LONG).show();
              return false;
            }
            else {
              api.setBatteryThreshold(val);
              // Update data usage profile in SharedPreference
              api.getBatteryThreshold();
              return true;
            }
          } catch (ClassCastException e) {
            Logger.e("Cannot cast battery preference value to Integer");
            return false;
          } catch (NumberFormatException e) {
            Logger.e("Cannot cast battery preference value to Integer");
            return false;
          } catch (MeasurementError e) {
            Logger.e("Error in setting battery threshold: " + e.getMessage());
            return false;
          }
        }
        return true;
      }
    };
    intervalPref.setOnPreferenceChangeListener(prefChangeListener);
    batteryPref.setOnPreferenceChangeListener(prefChangeListener);
    
    /**
     * Setting authentication account 
     */
    final CharSequence[] items = AccountSelector.getAccountList(getApplicationContext());
    accountPref.setEntries(items);
    accountPref.setEntryValues(items);
   
    // Restore current settings.
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    String selectedAccount = prefs.getString(MobiperfConfig.PREF_KEY_SELECTED_ACCOUNT, null);
    if (selectedAccount != null) {
      accountPref.setValue(selectedAccount);
    }
    
    accountPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String account = newValue.toString();
        Logger.i("account selected is: " + account);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(MobiperfConfig.PREF_KEY_SELECTED_ACCOUNT, account);
        editor.commit();
        return true;
      }
    });
    
    dataLimitPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String dataLimit = newValue.toString();
        Logger.i("data limit is: " + dataLimit);
        try {
          api.setDataUsage(DataUsageProfile.valueOf(dataLimit));
          // Update data usage profile in SharedPreference
          api.getDataUsage();
        } catch (MeasurementError e) {
          Logger.e("Error setting data limit: " + e.getMessage());
          return false;
        }
        return true;
      }
    });
  }
  
  /** 
   * As we leave the settings page, changes should be reflected in various applicable components
   * */
  @Override
  protected void onDestroy() {
    super.onDestroy();
    this.unregisterReceiver(receiver);
  }
}
