package com.ascent.quest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
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
 private static final int PERMISSION_LOCATION=100,PERMISSION_NOTIFICATION=101,SOUND_PICK=102,WEB_PICK=103,BACKUP_CREATE=104,PERMISSION_CAMERA=105;
 WebView web;
 private ValueCallback<Uri[]> fileResult;
 private PermissionRequest pendingCameraRequest;
 private String pendingBackup=null;

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
   @Override public void onPermissionRequest(PermissionRequest request){
     runOnUiThread(()->{
       Uri origin=request.getOrigin();
       if(origin==null||!"https".equals(origin.getScheme())||!"appassets.androidplatform.net".equals(origin.getHost())){request.deny();return;}
       boolean camera=false;for(String item:request.getResources())if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(item))camera=true;
       if(!camera){request.deny();return;}
       if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});}
       else{pendingCameraRequest=request;requestPermissions(new String[]{Manifest.permission.CAMERA},PERMISSION_CAMERA);}
     });
   }
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
  /** Live native diagnostics: a request being accepted is NOT proof of placement. */
  @JavascriptInterface public String widgetStatus(){
    org.json.JSONObject result=new org.json.JSONObject();
    try{
      AppWidgetManager manager=AppWidgetManager.getInstance(MainActivity.this);
      ComponentName component=new ComponentName(MainActivity.this,QuestWidget.class);
      boolean registered=false;
      for(AppWidgetProviderInfo info:manager.getInstalledProviders()){
        if(component.equals(info.provider)){registered=true;break;}
      }
      int[] installedIds=manager.getAppWidgetIds(component);
      android.content.pm.PackageInfo pkg=getPackageManager().getPackageInfo(getPackageName(),0);
      long version=Build.VERSION.SDK_INT>=28?pkg.getLongVersionCode():pkg.versionCode;
      android.content.SharedPreferences prefs=NativeScheduler.prefs(MainActivity.this);
      result.put("native",true);
      result.put("version",pkg.versionName);
      result.put("versionCode",version);
      result.put("registered",registered);
      result.put("pinned",installedIds.length);
      result.put("pinSupported",Build.VERSION.SDK_INT>=26&&manager.isRequestPinAppWidgetSupported());
      result.put("pinState",prefs.getString("widget_pin_state","never"));
      result.put("pinResultId",prefs.getInt("widget_pin_result_id",-1));
      result.put("pinError",prefs.getString("widget_pin_error",""));
      long requestedAt=prefs.getLong("widget_pin_requested_at",0);
      result.put("pinAgeSeconds",requestedAt>0?Math.max(0,(System.currentTimeMillis()-requestedAt)/1000):0);
      try{
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo launcher=getPackageManager().resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);
        result.put("launcher",launcher!=null&&launcher.activityInfo!=null?launcher.activityInfo.packageName:"unknown");
      }catch(Exception ignored){result.put("launcher","unknown");}
    }catch(Exception ex){
      try{result.put("error",ex.getClass().getSimpleName()+": "+ex.getMessage());}catch(Exception ignored){}
    }
    return result.toString();
  }
  /** Request Xiaomi/Android placement; report actual result via WidgetPinResultReceiver. */
  @JavascriptInterface public void pinWidget(){runOnUiThread(()->{
    android.content.SharedPreferences prefs=NativeScheduler.prefs(MainActivity.this);
    try{
      AppWidgetManager manager=AppWidgetManager.getInstance(MainActivity.this);
      ComponentName provider=new ComponentName(MainActivity.this,QuestWidget.class);
      boolean registered=false;
      for(AppWidgetProviderInfo info:manager.getInstalledProviders()){
        if(provider.equals(info.provider)){registered=true;break;}
      }
      if(!registered){
        prefs.edit().putString("widget_pin_state","unregistered").apply();
        new AlertDialog.Builder(MainActivity.this).setTitle("Android ไม่พบวิดเจ็ต ASCENT")
          .setMessage("APK ที่ติดตั้งยังไม่มีวิดเจ็ตที่ลงทะเบียนถูกต้อง กรุณาตรวจว่าเป็น APK v3.8.9 (build 18) ที่ Build จากโปรเจกต์นี้ ไม่ใช่เวอร์ชันเว็บ")
          .setPositiveButton("ตกลง",null).show();return;
      }
      if(Build.VERSION.SDK_INT>=26&&manager.isRequestPinAppWidgetSupported()){
        Intent callback=new Intent(MainActivity.this,WidgetPinResultReceiver.class).setAction("com.ascent.quest.WIDGET_PIN_CONFIRMED");
        PendingIntent success=PendingIntent.getBroadcast(MainActivity.this,987,callback,
          PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        prefs.edit().putString("widget_pin_state","requested")
          .putLong("widget_pin_requested_at",System.currentTimeMillis())
          .remove("widget_pin_error").remove("widget_pin_result_id").apply();
        if(manager.requestPinAppWidget(provider,null,success)){
          android.widget.Toast.makeText(MainActivity.this,"ส่งคำขอแล้ว โปรดยืนยันการเพิ่มวิดเจ็ต และกลับมาตรวจสถานะ",android.widget.Toast.LENGTH_LONG).show();
          return;
        }
      }
      prefs.edit().putString("widget_pin_state","unsupported").apply();
      new AlertDialog.Builder(MainActivity.this).setTitle("Launcher ไม่รองรับการเพิ่มอัตโนมัติ")
        .setMessage("ให้เพิ่มด้วยตนเอง: แตะค้างบนหน้าจอหลัก → วิดเจ็ต → เข้าเมนู Android widgets / วิดเจ็ต Android หรือหมวดวิดเจ็ตจากแอป → ASCENT Fitness RPG\n\nช่องค้นหาวิดเจ็ตแนะนำของ Xiaomi อาจไม่แสดงแอปภายนอก")
        .setPositiveButton("ตกลง",null).show();
    }catch(Exception ex){
      prefs.edit().putString("widget_pin_state","error")
        .putString("widget_pin_error",ex.getClass().getSimpleName()+": "+ex.getMessage()).apply();
      android.util.Log.e("ASCENT","pin widget failed",ex);
      new AlertDialog.Builder(MainActivity.this).setTitle("เพิ่มวิดเจ็ตไม่ได้")
        .setMessage("Android Launcher ตอบกลับข้อผิดพลาด: "+ex.getClass().getSimpleName())
        .setPositiveButton("ตกลง",null).show();
    }
  });}

  /** Offline on-device ML Kit text recognition for nutrition labels. Never uploads the photo. */
  @JavascriptInterface public void recognizeNutritionImage(String dataUri){
    new Thread(()->{
      try{
        if(dataUri==null || dataUri.length()>7_500_000)throw new IOException("image too large");
        int comma=dataUri.indexOf(',');if(comma<0||!dataUri.substring(0,comma).startsWith("data:image/"))throw new IOException("not an image");
        byte[] content=android.util.Base64.decode(dataUri.substring(comma+1),android.util.Base64.DEFAULT);
        if(content.length>5_000_000)throw new IOException("image too large");
        android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(content,0,content.length);
        if(bitmap==null)throw new IOException("invalid image");
        com.google.mlkit.vision.common.InputImage image=com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap,0);
        com.google.mlkit.vision.text.TextRecognizer recognizer=com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image).addOnSuccessListener(result->{
          try{org.json.JSONObject payload=new org.json.JSONObject();payload.put("ok",true);payload.put("text",result.getText());sendNutritionOcr(payload);}catch(Exception ignored){}
          bitmap.recycle();recognizer.close();
        }).addOnFailureListener(error->{sendNutritionOcrError(error.getMessage());bitmap.recycle();recognizer.close();});
      }catch(Exception ex){sendNutritionOcrError(ex.getMessage());}
    }).start();
  }
  @JavascriptInterface public void openHealthPermissions(){runOnUiThread(()->HealthConnectBridge.permissions(MainActivity.this));}
  @JavascriptInterface public void readHealthConnect(){runOnUiThread(()->HealthConnectBridge.read(MainActivity.this));}
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
  @JavascriptInterface public void exportBackup(String json){runOnUiThread(()->{
    try{if(json==null||json.length()>8_000_000)throw new IOException("Backup too large");pendingBackup=json;
      Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT);create.addCategory(Intent.CATEGORY_OPENABLE);
      create.setType("application/json");create.putExtra(Intent.EXTRA_TITLE,"ascent-backup.json");
      startActivityForResult(create,BACKUP_CREATE);
    }catch(Exception ex){new AlertDialog.Builder(MainActivity.this).setMessage("Backup error: "+ex.getMessage()).setPositiveButton("OK",null).show();}
  });}
  @JavascriptInterface public String getRunState(){return RunService.state(MainActivity.this);}
  @JavascriptInterface public void storeAutoBackup(String json){
   if(json==null||json.length()>3_000_000)return;
   try(java.io.FileOutputStream out=openFileOutput("ascent-autobackup.json",MODE_PRIVATE)){
    out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   }catch(Exception ex){android.util.Log.w("ASCENT","auto backup failed",ex);}
  }
  @JavascriptInterface public String getAutoBackup(){
   try(java.io.FileInputStream in=openFileInput("ascent-autobackup.json")){
    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;
    while((n=in.read(bytes))!=-1){if(out.size()>3_000_000)return "{}";out.write(bytes,0,n);}
    return out.toString("UTF-8");
   }catch(Exception ex){return "{}";}
  }
  @JavascriptInterface public int consumeWidgetWater(){
   android.content.SharedPreferences p=NativeScheduler.prefs(MainActivity.this);
   String today=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date());
   int amount=today.equals(p.getString("pending_widget_day",""))?Math.max(0,Math.min(12,p.getInt("pending_widget_water",0))):0;
   p.edit().putInt("pending_widget_water",0).apply();return amount;
  }
 }
 void sendNutritionOcr(org.json.JSONObject payload){if(web!=null)web.post(()->web.evaluateJavascript("window.ascentNutritionOcrResult&&window.ascentNutritionOcrResult("+org.json.JSONObject.quote(payload.toString())+")",null));}
 void sendNutritionOcrError(String message){try{org.json.JSONObject out=new org.json.JSONObject();out.put("ok",false);out.put("error",message==null?"OCR failed":message);sendNutritionOcr(out);}catch(Exception ignored){}}
 void runError(String message){if(web==null)return;web.post(()->web.evaluateJavascript("window.ascentNativeRunError&&window.ascentNativeRunError("+org.json.JSONObject.quote(message)+")",null));}
 void startRunService(){try{startForegroundService(new Intent(this,RunService.class).setAction("START"));}catch(Exception ex){android.util.Log.e("ASCENT","run start",ex);runError("ไม่สามารถเปิดบริการ GPS: "+ex.getMessage());}}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==PERMISSION_LOCATION){if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)startRunService();else runError("ยังไม่ได้อนุญาตตำแหน่ง GPS กรุณาให้สิทธิ์ตำแหน่งแบบแม่นยำ");}if(request==PERMISSION_CAMERA&&pendingCameraRequest!=null){PermissionRequest pr=pendingCameraRequest;pendingCameraRequest=null;if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)pr.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});else pr.deny();}}
 @Override protected void onResume(){super.onResume();if(web!=null)web.postDelayed(()->web.evaluateJavascript("window.ascentWidgetSync&&window.ascentWidgetSync();window.ascentWidgetRefreshIfOpen&&window.ascentWidgetRefreshIfOpen()",null),400);}
 @Override protected void onActivityResult(int request,int result,Intent info){super.onActivityResult(request,result,info);
   if(request==BACKUP_CREATE){String json=pendingBackup;pendingBackup=null;
    if(result==RESULT_OK&&info!=null&&info.getData()!=null&&json!=null){
      try(java.io.OutputStream stream=getContentResolver().openOutputStream(info.getData())){
        if(stream==null)throw new IOException("Cannot open document");stream.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        new AlertDialog.Builder(this).setMessage("สำรองข้อมูล ASCENT สำเร็จแล้ว").setPositiveButton("ตกลง",null).show();
      }catch(Exception ex){new AlertDialog.Builder(this).setMessage("สำรองข้อมูลไม่สำเร็จ: "+ex.getMessage()).setPositiveButton("ตกลง",null).show();}
    }return;
   }
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
