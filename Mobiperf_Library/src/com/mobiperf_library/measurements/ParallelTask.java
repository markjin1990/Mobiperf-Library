package com.mobiperf_library.measurements;


import java.io.InvalidClassException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.mobiperf_library.Config;
import com.mobiperf_library.MeasurementDesc;
import com.mobiperf_library.MeasurementResult;
import com.mobiperf_library.MeasurementTask;
import com.mobiperf_library.exceptions.MeasurementError;



public class ParallelTask extends MeasurementTask{

  private long duration;
  private List<MeasurementTask> tasks;

  private ExecutorService executor;

  // Type name for internal use
  public static final String TYPE = "parallel";
  // Human readable name for the task
  public static final String DESCRIPTOR = "parallel";


  public static class ParallelDesc extends MeasurementDesc {     

    public ParallelDesc(String key, Date startTime,
                        Date endTime, double intervalSec, long count, long priority, 
                        Map<String, String> params) throws InvalidParameterException {
      super(ParallelTask.TYPE, key, startTime, endTime, intervalSec, count,
        priority, params);  
      //      initializeParams(params);

    }

    @Override
    protected void initializeParams(Map<String, String> params) {
    }

    @Override
    public String getType() {
      return ParallelTask.TYPE;
    }  
  }

  @SuppressWarnings("rawtypes")
  public static Class getDescClass() throws InvalidClassException {
    return ParallelDesc.class;
  }



  public ParallelTask(MeasurementDesc desc,  ArrayList<MeasurementTask> tasks) {
    super(new ParallelDesc(desc.key, desc.startTime, desc.endTime, desc.intervalSec,
      desc.count, desc.priority, desc.parameters));
    this.tasks=(List<MeasurementTask>) tasks.clone();
    executor=Executors.newFixedThreadPool(this.tasks.size());
    long maxduration=0;
    for(MeasurementTask mt: tasks){
      if(mt.getDuration()>maxduration){
        maxduration=mt.getDuration();
      }
    }
    this.duration=maxduration;

  }

  @Override
  public String getDescriptor() {
    return DESCRIPTOR;
  }

  @Override
  public MeasurementResult[] call() throws MeasurementError {
    long timeout=duration;


    if(timeout==0){
      timeout=Config.DEFAULT_PARALLEL_TASK_DURATION;
    }else{
      timeout*=2;//TODO
    }
    ArrayList<MeasurementResult> allresults=new ArrayList<MeasurementResult>();
    List<Future<MeasurementResult[]>> futures;
    try {
      futures=executor.invokeAll(this.tasks,timeout,TimeUnit.MILLISECONDS);
      for(Future<MeasurementResult[]> f: futures){
        MeasurementResult[] r=f.get();
        for(int i=0;i<r.length;i++){//TODO
          allresults.add(r[i]);
        }
      }

    } catch (InterruptedException e) {
      //TODO test it--> timeout

      e.printStackTrace();
    }catch (ExecutionException e) {
      //TODO
      e.printStackTrace();
    }
    finally{
      executor.shutdown();
    }
    return (MeasurementResult[])allresults.toArray();
  }

  @Override
  public String getType() {
    return ParallelTask.TYPE;
  }

  @Override
  public MeasurementTask clone() {
    MeasurementDesc desc = this.measurementDesc;
    ParallelDesc newDesc = new ParallelDesc(desc.key, desc.startTime, desc.endTime, 
      desc.intervalSec, desc.count, desc.priority, desc.parameters);
    ArrayList<MeasurementTask> newTaskList=new ArrayList<MeasurementTask>();
    for(MeasurementTask mt: tasks){
      newTaskList.add(mt.clone());
    }
    return new ParallelTask(newDesc,newTaskList);
  }

  @Override
  public boolean stop() {
    return false;
  }

  @Override
  public long getDuration() {
    return duration;
  }

  @Override
  public void setDuration(long newDuration) {
    if(newDuration<0){
      this.duration=0;
    }else{
      this.duration=newDuration;
    }
  }

}
