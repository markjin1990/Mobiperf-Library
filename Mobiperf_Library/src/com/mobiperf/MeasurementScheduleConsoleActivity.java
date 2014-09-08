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

package com.mobiperf;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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
 * Activity that shows the current measurement schedule of the scheduler
 */
public class MeasurementScheduleConsoleActivity extends Activity {
  public static final String TAB_TAG = "MEASUREMENT_SCHEDULE";

  private SpeedometerApp parent;
//  private Console console;
  private API api;

  private TaskItemAdapter adapter;
  private ArrayList<TaskItem> taskItems= new ArrayList<TaskItem>();
  private ListView consoleView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.measurement_schedule);

    this.adapter= new TaskItemAdapter(this, R.layout.measurement_schedule, taskItems);

    parent = (SpeedometerApp) this.getParent();
    this.api = API.getAPI(parent, MobiperfConfig.CLIENT_KEY);

//    this.console = parent.getConsole();

    this.consoleView = (ListView) this.findViewById(R.id.measurementScheduleConsole);
    this.consoleView.setAdapter(adapter);
    registerForContextMenu(consoleView);
    consoleView.setOnItemLongClickListener(new OnItemLongClickListener() {
      /**
       * Records which item in the list is selected
       */
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
      }
    });
  }

  /**
   * Handles context menu creation for the ListView in the console
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
                                  ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.scheduler_console_context_menu, menu);
  }

  @Override
  protected void onResume() {
    updateTasksFromConsole();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  class TaskItemAdapter extends ArrayAdapter<TaskItem> {
    private ArrayList<TaskItem> taskItems;
    public TaskItemAdapter(Context context, int textViewResourceId, ArrayList<TaskItem> items) {
      super(context, textViewResourceId, items);
      this.taskItems = items;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Console console = MeasurementScheduleConsoleActivity.this.parent.getConsole();
      View v = convertView;
      if (v == null) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.scheduled_task_list_item, null);
      }
      TaskItem  item = taskItems.get(position);
      if(item!=null){
        String taskId=item.getTaskId();
        ToggleButton pauseButton=(ToggleButton) (v.findViewById(R.id.pausebutton));
        // check whether console is initialized
        if (console != null) {
          if(console.isPaused(taskId)){
            pauseButton.setChecked(true);
          }
          else{
            pauseButton.setChecked(false);
          }
        }
        pauseButton.setOnClickListener(new View.OnClickListener() {
          private Console console = MeasurementScheduleConsoleActivity.this.parent.getConsole();
          private TaskItem taskitem;
          public void onClick(View v) {
            boolean paused = ((ToggleButton) v).isChecked();
            if (paused) {
              //canceling the task
              try {
                MeasurementScheduleConsoleActivity.this.api.cancelTask(taskitem.getTaskId());
                console.addToPausedTasks(taskitem.getTaskId());
                console.persistState();
              } catch (MeasurementError e) {
                Logger.e(e.toString());
                Toast.makeText(MeasurementScheduleConsoleActivity.this, R.string.cancelUserMeasurementFailureToast,
                  Toast.LENGTH_LONG).show();
              }
            }else{
              //creating another task
              console.removeFromPausedTasks(taskitem.getTaskId());
              console.persistState();
              String taskDesc=taskitem.getDescription();
              MeasurementTask newTask = null;
              TaskType measurementType = TaskType.INVALID;
              Map<String, String> params = new HashMap<String, String>();
              if(taskDesc.startsWith(TracerouteTask.TYPE)){
                measurementType=TaskType.TRACEROUTE;
                params.put("target", taskDesc.substring(taskDesc.indexOf(',')+1));
              }else if(taskDesc.startsWith(PingTask.TYPE)){
                measurementType=TaskType.PING;
                params.put("target", taskDesc.substring(taskDesc.indexOf(',')+1));
              }else if(taskDesc.startsWith(DnsLookupTask.TYPE)){
                measurementType=TaskType.DNSLOOKUP;
                params.put("target", taskDesc.substring(taskDesc.indexOf(',')+1));
              }else if(taskDesc.startsWith(HttpTask.TYPE)){
                measurementType=TaskType.HTTP;
                params.put("url", taskDesc.substring(taskDesc.indexOf(',')+1));
                params.put("method", "get");
              }else if(taskDesc.startsWith(UDPBurstTask.TYPE)){
                measurementType=TaskType.UDPBURST;
                params.put("target", MLabNS.TARGET);
                params.put("direction", taskDesc.substring(taskDesc.indexOf(',')+1));
              }else if(taskDesc.startsWith(TCPThroughputTask.TYPE)){
                measurementType=TaskType.TCPTHROUGHPUT;
                params.put("target", MLabNS.TARGET);
                params.put("dir_up", taskDesc.substring(taskDesc.indexOf(',')+1));
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
                  MeasurementScheduleConsoleActivity.this.api.submitTask(newTask);
                }
              } catch (MeasurementError e) {
                Logger.e(e.toString());
                Toast.makeText(MeasurementScheduleConsoleActivity.this, R.string.userMeasurementFailureToast,
                  Toast.LENGTH_LONG).show();
              }
              console.removeUserTask(taskitem.getTaskId());
              console.addUserTask(newTask.getTaskId(), taskDesc);
              console.persistState();
            }
          }

          public OnClickListener init(TaskItem ti) {
            taskitem=ti;
            return this;
          }
        }.init(item));
        Button cancelButton=(Button)(v.findViewById(R.id.cancelbutton));
        cancelButton.setOnClickListener(new View.OnClickListener() {
          private Console console = MeasurementScheduleConsoleActivity.this.parent.getConsole();
          private TaskItem taskitem;
          public void onClick(View v) {
            try {
              MeasurementScheduleConsoleActivity.this.api.cancelTask(taskitem.getTaskId());
              console.removeUserTask(taskitem.getTaskId());
              console.persistState();
              TaskItemAdapter.this.remove(taskitem);
              TaskItemAdapter.this.notifyDataSetChanged();

            } catch (MeasurementError e) {
              Logger.e(e.toString());
              Toast.makeText(MeasurementScheduleConsoleActivity.this, R.string.cancelUserMeasurementFailureToast,
                Toast.LENGTH_LONG).show();
            }
          }

          public 	OnClickListener init(TaskItem ti) {
            taskitem=ti;
            return this;
          }
        }.init(item));
        TextView text= (TextView) (v.findViewById(R.id.taskdesc));
        text.setText(item.toString());
      }

      return v;
    }
  }

  class TaskItem{
    private String description;
    private String taskId;
    public void setTaskId(String id){
      this.taskId=id;
    }
    public String getTaskId(){
      return this.taskId;
    }
    public void setDescription(String desc){
      this.description=desc;
    }
    public String getDescription(){
      return this.description;
    }
    public TaskItem(){

    }
    public TaskItem(String taskId, String desc){
      this.description=desc;
      this.taskId=taskId;
    }
    @Override
    public String toString() {
      String result="";
      if(description.startsWith(TracerouteTask.TYPE)){
        result+="["+TracerouteTask.TYPE+"]\ntarget: "+description.substring(description.indexOf(',')+1);
      }else if(description.startsWith(PingTask.TYPE)){
        result+="["+PingTask.TYPE+"]\ntarget: "+description.substring(description.indexOf(',')+1);
      }else if(description.startsWith(DnsLookupTask.TYPE)){
        result+="["+DnsLookupTask.TYPE+"]\ntarget: "+description.substring(description.indexOf(',')+1);
      }else if(description.startsWith(HttpTask.TYPE)){
        result+="["+HttpTask.TYPE+"]\ntarget: "+description.substring(description.indexOf(',')+1);
      }else if(description.startsWith(UDPBurstTask.TYPE)){
        result+="["+UDPBurstTask.TYPE+"]\ndirection: "+description.substring(description.indexOf(',')+1);
      }else if(description.startsWith(TCPThroughputTask.TYPE)){
        result+="["+TCPThroughputTask.TYPE+"]\ndirection: "+description.substring(description.indexOf(',')+1);
      }
      return result;
    }
  }


  private synchronized void updateTasksFromConsole(){
    Console console = parent.getConsole();
    if (console != null) {
      taskItems.clear();
      final List<String> user_tasks=console.getUserTasks();
      for(String taskStr: user_tasks){
        String taskId=taskStr.substring(0, taskStr.indexOf(','));
        String taskDesc=taskStr.substring(taskStr.indexOf(',')+1);
        taskItems.add(new TaskItem(taskId,taskDesc));
      }
      runOnUiThread(new Runnable() {
        public void run() { adapter.notifyDataSetChanged(); }
      });


    }
  }
}
