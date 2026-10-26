package com.ascent.quest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.webkit.*;
import android.net.Uri;
import android.view.*;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import java.io.*;

public class MainActivity extends Activity {
 private static final int PERMISSION_LOCATION=100,PERMISSION_NOTIFICATION=101,SOUND_PICK=102,WEB_PICK=103;
 WebView web;
 private ValueCallback<Uri[]> fileResult;

 @SuppressLint("SetJavaScriptEnabled") @Override public void onCreate(Bundle saved){super.onCreate(saved);
  NativeScheduler.channels(this);
  web=new WebView(this);web.setBackgroundColor(0xFF080D18);setContentView(web);
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
  settings.setAllowFileAccess(false);settings.setAllowContentAccess(true);
  final WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClientCompat(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return loader.shouldInterceptRequest(request.getUrl());}
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){Uri u=req.getUrl();if("appassets.androidplatform.net".equals(u.getHost()))return false;if("https".equals(u.getScheme())){startActivity(new Intent(Intent.ACTION_VIEW,u));}return true;}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){callback.invoke(origin,checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED,false);}
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){
     if(fileResult!=null)fileResult.onReceiveValue(null);fileResult=callback;
     try{startActivityForResult(params.createIntent(),WEB_PICK);return true;}
     catch(Exception ex){fileResult=null;callback.onReceiveValue(null);return false;}
   }
  });
  web.addJavascriptInterface(new Bridge(),"AscentNative");
  web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 public class Bridge {
  @JavascriptInterface public void scheduleReminders(String json){runOnUiThread(()->NativeScheduler.schedule(MainActivity.this,json));}
  @JavascriptInterface public void updateWidget(String json){runOnUiThread(()->{NativeScheduler.prefs(MainActivity.this).edit().putString("widget",json).apply();QuestWidget.refresh(MainActivity.this);});}
  @JavascriptInterface public void requestPermission(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},PERMISSION_NOTIFICATION);});}
  @JavascriptInterface public void importSoundData(String dataUri,String mime){runOnUiThread(()->{
    try{int index=dataUri.indexOf(',');if(index<0)throw new IOException("invalid format");byte[] bytes=android.util.Base64.decode(dataUri.substring(index+1),android.util.Base64.DEFAULT);if(bytes.length>3_000_000)throw new IOException("audio too large");
      try(FileOutputStream out=openFileOutput("quest_alert.mp3",MODE_PRIVATE)){out.write(bytes);}
      android.content.SharedPreferences p=NativeScheduler.prefs(MainActivity.this);p.edit().putInt("sound_version",p.getInt("sound_version",0)+1).putString("sound_mime",mime).apply();NativeScheduler.channels(MainActivity.this);NativeScheduler.schedule(MainActivity.this,p.getString("reminders","[]"));
      new AlertDialog.Builder(MainActivity.this).setMessage("ตั้งเสียงเควสสำหรับแจ้งเตือน Android แล้ว").setPositiveButton("ตกลง",null).show();
    }catch(Exception ex){new AlertDialog.Builder(MainActivity.this).setMessage("ตั้งเสียงไม่สำเร็จ: "+ex.getMessage()).setPositiveButton("ตกลง",null).show();}
  });}
  @JavascriptInterface public void setSound(String command){runOnUiThread(()->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("audio/*");startActivityForResult(pick,SOUND_PICK);});}
  @JavascriptInterface public void startRun(){runOnUiThread(()->{if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_LOCATION);}else startRunService();});}
  @JavascriptInterface public void pauseRun(){runOnUiThread(()->startService(new Intent(MainActivity.this,RunService.class).setAction("PAUSE")));}
  @JavascriptInterface public void finishRun(){runOnUiThread(()->startService(new Intent(MainActivity.this,RunService.class).setAction("FINISH")));}
  @JavascriptInterface public String getRunState(){return RunService.state(MainActivity.this);}
 }
 void startRunService(){try{startForegroundService(new Intent(this,RunService.class).setAction("START"));}catch(Exception ex){android.util.Log.e("ASCENT","run start",ex);}}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==PERMISSION_LOCATION&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)startRunService();}
 @Override protected void onActivityResult(int request,int result,Intent info){super.onActivityResult(request,result,info);
   if(request==WEB_PICK){if(fileResult!=null){fileResult.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,info));fileResult=null;}return;}
   if(request!=SOUND_PICK||result!=RESULT_OK||info==null||info.getData()==null)return;
   try{Uri uri=info.getData();String mime=getContentResolver().getType(uri);try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=openFileOutput("quest_alert.mp3",MODE_PRIVATE)){
     if(in==null)throw new IOException("No audio");byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){total+=n;if(total>3_000_000)throw new IOException("audio too large");out.write(buffer,0,n);} }
     android.content.SharedPreferences p=NativeScheduler.prefs(this);p.edit().putInt("sound_version",p.getInt("sound_version",0)+1).putString("sound_mime",mime==null?"audio/mpeg":mime).apply();NativeScheduler.channels(this);NativeScheduler.schedule(this,p.getString("reminders","[]"));
     new AlertDialog.Builder(this).setMessage("เลือกเสียงแจ้งเตือนแล้ว · อาจต้องอนุญาตเสียงในตั้งค่า Notification ของ Android").setPositiveButton("ตกลง",null).show();
   }catch(Exception ex){new AlertDialog.Builder(this).setMessage("บันทึกเสียงไม่สำเร็จ: "+ex.getMessage()).setPositiveButton("ตกลง",null).show();}
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}
