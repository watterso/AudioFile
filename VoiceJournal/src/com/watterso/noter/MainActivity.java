package com.watterso.noter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.watterso.noter.AudioIntentService.AudioBinder;

public class MainActivity extends Activity implements OnPreparedListener, MediaLayout.MediaPlayerControl {
	  private static final String TAG = "RecordTaker";
	  public static final String EXTRA_MESSAGE = "com.watterso.noter.MESSAGE";
	  public static final String PREFS_NAME = "RecPref";
	  public static final int RECORD_DIALOG = 1;
	  public Dialog dialog;
	  public boolean rec = false;
	  public MediaPlayer mediaPlayer;
	  private MediaLayout mediaController;
	  private MediaRecorder mRecorder;
	  private String audioFile;
	  private Bundle saveThisInstance;
	  private ListView recordList;
	  private Handler handler = new Handler();
	  private Context mContext;
	  private int currentTag = 0;
	  private ArrayList<String> tags;
	  private ArrayList<String> tags1;
	  private ArrayList<Entry> entries;
	  ArrayAdapter<String> spinDapt; 
	  boolean mExternalStorageAvailable = false;
	  boolean mExternalStorageWriteable = false;
	  boolean mBound = false;
	  Entry recEntry;
	  AudioIntentService mService;
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
        mContext = this;
        DatabaseHandler db = new DatabaseHandler(this);
        tags = (ArrayList<String>) db.getTags();
    	Collections.sort(tags);
        tags1 = new ArrayList<String>();
        for(String d : tags){
        	tags1.add("#"+d);
        }
        tags1.add(0, "#All");
        tags.add(0, "All");
        spinDapt= new ArrayAdapter<String>(this, R.layout.spin, 
        				android.R.id.text1,tags1);							//Spinner Stuff
        spinDapt.setDropDownViewResource(R.layout.drop);
        ActionBar action = getActionBar();
        action.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        action.setListNavigationCallbacks(spinDapt, new ActionBar.OnNavigationListener() {
			
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				if(itemPosition==0){
					fillData();
				}
				else{
					currentTag = itemPosition;
					fillData(tags.get(itemPosition));
				}
				Log.d("Tag #"+currentTag, tags1.get(currentTag).toString());
				return false;
			}
		});
        
        recordList= (ListView) findViewById(R.id.recordings);
        fillData();																		//ListView Populate
        recordList.setOnItemClickListener(mClickListener);
        recordList.setOnItemLongClickListener(mLongListener);
        
        Intent intent = new Intent(this, AudioIntentService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		Log.d("Bound the", "Service");
        mediaController = (MediaLayout)findViewById(R.id.controller);
        if(mediaPlayer!=null && mediaPlayer.isPlaying()){
        	Log.d("Media Player", "Was Playing");
        	mediaController.setMediaPlayer(this);
        	mediaController.updateSeek();
        	
        }
        db.close();
    }
    public void firstTime(){
    	if(mExternalStorageWriteable){
    		ContextWrapper cw = new ContextWrapper(this);
    		File newDir = cw.getDir("Recordings", Context.MODE_WORLD_READABLE);

    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    		SharedPreferences.Editor editor = settings.edit();
    		editor.putBoolean("first", true);
    		editor.commit();
    	}
    }
    public void fillData(){
    	DatabaseHandler db = new DatabaseHandler(this);
    	entries = (ArrayList<Entry>) db.getAllEntries();
    	Collections.reverse(entries);					//most recent entries at top
    	CustomArrayAdapter temp = new CustomArrayAdapter(this, R.layout.listitem, entries);
    	//ArrayAdapter<File> temp = new ArrayAdapter<File>(this, R.layout.listitem, new File(REC_PATH).listFiles());
    	if(!temp.isEmpty()){
    		Log.d("ArrayAdapter", "NOT EMPTY");
    		recordList.setAdapter(temp);
    	}else{
    		Log.d("ArrayAdapter", "EMPTY");
    		String [] tempS = {""};
    		ArrayAdapter<String> temp1 = new ArrayAdapter<String>(this,R.layout.listitem, tempS);
    		recordList.setAdapter(temp1);
    	}
    	db.close();
    }
    public void fillData(String tag){
    	DatabaseHandler db = new DatabaseHandler(this);
    	entries = (ArrayList<Entry>) db.getAllEntries(tag);
    	Collections.reverse(entries);					//most recent entries at top
    	CustomArrayAdapter temp = new CustomArrayAdapter(this, 
    			R.layout.listitem, entries);				//for best results, tag should start with a '#'
    	if(!temp.isEmpty()){
    		Log.d("ArrayAdapter", "NOT EMPTY("+tags.get(currentTag)+")");
    		recordList.setAdapter(temp);
    	}else{
    		Log.d("ArrayAdapter", "EMPTY("+tags.get(currentTag)+")");
    		String [] tempS = {""};
    		ArrayAdapter<String> temp1 = new ArrayAdapter<String>(this,R.layout.listitem, tempS);
    		recordList.setAdapter(temp1);
    	}
    	db.close();
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
    public OnItemLongClickListener mLongListener = new OnItemLongClickListener(){

		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(50);
			showEditDialog(arg2);
			return false;
		}
    	
    };
    public void showEditDialog(int entNum){
    	final Entry editing = entries.get(entNum);
    	final Dialog dialog1 = new Dialog(this);
		dialog1.setOwnerActivity(this);
		dialog1.setContentView(R.layout.edit_popup);
		dialog1.setTitle("Edit an Entry");
		dialog1.setCanceledOnTouchOutside(false);
		dialog1.show();
		final EditText top = (EditText)dialog1.findViewById(R.id.entryNameEdit);
		top.setText(editing.getName());
		final AutoCompleteTextView bot = (AutoCompleteTextView) dialog1.findViewById(R.id.taggerEdit);
		ArrayAdapter<String> copSpin = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1, tags );
		copSpin.setDropDownViewResource(R.layout.drop);
		bot.setAdapter(copSpin);
		bot.setText(editing.getTag());
		Button cancel =(Button)dialog1.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog1.dismiss();
			}
		});
		Button okay = (Button)dialog1.findViewById(R.id.okay);
		okay.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				editing.setName(top.getText().toString());
				editing.setTag(bot.getText().toString());
				DatabaseHandler db = new DatabaseHandler(mContext);
				db.updateEntry(editing);
				db.close();
				dialog1.dismiss();
				updateScroll();
				refreshList();
			}
		});
		Button delete = (Button)dialog1.findViewById(R.id.delete);
		delete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DatabaseHandler db = new DatabaseHandler(mContext);
				db.deleteEntry(editing);
				db.close();
				ContextWrapper cw = new ContextWrapper(mContext);
				File file = new File(cw.getDir("Recordings", Context.MODE_WORLD_READABLE).getPath()+editing.getFile());
				file.delete();
				updateScroll();
				refreshList();
				dialog1.dismiss();
			}
		});
    }
    public OnItemClickListener mClickListener = new OnItemClickListener() {
		  
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
			Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(50);
			mediaPlayer.reset();
			CustomArrayAdapter tempC = (CustomArrayAdapter)recordList.getAdapter();
			Log.d("SERVICE SAYS","#"+mService.getPlaying()+" was playing");
			if(mService.getPlaying()!=-1){
				View tempV = tempC.getView(mService.getPlaying(), null, recordList);
				Drawable back1  = tempV.getBackground();
				back1.setColorFilter(null);
			}
			tempC.setShaded(position);
			Drawable back = arg1.getBackground();
        	back.mutate().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        	recordList.invalidateViews();
			mService.setPlaying(position);
			if(entries.size()==0) return;
			audioFile = entries.get(position).getFile();
			//audioFile = ((TextView)arg1).getText().toString();
			if(audioFile==null) return;
			onPlay(entries.get(position));
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
    			if(mediaPlayer.isPlaying()){
    				mediaPlayer.reset();
    			}
        		dialog = new Dialog(this);
        		dialog.setOwnerActivity(this);
    			dialog.setContentView(R.layout.activity_record);
    			dialog.setTitle("Make a Recording");
    			dialog.setCanceledOnTouchOutside(false);
    			final ImageButton butt = (ImageButton) dialog.findViewById(R.id.butt);
    			final EditText top = (EditText)dialog.findViewById(R.id.entryName);
    			top.setTag(top.getKeyListener());
    			final AutoCompleteTextView bot = (AutoCompleteTextView) dialog.findViewById(R.id.tagger);
    			ArrayAdapter<String> copSpin = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1, tags );
    			copSpin.setDropDownViewResource(R.layout.drop);
    			bot.setAdapter(copSpin);
    			bot.setTag(bot.getKeyListener());
				butt.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						if(!rec){
							if(top.getText().length()==0){
								CharSequence text = "Please title this entry";
								int duration = Toast.LENGTH_SHORT;

								Toast toast = Toast.makeText(getApplicationContext(), text, duration);
								toast.show();
								return;
							}
							if(bot.getText().length()==0){
								CharSequence text = "Please add a tag";
								int duration = Toast.LENGTH_SHORT;

								Toast toast = Toast.makeText(getApplicationContext(), text, duration);
								toast.show();
								return;
							}
							butt.setImageResource(R.drawable.ic_butt_stop);
							Log.d("Recording", "butt changed");
							top.setKeyListener(null);
							bot.setKeyListener(null);
							Log.d("Recording", "listeners gone");
							//begin recording
							mRecorder = mService.getRecorder();
							Entry theEntry = new Entry(top.getText().toString(), bot.getText().toString());
							Log.d("Recording", "entry made");
							onRecord(theEntry);
							Log.d("Recording", "onRecord hapened");
							rec = true;
						}else{
							//save stuff
							DatabaseHandler db = new DatabaseHandler(mContext);
							db.addEntry(recEntry);
							top.setKeyListener((KeyListener)top.getTag());
							bot.setKeyListener((KeyListener)bot.getTag());
							mService.stoppedRecording();
							mRecorder = null;
							rec = false;
							dialog.dismiss();
							db.close();
							updateScroll();
							refreshList();
						}
					}
			    });
    			dialog.show();
				
			return true;
    		default: 
    			return super.onOptionsItemSelected(item);
    	}
    	
    }
    private void updateScroll(){
    	DatabaseHandler db = new DatabaseHandler(mContext);
    	tags = (ArrayList<String>) db.getTags();
    	Collections.sort(tags);
        tags1 = new ArrayList<String>();
        for(String d : tags){
        	tags1.add("#"+d);
        }
        tags1.add(0, "#All");
        tags.add(0, "All");
        spinDapt.clear();
        spinDapt.addAll(tags1);
        db.close();
    	
    }
    private void onRecord(Entry theEntry){
    	if(mRecorder!=null){
    		recEntry = theEntry;
			Log.d("onRecord", "entry set");
			ContextWrapper cw = new ContextWrapper(mContext);
    		mRecorder.setOutputFile(cw.getDir("Recordings", Context.MODE_WORLD_READABLE)+theEntry.getFile());
			Log.d("onRecord", "output file set");
    		try {
    			Log.d("onRecord", "trying to prepare");
    			mRecorder.prepare();
    		} catch (IOException e) {
    			Log.e("Mic Stuff", "prepare() failed");
    		}
    		Log.d("onREcord","mRecorder Prepared");
    		mService.startedRecording(theEntry);
			Log.d("onRecord", "service started recording");
    		mRecorder.start();
			Log.d("onRecord", "mrecorder started");
    		
         }
    }
    private void onPlay(Entry ent){
        if(mediaPlayer!=null){
        	mediaPlayer.reset();
        	try {
				ContextWrapper cw = new ContextWrapper(mContext);
        		mediaPlayer.setDataSource(cw.getDir("Recordings", Context.MODE_WORLD_READABLE)+audioFile);
        		mediaPlayer.prepare();
        		mediaPlayer.start();
        		mService.startedPlaying(ent);
        	} catch (IOException e) {
        		Log.d(TAG, "Could not open file " + audioFile + " for playback.", e);
        		CharSequence text = "Could not open file " + audioFile + " for playback.";
        		int duration = Toast.LENGTH_SHORT;

        		Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        		toast.show();
        	}
        }
    }
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to service, cast the IBinder and get service instance
        	Log.d("Connected to", "Service");
            AudioBinder binder = (AudioBinder) service;
            mService = binder.getService();
            mediaPlayer = mService.getPlayer();
            addPrepListener();
            mRecorder = mService.getRecorder();
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private void addPrepListener(){
    	mediaPlayer.setOnPreparedListener(this);
    }
    protected void onStart(){
  	  super.onStart();
  	  if(mService!=null)
  		  mService.setBackground(false);
  	  //this.onCreate(saveThisInstance);			OH GOD WHY?!?!! NEVER EVER do this!! DUMMY!
    }
    @Override
    protected void onStop() {
      super.onStop();
      mService.setBackground(true);
      //mediaPlayer.stop();
      //mediaPlayer.release();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
    public void refreshList(){
    	if(currentTag!=0){
			fillData(tags.get(currentTag));
		}else{
			fillData();
		}
    }
    public void onPrepared(MediaPlayer mediaPlayer) {
      Log.d(TAG, "onPrepared");
      mediaController.setMediaPlayer(this);

      handler.post(new Runnable() {
        public void run() {
        	mediaController.setEnabled(true);
        	mediaController.updateSeek();
        }
      });
    }
  }
