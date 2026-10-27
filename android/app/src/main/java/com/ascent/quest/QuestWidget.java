package com.ascent.quest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.util.Locale;

/** Real Android home-screen widget: one tap opens ASCENT, no overlay window. */
public class QuestWidget extends AppWidgetProvider {
  static void refresh(Context context) {
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    int[] ids = manager.getAppWidgetIds(new ComponentName(context, QuestWidget.class));
    if (ids.length > 0) draw(context, manager, ids);
  }

  static void draw(Context context, AppWidgetManager manager, int[] ids) {
    int level = 1, exp = 0, water = 0, waterGoal = 6;
    double totalKm = 0, todayKm = 0, goalKm = 0;
    String rank = "E", next = "เปิด ASCENT เพื่อดูเควส";
    boolean done = false;
    String widgetDay = "";
    String json = NativeScheduler.prefs(context).getString("widget", "{}");
    try {
      JSONObject data = new JSONObject(json);
      widgetDay = data.optString("day", "");
      level = Math.max(1,data.optInt("level",1));
      exp = Math.max(0,data.optInt("xp",0));
      rank = data.optString("rank","E");
      totalKm = Math.max(0,data.optDouble("km",0));
      todayKm = Math.max(0,data.optDouble("todayKm",0));
      goalKm = Math.max(0,data.optDouble("goalKm",0));
      done = data.optBoolean("done",false);
      water = Math.max(0,data.optInt("water",0));
      waterGoal = Math.max(1,data.optInt("waterGoal",6));
      next = data.optString("next",next);
    } catch (Exception ignored) { }
    if (rank.isEmpty() || rank.length() > 2) rank = "E";
    Intent open = new Intent(context, MainActivity.class);
    open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent openApp = PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Intent waterAction = new Intent(context, WidgetWaterReceiver.class).setAction("com.ascent.quest.ADD_WATER");
    PendingIntent addWater = PendingIntent.getBroadcast(context,42,waterAction,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    android.content.SharedPreferences prefs = NativeScheduler.prefs(context);
    String today = new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date());
    if (!today.equals(widgetDay)) { todayKm = 0; goalKm = 0; done = false; next = "เปิด ASCENT เพื่อดูภารกิจวันนี้"; }
    int pending = today.equals(prefs.getString("pending_widget_day","")) ? prefs.getInt("pending_widget_water",0) : 0;
    for (int id : ids) {
      RemoteViews view = new RemoteViews(context.getPackageName(),R.layout.quest_widget);
      view.setTextViewText(R.id.widget_level,"LV."+level+"  ·  "+rank+"-RANK");
      view.setTextViewText(R.id.widget_exp,exp+" EXP");
      view.setTextViewText(R.id.widget_distance,goalKm>0
          ?String.format(Locale.US,"%.2f / %.1f กม.",todayKm,goalKm)
          :String.format(Locale.US,"%.2f กม. · วิ่งอิสระ",todayKm));
      view.setTextViewText(R.id.widget_total,String.format(Locale.US,"สะสม %.1f กม.",totalKm));
      view.setTextViewText(R.id.widget_quest,done?"✓ เควสวิ่งวันนี้สำเร็จ":"ถัดไป: "+next);
      view.setTextViewText(R.id.widget_water,"💧 "+Math.min(waterGoal,water+pending)+"/"+waterGoal+" · +1");
      view.setProgressBar(R.id.widget_distance_progress,100,goalKm>0?(int)Math.min(100,Math.round(todayKm/goalKm*100)):0,false);
      view.setOnClickPendingIntent(R.id.widget_root,openApp);
      view.setOnClickPendingIntent(R.id.widget_water,addWater);
      manager.updateAppWidget(id,view);
    }
  }
  @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {draw(context,manager,ids);}
  @Override public void onReceive(Context context,Intent intent) {
    super.onReceive(context,intent);
    if (intent!=null && Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) refresh(context);
  }
}
