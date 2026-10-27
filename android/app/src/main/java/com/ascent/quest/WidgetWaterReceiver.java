package com.ascent.quest;
import android.content.*;
/** User-initiated widget action: queues one drink entry for the app to consume atomically. */
public class WidgetWaterReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  if(i==null||!"com.ascent.quest.ADD_WATER".equals(i.getAction()))return;
  android.content.SharedPreferences p=NativeScheduler.prefs(c);
  String today=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date());
  int old=today.equals(p.getString("pending_widget_day",""))?p.getInt("pending_widget_water",0):0;
  int n=Math.min(12,Math.max(0,old)+1);
  p.edit().putString("pending_widget_day",today).putInt("pending_widget_water",n).apply();QuestWidget.refresh(c);
 }
}
