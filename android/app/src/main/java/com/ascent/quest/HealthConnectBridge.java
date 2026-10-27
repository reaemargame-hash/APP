package com.ascent.quest;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.*;
import android.health.connect.datatypes.*;
import android.os.Build;
import android.os.OutcomeReceiver;
import org.json.JSONObject;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Android 14+ platform Health Connect reader. All data stays on-device in ASCENT.
 * No write permissions and no automatic imports into run history or EXP.
 * Android 13 and older use manual tracking until an SDK-backed connector is added.
 */
public final class HealthConnectBridge {
 private HealthConnectBridge(){}
 private static final String[] PERMISSIONS={
  "android.permission.health.READ_SLEEP",
  "android.permission.health.READ_STEPS",
  "android.permission.health.READ_HEART_RATE",
  "android.permission.health.READ_WEIGHT"
 };
 public static void permissions(Activity activity){
  if(Build.VERSION.SDK_INT<34){message(activity,"ต้องใช้ Android 14 ขึ้นไปสำหรับตัวเชื่อมรุ่นนี้");return;}
  try{
   List<String> missing=new ArrayList<>();for(String s:PERMISSIONS)if(activity.checkSelfPermission(s)!=PackageManager.PERMISSION_GRANTED)missing.add(s);
   if(missing.size()>0)activity.requestPermissions(missing.toArray(new String[0]),278);
   else message(activity,"อนุญาตแล้ว · กลับไปกดอ่านข้อมูลล่าสุดได้เลย");
  }catch(Exception e){message(activity,"ขอสิทธิ์ไม่สำเร็จ: "+e.getMessage());}
 }
 private static void message(Activity activity,String msg){new android.app.AlertDialog.Builder(activity).setMessage(msg).setPositiveButton("ตกลง",null).show();}
 private static void send(MainActivity a,JSONObject data){
  a.runOnUiThread(()->{if(a.web!=null)a.web.evaluateJavascript("window.ascentHealthResult&&window.ascentHealthResult("+JSONObject.quote(data.toString())+")",null);});
 }
 private static void error(JSONObject result,String key,String reason){try{result.put(key+"Error",reason);}catch(Exception ignored){}}
 private static TimeInstantRangeFilter range(Instant start,Instant end){return new TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build();}
 public static void read(MainActivity activity){
  final JSONObject result=new JSONObject();
  if(Build.VERSION.SDK_INT<34){try{result.put("error","ต้องใช้ Android 14+ สำหรับ Health Connect ใน v3.7");}catch(Exception ignored){}send(activity,result);return;}
  HealthConnectManager client=activity.getSystemService(HealthConnectManager.class);
  if(client==null){try{result.put("error","ไม่พบ Health Connect บนอุปกรณ์");}catch(Exception ignored){}send(activity,result);return;}
  boolean hasAny=false;for(String s:PERMISSIONS)if(activity.checkSelfPermission(s)==PackageManager.PERMISSION_GRANTED)hasAny=true;
  if(!hasAny){try{result.put("error","ยังไม่ได้อนุญาตให้อ่านข้อมูล กรุณากดขอสิทธิ์ก่อน");}catch(Exception ignored){}send(activity,result);return;}
  final AtomicInteger pending=new AtomicInteger(0);final Runnable done=()->{if(pending.decrementAndGet()==0)send(activity,result);};
  final Instant now=Instant.now();
  // Asynchronously read up to 72 hours for the latest sleep session, but never merge duplicates blindly.
  if(activity.checkSelfPermission(PERMISSIONS[0])==PackageManager.PERMISSION_GRANTED){pending.incrementAndGet();
   try{ReadRecordsRequestUsingFilters<SleepSessionRecord> rq=new ReadRecordsRequestUsingFilters.Builder<>(SleepSessionRecord.class).setTimeRangeFilter(range(now.minus(Duration.ofDays(3)),now)).build();
    client.readRecords(rq,activity.getMainExecutor(),new OutcomeReceiver<ReadRecordsResponse<SleepSessionRecord>,HealthConnectException>(){
     @Override public void onResult(ReadRecordsResponse<SleepSessionRecord> response){try{
      SleepSessionRecord last=null;for(SleepSessionRecord r:response.getRecords())if(last==null||r.getEndTime().isAfter(last.getEndTime()))last=r;
      if(last!=null){double h=Duration.between(last.getStartTime(),last.getEndTime()).toMinutes()/60.0;if(h>0&&h<=24){result.put("sleepHours",Math.round(h*10)/10.0);result.put("sleepEnd",last.getEndTime().toString());}}
     }catch(Exception e){error(result,"sleep",e.getMessage());}finally{done.run();}}
     @Override public void onError(HealthConnectException ex){error(result,"sleep",ex.getMessage());done.run();}
    });
   }catch(Exception ex){error(result,"sleep",ex.getMessage());done.run();}}
  if(activity.checkSelfPermission(PERMISSIONS[3])==PackageManager.PERMISSION_GRANTED){pending.incrementAndGet();
   try{ReadRecordsRequestUsingFilters<WeightRecord> rq=new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class).setTimeRangeFilter(range(now.minus(Duration.ofDays(30)),now)).build();
    client.readRecords(rq,activity.getMainExecutor(),new OutcomeReceiver<ReadRecordsResponse<WeightRecord>,HealthConnectException>(){
     @Override public void onResult(ReadRecordsResponse<WeightRecord> response){try{
      WeightRecord last=null;for(WeightRecord r:response.getRecords())if(last==null||r.getTime().isAfter(last.getTime()))last=r;
      if(last!=null){double kg=last.getWeight().getInGrams()/1000;if(kg>=25&&kg<=300)result.put("weightKg",Math.round(kg*10)/10.0);}
     }catch(Exception ex){error(result,"weight",ex.getMessage());}finally{done.run();}}
     @Override public void onError(HealthConnectException ex){error(result,"weight",ex.getMessage());done.run();}
    });
   }catch(Exception ex){error(result,"weight",ex.getMessage());done.run();}}
  // Aggregate steps rather than summing raw records from several smartwatch providers.
  if(activity.checkSelfPermission(PERMISSIONS[1])==PackageManager.PERMISSION_GRANTED){pending.incrementAndGet();
   try{Instant start=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
    AggregateRecordsRequest<Long> rq=new AggregateRecordsRequest.Builder<Long>(range(start,now)).addAggregationType(StepsRecord.STEPS_COUNT_TOTAL).build();
    client.aggregate(rq,activity.getMainExecutor(),new OutcomeReceiver<AggregateRecordsResponse<Long>,HealthConnectException>(){
     @Override public void onResult(AggregateRecordsResponse<Long> response){try{Long n=response.get(StepsRecord.STEPS_COUNT_TOTAL);if(n!=null)result.put("steps",n);}catch(Exception ex){error(result,"steps",ex.getMessage());}finally{done.run();}}
     @Override public void onError(HealthConnectException ex){error(result,"steps",ex.getMessage());done.run();}
    });
   }catch(Exception ex){error(result,"steps",ex.getMessage());done.run();}}
  if(activity.checkSelfPermission(PERMISSIONS[2])==PackageManager.PERMISSION_GRANTED){pending.incrementAndGet();
   try{AggregateRecordsRequest<Long> rq=new AggregateRecordsRequest.Builder<Long>(range(now.minus(Duration.ofHours(24)),now)).addAggregationType(HeartRateRecord.BPM_AVG).build();
    client.aggregate(rq,activity.getMainExecutor(),new OutcomeReceiver<AggregateRecordsResponse<Long>,HealthConnectException>(){
     @Override public void onResult(AggregateRecordsResponse<Long> response){try{Long n=response.get(HeartRateRecord.BPM_AVG);if(n!=null)result.put("heartRate",n);}catch(Exception ex){error(result,"heartRate",ex.getMessage());}finally{done.run();}}
     @Override public void onError(HealthConnectException ex){error(result,"heartRate",ex.getMessage());done.run();}
    });
   }catch(Exception ex){error(result,"heartRate",ex.getMessage());done.run();}}
  if(pending.get()==0){try{result.put("error","ไม่ได้รับอนุญาตอ่านข้อมูล Health Connect");}catch(Exception ignored){}send(activity,result);}
 }
}
