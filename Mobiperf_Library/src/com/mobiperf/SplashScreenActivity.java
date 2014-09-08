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


import com.mobiperf.R;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The splash screen for Speedometer
 */
public class SplashScreenActivity extends Activity {
  private Bitmap logo;
  private ImageView logoView;
  
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.splash_screen);
    // Make sure the splash screen is shown in portrait orientation
    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    // Show Logo
    logoView = (ImageView)findViewById(R.id.splash_logo);
//    logo = BitmapFactory.decodeResource(getResources(), R.drawable.splashscreen);
//    if (logo != null) {
//      logoView.setImageBitmap(logo);
//    }
    
    Picasso.with(this).load(R.drawable.splashscreen).into(logoView);
    
    // Display version
    TextView version = (TextView)findViewById(R.id.splash_version);
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      version.setText(pInfo.versionName);
    } catch (NameNotFoundException e) {
    }
    
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        Intent intent = new Intent();
        intent.setClassName(SplashScreenActivity.this.getApplicationContext(),
                            SpeedometerApp.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        SplashScreenActivity.this.getApplication().startActivity(intent);
        SplashScreenActivity.this.finish();
        // Recycle logo bitmap to avoid OOM exception
//        if (logo!=null && !logo.isRecycled()) {
//          Logger.i("Recycle logo bitmap");
//          logo.recycle();
//          logo=null;
//          System.gc();
//        }
        
//        BitmapDrawable bitmapDrawable = ((BitmapDrawable) logoView.getDrawable());
        
//
//        if (null != bitmapDrawable && !bitmapDrawable.getBitmap().isRecycled()) {
//          Logger.e("Recycle logo bitmap");
//            bitmapDrawable.getBitmap().recycle();
//        } else {
//
//            Logger.e("Bitmap is already recycled");
//        }
//
//        bitmapDrawable = null;
        System.gc();
        
      }
    }, MobiperfConfig.SPLASH_SCREEN_DURATION_MSEC);

  }
  
}
