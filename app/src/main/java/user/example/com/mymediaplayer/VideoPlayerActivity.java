package user.example.com.mymediaplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
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
            if (videoView.getVideoHeight() == 0) {
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
                   int i=videoView.getCurrentPosition();
                   seekBar.setProgress(i);
                   i/=100;
                   int minute=i/60;
                   int hour=minute/60;
                   int second=i%60;
                   minute%=60;

                   break;
               }
               case HIDE_CONTROLLER:{
                   hideController();
                   break;
               }
           }
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        getScreenSize();
        if(isControllerShow){
            cancelDelayHide();
            hideController();
            showController();
            hideControllerDelay();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        playedTime=videoView.getCurrentPosition();
        videoView.pause();
        playStopButton.setImageResource(R.drawable.play);
        super.onPause();
    }

    @Override
    protected void onResume() {
        videoView.seekTo(playedTime);
        videoView.start();
        if(videoView.getVideoHeight()!=0){
         playStopButton.setImageResource(R.drawable.pause);
            hideControllerDelay();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(controller.isShowing()){
            controller.dismiss();
        }
        if(mSoundWindow.isShowing()){
            mSoundWindow.dismiss();
        }
        myHandler.removeMessages(PROGRESS_CHANGED);
        myHandler.removeMessages(HIDE_CONTROLLER);
        playList.clear();
        super.onDestroy();
    }
    private void getScreenSize(){
        Display display=getWindowManager().getDefaultDisplay();
        screenWidth=display.getWidth();
        screenHeight=display.getHeight();
        controllerHeight=screenHeight/4;
    }
    private void hideController(){
        if(controller.isShowing()){
            controller.update(0,0,0,0);
            isControllerShow=false;
        }
        if(mSoundWindow.isShowing()){
            mSoundWindow.dismiss();
            isSoundShow=false;
        }
    }
    private void hideControllerDelay(){
        myHandler.sendEmptyMessageDelayed(HIDE_CONTROLLER,TIME);
    }
    private void showController(){
        controller.update(0,0,screenWidth,controllerHeight);
        isControllerShow=true;
    }
    private void cancelDelayHide(){
        myHandler.removeMessages(HIDE_CONTROLLER);
    }

    private final static int SCREEN_FULL=0;
    private final static int SCREEN_DEFAULT=1;
    private void setVideoScale(int flag){
        switch (flag){
            case SCREEN_FULL:
            videoView.setVideoScale(screenWidth, screenHeight);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;
            case SCREEN_DEFAULT:
                int videoWidth=videoView.getVideoWidth();
                int videoHeight=videoView.getVideoHeight();
                int mWidth=screenWidth;
                int mHeight=screenHeight-25;
                if(videoWidth>0&&videoHeight>0){
                    if(videoWidth*mHeight>mWidth*videoHeight){
                        mHeight=mWidth*videoHeight/videoWidth;
                    }else if(videoWidth*mHeight<mWidth*videoHeight){
                        mWidth=mHeight*videoWidth/videoHeight;
                    }
                }
                videoView.setVideoScale(mWidth,mHeight);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;
        }

    }
    private float findAlphaFromSound(){
        if(mAudioManager!=null){
            int alpha=currentVolumn*(0xCC-0x55)/maxVolume+0x55;
            return (float)alpha;
        }else{
            return (float)0xCC;
        }
    }
    private void updateVolume(int index){
        if(mAudioManager!=null){
            if(isSlient){
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
            }else{
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,index,0);
            }
            currentVolume=index;
            volumeButton.setAlpha(findAlphaFromSound());
        }
    }
    private void getVideoFile(final LinkedList<MovieInfo> list,File file){
        file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name=pathname.getName();
                int i=name.indexOf('.');
                if(i!=-1){
                    name=name.substring(i);
                    if(name.equalsIgnoreCase(".mp4")||name.equalsIgnoreCase(".3gp")){
                        MovieInfo mi=new MovieInfo();
                        mi.displayName=pathname.getName();
                        mi.path=pathname.getAbsolutePath();
                        list.add(mi);
                        return true;
                    }
                }else if(pathname.isDirectory()){
                    getVideoFile(pathname,file);
                }
                return false;
            }
        });
    }
}
