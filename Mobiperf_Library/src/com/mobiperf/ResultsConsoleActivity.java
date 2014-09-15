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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.mobiperf.util.Logger;
import com.mobiperf.R;
import com.mobilyzer.api.API;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

/**
 * The activity that provides a console and progress bar of the ongoing measurement
 */
public class ResultsConsoleActivity extends Activity {

  public static final String TAB_TAG = "MY_MEASUREMENTS";

  private SpeedometerApp parent;
  private ListView consoleView;
  private ArrayAdapter<String> results;
  BroadcastReceiver receiver;
  private ToggleButton showUserResultButton;
  private ToggleButton showSystemResultButton;
//  private Console console;
  boolean userResultsActive = false;

  // private Button visLinkButton;
  private RadioGroup radioGroup;
  private RadioButton radioButtonDetailed;
  private RadioButton radioButtonGraph;
  private LinearLayout graphConsole;
  private RelativeLayout checkboxes;
  private CheckBox checkCompleted;
  private CheckBox checkFailed;
  private boolean completedIsChecked;
  private boolean failedIsChecked;

  private HashMap<Double, Integer> thrReverseIndexMap;
  private HashMap<Double, Integer> rttReverseIndexMap;

  private API api;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Logger.d("ResultsConsoleActivity.onCreate called");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.results);

    this.parent = (SpeedometerApp) this.getParent();
    this.consoleView = (ListView) this.findViewById(R.id.resultConsole);
    this.results = new CustomArrayAdapter(getApplicationContext(), R.layout.list_item);
    this.consoleView.setAdapter(this.results);

    showUserResultButton = (ToggleButton) findViewById(R.id.showUserResults);
    showSystemResultButton = (ToggleButton) findViewById(R.id.showSystemResults);
    showUserResultButton.setChecked(true);
    showSystemResultButton.setChecked(false);
    userResultsActive = true;



    this.radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
    this.radioButtonDetailed = (RadioButton) findViewById(R.id.radio_detailed);
    this.radioButtonGraph = (RadioButton) findViewById(R.id.radio_graph);
    this.radioButtonDetailed.setChecked(true);



    OnClickListener radioListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        RadioButton rb = (RadioButton) v;

        if (rb.getText().equals("Graph View")) {
          consoleView.setVisibility(View.INVISIBLE);
          checkboxes.setVisibility(View.INVISIBLE);
          graphConsole.setVisibility(View.VISIBLE);


          // Logger.e("ashnik_debug: calling populateGraphs with "+userResultsActive+" from 1");
          populateGraphs(!userResultsActive);

        } else {
          consoleView.setVisibility(View.VISIBLE);
          checkboxes.setVisibility(View.VISIBLE);
          graphConsole.setVisibility(View.INVISIBLE);
        }

      }
    };



    this.graphConsole = (LinearLayout) findViewById(R.id.graphConsole);
    this.graphConsole.setVisibility(View.INVISIBLE);
    this.checkboxes = (RelativeLayout) findViewById(R.id.checkBoxFilters);
    this.checkboxes.setVisibility(View.VISIBLE);

    this.checkCompleted = (CheckBox) findViewById(R.id.chkCompleted);
    this.checkCompleted.setChecked(true);
    this.completedIsChecked = true;

    this.checkFailed = (CheckBox) findViewById(R.id.chkFailed);
    this.checkFailed.setChecked(true);
    this.failedIsChecked = true;

    this.checkCompleted.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        if (((CheckBox) v).isChecked()) {
          completedIsChecked = true;

        } else {
          completedIsChecked = false;
        }
        getConsoleContentFromScheduler(completedIsChecked, failedIsChecked);
      }
    });

    this.checkFailed.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        if (((CheckBox) v).isChecked()) {
          failedIsChecked = true;
        } else {
          failedIsChecked = false;
        }
        getConsoleContentFromScheduler(completedIsChecked, failedIsChecked);
      }
    });

    this.radioButtonDetailed.setOnClickListener(radioListener);
    this.radioButtonGraph.setOnClickListener(radioListener);


    // We enforce a either-or behavior between the two ToggleButtons
    OnCheckedChangeListener buttonClickListener = new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Logger.d("onCheckedChanged");
        switchBetweenResults(buttonView == showUserResultButton ? isChecked : !isChecked);

        if (isChecked) {
          // Logger.e("ashnik_debug: calling populateGraphs with "+(buttonView ==
          // showUserResultButton ? !isChecked : isChecked)+" from 2");
          populateGraphs(buttonView == showUserResultButton ? !isChecked : isChecked);
        }

      }
    };
    showUserResultButton.setOnCheckedChangeListener(buttonClickListener);
    showSystemResultButton.setOnCheckedChangeListener(buttonClickListener);



    // get API singleton object
    this.api = API.getAPI(this, MobiperfConfig.CLIENT_KEY);

//    // get console singleton
//    this.console = ((SpeedometerApp) this.getParent()).getConsole();

    IntentFilter filter = new IntentFilter();
    filter.addAction(MobiperfIntent.SCHEDULER_CONNECTED_ACTION);
    filter.addAction(api.userResultAction);
    filter.addAction(API.SERVER_RESULT_ACTION);
    this.receiver = new BroadcastReceiver() {
      @Override
      // All onXyz() callbacks are single threaded
      public void onReceive(Context context, Intent intent) {
        // the console object should be obtained via api call at each time
        // in case that the service hasn't been connected
        ResultsConsoleActivity instance = (ResultsConsoleActivity)context;
        Console console = instance.parent.getConsole();
        
        if (intent.getAction().equals(api.userResultAction)) {
          Logger.d("receive user results");
          switchBetweenResults(true);
          // check whether console is initialized
          if (console != null) {
            console.updateStatus(null);
            console.persistState();
            populateGraphs(!userResultsActive);
          }
        } else if (intent.getAction().equals(API.SERVER_RESULT_ACTION)) {
          getConsoleContentFromScheduler(completedIsChecked, failedIsChecked);
          // check whether console is initialized
          if (console != null) {
            console.updateStatus(null);
            console.persistState();
            populateGraphs(!userResultsActive);
          }
        } else if (intent.getAction().equals(MobiperfIntent.SCHEDULER_CONNECTED_ACTION)) {
          Logger.d("scheduler connected");
          switchBetweenResults(userResultsActive);
        }
      }
    };
    this.registerReceiver(this.receiver, filter);

    getConsoleContentFromScheduler(completedIsChecked, failedIsChecked);
  }

  /**
   * Change the underlying adapter for the ListView.
   * 
   * @param showUserResults If true, show user results; otherwise, show system results.
   */
  private synchronized void switchBetweenResults(boolean showUserResults) {
    userResultsActive = showUserResults;
    getConsoleContentFromScheduler(completedIsChecked, failedIsChecked);
    showUserResultButton.setChecked(showUserResults);
    showSystemResultButton.setChecked(!showUserResults);
    Logger.d("switchBetweenResults: showing " + results.getCount() + " "
        + (showUserResults ? "user" : "system") + " results");
  }

  @Override
  protected void onDestroy() {
    Logger.d("ResultsConsoleActivity.onDestroy called");
    super.onDestroy();
    this.unregisterReceiver(this.receiver);
  }

  private synchronized void getConsoleContentFromScheduler(boolean completed, boolean failed) {
    Logger.d("ResultsConsoleActivity.getConsoleContentFromScheduler called");
    // Scheduler may have not had time to start yet. When it does, the intent above will call this
    // again.

    // the console object should be obtained via api call at each time
    // in case that the service hasn't been connected
    Console console = ((SpeedometerApp)getParent()).getConsole();
    if (console != null) {
      Logger.d("Updating measurement results from thread " + Thread.currentThread().getName());
      results.clear();
      final List<String> scheduler_results =
          (userResultsActive ? console.getUserResults() : console.getSystemResults());
      for (String result : scheduler_results) {
        if (result.contains("Error:") && failed) {
          results.add(result);
        } else if (!result.contains("Error:") && completed) {
          results.add(result);
        }
      }
      runOnUiThread(new Runnable() {
        public void run() {
          results.notifyDataSetChanged();
        }
      });
    } else {
      Logger.e("Console is not instantialized!");
    }
  }



  private void populateGraphs(boolean serverResults) {
    LinearLayout graph1 = (LinearLayout) findViewById(R.id.graph1);
    LinearLayout.LayoutParams params = (LayoutParams) graph1.getLayoutParams();
    params.height = (int) getResources().getDimension(R.dimen.graph_height);
    graph1.setLayoutParams(params);
    graph1.removeAllViews();

    GraphView graphView = null;
    try {
      graphView = generateThroughputGraph(serverResults);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (graphView != null) {
      graph1.addView(graphView);
    } else {
      Logger.e("ashnik_debug: graph1 is null");
      params.height = 0;
      graph1.setLayoutParams(params);
    }

    LinearLayout graph2 = (LinearLayout) findViewById(R.id.graph2);
    graph2.removeAllViews();

    GraphView graphView2 = null;
    try {
      graphView2 = generateLatencyGraph(serverResults);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (graphView2 != null) {
      graph2.addView(graphView2);
    }



  }

  private GraphView generateThroughputGraph(boolean serverResults) throws IOException {

    long current_time = System.currentTimeMillis();
    // check whether console is initialized
    Console console = this.parent.getConsole();
    if (console == null) {
      return null;
    }
    HashMap<String, ArrayList<String>> thr_results_map =
        console.readThroughputResultsFromMemory(serverResults);
    if (thr_results_map == null) {
      return null;// TODO
    }

    HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>();

    GraphView graphView = new LineGraphView(ResultsConsoleActivity.this, "Throughput (Kbps)");
    HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
    thrReverseIndexMap = new HashMap<Double, Integer>();
    graphView.setCustomLabelFormatter(new CustomLabelFormatter() {

      @Override
      public String formatLabel(double value, boolean isValueX) {
        if (isValueX) {
          String v = "";
          // Logger.e("ashnik_debug: formatLabel "+value);

          // for(Double kk : thrReverseIndexMap.keySet()){
          // Logger.e("ashnik_debug: kk "+kk+" vv "+thrReverseIndexMap.get(kk));
          // }

          if (!thrReverseIndexMap.containsKey(value)) {
            return "";
          }
          int realValue = thrReverseIndexMap.get(value);

          if (realValue < 1000) {// ms
            v = realValue + "ms";

          } else if (realValue < 60 * 1000) {// s
            v = ((int) (realValue / 1000)) + "s";
          } else if (realValue < 60 * 60 * 1000) {// min
            v = ((int) (realValue / (60 * 1000))) + "m";
          } else if (realValue < 24 * 60 * 60 * 1000) {// hour
            v = ((int) (realValue / (60 * 60 * 1000))) + "h";
          } else if (realValue < 10 * 24 * 60 * 60 * 1000) {// 10 days
            v = ((int) (realValue / (24 * 60 * 60 * 1000))) + "d";
          }

          // Logger.e("ashnik_debug: value " + value + " realvalue " + realValue + " output " + v);

          return v;

        } else {

          return ((int) value) + "";
        }
      }
    });


    if (thr_results_map.keySet().size() == 0) {
      return null;// TODO
    }

    HashMap<String, ArrayList<Pair<Integer, Integer>>> avg_map =
        preprocessDataPoints(thr_results_map, current_time);


    for (String dir : avg_map.keySet()) {

      for (int i = 0; i < avg_map.get(dir).size(); i++) {
        Pair<Integer, Integer> pair = avg_map.get(dir).get(i);
        if (!indexMap.containsKey(pair.first)) {
          indexMap.put(pair.first, null);
        }
      }
    }

    Object[] sorted_array = new Object[indexMap.keySet().size()];
    sorted_array = indexMap.keySet().toArray();
    Arrays.sort(sorted_array);

    int y_max = 0;
    int x_min = sorted_array.length;
    int x_max = 0;



    for (int i = 0; i < sorted_array.length; i++) {
      // Logger.e("ashnik_debug: put into maps "+((double) sorted_array.length - i)+" "+(Integer)
      // sorted_array[i]);
      indexMap.put((Integer) sorted_array[i], sorted_array.length - i);
      thrReverseIndexMap.put((double) sorted_array.length - i, (Integer) sorted_array[i]);
    }

    for (String dir : avg_map.keySet()) {

      int all_data_size = avg_map.get(dir).size();
      int trimmed_data_size;
      GraphViewData[] data_points;
      if (all_data_size > 20) {
        trimmed_data_size = 20;
        data_points = new GraphViewData[20];
      } else {
        trimmed_data_size = all_data_size;
        data_points = new GraphViewData[all_data_size];
      }


      int shift = all_data_size - trimmed_data_size;

      for (int i = shift; i < avg_map.get(dir).size(); i++) {
        // Logger.e("ashnik_debug :::"+i+" "+thr_results_map.get(dir).get(i));

        Pair<Integer, Integer> pair = avg_map.get(dir).get(i);
        if (pair.second > y_max) {
          y_max = pair.second;
        }
        int newIndex = indexMap.get(pair.first);
        data_points[trimmed_data_size - (i - shift) - 1] =
            new GraphViewData(newIndex, pair.second);
        // Logger.e("ashnik_debug: " + dir + ": arrayindex "
        // + (avg_map.get(dir).size() - (i - shift) - 1) + " new x: " + newIndex + " real x "
        // + pair.first + " value: " + pair.second);
        if (indexMap.get(pair.first) > x_max) {
          x_max = newIndex;
        }
        if (indexMap.get(pair.first) < x_min) {
          x_min = newIndex;
        }
      }

      
      if(data_points.length==0){
    	  return null;
      }
      

      GraphViewSeries series;
      if (dir.equals("Up") || dir.equals("true")) {
        series =
            new GraphViewSeries("Uplink", new GraphViewSeriesStyle(Color.BLUE, 2), data_points);
      } else {
        series =
            new GraphViewSeries("Downlink", new GraphViewSeriesStyle(Color.MAGENTA, 2), data_points);
      }
      graphView.addSeries(series);

    }



    int interval;
    if (y_max < 100) {
      y_max = 100;
      interval = 20;
    } else if (y_max < 200) {
      y_max = 200;
      interval = 50;
    } else if (y_max < 400) {
      y_max = 400;
      interval = 100;
    } else if (y_max < 500) {
      y_max = 500;
      interval = 100;
    } else if (y_max < 1000) {
      y_max = 1000;
      interval = 100;
    } else {
      y_max = (int) (Math.ceil(y_max / 100.0)) * 100;
      interval = y_max / 5;
      if (interval % 100 != 0) {
        interval = 100 - (interval % 100) + interval;
      }
      y_max = interval * 5;
    }
    graphView.setManualYAxisBounds(y_max, 0);
    graphView.getGraphViewStyle().setNumVerticalLabels(y_max / interval + 1);


    if(x_max==1){
       GraphViewSeries hiddenSeries =
       new GraphViewSeries("", new GraphViewSeriesStyle(Color.TRANSPARENT, 0),
       new GraphViewData[] {new GraphViewData(0, y_max * 2),
       new GraphViewData(x_max + 1, y_max * 2)});
       graphView.addSeries(hiddenSeries);

    }


    graphView.setShowLegend(true);
    graphView.setShowHorizontalLabels(true);
    graphView.setLegendAlign(LegendAlign.BOTTOM);
    
    graphView.getGraphViewStyle().setLegendWidth(20*8);
//    graphView.getGraphViewStyle().setLegendMarginBottom(-20);
    graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.regularTextSize));

    ((LineGraphView) graphView).setDrawDataPoints(true);
    ((LineGraphView) graphView).setDataPointsRadius(8f);

    // Logger.e("ashnik_debug: x_max " + x_max + " x_min " + x_min);

    if (x_max > 10) {
      graphView.getGraphViewStyle().setNumHorizontalLabels(10);
      graphView.setViewPort(x_max - 10 + 1, 9);
      graphView.setScrollable(true);
    } else {
      graphView.getGraphViewStyle().setNumHorizontalLabels(x_max);
    }


    return graphView;
  }



  private GraphView generateLatencyGraph(boolean serverResults) throws IOException {

    long current_time = System.currentTimeMillis();
    
    // check whether console is initialized
    Console console = this.parent.getConsole();
    if (console == null) {
      return null;
    }
    HashMap<String, ArrayList<String>> rtt_results_map =
        console.readLatencyResultsFromMemory(serverResults);
    if (rtt_results_map == null) {
      return null;// TODO
    }

    HashMap<Integer, Integer> labels = new HashMap<Integer, Integer>();

    GraphView graphView = new LineGraphView(ResultsConsoleActivity.this, "\nLatency (ms)");
    HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
    rttReverseIndexMap = new HashMap<Double, Integer>();
    graphView.setCustomLabelFormatter(new CustomLabelFormatter() {

      @Override
      public String formatLabel(double value, boolean isValueX) {
        if (isValueX) {
          String v = "";
          // Logger.e("ashnik_debug: formatLabel "+value);
          if (!rttReverseIndexMap.containsKey(value)) {
            return "";
          }
          int realValue = rttReverseIndexMap.get(value);

          if (realValue < 1000) {// ms
            v = realValue + "ms";

          } else if (realValue < 60 * 1000) {// s
            v = ((int) (realValue / 1000)) + "s";
          } else if (realValue < 60 * 60 * 1000) {// min
            v = ((int) (realValue / (60 * 1000))) + "m";
          } else if (realValue < 24 * 60 * 60 * 1000) {// hour
            v = ((int) (realValue / (60 * 60 * 1000))) + "h";
          } else if (realValue < 10 * 24 * 60 * 60 * 1000) {// 10 days
            v = ((int) (realValue / (24 * 60 * 60 * 1000))) + "d";
          }

          // Logger.e("ashnik_debug: value " + value + " realvalue " + realValue +" output "+v);

          return v;

        } else {

          return ((int) value) + "";
        }
      }
    });


    if (rtt_results_map.keySet().size() == 0) {
      return null;// TODO
    }

    HashMap<String, ArrayList<Pair<Integer, Integer>>> avg_map =
        preprocessDataPoints(rtt_results_map, current_time);


    for (String dir : avg_map.keySet()) {

      for (int i = 0; i < avg_map.get(dir).size(); i++) {
        Pair<Integer, Integer> pair = avg_map.get(dir).get(i);
        if (!indexMap.containsKey(pair.first)) {
          indexMap.put(pair.first, null);
        }
      }
    }

    Object[] sorted_array = new Object[indexMap.keySet().size()];
    sorted_array = indexMap.keySet().toArray();
    Arrays.sort(sorted_array);

    int y_max = 0;
    int x_min = sorted_array.length;
    int x_max = 0;

    int[] colors =
        {Color.BLACK, Color.GREEN, Color.BLUE, Color.CYAN, Color.RED, Color.DKGRAY, Color.MAGENTA,
            Color.GRAY, Color.LTGRAY, Color.YELLOW};
    int color_index = 0;

    for (int i = 0; i < sorted_array.length; i++) {
      indexMap.put((Integer) sorted_array[i], sorted_array.length - i);
      rttReverseIndexMap.put((double) sorted_array.length - i, (Integer) sorted_array[i]);
    }

    for (String dir : avg_map.keySet()) {

      int all_data_size = avg_map.get(dir).size();
      int trimmed_data_size;
      GraphViewData[] data_points;
      if (all_data_size > 20) {
        trimmed_data_size = 20;
        data_points = new GraphViewData[20];
      } else {
        trimmed_data_size = all_data_size;
        data_points = new GraphViewData[all_data_size];
      }


      int shift = all_data_size - trimmed_data_size;

      for (int i = shift; i < avg_map.get(dir).size(); i++) {
        // Logger.e("ashnik_debug :::"+i+" "+thr_results_map.get(dir).get(i));

        Pair<Integer, Integer> pair = avg_map.get(dir).get(i);
        if (pair.second > y_max) {
          y_max = pair.second;
        }
        int newIndex = indexMap.get(pair.first);
        data_points[trimmed_data_size - (i - shift) - 1] =
            new GraphViewData(newIndex, pair.second);
        // Logger.e("ashnik_debug: " + dir + ": arrayindex "
        // + (avg_map.get(dir).size() - (i - shift) - 1) + " new x: " + newIndex + " real x "
        // + pair.first + " value: " + pair.second);
        if (indexMap.get(pair.first) > x_max) {
          x_max = newIndex;
        }
        if (indexMap.get(pair.first) < x_min) {
          x_min = newIndex;
        }
      }
      
      if(data_points.length==0){
    	  return null;
      }

      GraphViewSeries series =
          new GraphViewSeries(dir, new GraphViewSeriesStyle(colors[color_index], 2), data_points);
      graphView.addSeries(series);
      color_index++;
      if (color_index >= colors.length) {
        break;
      }


    }



    int interval;
    if (y_max < 100) {
      y_max = 100;
      interval = 20;
    } else if (y_max < 200) {
      y_max = 200;
      interval = 50;
    } else if (y_max < 400) {
      y_max = 400;
      interval = 100;
    } else if (y_max < 500) {
      y_max = 500;
      interval = 100;
    } else if (y_max < 1000) {
      y_max = 1000;
      interval = 100;
    } else {
      y_max = (int) (Math.ceil(y_max / 100.0)) * 100;
      interval = y_max / 5;
      if (interval % 100 != 0) {
        interval = 100 - (interval % 100) + interval;
      }
      y_max = interval * 5;
    }
    graphView.setManualYAxisBounds(y_max, 0);
    graphView.getGraphViewStyle().setNumVerticalLabels(y_max / interval + 1);
    
    
    if(x_max==1){
      GraphViewSeries hiddenSeries =
      new GraphViewSeries("", new GraphViewSeriesStyle(Color.TRANSPARENT, 0),
      new GraphViewData[] {new GraphViewData(0, y_max * 2),
      new GraphViewData(x_max + 1, y_max * 2)});
      graphView.addSeries(hiddenSeries);

   }


    graphView.setShowLegend(true);
    graphView.setShowHorizontalLabels(true);
//    graphView.getGraphViewStyle().setLegend
    graphView.setLegendAlign(LegendAlign.BOTTOM);
    
    if (avg_map.containsKey("Akamai") || avg_map.containsKey("Google")
        || avg_map.containsKey("Amazon") || avg_map.containsKey("Limelight")
        || avg_map.containsKey("EdgeCast")) {
      graphView.getGraphViewStyle().setLegendWidth(8*20);
      
    }else{
      int max_length=0;
      for (String dir_key: avg_map.keySet()){
        if(dir_key.length()>max_length){
          max_length=dir_key.length();
        }
      }
      graphView.getGraphViewStyle().setLegendWidth(20*max_length);
    }
    
    
    graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.regularTextSize));

    ((LineGraphView) graphView).setDrawDataPoints(true);
    ((LineGraphView) graphView).setDataPointsRadius(8f);

    // Logger.e("ashnik_debug: x_max " + x_max+ " x_min "+x_min);

    if (x_max > 10) {
      graphView.getGraphViewStyle().setNumHorizontalLabels(10);
      graphView.setViewPort(x_max - 10 + 1, 9);
      graphView.setScrollable(true);
    } else {
      graphView.getGraphViewStyle().setNumHorizontalLabels(x_max);
    }


    return graphView;
  }

  private HashMap<String, ArrayList<Pair<Integer, Integer>>> preprocessDataPoints(
      HashMap<String, ArrayList<String>> all_points, long base_time) {
    HashMap<String, ArrayList<Pair<Integer, Integer>>> final_results =
        new HashMap<String, ArrayList<Pair<Integer, Integer>>>();
    for (String target_key : all_points.keySet()) {
      ArrayList<String> list = all_points.get(target_key);
      HashMap<Integer, ArrayList<Integer>> avg_map = new HashMap<Integer, ArrayList<Integer>>();
      for (int i = 0; i < list.size(); i++) {
        String[] toks = list.get(i).split("\\|");
        long timestamp = Long.parseLong(toks[0]);
        int delta = (int) (base_time - timestamp);
        if (delta < 1000) {// ms
          ArrayList<Integer> base_list;
          if (avg_map.containsKey(delta)) {
            base_list = avg_map.get(delta);
          } else {
            base_list = new ArrayList<Integer>();
          }
          base_list.add(Integer.parseInt(toks[1]));
          avg_map.put(delta, base_list);

        } else if (delta < 60 * 1000) {// s
          ArrayList<Integer> base_list;
          int k = (int) (delta / 1000);
          k = k * 1000;
          if (avg_map.containsKey(k)) {
            base_list = avg_map.get(k);
          } else {
            base_list = new ArrayList<Integer>();
          }
          base_list.add(Integer.parseInt(toks[1]));
          avg_map.put(k, base_list);
        } else if (delta < 60 * 60 * 1000) {// min
          ArrayList<Integer> base_list;
          int k = (int) (delta / (60 * 1000));
          k = k * 60 * 1000;
          if (avg_map.containsKey(k)) {
            base_list = avg_map.get(k);
          } else {
            base_list = new ArrayList<Integer>();
          }
          base_list.add(Integer.parseInt(toks[1]));
          avg_map.put(k, base_list);
        } else if (delta < 24 * 60 * 60 * 1000) {// hour
          ArrayList<Integer> base_list;
          int k = (int) (delta / (60 * 60 * 1000));
          k = k * 60 * 60 * 1000;
          if (avg_map.containsKey(k)) {
            base_list = avg_map.get(k);
          } else {
            base_list = new ArrayList<Integer>();
          }
          base_list.add(Integer.parseInt(toks[1]));
          avg_map.put(k, base_list);
        } else if (delta < 10 * 24 * 60 * 60 * 1000) {// 10 days
          ArrayList<Integer> base_list;
          int k = (int) (delta / (24 * 60 * 60 * 1000));
          k = k * (24 * 60 * 60 * 1000);
          if (avg_map.containsKey(k)) {
            base_list = avg_map.get(k);
          } else {
            base_list = new ArrayList<Integer>();
          }
          base_list.add(Integer.parseInt(toks[1]));
          avg_map.put(k, base_list);
        }
      }
      ArrayList<Pair<Integer, Integer>> base_data_pairs = new ArrayList<Pair<Integer, Integer>>();

      Object[] sorted_array = new Object[avg_map.keySet().size()];
      sorted_array = avg_map.keySet().toArray();
      Arrays.sort(sorted_array);
      // Logger.e("ashnik_debug key size for " + target_key + " " + sorted_array.length);

      for (int j = 0; j < sorted_array.length; j++) {
        ArrayList<Integer> base_list = avg_map.get((Integer) sorted_array[j]);
        int sum = 0;
        for (int i = 0; i < base_list.size(); i++) {
          sum += base_list.get(i);
        }
        int avg = sum / base_list.size();

        Pair<Integer, Integer> pair = new Pair<Integer, Integer>((Integer) sorted_array[j], avg);
        base_data_pairs.add(pair);


      }



      final_results.put(target_key, base_data_pairs);
    }
    return final_results;

  }


}


class CustomArrayAdapter extends ArrayAdapter<String> {

  public CustomArrayAdapter(Context context, int resource) {
    super(context, resource);

  }


  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    View v = super.getView(position, convertView, parent);
    String result = getItem(position);
    if (result.contains("Error:")) {
      v.setBackgroundColor(Color.rgb(255, 178, 178));
      // Logger.e("ashnik_debug: RED "+position+ " "+result.replace('\n', '-')+"---");
    } else {
      v.setBackgroundColor(Color.WHITE);
    }


    // else if (!result.equals("Automatically-scheduled measurement results will " + "appear here.")
    // &&
    // !result.equals("Your measurement results will appear here.") ){
    // v.setBackgroundColor(Color.rgb(178,255,178));
    // // Logger.e("ashnik_debug: GREEN "+position+ " "+result.replace('\n', '-')+"---");
    // }else{
    // // Logger.e("ashnik_debug: WHITE "+position+ " "+result.replace('\n', '-')+"---");
    // v.setBackgroundColor(Color.WHITE);
    // }

    return v;
  }


}
