/***
  Copyright (c) 2008-2012 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain	a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
	
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
*/

package com.commonsware.android.download;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class DownloadDemo extends Activity {
	
  private static final String TAG = "DownloadDemo";
  private DownloadManager mgr=null;
  private long lastDownload=-1L;
  
  /**
   * On create we just setup our download manager and register a couple receivers.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    mgr=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
    registerReceiver(onComplete,
                     new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    registerReceiver(onNotificationClick,
                     new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
  }
  
  /**
   * On destroy we just maintain our receivers
   */
  @Override
  public void onDestroy() {
    super.onDestroy();
    
    unregisterReceiver(onComplete);
    unregisterReceiver(onNotificationClick);
  }
  
  /**
   * Called onClick.
   * This starts a download fora hardcoded APK file.
   * @param v
   */
  public void startDownload(View v) {
    //This is the download request.
    Request request = new Request(Uri.parse("http://file.appsapk.com/download/Flashlight%20Call.apk"));
    
    //setVisibleInDownloadsUi will flag the visibility of the downloaded file in the Downloads app.
    //If the flag is false, the file will not be visible.
    request.setVisibleInDownloadsUi(false);
    
    //setNotificationVisibility will flag the visibility of the notification while downloading
    //If the flag is false, you wont see any notification
    request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
    
    //This will queue the file up for download. You will get back a long which will be the download id once
    //it is completed.
    lastDownload = mgr.enqueue(request);
    
    v.setEnabled(false);
    findViewById(R.id.query).setEnabled(true);
  }
  
  /**
   * This will query the current download and log the specifics to the logcat.
   * @param v
   */
  public void queryStatus(View v) {
    Cursor c=mgr.query(new DownloadManager.Query().setFilterById(lastDownload));
    
    if (c==null) {
      Toast.makeText(this, "Download not found!", Toast.LENGTH_LONG).show();
    }
    else {
      c.moveToFirst();
      
      Log.d(getClass().getName(), "COLUMN_ID: "+
            c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
      Log.d(getClass().getName(), "COLUMN_BYTES_DOWNLOADED_SO_FAR: "+
            c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
      Log.d(getClass().getName(), "COLUMN_LAST_MODIFIED_TIMESTAMP: "+
            c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
      Log.d(getClass().getName(), "COLUMN_LOCAL_URI: "+
            c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
      Log.d(getClass().getName(), "COLUMN_STATUS: "+
            c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
      Log.d(getClass().getName(), "COLUMN_REASON: "+
            c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
      
      Toast.makeText(this, statusMessage(c), Toast.LENGTH_LONG).show();
    }
  }
  
  /**
   * This will show the download manager logs. Should show nothing.
   * @param v
   */
  public void viewLog(View v) {
    startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
  }
  
  private String statusMessage(Cursor c) {
    String msg="???";
    
    switch(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
      case DownloadManager.STATUS_FAILED:
        msg="Download failed!";
        break;
      
      case DownloadManager.STATUS_PAUSED:
        msg="Download paused!";
        break;
      
      case DownloadManager.STATUS_PENDING:
        msg="Download pending!";
        break;
      
      case DownloadManager.STATUS_RUNNING:
        msg="Download in progress!";
        break;
      
      case DownloadManager.STATUS_SUCCESSFUL:
        msg="Download complete!";
        break;
      
      default:
        msg="Download is nowhere in sight";
        break;
    }
    
    return(msg);
  }
  
  /**
   * Broadcast receiver for completion. 
   */
  BroadcastReceiver onComplete=new BroadcastReceiver() {
    public void onReceive(Context ctxt, Intent intent) {
        findViewById(R.id.start).setEnabled(true);
    	String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            //Create a query for the download ID we queued up earlier.
            //Should probably add a handle incase we lost the enqueue id.
            Query query = new Query();
            query.setFilterById(lastDownload);
            Cursor c = mgr.query(query);
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                
                //There are other STATUS that we can look at such as STATUS_FAILED, PAUSED, PENDING, etc.
                if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                    Log.d(TAG, "Download was successful!");
                    String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    
                    //If we got a successful download, then we should just install the apk because we hardcoded an APK file.
                    PackageManager pm = getPackageManager();
                    Uri uri = Uri.parse(uriString);
                    //There are other parameters we can use here such as installation observers.
                    //Check the documentation for more info.
                    pm.installPackage(uri, null, 0, null);
                    Toast.makeText(DownloadDemo.this, "Installing an APK file!", Toast.LENGTH_LONG).show();
                
                }
            }
        }
    }
  };
  
  /**
   * Receiver for notification onClick.
   * Does nothing since our notification is invisible but you can see how it works.
   */
  BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
    public void onReceive(Context ctxt, Intent intent) {
      Toast.makeText(ctxt, "Ummmm...hi!", Toast.LENGTH_LONG).show();
    }
  };
}
