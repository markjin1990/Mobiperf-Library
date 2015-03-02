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
import com.mobilyzer.measurements.VideoQoETask;
import com.mobilyzer.util.MLabNS;
import com.mobilyzer.util.video.util.DemoUtil;

/**
 * The UI Activity that allows users to create their own measurements
 */
public class MeasurementCreationActivity extends Activity {

  private static final int NUMBER_OF_COMMON_VIEWS = 1;
  public static final String TAB_TAG = "MEASUREMENT_CREATION";
  public static final String[] testVideoLists = {
//    "iNYLhPdCCYY",//    The Suns Congratulate a Fan That Hit a $77,777 Halfcourt Shot!  60
//    "iVAgTiBrrDA", //The Hobbit: The Battle of the Five Armies - Official Main Trailer
//    "Kwwl9jiJ1A4", //"Take Back the Night" - A Minecraft Original Music Video   391
//    "IGJ2jMZ-gaI",//    Casting a Fire Ant Colony with Molten Aluminum (Cast #043)    166
//    "JLRSHzxIotY",//    Did A Meteorite Finally Reveal Life On Mars?    182
//    "MeKKHxcJfh0",//    Way of a Warrant    205
    "n_-E-D25XkU",//    HAUNTED BY A GHOST (Garry's Mod Hide and Seek)  195
//    "n6dHpFayYPc",//    Sex Nurses Are The Answer To A Problem You Might Not Know About 385
    "nvs7QTK_Iqo",//    How Neymar Jr prepares for a game   201
//    "erPNRUjwg_g",//    Cher Lloyd - Want U Back (MattyBRaps Cover) 162
//    "eWeBTdroGv4",//    Minecraft CRAZY CRAFT 7 - LETS FIND A GIRLFRIEND(Minecraft Mod Survival)    1273
//    "5F-nSajuG4k",//    Minecraft: FLYING WITCH AIRSHIP!!! - Attack of the B-Team Ep. 51 (HD)   1740
    "-gofC8c0Zh8",//    Minecraft | Attack of the B-Team | E14 "Flim No-Flam Realty!"   1661
    "PqWTyQhaC9U",//    Earth Hour 2014 B-Roll 2 Australia, China, Japan, South Korea   42
    "HS2lmvWbna4",//    Minecraft: I'VE BEEN PRANKED!!! - Attack of the B-Team Ep. 49 (HD)  1404
//    "vVwIH7C3mXY",//    Dear Notch: Stop Crying! Signed Cliffy B. and I ;-) 397
    "_n9NtLM7UN4",//    GOAT SIMULATOR?!?! Ep 01 - "Dis Gon B Gud!!!"   1380
//    "l0yLOEAXJO4",//    Perm It Up- Rihanna (Pour It Up Parody) 329
//    "iQ2r8lXlrgs",//  Funny cats in water, EPIC   199
//    "mEh7tA5xAXc",//    [Hát với TÙNG TÔM] Em của ngày AutoTune - MTP - Hội B-CRAFT™ Vietnam    162
//    "dh8f6IvOKYs",//    Minecraft: ATTACKING THE B-TEAM (Mineplex Super Smash Mobs) 768
//    "l5pbrj0W1BQ",//    Minecraft - Attack of the B-Team! Breakfast Post-Battle Chat with Skyzm - E19   1031
//    "tmzc9RiL-qI",//    Shakira trouve que Cauet est son âme soeur ! - C'Cauet sur NRJ 298
    "ZttE-pH_F_Q",//    C'mon Hautelook!!! - March 28, 2014 - itsjudyslife vlog 806
    "q5Uryt7t5lU",//    LA CASITA DE LA MUERTE!! c/ Alex, Luzu y Vegetta | Garry`s Mod The Murder #16   307
    "Z-ClU2l--d8",//    Mercedes-Benz C Сlass 2014 Тест-драйв.Anton Avtoman.    1397
//    "LrhZ0BL6vrw",//    A Liga dos Lendários 2 - MINI-TORNEIO TROLL (c/ Miss, M4ster e Jvnq) - #6 - Pixelmon Minecraft  1333
    "4eWllg5jRJk",//    The Walking Craft - A MEGA ALIANÇA! :O (c/ Pac, Mike e Jvnq) - #3 - Minecraft   1632
    "yk7uXQ7u6-k",//    Simply K-Pop - Ep107C11 C-Clown - Justice   210
//    "Zd6cQMclpRw",//    C-12 - 揀一個死法 Choose a way to Die (Official Music Video) 235
//    "f_ql-ZBJ8uI",//    GAMER GETS TROLL'D! 360
    "_6eotOZ7fQw",//    Miley Cyrus gets Punk'd by Justin Bieber - FULL CLIP    242
//    "PPsPsLQHz8s",//    COD GHOSTS Prestige 5 (Ali-A) - Classes, K/D Stats & Tips! - (Call of Duty: Ghost Multiplayer)  434
    "3N6X-Thl1sw",//    How to Spray on 3-D Makeup | Airbrush Makeup    423
    "3ud0V4FLzOk",//    Bajheera - "Well That Escalated Quickly" - Hunter gets R-E-K-T, REKT on EU :D   105
    "eOnfDb8RxXU",//    Deadpool vs Gentleman - A PSY Parody    221
    "haTjlJCsdtQ",//    『頭文字D パーフェクトシフト ONLINE』紹介動画 234
    "gpnUmMcngYM",//    Paul George Mic'd Up During Dunk on LeBron James    33
//    "UInlLyWEFqY",//    Battlefield 4 Golf D'Oman 2014 Ruée Bonus Tempête De Sable  870
//    "PV2b5Kw7akw",//    SORPRESÓN DE LOS BUENOS =D | Josemi 235
    "Lx9uyLmifsw",//    Clã e Garoto Propaganda!    549
    
//    CAGAR E TOMAR BANHO 1170
//    SOUTH PARK THE STICK OF TRUTH #12 - ESPECIAL: Humanos e Elfos!  1529
//    MALHAÇÃO DE NERD! - 2048 e Nerdy Workout (iPad) 407
//    Minecraft Survival Ep.66 - Kraken e Prancha Voadora !!  962
//    WWE Main Event 3/25/14 Results: Big E vs. Christian 404
//    La prova del cuoco - Polpo rosticciato con asparagina e patate  422
//    E-girls / 「ごめんなさいのKissing You」 ～Short ver.～ 447
//    Обзор Asus Padfone E: смартфон + планшет    382
//    Novo híbrido LG SlidePad 2 está mais fino e potente [LG Digital Experience 2014] - Tecmundo 161
  };

  private SpeedometerApp parent;
  private String measurementTypeUnderEdit;
  private ArrayAdapter<String> spinnerValues;
  private String udpDir;
  private String tcpDir;
  private String videoAlgorithm;

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
      if (name.equals(VideoQoETask.DESCRIPTOR)) {
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
    this.videoAlgorithm = "CBA";
    
    final RadioButton radioUDPUp = (RadioButton) findViewById(R.id.UDPBurstUpButton);
    final RadioButton radioUDPDown = (RadioButton) findViewById(R.id.UDPBurstDownButton);
    final RadioButton radioTCPUp = (RadioButton) findViewById(R.id.TCPThroughputUpButton);
    final RadioButton radioTCPDown = (RadioButton) findViewById(R.id.TCPThroughputDownButton);
    final RadioButton radioVideoCBA = (RadioButton) findViewById(R.id.VideoQoECBA);
    final RadioButton radioVideoBBA = (RadioButton) findViewById(R.id.VideoQoEBBA);
    final RadioButton radioVideoProgressive = (RadioButton) findViewById(R.id.VideoQoEProgressive);
    
    radioUDPUp.setChecked(true);
    radioUDPUp.setOnClickListener(new UDPRadioOnClickListener());
    radioUDPDown.setOnClickListener(new UDPRadioOnClickListener());
    Button udpSettings = (Button)findViewById(R.id.UDPSettingsButton);
    udpSettings.setOnClickListener(new UDPSettingsOnClickListener());
    
    radioTCPUp.setChecked(true);
    radioTCPUp.setOnClickListener(new TCPRadioOnClickListener());
    radioTCPDown.setOnClickListener(new TCPRadioOnClickListener());
    
    radioVideoCBA.setChecked(true);
    radioVideoCBA.setOnClickListener(new VideoRadioOnClickListener());
    radioVideoBBA.setOnClickListener(new VideoRadioOnClickListener());
    radioVideoProgressive.setOnClickListener(new VideoRadioOnClickListener());
    
    // Set test videos button
    Button runTestVideosButton = (Button) this.findViewById(R.id.runTestVideosButton);
    runTestVideosButton.setOnClickListener(new RunTestVideosListener());
  }

  private class RunTestVideosListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      MeasurementTask newTask = null;
      boolean showLengthWarning = false;
      Map<String, String> params = new HashMap<String, String>();
      String taskTarget="";
      TaskType measurementType = TaskType.INVALID;

      measurementType = TaskType.VIDEOQOE;
      params.put("content_url", "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?"
          + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,as&ip=0.0.0.0&"
          + "ipbits=0&expire=19000000000&signature=255F6B3C07C753C88708C07EA31B7A1A10703C8D."
          + "2D6A28B21F921D0B245CDCF36F7EB54A2B5ABFC2&key=ik0");
      
      int adaptationCode = DemoUtil.TYPE_DASH_VOD;
      if (videoAlgorithm.equals("CBA")) {
        adaptationCode = DemoUtil.TYPE_DASH_VOD;
      }
      else if (videoAlgorithm.equals("BBA")) {
        adaptationCode = DemoUtil.TYPE_BBA;
      }
      else if (videoAlgorithm.equals("Progressive")) {
        adaptationCode = DemoUtil.TYPE_PROGRESSIVE;
      }
      
      params.put("content_type", String.valueOf(adaptationCode));
      // Get energy saving percentage
      EditText videoEnergySavingText = 
          (EditText) findViewById(R.id.VideoEnergySavingText);
      params.put("energy_saving", 
          videoEnergySavingText.getText().toString());
      // Get # of buffer block
      EditText videoBufferSizeText = 
          (EditText) findViewById(R.id.VideoBufferSizeText);
      params.put("buffer_segments", 
          videoBufferSizeText.getText().toString());


      for (String videoId : testVideoLists) {
        params.put("content_id", videoId);
        Logger.i("Test video: " + videoId);
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
      }
    }
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
    } else if (this.measurementTypeUnderEdit.compareTo(VideoQoETask.TYPE) == 0) {
      this.findViewById(R.id.VideoQoEAlgorithmView).setVisibility(View.VISIBLE);
      this.findViewById(R.id.VideoEnergySavingView).setVisibility(View.VISIBLE);
      this.findViewById(R.id.VideoBufferSizeView).setVisibility(View.VISIBLE);
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
  
  private class VideoRadioOnClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      RadioButton rb = (RadioButton) v;
      MeasurementCreationActivity.this.videoAlgorithm = (String) rb.getText();
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
        else if (measurementTypeUnderEdit.equals(VideoQoETask.TYPE)) {
//          params.put("", value)
          measurementType = TaskType.VIDEOQOE;
          params.put("content_url", "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?"
              + "as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,as&ip=0.0.0.0&"
              + "ipbits=0&expire=19000000000&signature=255F6B3C07C753C88708C07EA31B7A1A10703C8D."
              + "2D6A28B21F921D0B245CDCF36F7EB54A2B5ABFC2&key=ik0");
//          params.put("content_id", "bf5bb2419360daf1");
//          params.put("content_id", "iVAgTiBrrDA");
          params.put("content_id", "VvpS20gCXrM");
//          params.put("content_id", "gpnUmMcngYM");
//          params.put("content_id", "tZmcFOt0E7M");
          
          
          int adaptationCode = DemoUtil.TYPE_DASH_VOD;
          if (videoAlgorithm.equals("CBA")) {
            adaptationCode = DemoUtil.TYPE_DASH_VOD;
          }
          else if (videoAlgorithm.equals("BBA")) {
            adaptationCode = DemoUtil.TYPE_BBA;
          }
          else if (videoAlgorithm.equals("Progressive")) {
            adaptationCode = DemoUtil.TYPE_PROGRESSIVE;
          }
          params.put("content_type", String.valueOf(adaptationCode));
          // Get energy saving percentage
          EditText videoEnergySavingText = 
              (EditText) findViewById(R.id.VideoEnergySavingText);
          params.put("energy_saving", 
              videoEnergySavingText.getText().toString());
          // Get # of buffer block
          EditText videoBufferSizeText = 
              (EditText) findViewById(R.id.VideoBufferSizeText);
          params.put("buffer_segments", 
              videoBufferSizeText.getText().toString());
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