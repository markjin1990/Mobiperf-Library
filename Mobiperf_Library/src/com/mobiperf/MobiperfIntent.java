package com.mobiperf;



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
  
  // Different types of actions that this intent can represent:
  private static final String PACKAGE_PREFIX =
      MobiperfIntent.class.getPackage().getName();
  public static final String SYSTEM_STATUS_UPDATE_ACTION =
      PACKAGE_PREFIX + ".SYSTEM_STATUS_UPDATE_ACTION";
  public static final String SCHEDULER_CONNECTED_ACTION =
      PACKAGE_PREFIX + ".SCHEDULER_CONNECTED_ACTION";
  
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
