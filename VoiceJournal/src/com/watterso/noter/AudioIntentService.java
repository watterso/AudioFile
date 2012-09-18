package com.watterso.noter;

import java.util.Formatter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class AudioIntentService extends IntentService {
	private MediaRecorder mRecorder;
	private MediaPlayer mPlayer;
	private boolean isBackground = false;
	private String time;
	private final IBinder mBinder = new AudioBinder();
	final static int NOTI_ID = 5;
	private StringBuilder mFormatBuilder = new StringBuilder();
	private Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    private long starttime;
    private Timer timer;
    private Bitmap rBitmap;
    private Bitmap pBitmap;
    private Notification noti;
    private Intent recieved;
    private Entry notiEntry;
    public boolean showRec = true;
    private int curPlay = -1;
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
    class recordTask extends TimerTask {
		@Override
        public void run() {
            h.sendEmptyMessage(0);
            if(isBackground && showRec)	{
            	 Intent intented = new Intent(getApplicationContext(), MainActivity.class);
                 intented.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                 noti = new Notification(R.drawable.ic_butt_record, "Now Recording: "+notiEntry.getName(),System.currentTimeMillis());
                 noti.setLatestEventInfo(getApplicationContext(), "Recording: "+notiEntry.getName(), time,
                 		PendingIntent.getActivity(getApplicationContext(), 0, intented, PendingIntent.FLAG_CANCEL_CURRENT));
            									//confusing wording, if you are not looking at 
            	startForeground(NOTI_ID, noti); //MainActivity, show the notification
            	//Log.d("RECORD NOTE", "IS SHOWING");
            }else{
            	stopForeground(true);
            	//Log.d("RECORD NOTE", "CLEARED");
            }
        }
   };
   class playTask extends TimerTask {
       @Override
       public void run() {
    	   int totalSeconds = mPlayer.getCurrentPosition() / 1000;

           int seconds = totalSeconds % 60;
           int minutes = (totalSeconds / 60) % 60;
           int hours   = totalSeconds / 3600;

           mFormatBuilder.setLength(0);
           if (hours > 0) {
               time = mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
           } else {
               time = mFormatter.format("%02d:%02d", minutes, seconds).toString();
           }
           Intent intented = new Intent(getApplicationContext(), MainActivity.class);
           intented.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
           noti = new Notification(android.R.drawable.ic_lock_silent_mode_off, "Now Playing: "+notiEntry.getName(),System.currentTimeMillis());
           noti.setLatestEventInfo(getApplicationContext(), "Playing: "+notiEntry.getName(), time,
           		PendingIntent.getActivity(getApplicationContext(), 0, intented, PendingIntent.FLAG_CANCEL_CURRENT));
           
           if(isBackground&&mPlayer.isPlaying())	{				//confusing wording, if you are not looking at 
           	startForeground(NOTI_ID, noti); //MainActivity, show the notification
           }else{
           	stopForeground(true);
           }
       }
  };
	
	public void setBackground(boolean yesno){
		isBackground = yesno;
	}
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
		recieved = intent;
        return mBinder;
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("big friends", "intent handled");
		
	}
	public MediaPlayer getPlayer(){
		mPlayer = new MediaPlayer();
		return mPlayer;
	}
	public MediaRecorder getRecorder(){
		mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        return mRecorder;
	}
	public void startedPlaying(Entry ent){
		notiEntry = ent;
		timer = new Timer();
        timer.schedule(new playTask(), 0, 500);
        starttime = System.currentTimeMillis();
        pBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
	}
	public void startedRecording(Entry ent){
		notiEntry = ent;
        timer = new Timer();
		Log.d("service", "timer made");
        timer.schedule(new recordTask(), 0, 500);
		Log.d("service", "scheduled");
        starttime = System.currentTimeMillis();
        rBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
	}
	public void stoppedRecording() {
		timer.cancel();
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }
	public int getPlaying(){
		return curPlay;
	}
	public void setPlaying(int i){				//doesn't actually set whats playing, just for listview filter
		curPlay = i;
	}
}
