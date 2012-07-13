package com.watterso.noter;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class AudioIntentService extends IntentService {
	private MediaRecorder mRecorder;
	private String time;
	private final IBinder mBinder = new AudioBinder();
	final static int NOTI_ID = 5;
	private String mFileName;
	StringBuilder mFormatBuilder = new StringBuilder();
    Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    private long starttime;
    Timer timer;
    Bitmap aBitmap;
    Notification noti;
	final Handler h = new Handler() {
        @Override
        public void handleMessage(Message msg) {
           long millis = System.currentTimeMillis() - starttime;
           int seconds = (int) (millis / 1000);
           int minutes = seconds / 60;
           int hours   = seconds / 3600;
           seconds     = seconds % 60;
           mFormatBuilder.setLength(0);
           if (hours > 0) {
        	   time = mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
           } else {
        	   time = mFormatter.format("%02d:%02d", minutes, seconds).toString();
           }
        }
    };
    class timeTask extends TimerTask {
        @Override
        public void run() {
            h.sendEmptyMessage(0);
            noti = new Notification.Builder(getApplicationContext())
            .setContentTitle("Recording to " + mFileName)
            .setContentText(time)
            .setSmallIcon(R.drawable.ic_butt_record)
            .setLargeIcon(aBitmap)
            .build();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
            mNotificationManager.notify(NOTI_ID, noti);
        }
   };
	
	
	public AudioIntentService() {
		super("thisemptyholmes");
		//empty construct necessitated by error
	}
    public AudioIntentService(String name) {
		super(name);
	}
    public class AudioBinder extends Binder {
        public AudioIntentService getService() {
            // Return this instance of service so clients can call public methods
            return AudioIntentService.this;
        }
    }

	@Override
    public IBinder onBind(Intent intent) {
		mFileName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        return mBinder;
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("big friends", "intent handled");
		
	}
	public void startRecording(){ 
		mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(MainActivity.REC_PATH+mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("Mic Stuff", "prepare() failed");
        }
        mRecorder.start();
        timer = new Timer();
        timer.schedule(new timeTask(), 0, 500);
        starttime = System.currentTimeMillis();
        aBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.record_buton);
        noti = new Notification.Builder(this)
        .setContentTitle("Recording to " + mFileName)
        .setContentText(time)
        .setSmallIcon(R.drawable.ic_butt_record)
        .setLargeIcon(aBitmap)
        .build();
        startForeground(NOTI_ID, noti);
	}
	public void stopRecording() {
		timer.cancel();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
        mNotificationManager.cancel(NOTI_ID);
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }
}
