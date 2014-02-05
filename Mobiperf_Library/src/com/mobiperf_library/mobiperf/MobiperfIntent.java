package com.mobiperf_library.mobiperf;



import java.security.InvalidParameterException;

import android.content.Intent;
/**
 * A repackaged Intent class that includes MobiLib-specific information. 
 */
public class MobiperfIntent extends Intent {
  
  // Different types of payloads that this intent can carry:
  public static final String MSG_PAYLOAD = "MSG_PAYLOAD";
  
  public static final String STATUS_MSG_PAYLOAD = "STATUS_MSG_PAYLOAD";
  public static final String STATS_MSG_PAYLOAD = "STATS_MSG_PAYLOAD";
  public static final String STRING_PAYLOAD = "STRING_PAYLOAD";
  
  
//  public static final String TASK_STATUS_PAYLOAD = "TASK_STATUS_PAYLOAD";
//  public static final String TASKID_PAYLOAD = "TASKID_PAYLOAD";
//  public static final String TASKKEY_PAYLOAD = "TASKKEY_PAYLOAD";
//  public static final String TASK_PRIORITY_PAYLOAD = "TASK_PRIORITY_PAYLOAD";
//  public static final String RESULT_PAYLOAD = "RESULT_PAYLOAD";
  
  
  // Different types of actions that this intent can represent:
  private static final String PACKAGE_PREFIX =
      MobiperfIntent.class.getPackage().getName();
  public static final String SYSTEM_STATUS_UPDATE_ACTION =
      PACKAGE_PREFIX + ".SYSTEM_STATUS_UPDATE_ACTION";
  public static final String PREFERENCE_ACTION =
      PACKAGE_PREFIX + ".PREFERENCE_ACTION";
  public static final String MSG_ACTION =
      PACKAGE_PREFIX + ".MSG_ACTION";
  public static final String SCHEDULER_CONNECTED_ACTION =
      PACKAGE_PREFIX + ".SCHEDULER_CONNECTED_ACTION";
  
//  public static final String MEASUREMENT_ADDED_ACTION =
//	      PACKAGE_PREFIX + ".MEASUREMENT_ADDED_ACTION";
  
////  public static final String PREFERENCE_ACTION =
////      PACKAGE_PREFIX + ".PREFERENCE_ACTION";
//  public static final String MEASUREMENT_ACTION =
//      PACKAGE_PREFIX + ".MEASUREMENT_ACTION";
//  public static final String CHECKIN_ACTION =
//      PACKAGE_PREFIX + ".CHECKIN_ACTION";
//  public static final String CHECKIN_RETRY_ACTION =
//      PACKAGE_PREFIX + ".CHECKIN_RETRY_ACTION";
//  public static final String MEASUREMENT_PROGRESS_UPDATE_ACTION =
//      PACKAGE_PREFIX + ".MEASUREMENT_PROGRESS_UPDATE_ACTION";
//  
//  public static final String USER_RESULT_ACTION =
//      PACKAGE_PREFIX + ".USER_RESULT_ACTION";  
//  public static final String SERVER_RESULT_ACTION =
//      PACKAGE_PREFIX + ".SERVER_RESULT_ACTION";
////  public static final String SCHEDULE_UPDATE_ACTION =
////      PACKAGE_PREFIX + ".SCHEDULE_UPDATE_ACTION";
//
//  // TODO(Hongyi): make it formal
//  public static final String APP_ACTION = PACKAGE_PREFIX + ".APP_ACTION";
  /**
   * Creates an intent of the specified action with an optional message
   */
  protected MobiperfIntent(String strMsg, String action)
      throws InvalidParameterException {
    super();
    if (action == null) {
      throw new InvalidParameterException("action of UpdateIntent should not be null");
    }
    this.setAction(action);
    this.putExtra(MSG_PAYLOAD, strMsg);
  }
}
