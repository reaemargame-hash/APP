package com.ascent.quest;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
public class SoundProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
   File f=new File(getContext().getFilesDir(),"quest_alert.mp3");
   if(!f.exists())throw new FileNotFoundException();
   return ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
 }
 @Override public String getType(Uri uri){return NativeScheduler.prefs(getContext()).getString("sound_mime","audio/mpeg");}
 @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
 @Override public Uri insert(Uri u,ContentValues v){return null;}
 @Override public int delete(Uri u,String s,String[] a){return 0;}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
