package com.ascent.quest;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.*;
import java.util.*;

/** Foreground tracking continues when the phone screen turns off, while the service remains alive.
    The user must start this from a visible Activity. No continuous tracking without consent. */
public class RunService extends Service implements LocationListener {
 public static final String CHANNEL="ascent_gps";
 LocationManager lm;Location last;long stillSince=0;int rejected=0;
 static android.content.SharedPreferences p(Context c){return c.getSharedPreferences("ascent_run",MODE_PRIVATE);}
 static String state(Context c){android.content.SharedPreferences p=p(c);try{JSONObject j=new JSONObject();boolean active=p.getBoolean("active",false);long now=System.currentTimeMillis();
    long elapsed=p.getLong("elapsed",0)+(active?Math.max(0,now-p.getLong("started",now)):0)
      -p.getLong("idle_ms",0)-(p.getBoolean("auto_paused",false)?Math.max(0,now-p.getLong("idle_since",now)):0);
    elapsed=Math.max(0,elapsed);j.put("active",active);j.put("elapsed",elapsed);j.put("km",p.getFloat("distance",0));j.put("points",p.getInt("points",0));j.put("autoPaused",p.getBoolean("auto_paused",false));j.put("route",new JSONArray(p.getString("route","[]")));return j.toString();}catch(Exception e){return "{}";}}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
   String action=intent==null?"":intent.getAction();
   if("FINISH".equals(action)){pause();p(this).edit().clear().apply();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
   if("PAUSE".equals(action)){pause();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
   if(!"START".equals(action)){if(intent==null&&p(this).getBoolean("active",false))action="START";else{stopSelf();return START_NOT_STICKY;}}
   NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   nm.createNotificationChannel(new NotificationChannel(CHANNEL,"ASCENT GPS tracking",NotificationManager.IMPORTANCE_LOW));
   Intent open=new Intent(this,MainActivity.class);
   PendingIntent click=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Intent pause=new Intent(this,RunService.class).setAction("PAUSE");
   PendingIntent pausePi=PendingIntent.getService(this,2,pause,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_launcher)
     .setContentTitle("ASCENT · กำลังบันทึกการวิ่ง").setContentText("GPS กำลังทำงาน แตะเพื่อกลับไปดูระยะ")
     .setContentIntent(click).setOngoing(true).addAction(new Notification.Action.Builder(R.drawable.ic_launcher,"หยุดชั่วคราว",pausePi).build()).build();
   if(Build.VERSION.SDK_INT>=29)startForeground(9821,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(9821,n);
   android.content.SharedPreferences p=p(this);
   boolean resuming=p.getBoolean("active",false);long previousStart=p.getLong("started",System.currentTimeMillis());
   p.edit().putBoolean("active",true).putLong("started",System.currentTimeMillis()).apply();
   if(resuming){p.edit().putLong("elapsed",p.getLong("elapsed",0)+Math.min(120000,Math.max(0,System.currentTimeMillis()-previousStart))).apply();}
   try{lm=(LocationManager)getSystemService(LOCATION_SERVICE);if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500,3,this,Looper.getMainLooper());}else{pause();stopSelf();}}catch(Exception ex){android.util.Log.e("ASCENT","location",ex);pause();stopSelf();}
   return START_STICKY;
 }
 private void pause(){android.content.SharedPreferences p=p(this);if(p.getBoolean("active",false)){
   long now=System.currentTimeMillis();long idle=p.getLong("idle_ms",0)+(p.getBoolean("auto_paused",false)?Math.max(0,now-p.getLong("idle_since",now)):0);
   long total=p.getLong("elapsed",0)+Math.max(0,now-p.getLong("started",now));
   p.edit().putLong("elapsed",Math.max(0,total-idle)).putLong("idle_ms",0).putBoolean("auto_paused",false).putBoolean("active",false).apply();}try{if(lm!=null)lm.removeUpdates(this);}catch(Exception ignored){}last=null;stillSince=0;}
 @Override public void onLocationChanged(Location location){
  android.content.SharedPreferences pref=p(this);
  if(!pref.getBoolean("active",false)||!location.hasAccuracy()||location.getAccuracy()>35)return;
  // Reject impossible jumps; route consists of accepted GPS positions only. No manual distance input.
  if(last!=null){
   float meters=last.distanceTo(location),secs=Math.max(1,(location.getElapsedRealtimeNanos()-last.getElapsedRealtimeNanos())/1_000_000_000f);
   float speed=meters/secs;
   if(meters<4){if(stillSince==0)stillSince=System.currentTimeMillis();if(System.currentTimeMillis()-stillSince>80000&&!pref.getBoolean("auto_paused",false))pref.edit().putBoolean("auto_paused",true).putLong("idle_since",System.currentTimeMillis()).apply();return;}
   if(meters>100||speed>5.5f||secs>180){rejected++;last=location;return;}
   stillSince=0;
   if(pref.getBoolean("auto_paused",false))pref.edit().putLong("idle_ms",pref.getLong("idle_ms",0)+Math.max(0,System.currentTimeMillis()-pref.getLong("idle_since",System.currentTimeMillis()))).putBoolean("auto_paused",false).apply();
   if(meters>=4){pref.edit().putFloat("distance",pref.getFloat("distance",0)+meters/1000f).putInt("points",pref.getInt("points",0)+1).apply();}
  }else{pref.edit().putInt("points",pref.getInt("points",0)+1).apply();}
  last=location;
  try{JSONArray route=new JSONArray(pref.getString("route","[]"));if(route.length()<1800){JSONArray point=new JSONArray();point.put(Math.round(location.getLatitude()*1e6)/1e6);point.put(Math.round(location.getLongitude()*1e6)/1e6);point.put(location.getTime());route.put(point);pref.edit().putString("route",route.toString()).apply();}}catch(Exception ex){android.util.Log.w("ASCENT","GPS route",ex);}
 }
 @Override public void onDestroy(){pause();super.onDestroy();}
}
