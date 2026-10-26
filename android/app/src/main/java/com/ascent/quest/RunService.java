package com.ascent.quest;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.*;

/** Foreground tracking continues when the phone screen turns off, while the service remains alive.
    The user must start this from a visible Activity. No continuous tracking without consent. */
public class RunService extends Service implements LocationListener {
 public static final String CHANNEL="ascent_gps";
 LocationManager lm;Location last;
 static android.content.SharedPreferences p(Context c){return c.getSharedPreferences("ascent_run",MODE_PRIVATE);}
 static String state(Context c){android.content.SharedPreferences p=p(c);try{JSONObject j=new JSONObject();boolean active=p.getBoolean("active",false);long elapsed=p.getLong("elapsed",0)+(active?Math.max(0,System.currentTimeMillis()-p.getLong("started",System.currentTimeMillis())):0);j.put("active",active);j.put("elapsed",elapsed);j.put("km",p.getFloat("distance",0));j.put("points",p.getInt("points",0));return j.toString();}catch(Exception e){return "{}";}}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
   String action=intent==null?"":intent.getAction();
   if("FINISH".equals(action)){pause();p(this).edit().clear().apply();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
   if("PAUSE".equals(action)){pause();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
   if(!"START".equals(action)){stopSelf();return START_NOT_STICKY;}
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
   android.content.SharedPreferences p=p(this);p.edit().putBoolean("active",true).putLong("started",System.currentTimeMillis()).apply();
   try{lm=(LocationManager)getSystemService(LOCATION_SERVICE);if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500,3,this,Looper.getMainLooper());}else{pause();stopSelf();}}catch(Exception ex){android.util.Log.e("ASCENT","location",ex);pause();stopSelf();}
   return START_STICKY;
 }
 private void pause(){android.content.SharedPreferences p=p(this);if(p.getBoolean("active",false))p.edit().putLong("elapsed",p.getLong("elapsed",0)+Math.max(0,System.currentTimeMillis()-p.getLong("started",System.currentTimeMillis()))).putBoolean("active",false).apply();try{if(lm!=null)lm.removeUpdates(this);}catch(Exception ignored){}last=null;}
 @Override public void onLocationChanged(Location location){if(!p(this).getBoolean("active",false))return;if(location.getAccuracy()>40)return;
  if(last!=null){float meters=last.distanceTo(location);float elapsed=Math.max(1,(location.getTime()-last.getTime())/1000f);if(meters>5&&meters<100&&meters/elapsed<5.5f){android.content.SharedPreferences p=p(this);p.edit().putFloat("distance",p.getFloat("distance",0)+meters/1000f).putInt("points",p.getInt("points",0)+1).apply();last=location;}else if(meters>100||meters/elapsed>=5.5f)last=location;}else{last=location;p(this).edit().putInt("points",p(this).getInt("points",0)+1).apply();}
 }
 @Override public void onDestroy(){pause();super.onDestroy();}
}
