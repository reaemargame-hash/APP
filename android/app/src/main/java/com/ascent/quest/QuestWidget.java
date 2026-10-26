package com.ascent.quest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import org.json.*;
public class QuestWidget extends AppWidgetProvider {
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);android.content.ComponentName who=new android.content.ComponentName(c,QuestWidget.class);int[] ids=m.getAppWidgetIds(who);if(ids.length>0)draw(c,m,ids);}
 static void draw(Context c,AppWidgetManager mgr,int[] ids){
  String json=NativeScheduler.prefs(c).getString("widget","{}");int lv=1,xp=0;double km=0;
  try{JSONObject j=new JSONObject(json);lv=j.optInt("level",1);xp=j.optInt("xp",0);km=j.optDouble("km",0);}catch(Exception ignored){}
  Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  for(int id:ids){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.quest_widget);v.setTextViewText(R.id.widget_level,"LV."+lv+"  ·  "+xp+" EXP");v.setTextViewText(R.id.widget_distance,String.format(java.util.Locale.US,"%.1f km TOTAL",km));v.setOnClickPendingIntent(R.id.widget_root,pi);v.setOnClickPendingIntent(R.id.widget_title,pi);v.setOnClickPendingIntent(R.id.widget_level,pi);v.setOnClickPendingIntent(R.id.widget_distance,pi);mgr.updateAppWidget(id,v);}
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){draw(c,m,ids);}
}
