package com.example.twj.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.twj.view.IconTextView;
import com.example.twj.view.PermissionHelper;
import com.example.twj.view.PermissionInterface;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, PermissionInterface {

    @BindView(R.id.surface)
    TextureView mTextureView;
    //半透明黑色背景
    @BindView(R.id.back_view)
    View backView;
    //播放进度条
    @BindView(R.id.seek_bar)
    SeekBar seekBar;
    //播放时间
    @BindView(R.id.time_left)
    TextView timeLeft;
    //总时间
    @BindView(R.id.time_right)
    TextView timeRight;
    //时间加进度条的布局
    @BindView(R.id.seek_and_time)
    LinearLayout seekAndTime;
    //上一个按钮
    @BindView(R.id.video_last)
    IconTextView videoLast;
    //暂停播放按钮
    @BindView(R.id.video_play)
    IconTextView videoPlay;
    //下一个按钮
    @BindView(R.id.video_next)
    IconTextView videoNext;
    //控制台布局
    @BindView(R.id.control_layout)
    RelativeLayout controlLayout;
    //重播按钮
    @BindView(R.id.video_again)
    IconTextView videoAgain;
    //关闭按钮
    @BindView(R.id.video_close)
    IconTextView videoClose;
    //音量亮度图标
    @BindView(R.id.icon_view)
    IconTextView iconView;
    //音量亮度百分比
    @BindView(R.id.icon_text)
    TextView iconText;
    //音量亮度布局
    @BindView(R.id.digital_layout)
    LinearLayout digitalLayout;

    private Unbinder bind;
    private MediaPlayer mMediaPlayer;
    private String path;
    private Surface surf;
    private boolean mIsVideoReadyToBePlayed = false;
    private Uri uri;
    //是否正在播放
    private boolean isPlay = true;
    //控制台是否显示
    private boolean isControl = false;
    //视频是否播放完成
    private boolean isPlayFinish = false;

    //点击纵坐标
    private float dY = 0;
    //点击横坐标
    private float dX = 0;
    //抬起纵坐标
    private float uY = 0;
    //抬起横坐标
    private float uX = 0;
    //媒体音量管理
    private AudioManager audioManager;
    //屏幕当前亮度百分比
    private float f = 0;
    //手机当前亮度模式 0 1
    private int countLight;
    //系统当前亮度 1-255
    private int currLight;
    //UI界面改变,进度条,时间跳动
    private Handler mHandler;
    //音量
    private double MaxSound;
    //用来记录坐标值
    private float uY1;

    private PermissionHelper mPermissionHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bind = ButterKnife.bind(this);
        mHandler = new Handler();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //系统最大音量
        MaxSound = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //初始化并发起权限申请
        mPermissionHelper = new PermissionHelper(this, this);
        mPermissionHelper.requestPermissions();

        init();
    }

    private void init() {
        //控制台显示隐藏
        refreshControlLayout();

        mTextureView.setSurfaceTextureListener(this);
        //旋转
        mTextureView.setRotation(0);
        File file = new File("/sdcard/Download/321.mp4");
        uri = Uri.fromFile(file);
        initScreenLight();

        videoAgain.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
                        videoAgain.setTextSize(45f);
                        break;
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        videoAgain.setTextSize(50f);
                        break;
                }
                return false;
            }
        });

        //手势控制
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    //按下
                    case MotionEvent.ACTION_DOWN:
                        dX = motionEvent.getX();
                        dY = motionEvent.getY();
                        uY1 = dY;
                        if (dX > getWidth() / 2) {//声音控制
                            //获取当前音量
                            double currentSount = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            double i = currentSount / MaxSound;
                            if (i == 0) {
                                iconView.setText(R.string.ic_volume_no);
                            } else {
                                iconView.setText(R.string.ic_volume);
                            }
                            //设置百分比
                            iconText.setText(doubleToString(i) + "");
                        } else if (dX <= getWidth() / 2) {//亮度控制
                            iconView.setText(R.string.ic_sun);
                            //设置百分比
                            iconText.setText(doubleToString(f));
                        }
                        break;
                    //抬起
                    case MotionEvent.ACTION_UP:
                        digitalLayout.setVisibility(View.GONE);
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        uY = motionEvent.getY();
                        uX = motionEvent.getX();
                        if (uY == uY1) {
                            Log.i("--==", "滑动停止");
                        } else {
                            Log.i("--==", "正在滑动");
                            if (dX > getWidth() / 2) {//声音控制
                                if (Math.abs(uY1-uY) > 3)
                                    setVolume(uY1-uY);
                            } else if (dX <= getWidth() / 2) {//亮度控制
                                if (Math.abs(uY1-uY) > 1)
                                    setLight(uY1-uY);
                            }
                            uY1 = uY;
                        }

                        break;
                }
                return false;
            }
        });
    }


    //初始化屏幕亮度
    private void initScreenLight() {
        try {
            //获取亮度模式 0：手动 1：自动
            countLight = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            //设置手动设置
//            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            //获取屏幕亮度,获取失败则返回255
            currLight = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    255);
            f = currLight / 255f;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    //手势调节音量
    private void setVolume(float vol) {
        digitalLayout.setVisibility(View.VISIBLE);
        if (vol > 0) {//增大音量
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                    0);
        } else if (vol < 0) {//降低音量
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                    0);
        } else if (vol == 0) {

        }

//        //----------------------
//        f += vol / getWidth();
//        if (f > 1) {
//            f = 15f;
//        } else if (f <= 0) {
//            f = 0.000f;
//        }
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) f, 0);

        //获取当前音量
        double currentSount = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        double i = currentSount / MaxSound;
        if (i == 0) {
            iconView.setText(R.string.ic_volume_no);
        } else {
            iconView.setText(R.string.ic_volume);
        }
        //设置百分比
        iconText.setText(doubleToString(i) + "");

        //音量控制Bar的当前值设置为系统音量当前值
//        volumeProgressBar.setProgress(currentSount);
    }

    /**
     * double转String,保留小数点后两位
     *
     * @param num
     * @return
     */
    public static String doubleToString(double num) {
        double v = num * 100;
        //使用0.00不足位补0，#.##仅保留有效位
        return new DecimalFormat("0").format(v);
    }

    /**
     * 手势设置屏幕亮度
     * 设置当前的屏幕亮度值，及时生效 0.004-1
     * 该方法仅对当前应用屏幕亮度生效
     */
    private void setLight(float vol) {
        digitalLayout.setVisibility(View.VISIBLE);
        Window localWindow = getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        f += (vol / getWidth()) * 4;
        if (f > 1) {
            f = 1f;
        } else if (f <= 0) {
            f = 0.000f;
        }
        localLayoutParams.screenBrightness = f;
        localWindow.setAttributes(localLayoutParams);
        iconView.setText(R.string.ic_sun);
        //设置百分比
        iconText.setText(doubleToString(f));
    }

    public int getWidth() {
        WindowManager manager = getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public int getHeight() {
        WindowManager manager = getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    @OnClick({R.id.video_last, R.id.video_play, R.id.video_next, R.id.surface, R.id.video_again, R.id.video_close})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //播放暂停按钮
            case R.id.video_play:
                if (isPlayFinish) {
                    //如果播放完成
                    isPlayFinish = false;
                    isPlay = true;
                    mMediaPlayer.start();
                    visibleButton();
                    mHandler.post(seekAndTimeRunnable);
                    videoPlay.setText(R.string.ic_video_suspend);
                } else {
                    //如果正在播放
                    if (isPlay) {
                        //暂停
                        mMediaPlayer.pause();
                        videoPlay.setText(R.string.ic_video_play);
                        isPlay = false;
                    } else {
                        //播放
                        mMediaPlayer.start();
                        videoPlay.setText(R.string.ic_video_suspend);
                        isPlay = true;
                    }
                }
                break;
            //上一个按钮
            case R.id.video_last:
                //获得视频地址
                // TODO: 2018/8/16
                if (path == null) {
                    return;
                } else {
                    mMediaPlayer.reset();
                    try {
                        mMediaPlayer.setDataSource(path);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        isPlay = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    videoPlay.setText(R.string.ic_video_suspend);
//                    Toast.makeText(this, "上一个", Toast.LENGTH_SHORT).show();
                }
                break;
            //下一个按钮
            case R.id.video_next:
                // TODO: 2018/8/16
                if (path == null) {
                    return;
                } else {

                    mMediaPlayer.reset();
                    try {
                        mMediaPlayer.setDataSource(path);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        isPlay = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    videoPlay.setText(R.string.ic_video_suspend);
//                    Toast.makeText(this, "下一个", Toast.LENGTH_SHORT).show();
                }
                break;
            //重播按钮
            case R.id.video_again:
                isPlayFinish = false;
                // TODO: 2018/8/16
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(path);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                    isPlay = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoPlay.setText(R.string.ic_video_suspend);
                isPlay = true;
                break;
            case R.id.surface:
                mHandler.removeCallbacks(refreshControlRunnable);
                refreshControlLayout();
                break;
            //关闭
            case R.id.video_close:
                mHandler.removeCallbacks(seekAndTimeRunnable);
                mHandler.removeCallbacks(refreshControlRunnable);
                finish();
                break;
        }
    }

    @SuppressLint("NewApi")
    private void playVideo(SurfaceTexture surfaceTexture) {
        mIsVideoReadyToBePlayed = false;
        try {
            path = String.valueOf(uri);
            if (path == null) {
                return;
            }
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            if (surf == null) {
                surf = new Surface(surfaceTexture);
            }
            mMediaPlayer.setSurface(surf);
            mMediaPlayer.prepareAsync();
            //监听事件，网络流媒体的缓冲监听
            mMediaPlayer.setOnBufferingUpdateListener(this);
            //监听事件，网络流媒体播放结束监听
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            // 左右声道控制         左声道(门外)   右声道(门内)
            mMediaPlayer.setVolume(0, 1);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
        }
    }

    //开始播放
    private void startVideoPlayback() {
        mMediaPlayer.start();
        isPlay = true;
        visibleButton();
        Log.i("--==>>", "开始播放");
    }

    //刷新控制台 显示则隐藏 隐藏则显示 并5S之后隐藏
    private void refreshControlLayout() {
        if (isControl) {
            controlLayout.setVisibility(View.INVISIBLE);
            isControl = false;
        } else {
            controlLayout.setVisibility(View.VISIBLE);
            isControl = true;
            mHandler.removeCallbacks(refreshControlRunnable);
            mHandler.postDelayed(refreshControlRunnable, 5000);
        }
    }

    //隐藏重播按钮
    private void visibleButton() {
        if (isPlay) {
            backView.setVisibility(View.INVISIBLE);
            videoAgain.setVisibility(View.GONE);
        }
    }

    //释放MediaPlayer资源
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    //时间格式
    private String formatTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        return format.format(time);
    }

    //准备完成
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoReadyToBePlayed) {
            startVideoPlayback();
        }
        //计算视频的长度
        int position = mMediaPlayer.getDuration();
        timeRight.setText(formatTime(position));
        seekBar.setMax(position);
        mHandler.post(seekAndTimeRunnable);
        final View.OnTouchListener seekBarTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    //按下
                    case MotionEvent.ACTION_DOWN:
                        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                        mHandler.removeCallbacks(seekAndTimeRunnable);
                        break;
                    //抬起
                    case MotionEvent.ACTION_UP:
                        seekBar.setOnSeekBarChangeListener(null);
                        mHandler.post(seekAndTimeRunnable);
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                        mHandler.removeCallbacks(seekAndTimeRunnable);
                        break;
                }
                return false;
            }
        };
        seekBar.setOnTouchListener(seekBarTouchListener);
    }

    //播放完成
    @SuppressLint("ResourceAsColor")
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        isPlayFinish = true;
        videoPlay.setText(R.string.ic_video_play);
        isPlay = false;
        visibleButton();
        //屏幕变灰效果
        backView.setVisibility(View.VISIBLE);
        backView.setAlpha(0.5f);
        videoAgain.setVisibility(View.VISIBLE);
        isControl = false;
        refreshControlLayout();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        playVideo(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    //缓冲中
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(seekAndTimeRunnable);
        mHandler.removeCallbacks(refreshControlRunnable);
//        mHandler.removeMessages(0);
//        mHandler.removeMessages(-1);
        super.onPause();
        releaseMediaPlayer();
        mIsVideoReadyToBePlayed = false;
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(seekAndTimeRunnable);
        mHandler.removeCallbacks(refreshControlRunnable);
        super.onDestroy();
        mIsVideoReadyToBePlayed = false;
        releaseMediaPlayer();
//        bind.unbind();
    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            Log.i("--==>>", seekBar.getProgress() + "");
            mMediaPlayer.seekTo(seekBar.getProgress());
            timeLeft.setText(formatTime(seekBar.getProgress()));
            if (timeLeft != timeRight) {
                backView.setVisibility(View.INVISIBLE);
                videoAgain.setVisibility(View.GONE);
            } else {
                //屏幕变灰效果
                backView.setVisibility(View.VISIBLE);
                backView.setAlpha(0.5f);
                videoAgain.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            timeLeft.setText(formatTime(seekBar.getProgress()));
            visibleButton();
            isControl = false;
            refreshControlLayout();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            timeLeft.setText(formatTime(seekBar.getProgress()));
            visibleButton();
            isControl = false;
            refreshControlLayout();
        }
    };

    private Runnable seekAndTimeRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO: 2018/3/22
            int currentPosition = mMediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            String time = formatTime(currentPosition);
            timeLeft.setText(time);
            mHandler.postDelayed(seekAndTimeRunnable, 10);
        }
    };

    private Runnable refreshControlRunnable = new Runnable() {

        @Override
        public void run() {
            refreshControlLayout();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }


    @Override
    public int getPermissionsRequestCode() {
        //设置权限请求requestCode，只有不跟onRequestPermissionsResult方法中的其他请求码冲突即可。
        return 10000;
    }

    @Override
    public String[] getPermissions() {
        //设置该界面所需的全部权限
        return new String[]{
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

    }

    @Override
    public void requestPermissionsSuccess() {
//权限请求不被用户允许。可以提示并退出或者提示权限的用途并重新发起权限申请。
        finish();
    }

    @Override
    public void requestPermissionsFail() {
//已经拥有所需权限，可以放心操作任何东西了
    }
}
