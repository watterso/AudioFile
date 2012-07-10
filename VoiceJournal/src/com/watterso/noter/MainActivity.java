package com.watterso.noter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

public class MainActivity extends Activity implements OnPreparedListener, MediaLayout.MediaPlayerControl {
	  private static final String TAG = "RecordTaker";
	  public static final String EXTRA_MESSAGE = "com.watterso.noter.MESSAGE";
	  private static final String REC_PATH  = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Recordings/";
	  public static final String PREFS_NAME = "RecPref";
	  public static final int RECORD_DIALOG = 1;
	  public Dialog dialog;
	  public boolean rec = false;
	  public MediaPlayer mediaPlayer;
	  private MediaLayout mediaController;
	  private String audioFile;
	  private Bundle saveThisInstance;
	  private ListView recordList;
	  private Handler handler = new Handler();
	  private DatabaseHandler db;
	  private ArrayList<String> tags;
	  private ArrayList<Entry> entries;
	  boolean mExternalStorageAvailable = false;
	  boolean mExternalStorageWriteable = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	saveThisInstance = savedInstanceState;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        checkStorage();
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean firstCheck = settings.getBoolean("first", false);	//Logically confusing but works better if false means its first time
        if(!firstCheck) firstTime();
       
        db = new DatabaseHandler(this);
        
        tags = (ArrayList<String>) db.getTags();
        tags.add(0, "#All");
        ArrayAdapter<String> spinDapt = new ArrayAdapter<String>(this, R.layout.spin, 
        				android.R.id.text1,tags );							//Spinner Stuff
        spinDapt.setDropDownViewResource(R.layout.drop);
        ActionBar action = getActionBar();
        action.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        action.setListNavigationCallbacks(spinDapt, new ActionBar.OnNavigationListener() {
			
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				if(itemPosition==0)
					fillData();
				else
					fillData(tags.get(itemPosition));
				return false;
			}
		});
        
        recordList= (ListView) findViewById(R.id.recordings);
        fillData();																		//ListView Populate
        recordList.setOnItemClickListener(mClickListener);
        
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaController = (MediaLayout)findViewById(R.id.controller);        
    }
    public void firstTime(){
    	if(mExternalStorageWriteable){
    		File newDir = new File(Environment.getExternalStorageDirectory(), "Recordings");
    		newDir.mkdir();

    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    		SharedPreferences.Editor editor = settings.edit();
    		editor.putBoolean("first", true);
    		editor.commit();
    	}
    }
    public void fillData(){
    	entries = (ArrayList<Entry>) db.getAllEntries();
    	ArrayAdapter<Entry> temp = new ArrayAdapter<Entry>(this, R.layout.listitem, entries);
    	if(!temp.isEmpty()){
    		Log.d("ArrayAdapter", "NOT EMPTY");
    		recordList.setAdapter(temp);
    	}else{
    		Log.d("ArrayAdapter", "EMPTY");
    		String [] tempS = {""};
    		ArrayAdapter<String> temp1 = new ArrayAdapter<String>(this,R.layout.listitem, tempS);
    		recordList.setAdapter(temp1);
    	}
    }
    public void fillData(String tag){
    	entries = (ArrayList<Entry>) db.getAllEntries(tag);
    	ArrayAdapter<Entry> temp = new ArrayAdapter<Entry>(this, 
    			R.layout.listitem, entries);				//for best results, tag should start with a '#'
    	if(!temp.isEmpty()){
    		Log.d("ArrayAdapter", "NOT EMPTY");
    		recordList.setAdapter(temp);
    	}else{
    		Log.d("ArrayAdapter", "EMPTY");
    		String [] tempS = {""};
    		ArrayAdapter<String> temp1 = new ArrayAdapter<String>(this,R.layout.listitem, tempS);
    		recordList.setAdapter(temp1);
    	}
    }
    public void checkStorage(){
    	String state = Environment.getExternalStorageState();
    	if (Environment.MEDIA_MOUNTED.equals(state)) {
    	    // We can read and write the media
    	    mExternalStorageAvailable = mExternalStorageWriteable = true;
    	} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	    // We can only read the media
    	    mExternalStorageAvailable = true;
    	    mExternalStorageWriteable = false;
    	} else {
    	    // Something else is wrong. It may be one of many other states, but all we need
    	    //  to know is we can neither read nor write
    	    mExternalStorageAvailable = mExternalStorageWriteable = false;
    	}
    }
    public OnItemClickListener mClickListener = new OnItemClickListener() {
		  
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
			mediaPlayer.reset();
			if(entries.size()==0) return;
			audioFile = entries.get(position).getFile();
			if(audioFile==null) return;
   	         try {
   	        	 mediaPlayer.setDataSource(audioFile);
   	        	 mediaPlayer.prepare();
   	        	 mediaPlayer.start();
   	         } catch (IOException e) {
   	           Log.e(TAG, "Could not open file " + audioFile + " for playback.", e);
   	         }
			
		}
      	};
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menu_settings:
    			return true;
    		case R.id.menu_record:
        		dialog = new Dialog(this);
        		dialog.setOwnerActivity(this);
    			dialog.setContentView(R.layout.activity_record);
    			dialog.setTitle("Make a Recording");
    			dialog.setCanceledOnTouchOutside(false);
    			final ImageButton butt = (ImageButton) dialog.findViewById(R.id.butt);
    			final ImageButton paus = (ImageButton) dialog.findViewById(R.id.pause);
    			paus.setImageResource(android.R.drawable.ic_media_pause);
    			paus.setVisibility(View.GONE);
				butt.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						if(!rec){
							butt.setImageResource(R.drawable.ic_butt_stop);
							paus.setVisibility(View.VISIBLE);
							//begin recording
							rec = true;
						}else{
							//save stuff
							rec = false;
							dialog.dismiss();
						}
					}
			    });
				paus.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						//pause recording
						rec=false;
		    			paus.setVisibility(View.GONE);
		    			butt.setImageResource(R.drawable.ic_butt_record);
					}
			    });
    			dialog.show();
				
			return true;
    		default: 
    			return super.onOptionsItemSelected(item);
    	}
    	
    }
    protected void onStart(){
  	  super.onStart();
  	  this.onCreate(saveThisInstance);  
    }
    @Override
    protected void onStop() {
      super.onStop();
      mediaPlayer.stop();
      mediaPlayer.release();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
      //the MediaController will hide after 3 seconds - tap the screen to make it appear again
      mediaController.show();
      return false;
    }
    //--MediaPlayerControl methods----------------------------------------------------
    public void start() {
      mediaPlayer.start();
    }

    public void pause() {
      mediaPlayer.pause();
    }

    public int getDuration() {
      return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
      return mediaPlayer!=null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int i) {
      mediaPlayer.seekTo(i);
    }

    public boolean isPlaying() {
      return mediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
      return 0;
    }

    public boolean canPause() {
      return true;
    }

    public boolean canSeekBackward() {
      return true;
    }

    public boolean canSeekForward() {
      return true;
    }
    //--------------------------------------------------------------------------------

    public void onPrepared(MediaPlayer mediaPlayer) {
      Log.d(TAG, "onPrepared");
      mediaController.setMediaPlayer(this);

      handler.post(new Runnable() {
        public void run() {
        	mediaController.setEnabled(true);
        	mediaController.show();
        }
      });
    }
  }
