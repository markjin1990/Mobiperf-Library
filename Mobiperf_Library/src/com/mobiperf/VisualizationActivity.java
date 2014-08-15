package com.mobiperf;

import java.lang.reflect.Field;

import com.mobilyzer.util.Logger;
import com.mobilyzer.util.PhoneUtils;
import com.mobiperf.R;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



public class VisualizationActivity extends Activity {
  public static final String TAB_TAG = "MEASUREMENT_VISUALIZATION";

  //    private Button button;
  private WebView webView;
  //  private Context context;
  private TextView locationTextView;
  RelativeLayout webViewPlaceholder;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Logger.e("ashnik_debug: onCreate");
    setContentView(R.layout.visualization);
    //    this.context=this;

    
    webViewPlaceholder= (RelativeLayout) findViewById(R.id.webViewPlaceholder);
    //        button = (Button) findViewById(R.id.closeButton);
    locationTextView= (TextView) findViewById(R.id.locationTextView);

    webView = (WebView) findViewById(R.id.webView);
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    PhoneUtils phoneUtils=PhoneUtils.getPhoneUtils();
    Location location=null;
    if(phoneUtils!=null){
      location=phoneUtils.getLocation();
    }

    Toast.makeText(getApplicationContext(), "Please wait for the page to load...", Toast.LENGTH_LONG).show();
    if(location==null ||location.getLatitude()==0.0 && location.getLongitude()==0.0){
      locationTextView.setText("Location Service is not enabled.");
//      webView.loadUrl("http://walrus.eecs.umich.edu/openmobiledata/visualization/heatmap/mobile");
      webView.loadUrl("http://openmobiledata.appspot.com/visualization");
    }else{
      String loc_string=((int)(location.getLatitude()*1000))/1000.0+","+((int)(location.getLongitude()*1000))/1000.0;
      locationTextView.setText("");
//      webView.loadUrl("http://walrus.eecs.umich.edu/openmobiledata/visualization/heatmap/mobile?location="+loc_string);
      webView.loadUrl("http://openmobiledata.appspot.com/visualization?location="+loc_string);
    }


    setConfigCallback((WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

  }

  @Override
  protected void onPause() {
    Logger.e("ashnik_debug: onPause");
    webView.destroyDrawingCache();

    super.onPause();
  }

  @Override
  protected void onResume() {
    Logger.e("ashnik_debug: onResume");
    webView.destroyDrawingCache();

    //    Location location=PhoneUtils.getPhoneUtils().getLocation();
    //    if(location.getLatitude()==0.0 && location.getLongitude()==0.0){
    //      locationTextView.setText("Location Service is not enabled.");
    //      webView.loadUrl("http://walrus.eecs.umich.edu/openmobiledata/visualization/heatmap/mobile");
    //    }else{
    //      String loc_string=((int)(location.getLatitude()*1000))/1000.0+","+((int)(location.getLongitude()*1000))/1000.0;
    //      locationTextView.setText("Current Location: "+loc_string);
    //      webView.loadUrl("http://walrus.eecs.umich.edu/openmobiledata/visualization/heatmap/mobile?location="+loc_string);  
    //    }

    super.onResume();
  }

  @Override
  protected void onDestroy() {
    Logger.e("ashnik_debug: onDestroy");
    webViewPlaceholder.removeAllViews();
    if(webView!=null){
      webView.removeAllViews();
      webView.destroyDrawingCache();
      webView.destroy();
    }


    setConfigCallback(null);
    unbindDrawables(findViewById(R.id.rootView));
    webView=null;
    System.gc();

//    finish();

    super.onDestroy();
  }


  private void unbindDrawables(View view) {
    if (view.getBackground() != null) {
      view.getBackground().setCallback(null);
    }
    if (view instanceof ViewGroup) {
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        unbindDrawables(((ViewGroup) view).getChildAt(i));
      }
      ((ViewGroup) view).removeAllViews();
    }
  }

  public void setConfigCallback(WindowManager windowManager) {
    try {
      Field field = WebView.class.getDeclaredField("mWebViewCore");
      field = field.getType().getDeclaredField("mBrowserFrame");
      field = field.getType().getDeclaredField("sConfigCallback");
      field.setAccessible(true);
      Object configCallback = field.get(null);

      if (null == configCallback) {
        return;
      }

      field = field.getType().getDeclaredField("mWindowManager");
      field.setAccessible(true);
      field.set(configCallback, windowManager);
    } catch(Exception e) {
    }
  }


}
