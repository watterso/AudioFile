package com.watterso.noter;

import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MediaLayout extends LinearLayout {
	private MediaPlayerControl  mPlayer;
    private ProgressBar         mProgress;
    private TextView            mEndTime, mCurrentTime;
    private boolean             mDragging;
    private static final int    FADE_OUT = 1;
    private static final int    SHOW_PROGRESS = 2;
    private boolean             mUseFastForward;
    private boolean             mFromXml;
    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder               mFormatBuilder;
    Formatter                   mFormatter;
    private ImageButton         mPauseButton;
    private ImageButton         mFfwdButton;
    private ImageButton         mRewButton;
    private ImageButton         mNextButton;
    private ImageButton         mPrevButton;

	public MediaLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.media_controller, this);
		mUseFastForward = true;
	}
	public MediaLayout(Context context) {
		super(context);
		inflate(context, R.layout.media_controller, this);
	}
	@Override
    public void onFinishInflate() {
            initControllerView(this);
    }
	public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }
    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(com.watterso.noter.R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFfwdButton = (ImageButton) v.findViewById(com.watterso.noter.R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (!mFromXml) {
                mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        mRewButton = (ImageButton) v.findViewById(com.watterso.noter.R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (!mFromXml) {
                mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called 
        mNextButton = (ImageButton) v.findViewById(com.watterso.noter.R.id.next);
        mNextButton.setVisibility(View.GONE);
        mPrevButton = (ImageButton) v.findViewById(com.watterso.noter.R.id.prev);
        mPrevButton.setVisibility(View.GONE);

        mProgress = (ProgressBar) v.findViewById(com.watterso.noter.R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(com.watterso.noter.R.id.time);
        mCurrentTime = (TextView) v.findViewById(com.watterso.noter.R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    break;
                case SHOW_PROGRESS:
                    if (mPlayer!=null) {
                    	//Log.d("Player Playing:",""+mPlayer.isPlaying());
                    	if (mPlayer.isPlaying()) {
                    		msg = obtainMessage(SHOW_PROGRESS);
                    		setProgress();							//This updates the seek bar
                    		sendMessageDelayed(msg, 1000);
                    	}
                    }
				break;
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

    	//Log.d("PROGRESS", "SET");
        return position;
    }
	public void updateSeek(){
    	setProgress();
    	updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
 
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN && (
                keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode ==  KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode ==  KeyEvent.KEYCODE_SPACE)) {
            doPauseResume();
            updateSeek();
            return true;
        } else if (keyCode ==  KeyEvent.KEYCODE_MEDIA_STOP) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {

            return true;
        } else {
        	updateSeek();
        }
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            updateSeek();
        }
    };

    private void updatePausePlay() {

        ImageButton button = (ImageButton) this.findViewById(com.watterso.noter.R.id.pause);
        if (button == null)
            return;
        if(mPlayer!=null){	
        	if (mPlayer.isPlaying()) {
        		button.setImageResource(android.R.drawable.ic_media_pause);
        	} else {
        		button.setImageResource(android.R.drawable.ic_media_play);
        	}
        }
    }

    private void doPauseResume() {
    	if(mPlayer!=null){
    		if (mPlayer.isPlaying()) {
    			mPlayer.pause();
    		} else {
    			mPlayer.start();
    		}
        }
        updatePausePlay();
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        long duration;
        public void onStartTrackingTouch(SeekBar bar) {
        	updateSeek();
        	if(mPlayer!=null)	duration = mPlayer.getDuration();
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromtouch) {
            if (fromtouch) {
                mDragging = true;
                if(mPlayer!=null){
                	duration = mPlayer.getDuration();
                	long newposition = (duration * progress) / 1000L;
                	mPlayer.seekTo( (int) newposition);
                	if (mCurrentTime != null)
                		mCurrentTime.setText(stringForTime( (int) newposition));
                }
            }
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            //mNextButton.setEnabled(enabled && mNextListener != null);
        }
        if (mPrevButton != null) {
            //mPrevButton.setEnabled(enabled && mPrevListener != null);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }

        super.setEnabled(enabled);
    }

    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		int pos = mPlayer.getCurrentPosition();
        		pos -= 5000; // milliseconds
        		mPlayer.seekTo(pos);
        		setProgress();
        	}
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
        	if(mPlayer!=null){
        		int pos = mPlayer.getCurrentPosition();
        		pos += 15000; // milliseconds
        		mPlayer.seekTo(pos);
        		setProgress();
        	}
        }
    };

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            //mNextButton.setOnClickListener(mNextListener);
            //mNextButton.setEnabled(mNextListener != null);
        }

        if (mPrevButton != null) {
            //mPrevButton.setOnClickListener(mPrevListener);
            //mPrevButton.setEnabled(mPrevListener != null);
        }
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;

        
        installPrevNextListeners();

        if (mNextButton != null && !mFromXml) {
        	//mNextButton.setVisibility(View.VISIBLE);
        }
        if (mPrevButton != null && !mFromXml) {
        	//mPrevButton.setVisibility(View.VISIBLE);
        }
        
    }

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
    };


}
