/* Copyright 2013 RobustNet Lab, University of Michigan. All Rights Reserved.
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
package com.mobiperf_library;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;

import android.content.Intent;

import com.mobiperf_library.MeasurementResult.TaskProgress;
import com.mobiperf_library.exceptions.MeasurementError;
import com.mobiperf_library.util.Logger;
import com.mobiperf_library.util.PhoneUtils;

public class UserMeasurementTask implements Callable<MeasurementResult[]> {
  private MeasurementTask realTask;
  private MeasurementScheduler scheduler;
  private ContextCollector contextCollector;

  public UserMeasurementTask(MeasurementTask task, MeasurementScheduler scheduler) {
    realTask = task;
    this.scheduler = scheduler;  
    this.contextCollector= new ContextCollector();
  }

  private void broadcastMeasurementStart() {
    Intent intent = new Intent();
    intent.setAction(UpdateIntent.MEASUREMENT_PROGRESS_UPDATE_ACTION);
    intent.putExtra(UpdateIntent.TASK_PRIORITY_PAYLOAD, MeasurementTask.USER_PRIORITY);
    intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_STARTED);
    intent.putExtra(UpdateIntent.TASKID_PAYLOAD, realTask.getTaskId());
    intent.putExtra(UpdateIntent.TASKKEY_PAYLOAD, realTask.getKey());
    scheduler.sendBroadcast(intent);
  }

  private void broadcastMeasurementEnd(MeasurementResult[] results) {
    Intent intent = new Intent();
    intent.setAction(UpdateIntent.MEASUREMENT_PROGRESS_UPDATE_ACTION);

    intent.putExtra(UpdateIntent.TASK_PRIORITY_PAYLOAD, MeasurementTask.USER_PRIORITY);//TODO fixed one value priority for all users task?
    intent.putExtra(UpdateIntent.TASKID_PAYLOAD, realTask.getTaskId());
    intent.putExtra(UpdateIntent.TASKKEY_PAYLOAD, realTask.getKey());

    if (results != null){
      // Hongyi: only single task can be paused
      if(results[0].getTaskProgress()==TaskProgress.PAUSED){
        intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_PAUSED);
      }
      else{
        intent.putExtra(UpdateIntent.TASK_STATUS_PAYLOAD, Config.TASK_FINISHED);
        intent.putExtra(UpdateIntent.RESULT_PAYLOAD, results);
      }
      scheduler.sendBroadcast(intent);
    }

  }

  /**
   * The call() method that broadcast intents before the measurement starts and after the
   * measurement finishes.
   */
  @Override
  public MeasurementResult[] call() throws MeasurementError {
    MeasurementResult[] results = null;
    PhoneUtils phoneUtils = PhoneUtils.getPhoneUtils();
    try {
      phoneUtils.acquireWakeLock();
      broadcastMeasurementStart();
      contextCollector.setInterval(realTask.getDescription().contextIntervalSec);
      contextCollector.startCollector();
      results = realTask.call();
      ArrayList<HashMap<String, String>> contextResults=contextCollector.stopCollector();
      //TODO attach the results to the MeasurementResults 
    } catch (MeasurementError e) {
      Logger.e("User measurement " + realTask.getDescriptor() + " has failed");
      Logger.e(e.getMessage());
      results = MeasurementResult.getFailureResult(realTask, e);
    } catch (Exception e) {
      Logger.e("User measurement " + realTask.getDescriptor() + " has failed");
      Logger.e("Unexpected Exception: " + e.getMessage());
      results = MeasurementResult.getFailureResult(realTask, e);
    } finally {
      broadcastMeasurementEnd(results);
      if(scheduler.getCurrentTask().equals(realTask)){
        scheduler.setCurrentTask(null);
      }
      phoneUtils.releaseWakeLock();
    }
    return results;
  }
  
  
}
