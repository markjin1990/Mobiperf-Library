/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.mobiperf;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.Toast;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.mobilyzer.MeasurementTask;
import com.mobiperf.util.Logger;
import com.mobiperf.R;
import com.mobilyzer.api.API;
import com.mobilyzer.api.API.TaskType;
import com.mobilyzer.exceptions.MeasurementError;
import com.mobilyzer.measurements.DnsLookupTask;
import com.mobilyzer.measurements.HttpTask;
import com.mobilyzer.measurements.PingTask;
import com.mobilyzer.measurements.TCPThroughputTask;
import com.mobilyzer.measurements.TracerouteTask;
import com.mobilyzer.measurements.UDPBurstTask;
import com.mobilyzer.util.MLabNS;

/**
 * The UI Activity that allows users to create their own measurements
 */
public class MeasurementCreationActivity extends Activity {

  private static final int NUMBER_OF_COMMON_VIEWS = 1;
  public static final String TAB_TAG = "MEASUREMENT_CREATION";

  private SpeedometerApp parent;
  private String measurementTypeUnderEdit;
  private ArrayAdapter<String> spinnerValues;
  private String udpDir;
  private String tcpDir;

  private API api;
//  private Console console;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.measurement_creation_main);

    assert (this.getParent().getClass().getName().compareTo("SpeedometerApp") == 0);
    this.parent = (SpeedometerApp) this.getParent();

    /* Initialize the measurement type spinner */
    Spinner spinner = (Spinner) findViewById(R.id.measurementTypeSpinner);
    spinnerValues = new ArrayAdapter<String>(this.getApplicationContext(), R.layout.spinner_layout);
    
//    spinnerValues.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
    
    this.api = API.getAPI(parent, MobiperfConfig.CLIENT_KEY);
//    this.console = parent.getConsole();

    // adding list of visible measurements
    for (String name : API.getMeasurementNames()) {
      /**
       *  TODO(Hongyi): Avoid keyboard popup problem.
       */
      if (name.equals(TCPThroughputTask.DESCRIPTOR)) {
        spinnerValues.insert(name, 0);
      }
      else {
        spinnerValues.add(name);
      }
    }
//    spinnerValues.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerValues.setDropDownViewResource(R.layout.spinner_dropdown_item);
    spinner.setAdapter(spinnerValues);
    spinner.setOnItemSelectedListener(new MeasurementTypeOnItemSelectedListener());
    spinner.requestFocus();
    /* Setup the 'run' button */
    Button runButton = (Button) this.findViewById(R.id.runTaskButton);
    runButton.setOnClickListener(new ButtonOnClickListener());
    
    setupEditTextFocusChangeListener();

    this.udpDir = "Up";
    this.tcpDir = "Up";
    
    final RadioButton radioUDPUp = (RadioButton) findViewById(R.id.UDPBurstUpButton);
    final RadioButton radioUDPDown = (RadioButton) findViewById(R.id.UDPBurstDownButton);
    final RadioButton radioTCPUp = (RadioButton) findViewById(R.id.TCPThroughputUpButton);
    final RadioButton radioTCPDown = (RadioButton) findViewById(R.id.TCPThroughputDownButton);
    
    radioUDPUp.setChecked(true);
    radioUDPUp.setOnClickListener(new UDPRadioOnClickListener());
    radioUDPDown.setOnClickListener(new UDPRadioOnClickListener());
    Button udpSettings = (Button)findViewById(R.id.UDPSettingsButton);
    udpSettings.setOnClickListener(new UDPSettingsOnClickListener());
    
    radioTCPUp.setChecked(true);
    radioTCPUp.setOnClickListener(new TCPRadioOnClickListener());
    radioTCPDown.setOnClickListener(new TCPRadioOnClickListener());
  }

  private void setupEditTextFocusChangeListener() {
    EditBoxFocusChangeListener textFocusChangeListener = new EditBoxFocusChangeListener();
    EditText text = (EditText) findViewById(R.id.pingTargetText);
    text.setOnFocusChangeListener(textFocusChangeListener);
    text = (EditText) findViewById(R.id.tracerouteTargetText);
    text.setOnFocusChangeListener(textFocusChangeListener);
    text = (EditText) findViewById(R.id.httpUrlText);
    text.setOnFocusChangeListener(textFocusChangeListener);
    text = (EditText) findViewById(R.id.dnsLookupText);
    text.setOnFocusChangeListener(textFocusChangeListener);
  }

  private void clearMeasurementSpecificViews(TableLayout table) {
    for (int i = NUMBER_OF_COMMON_VIEWS; i < table.getChildCount(); i++) {
      View v = table.getChildAt(i);
      v.setVisibility(View.GONE);
    }
  }

  private void populateMeasurementSpecificArea() {
    Button runButton = (Button) this.findViewById(R.id.runTaskButton);
    runButton.setOnClickListener(new ButtonOnClickListener());
    TableLayout table = (TableLayout) this.findViewById(R.id.measurementCreationLayout);
    this.clearMeasurementSpecificViews(table);
    if (this.measurementTypeUnderEdit.compareTo(PingTask.TYPE) == 0) {
      this.findViewById(R.id.pingView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(HttpTask.TYPE) == 0) {
      this.findViewById(R.id.httpUrlView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(TracerouteTask.TYPE) == 0) {
      this.findViewById(R.id.tracerouteView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(DnsLookupTask.TYPE) == 0) {
      this.findViewById(R.id.dnsTargetView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(UDPBurstTask.TYPE) == 0) {
      this.findViewById(R.id.UDPBurstDirView).setVisibility(View.VISIBLE);
      this.findViewById(R.id.UDPSettingsButton).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(TCPThroughputTask.TYPE) == 0) {
      this.findViewById(R.id.TCPThroughputDirView).setVisibility(View.VISIBLE);
    }
  }
  

  private void hideKeyboard(EditText textBox) {
    if (textBox != null) {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(textBox.getWindowToken(), 0);
    }
  }

  private class UDPRadioOnClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      RadioButton rb = (RadioButton) v;
      MeasurementCreationActivity.this.udpDir = (String) rb.getText();
    }
  }
  
  private class UDPSettingsOnClickListener implements OnClickListener {
    private boolean isShowSettings = false;
    @Override
    public void onClick(View v) {
      Button b = (Button)v;
      if ( isShowSettings == false ) {
        isShowSettings = true;
        b.setText("Collapse Advanced Settings");
        findViewById(R.id.UDPBurstPacketSizeView).setVisibility(View.VISIBLE);
        findViewById(R.id.UDPBurstPacketCountView).setVisibility(View.VISIBLE);
        findViewById(R.id.UDPBurstIntervalView).setVisibility(View.VISIBLE);
      }
      else {
        isShowSettings = false;
        b.setText("Expand Advanced Settings");
        findViewById(R.id.UDPBurstPacketSizeView).setVisibility(View.GONE);
        findViewById(R.id.UDPBurstPacketCountView).setVisibility(View.GONE);
        findViewById(R.id.UDPBurstIntervalView).setVisibility(View.GONE);
      }
    }
  }
  
  private class TCPRadioOnClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      RadioButton rb = (RadioButton) v;
      MeasurementCreationActivity.this.tcpDir = (String) rb.getText();
    }
  }
  
  private class ButtonOnClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      MeasurementTask newTask = null;
      boolean showLengthWarning = false;
      Map<String, String> params = new HashMap<String, String>();
      String taskTarget="";
      TaskType measurementType = TaskType.INVALID;
      try {
        if (measurementTypeUnderEdit.equals(PingTask.TYPE)) {
          EditText pingTargetText = (EditText) findViewById(R.id.pingTargetText);
          params.put("target", pingTargetText.getText().toString());
          taskTarget=pingTargetText.getText().toString();
          measurementType = TaskType.PING;
        } else if (measurementTypeUnderEdit.equals(HttpTask.TYPE)) {
          EditText httpUrlText = (EditText) findViewById(R.id.httpUrlText);
          params.put("url", httpUrlText.getText().toString());
          params.put("method", "get");
          taskTarget=httpUrlText.getText().toString();
          measurementType = TaskType.HTTP;
        } else if (measurementTypeUnderEdit.equals(TracerouteTask.TYPE)) {
          EditText targetText = (EditText) findViewById(R.id.tracerouteTargetText);
          params.put("target", targetText.getText().toString());
          measurementType = TaskType.TRACEROUTE;
          taskTarget=targetText.getText().toString();
          showLengthWarning = true;
        } else if (measurementTypeUnderEdit.equals(DnsLookupTask.TYPE)) {
          EditText dnsTargetText = (EditText) findViewById(R.id.dnsLookupText);
          params.put("target", dnsTargetText.getText().toString());
          taskTarget=dnsTargetText.getText().toString();
          measurementType = TaskType.DNSLOOKUP;
        } else if (measurementTypeUnderEdit.equals(UDPBurstTask.TYPE)) {
          // TODO(dominic): Support multiple servers for UDP. For now, just
          // m-lab.
          params.put("target", MLabNS.TARGET);
          params.put("direction", udpDir);
          // Get UDP Burst packet size
          EditText UDPBurstPacketSizeText = 
              (EditText) findViewById(R.id.UDPBurstPacketSizeText);
          params.put("packet_size_byte"
            , UDPBurstPacketSizeText.getText().toString());
          // Get UDP Burst packet count
          EditText UDPBurstPacketCountText = 
              (EditText) findViewById(R.id.UDPBurstPacketCountText);
          params.put("packet_burst"
            , UDPBurstPacketCountText.getText().toString());
          // Get UDP Burst interval
          EditText UDPBurstIntervalText = 
              (EditText) findViewById(R.id.UDPBurstIntervalText);
          params.put("udp_interval"
            , UDPBurstIntervalText.getText().toString());
          taskTarget=udpDir;
          measurementType = TaskType.UDPBURST;
        } else if (measurementTypeUnderEdit.equals(TCPThroughputTask.TYPE)) {
            params.put("target", MLabNS.TARGET);
            params.put("dir_up", tcpDir);
            measurementType = TaskType.TCPTHROUGHPUT;
            taskTarget=tcpDir;
            showLengthWarning = true;
        }
        

        try {
          newTask = api.createTask(measurementType,
            Calendar.getInstance().getTime(),
            null,
            MobiperfConfig.DEFAULT_USER_MEASUREMENT_INTERVAL_SEC,
            MobiperfConfig.DEFAULT_USER_MEASUREMENT_COUNT,
            API.USER_PRIORITY,
            MobiperfConfig.DEFAULT_CONTEXT_INTERVAL,
            params);
          if (newTask != null) {
            api.submitTask(newTask);
          }
        } catch (MeasurementError e) {
          Logger.e(e.toString());
          Toast.makeText(MeasurementCreationActivity.this, R.string.userMeasurementFailureToast,
            Toast.LENGTH_LONG).show();
        }


        Console console = parent.getConsole();
        if ( console != null ) {
          Logger.e("MeasurementCreationActivity@button click: console is " + console);
          console.updateStatus("User task " + newTask.getDescriptor()
            + " is submitted to scheduler");
          console.addUserTask(newTask.getTaskId(), newTask.getMeasurementType()+','+taskTarget);
          console.persistState();
        }


        SpeedometerApp parent = (SpeedometerApp) getParent();
        TabHost tabHost = parent.getTabHost();
        tabHost.setCurrentTabByTag(ResultsConsoleActivity.TAB_TAG);
        String toastStr =
            MeasurementCreationActivity.this.getString(R.string.userMeasurementSuccessToast);
        if (showLengthWarning) {
          toastStr += newTask.getDescriptor() + " measurements can be long. Please be patient.";
        }
        Toast.makeText(MeasurementCreationActivity.this, toastStr, Toast.LENGTH_LONG).show();

      } catch (InvalidParameterException e) {
        Logger.e("InvalidParameterException when creating user measurements", e);
        Toast.makeText(MeasurementCreationActivity.this,
                       R.string.invalidParameterExceptionMeasurementToast +
                       ": " + e.getMessage(),
                       Toast.LENGTH_LONG).show();
      }
    }

  }


  private class EditBoxFocusChangeListener implements OnFocusChangeListener {

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
      if (!hasFocus) {
        hideKeyboard((EditText) v);
      }
    }
  }

  private class MeasurementTypeOnItemSelectedListener implements OnItemSelectedListener {

    /*
     * Handles the ItemSelected event in the MeasurementType spinner. Populate the measurement
     * specific area based on user input
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
      measurementTypeUnderEdit =
          API.getTypeForMeasurementName(spinnerValues.getItem((int) id));
      if (measurementTypeUnderEdit != null) {
        populateMeasurementSpecificArea();
      }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
      // TODO(Wenjie): at the moment there is nothing we need to do here
    }
  }


}