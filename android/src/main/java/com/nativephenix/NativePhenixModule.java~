package com.nativephenix;

import android.widget.Toast;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.nativephenix.ui.MainActivity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class NativePhenixModule extends ReactContextBaseJavaModule {

  Context context;

  public NativePhenixModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.context=reactContext;
  }

  @Override
  public String getName() {
    return "NativePhenixModule";
  }

  @ReactMethod
  public void alert(String message) {
    Toast.makeText(getReactApplicationContext(), message, Toast.LENGTH_LONG).show();
  }

  @ReactMethod
  public void show() {
    Intent intent1=new Intent(getCurrentActivity(),MainActivity.class);
    intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent1);
  }

}
