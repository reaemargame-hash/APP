package com.ascent.quest;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class NativeScheduler {
  static final String PREFS = "ascent_native";
  static final String CH_DEFAULT = "ascent_reminders_default";
  static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);}
  static String channel(Context c){int ver=prefs(c).getInt("sound_version",0);return ver>0?"ascent_reminders_audio_"+ver:CH_DEFAULT;}
  static void channels(Context c){
    NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    if(nm.getNotificationChannel(CH_DEFAULT)==null){NotificationChannel nc=new NotificationChannel(CH_DEFAULT,"ASCENT Quest Alerts",NotificationManager.IMPORTANCE_HIGH);nc.enableVibration(true);nm.createNotificationChannel(nc);}
    if(prefs(c).getInt("sound_version",0)>0){String id=channel(c);if(nm.getNotificationChannel(id)==null){NotificationChannel nc=new NotificationChannel(id,"ASCENT Custom Quest Sound",NotificationManager.IMPORTANCE_HIGH);nc.setSound(Uri.parse("content://com.ascent.quest.sounds/audio/"+prefs(c).getInt("sound_version",0)),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());nc.enableVibration(true);nm.createNotificationChannel(nc);}}
  }
  static int id(String key){return (key.hashCode()&0x7fffffff);}
  static PendingIntent pi(Context c,int key){Intent i=new Intent(c,AlarmReceiver.class);return PendingIntent.getBroadcast(c,key,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  static void schedule(Context c,String json){
    try{
      AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
      String old=prefs(c).getString("reminder_ids","[]");JSONArray ids=new JSONArray(old);for(int j=0;j<ids.length();j++)am.cancel(pi(c,ids.getInt(j)));
      JSONArray events=new JSONArray(json),newIds=new JSONArray();
      for(int j=0;j<Math.min(110,events.length());j++){
        JSONObject e=events.optJSONObject(j);if(e==null)continue;
        String key=e.optString("key"),title=e.optString("title"),body=e.optString("body"),time=e.optString("time");if(key.length()==0||title.length()==0)continue;
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);f.setLenient(false);
        Date dt=f.parse(time);if(dt==null||dt.getTime()<System.currentTimeMillis()+1000)continue;
        int request=id(key);Intent intent=new Intent(c,AlarmReceiver.class);intent.putExtra("title",title);intent.putExtra("body",body);intent.putExtra("key",key);
        PendingIntent pi=PendingIntent.getBroadcast(c,request,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,dt.getTime(),pi);newIds.put(request);
      }
      prefs(c).edit().putString("reminders",json).putString("reminder_ids",newIds.toString()).apply();
    }catch(Exception e){android.util.Log.e("ASCENT","schedule failed",e);}
  }
}
