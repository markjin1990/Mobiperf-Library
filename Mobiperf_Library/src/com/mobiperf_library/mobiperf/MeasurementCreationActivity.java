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
package com.mobiperf_library.mobiperf;

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

import com.mobiperf_library.MeasurementTask;
import com.mobiperf_library.R;
import com.mobiperf_library.api.API;
import com.mobiperf_library.exceptions.MeasurementError;
import com.mobiperf_library.util.Logger;
import com.mobiperf_library.util.MLabNS;

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
  private Console console;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.measurement_creation_main);

    assert (this.getParent().getClass().getName().compareTo("SpeedometerApp") == 0);
    this.parent = (SpeedometerApp) this.getParent();

    /* Initialize the measurement type spinner */
    Spinner spinner = (Spinner) findViewById(R.id.measurementTypeSpinner);
    spinnerValues = new ArrayAdapter<String>(this.getApplicationContext(), R.layout.spinner_layout);
    
    this.api = API.getAPI(parent, "new mobiperf");
    this.console = parent.getConsole();
    Logger.e("MeasurementCreationActivity: console is " + console);
    
    for (String name : API.getMeasurementNames()) {
      // adding list of visible measurements
      /**
       * TODO(Hongyi): add getVisibilityForMeasurementName in library
       */
//      if (MeasurementTask.getVisibilityForMeasurementName(name)) {
//        spinnerValues.add(name);
//      }
      spinnerValues.add(name);
    }
    spinnerValues.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(spinnerValues);
    spinner.setOnItemSelectedListener(new MeasurementTypeOnItemSelectedListener());
    spinner.requestFocus();
    /* Setup the 'run' button */
    Button runButton = (Button) this.findViewById(R.id.runTaskButton);
    runButton.setOnClickListener(new ButtonOnClickListener());

    this.measurementTypeUnderEdit = API.PING_TYPE;
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

  @Override
  protected void onStart() {
    super.onStart();
    this.populateMeasurementSpecificArea();
  }

  private void clearMeasurementSpecificViews(TableLayout table) {
    for (int i = NUMBER_OF_COMMON_VIEWS; i < table.getChildCount(); i++) {
      View v = table.getChildAt(i);
      v.setVisibility(View.GONE);
    }
  }

  /**
   * TODO(Hongyi): user should not directly use those measurement task here?
   */
  private void populateMeasurementSpecificArea() {
    TableLayout table = (TableLayout) this.findViewById(R.id.measurementCreationLayout);
    this.clearMeasurementSpecificViews(table);
    if (this.measurementTypeUnderEdit.compareTo(API.PING_TYPE) == 0) {
      this.findViewById(R.id.pingView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(API.HTTP_TYPE) == 0) {
      this.findViewById(R.id.httpUrlView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(API.TRACEROUTE_TYPE) == 0) {
      this.findViewById(R.id.tracerouteView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(API.DNSLOOKUP_TYPE) == 0) {
      this.findViewById(R.id.dnsTargetView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(API.UDPBURST_TYPE) == 0) {
      this.findViewById(R.id.UDPBurstDirView).setVisibility(View.VISIBLE);
    } else if (this.measurementTypeUnderEdit.compareTo(API.TCPTHROUGHPUT_TYPE) == 0) {
      this.findViewById(R.id.TCPThroughputDirView).setVisibility(View.VISIBLE);
    }
  }

  private void hideKyeboard(EditText textBox) {
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
      int measurementType = -1;
      try {
        if (measurementTypeUnderEdit.equals(API.PING_TYPE)) {
          EditText pingTargetText = (EditText) findViewById(R.id.pingTargetText);
          params.put("target", pingTargetText.getText().toString());
          taskTarget=pingTargetText.getText().toString();
          measurementType = API.Ping;
        } else if (measurementTypeUnderEdit.equals(API.HTTP_TYPE)) {
          EditText httpUrlText = (EditText) findViewById(R.id.httpUrlText);
          params.put("url", httpUrlText.getText().toString());
          params.put("method", "get");
          taskTarget=httpUrlText.getText().toString();
          measurementType = API.HTTP;
        } else if (measurementTypeUnderEdit.equals(API.TRACEROUTE_TYPE)) {
          EditText targetText = (EditText) findViewById(R.id.tracerouteTargetText);
          params.put("target", targetText.getText().toString());
          measurementType = API.Traceroute;
          taskTarget=targetText.getText().toString();
          showLengthWarning = true;
        } else if (measurementTypeUnderEdit.equals(API.DNSLOOKUP_TYPE)) {
          EditText dnsTargetText = (EditText) findViewById(R.id.dnsLookupText);
          params.put("target", dnsTargetText.getText().toString());
          taskTarget=dnsTargetText.getText().toString();
          measurementType = API.DNSLookup;
        } else if (measurementTypeUnderEdit.equals(API.UDPBURST_TYPE)) {
          // TODO(dominic): Support multiple servers for UDP. For now, just
          // m-lab.
          params.put("target", MLabNS.TARGET);
          params.put("direction", udpDir);
          taskTarget=udpDir;
          measurementType = API.UDPBurst;
        } else if (measurementTypeUnderEdit.equals(API.TCPTHROUGHPUT_TYPE)) {
            params.put("target", MLabNS.TARGET);
            params.put("dir_up", tcpDir);
            measurementType = API.TCPThroughput;
            taskTarget=tcpDir;
            showLengthWarning = true;
        }
        
        newTask = api.createTask(measurementType,
          Calendar.getInstance().getTime(),
          null,
          MobiperfConfig.DEFAULT_USER_MEASUREMENT_INTERVAL_SEC,
          MobiperfConfig.DEFAULT_USER_MEASUREMENT_COUNT,
          API.USER_PRIORITY,
          MobiperfConfig.DEFAULT_CONTEXT_INTERVAL,
          params);

        if (newTask != null) {
          try {
            api.addTask(newTask);
          } catch (MeasurementError e) {
            Logger.e(e.toString());
            Toast.makeText(MeasurementCreationActivity.this, R.string.userMeasurementFailureToast,
              Toast.LENGTH_LONG).show();
          }
          Logger.e("MeasurementCreationActivity@button click: console is " + console);
          
          
          console = parent.getConsole();
          if ( console != null ) {
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

        }
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
        hideKyeboard((EditText) v);
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
      /**
       * TODO(Hongyi): expose getTypeForMeasurementName in API
       */
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