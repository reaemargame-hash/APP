package com.ascent.quest;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Called by Android only after a widget pin request succeeds. */
public class WidgetPinResultReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    int widgetId=intent==null?-1:intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
    NativeScheduler.prefs(context).edit()
      .putString("widget_pin_state","confirmed")
      .putInt("widget_pin_result_id",widgetId)
      .putLong("widget_pin_confirmed_at",System.currentTimeMillis())
      .apply();
    QuestWidget.refresh(context);
  }
}
