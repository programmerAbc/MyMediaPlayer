package user.example.com.mymediaplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedList;

public class VideoPlayerActivity extends AppCompatActivity {

    public static LinkedList<MovieInfo> playList = new LinkedList<MovieInfo>();
    private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private static int position = 0;
    private int playedTime = 0;
    private VideoView videoView = null;
    private SeekBar seekBar = null;
    private TextView durationTextView = null;
    private TextView playedTextView = null;
    private GestureDetector mGestureDetector = null;
    private AudioManager mAudioManager = null;
    private int maxVolume = 0;
    private int currentVolume = 0;
    private ImageButton ejctButton = null;
    private ImageButton backButton = null;
    private ImageButton playStopButton = null;
    private ImageButton forwardButton = null;
    private ImageButton volumeButton = null;
    private View controllerView = null;
    private PopupWindow controller = null;
    private SoundView mSoundView = null;
    private PopupWindow mSoundWindow = null;
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static int controllerHeight = 0;
    private final static int TIME = 6868;
    private boolean isControllerShow = true;
    private boolean isPaused = false;
    private boolean isFullScreen = false;
    private boolean isSlient = false;
    private boolean isSoundShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                if (controller != null && videoView.isShown()) {
                    controller.showAtLocation(videoView, Gravity.BOTTOM, 0, 0);
                    controller.update(0, 0, screenWidth, controllerHeight);
                }
                return false;
            }
        });
        controllerView = getLayoutInflater().inflate(R.layout.controller, null);
        controller = new PopupWindow(controllerView);
        durationTextView = (TextView) controllerView.findViewById(R.id.duration);
        playedTextView = (TextView) controllerView.findViewById(R.id.has_played);
        mSoundView = new SoundView(this);
        mSoundView.setOnVolumeChangedListener(new SoundView.OnVolumeChangedListener() {
            @Override
            public void setVolume(int index) {
                cancelDelayHide();
                updateVolume(index);
                hideControllerDelay();
            }
        });
        mSoundWindow = new PopupWindow(mSoundView);
        position = -1;
        ejctButton = (ImageButton) controllerView.findViewById(R.id.ejctButton);
        backButton = (ImageButton) controllerView.findViewById(R.id.backButton);
        playStopButton = (ImageButton) controllerView.findViewById(R.id.playStopButton);
        forwardButton = (ImageButton) controllerView.findViewById(R.id.forwardButton);
        volumeButton = (ImageButton) controllerView.findViewById(R.id.volumeButton);
        videoView = (VideoView) findViewById(R.id.videoView);
        Uri uri = getIntent().getData();
        if (uri != null) {
            if (videoView.getmVideoHeight() == 0) {
                videoView.setVideoURI(uri);
            }
            playStopButton.setImageResource(R.drawable.pause);
        } else {
            playStopButton.setImageResource(R.drawable.play);
        }
        getVideoFile(playList, new File("/sdcard/"));
        Cursor cursor = getContentResolver().query(videoListUri, new String[]{"_display_name", "_data"}, null, null, null);
        int n = cursor.getCount();
        cursor.moveToFirst();
        final LinkedList<MovieInfo> playList2 = new LinkedList<MovieInfo>();
        for (int i = 0; i != n; ++i) {
            MovieInfo mInfo = new MovieInfo();
            mInfo.displayName = cursor.getString(cursor.getColumnIndex("_display_name"));
            mInfo.path = cursor.getString(cursor.getColumnIndex("_data"));
            playList2.add(mInfo);
            cursor.moveToNext();
        }
        if (playList2.size() > playList.size()) {
            playList = playList2;
        }
        videoView.setMySizeChangeListener(new VideoView.MySizeChangeListener() {
            @Override
            public void sizeChanged() {
                setVideoScale(SCREEN_DEFAULT);
            }
        });
        ejctButton.setAlpha(0.73f);
        backButton.setAlpha(0.73f);
        playStopButton.setAlpha(0.73f);
        forwardButton.setAlpha(0.73f);
        volumeButton.setAlpha(0.73f);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeButton.setAlpha(findAlphaFromSound());
        ejctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VideoPlayerActivity.this, VideoChooseActivity.class);
                VideoPlayerActivity.this.startActivityForResult(intent, 0);
                cancelDelayHide();
            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = playList.size();
                if (++position < n) {
                    videoView.setVideoPath(playList.get(position).path);
                    cancelDelayHide();
                    hideControllerDelay();
                } else {
                    VideoPlayerActivity.this.finish();
                }
            }
        });
        playStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDelayHide();
                if (isPaused) {
                    videoView.start();
                    playStopButton.setImageResource(R.drawable.pause);
                    hideControllerDelay();
                } else {
                    videoView.pause();
                    playStopButton.setImageResource(R.drawable.play);
                }
                isPaused = !isPaused;
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (--position >= 0) {
                    videoView.setVideoPath(playList.get(position).path);
                    cancelDelayHide();
                    hideControllerDelay();
                } else {
                    VideoPlayerActivity.this.finish();
                }
            }
        });
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDelayHide();
                if (isSoundShow) {
                    mSoundWindow.dismiss();
                } else {
                    if (mSoundWindow.isShowing()) {
                        mSoundWindow.update(15, 0, SoundView.MY_WIDTH, SoundView.MY_HEIGHT);
                    } else {
                        mSoundWindow.showAtLocation(videoView, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 15, 0);
                        mSoundWindow.update(15, 0, SoundView.MY_WIDTH, SoundView.MY_HEIGHT);
                    }
                }
                isSoundShow = !isSoundShow;
                hideControllerDelay();
            }
        });
        volumeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isSlient) {
                    volumeButton.setImageResource(R.drawable.soundenable);
                } else {
                    volumeButton.setImageResource(R.drawable.sounddisable);
                }
                isSlient = !isSlient;
                updateVolume(currentVolume);
                cancelDelayHide();
                hideControlerDelay();
                return true;
            }
        });
        seekBar = (SeekBar) controllerView.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                myHandler.removeMessages(HIDE_CONTROLLER);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myHandler.sendEmptyMessageDelayed(HIDE_CONTROLLER,TIME);
            }
        });
        getScreenSize();
        mGestureDetector=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(isFullScreen){
                    setVideoScale(SCREEN_DEFAULT);
                }else{
                    setVideoScale(SCREEN_FULL);
                }
                isFullScreen=!isFullScreen;
                if(isControllerShow){
                    showController();
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
               if(!isControllerShow){
                   showController();
                   hideControllerDelay();
               }else{
                   cancelDelayHide();
                   hideController();
               }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if(isPaused){
                    videoView.start();
                    playStopButton.setImageResource(R.drawable.pause);
                    cancelDelayHide();
                    hideControllerDelay();
                }else{
                    videoView.pause();
                    playStopButton.setImageResource(R.drawable.play);
                    cancelDelayHide();
                    showController();
                }
                isPaused=!isPaused;
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                setViedoScale(SCREEN_DEFAULT);
                isFullScreen=false;
                if(isControllerShow){
                    showController();
                }
                int i=videoView.getDuration();
                seekBar.setMax(i);
                i/=1000;
                int minute=i/60;
                int hour =minute/60;
                int second=i%60;
                minute%=60;
                durationTextView.setText(String.format("%02d:%02d:%02d",hour,minute,second));
                videoView.start();
                playStopButton.setImageResource(R.drawable.pause);
                hideControllerDelay();
                myHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                int n=playList.size();
                if(++position<n){
                    videoView.setVideoPath(playList.get(position).path);
                }else{
                    VideoPlayerActivity.this.finish();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==0&&resultCode== Activity.RESULT_OK){
            int result=data.getIntExtra("CHOOSE",-1);
            if(result!=-1){
                videoView.setVideoPath(playList.get(result).path);
                position=result;
            }
            return;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }
    private final static int PROGRESS_CHANGED=0;
    private final static int HIDE_CONTROLLER=1;
    Handler myHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
           switch(msg.what){
               case PROGRESS_CHANGED: {
                   break;
               }
               case HIDE_CONTROLLER:{
                   break;
               }
           }
            super.handleMessage(msg);
        }
    };



}
