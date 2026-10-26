package com.ascent.quest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.Manifest;

public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent){
   if(android.os.Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   NativeScheduler.channels(c);
   Intent open=new Intent(c,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   PendingIntent click=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   String title=intent.getStringExtra("title"),body=intent.getStringExtra("body"),key=intent.getStringExtra("key");
   Notification n=new Notification.Builder(c,NativeScheduler.channel(c)).setSmallIcon(R.drawable.ic_launcher)
     .setContentTitle(title==null?"ASCENT QUEST":title).setContentText(body==null?"ถึงเวลาภารกิจแล้ว":body)
     .setContentIntent(click).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build();
   ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(key==null?0:NativeScheduler.id(key),n);
 }
}
