package com.example.andy.activity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.example.andy.PlayerStatus;
import com.example.andy.R;
import com.example.andy.RecordSegment;
import com.example.andy.TimeBarView;
import com.example.andy.Utils.CalendarUtil;
import com.example.andy.Utils.MyUtils;
import com.hikvision.open.hikvideoplayer.HikVideoPlayer;
import com.hikvision.open.hikvideoplayer.HikVideoPlayerCallback;
import com.hikvision.open.hikvideoplayer.HikVideoPlayerFactory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;

import hik.common.isms.hpsclient.AbsTime;

public class PlaybackActivity extends AppCompatActivity implements View.OnClickListener, HikVideoPlayerCallback, TextureView.SurfaceTextureListener {
    private static final String TAG = "PlaybackActivity";
    private static final String playbackUri = "rtsp://10.40.239.31:554/openUrl/xjmVph6";
    protected TextureView tuv_playback;
    protected ProgressBar progressBar;
    protected TextView tv_playHint;
    protected Button btn_start, btn_stop, btn_capture, btn_record, btn_sound, btn_pause;
    protected TimeBarView timeBar;
    protected EditText et_playbackUri;
    private TextView tv_mRecordFilePath;
    private String mUri;
    private HikVideoPlayer mPlayer;
    private boolean mSoundOpen = false;
    private boolean mRecording = false;
    private boolean mPausing = false;
    /*回放开始时间*/
    private Calendar mStartCalendar;
    /*回放结束时间*/
    private Calendar mEndCalendar;
    /*回放定位时间*/
    private Calendar mSeekCalendar = Calendar.getInstance();
    private PlayerStatus mPlayerStatus = PlayerStatus.IDLE;//默认闲置
    /**
     * 每隔400ms获取一次当前回放的系统时间
     * 更新时间条上的OSD时间
     */
    private final Runnable mGetOSDTimeTask = new Runnable() {
        @Override
        public void run() {
            long osdTime = mPlayer.getOSDTime();
            if (osdTime > -1) {
                timeBar.setCurrentTime(osdTime);
            }
            startUpdateTime();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);//防止键盘弹出
        setContentView(R.layout.activity_playback);
        initView();
        setListener();
        initTimeBarView();
        mPlayer = HikVideoPlayerFactory.provideHikVideoPlayer();
    }

    private void initView() {
        btn_start = findViewById(R.id.start);
        btn_stop = findViewById(R.id.stop);
        btn_capture = findViewById(R.id.capture_button);
        btn_record = findViewById(R.id.record_button);
        btn_sound = findViewById(R.id.sound_button);
        btn_pause = findViewById(R.id.pause_button);
        tuv_playback = findViewById(R.id.texture_view);
        tv_playHint = findViewById(R.id.result_hint_text);
        tv_mRecordFilePath = findViewById(R.id.record_file_path_text);
        et_playbackUri = findViewById(R.id.playback_uri_edit);
        et_playbackUri.setText(playbackUri);
        progressBar = findViewById(R.id.progress_bar);
        timeBar = findViewById(R.id.time_bar);
    }

    private void setListener() {
        btn_start.setOnClickListener(PlaybackActivity.this);
        btn_stop.setOnClickListener(PlaybackActivity.this);
        btn_capture.setOnClickListener(this);
        btn_record.setOnClickListener(this);
        btn_sound.setOnClickListener(this);
        btn_pause.setOnClickListener(PlaybackActivity.this);
        tuv_playback.setSurfaceTextureListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capture_button:
                executeCaptureEvent();
                break;
            case R.id.record_button:
                executeRecordEvent();
                break;
            case R.id.sound_button:
                executeSoundEvent();
                break;
            case R.id.pause_button:
                executePauseEvent();
                break;
            case R.id.start:
                if (mPlayerStatus != PlayerStatus.SUCCESS && getPlaybackUri()) {
                    startPlayback(tuv_playback.getSurfaceTexture());
                }
                break;
            case R.id.stop:
                if (mPlayerStatus == PlayerStatus.SUCCESS) {
                    mPlayerStatus = PlayerStatus.IDLE;//释放这个窗口
                    tv_mRecordFilePath.setText(null);
                    progressBar.setVisibility(View.GONE);
                    tv_playHint.setVisibility(View.VISIBLE);
                    tv_playHint.setText("");
                    cancelUpdateTime();
                    mPlayer.stopPlay();
                }
                break;
        }
    }


    /**
     * 判断Url是否为空和是否合法
     *
     * @return
     */
    private boolean getPlaybackUri() {
        mUri = et_playbackUri.getText().toString();
        if (TextUtils.isEmpty(mUri)) {
            et_playbackUri.setError("URI不能为空");
            return false;
        }

        if (!mUri.contains("rtsp")) {
            et_playbackUri.setError("非法URI");
            return false;
        }

        return true;
    }


    private void initTimeBarView() {
        RecordSegment recordSegment = new RecordSegment();
        //这里是模拟的假数据
        recordSegment.setBeginTime("2018-09-19T15:20:00.000+08:00");
        recordSegment.setEndTime("2018-09-19T15:30:00.000+08:00");
        //TODO 注意:TimeBarView中数据为你从服务器端获取到的录像片段列表
        timeBar.addFileInfoList(Arrays.asList(recordSegment));
        timeBar.setTimeBarCallback(new TimeBarView.TimePickedCallBack() {
            @Override
            public void onMoveTimeCallback(long currentTime) {

            }

            @Override
            public void onBarMoving(long currentTime) {

            }

            @Override
            public void onTimePickedCallback(long currentTime) {
                //TODO 注意: 定位操作的时间要在录像片段开始时间和结束时间之内，不再范围内不要执行以下操作
                mSeekCalendar.setTimeInMillis(currentTime);
                Log.e(TAG, "onTimePickedCallback: currentTime = " + CalendarUtil.calendarToyyyy_MM_dd_T_HH_mm_SSSZ(mSeekCalendar));
                final AbsTime start = CalendarUtil.calendarToABS(mSeekCalendar);
                progressBar.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cancelUpdateTime();//seek时停止刷新时间
                        if (!mPlayer.seekAbsPlayback(start, PlaybackActivity.this)) {
                            onPlayerStatus(Status.FAILED, mPlayer.getLastError());
                        }
                    }
                }).start();
            }

            @Override
            public void onMaxScale() {

            }

            @Override
            public void onMinScale() {

            }
        });
    }


    /**
     * 执行抓图事件
     */
    private void executeCaptureEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        //抓图
        if (mPlayer.capturePicture(MyUtils.getCaptureImagePath(this))) {
            ToastUtils.showShort("抓图成功");
        }
    }

    /**
     * 执行录像事件
     */
    private void executeRecordEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        if (!mRecording) {
            //开始录像
            tv_mRecordFilePath.setText(null);
            String path = MyUtils.getLocalRecordPath(this);
            if (mPlayer.startRecord(path)) {
                ToastUtils.showShort("开始录像");
                mRecording = true;
                btn_record.setText("关闭录像");
                tv_mRecordFilePath.setText(MessageFormat.format("当前本地录像路径:{0}", path));
            }
        } else {
            //关闭录像
            mPlayer.stopRecord();
            ToastUtils.showShort("关闭录像");
            mRecording = false;
            btn_record.setText("开始录像");
        }
    }

    /**
     * 执行声音开关事件
     */
    private void executeSoundEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        if (!mSoundOpen) {
            //打开声音
            if (mPlayer.enableSound(true)) {
                ToastUtils.showShort("声音开");
                mSoundOpen = true;
                btn_sound.setText("声音关");
            }
        } else {
            //关闭声音
            if (mPlayer.enableSound(false)) {
                ToastUtils.showShort("声音关");
                mSoundOpen = false;
                btn_sound.setText("声音开");
            }
        }
    }

    /**
     * 执行播放暂停和恢复播放事件
     */
    private void executePauseEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        if (!mPausing) {
            //暂停播放
            if (mPlayer.pause()) {
                ToastUtils.showShort("暂停播放");
                mPausing = true;
                btn_pause.setText("恢复");
            }
        } else {
            //恢复播放
            if (mPlayer.resume()) {
                ToastUtils.showShort("恢复播放");
                mPausing = false;
                btn_pause.setText("暂停");
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //TODO 注意:APP前后台切换时 SurfaceTextureListener可能在有某些 华为手机 上不会回调，例如：华为P20，所以我们在这里手动调用
        if (tuv_playback.isAvailable()) {
            Log.e(TAG, "onResume: onSurfaceTextureAvailable");
            onSurfaceTextureAvailable(tuv_playback.getSurfaceTexture(), tuv_playback.getWidth(), tuv_playback.getHeight());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO 注意:APP前后台切换时 SurfaceTextureListener可能在有某些 华为手机 上不会回调，例如：华为P20，所以我们在这里手动调用
        if (tuv_playback.isAvailable()) {
            Log.e(TAG, "onPause: onSurfaceTextureDestroyed");
            onSurfaceTextureDestroyed(tuv_playback.getSurfaceTexture());
        }
    }

    /**
     * 开始回放
     */
    private void startPlayback(SurfaceTexture surface) {
        progressBar.setVisibility(View.VISIBLE);
        tv_playHint.setVisibility(View.GONE);
        mPlayer.setSurfaceTexture(surface);
        //TODO 注意: 开始时间为你从服务端获取的录像片段列表中第一个片段的开始时间，结束时间为录像片段列表的最后一个片段的结束时间
        long startLongTime = CalendarUtil.getDefaultStartCalendar().getTimeInMillis();
        mStartCalendar = Calendar.getInstance();
        mEndCalendar = Calendar.getInstance();
        long endTime = CalendarUtil.getCurDayEndTime(startLongTime);
        mStartCalendar.setTimeInMillis(startLongTime);
        mEndCalendar.setTimeInMillis(endTime);
        final AbsTime startTimeST = CalendarUtil.calendarToABS(mStartCalendar);
        final AbsTime stopTimeST = CalendarUtil.calendarToABS(mEndCalendar);

        //TODO 注意: startPlayback() 方法会阻塞当前线程，需要在子线程中执行,建议使用RxJava
        new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO 注意: 不要通过判断 startPlayback() 方法返回 true 来确定播放成功，播放成功会通过HikVideoPlayerCallback回调，startPlayback() 方法返回 false 即代表 播放失败;
                //TODO 注意: seekTime 参数可以为NULL，表示无需定位到指定时间开始播放。
                if (!mPlayer.startPlayback(mUri, startTimeST, stopTimeST, PlaybackActivity.this)) {
                    onPlayerStatus(Status.FAILED, mPlayer.getLastError());
                }
            }
        }).start();
    }


    /**
     * 播放结果回调
     *
     * @param status    共四种状态：SUCCESS（播放成功）、FAILED（播放失败）、EXCEPTION（取流异常）、FINISH（录像回放结束）
     * @param errorCode 错误码，只有 FAILED 和 EXCEPTION 才有值
     */
    @Override
    @WorkerThread
    public void onPlayerStatus(@NonNull final Status status, final int errorCode) {
        //TODO 注意: 由于 HikVideoPlayerCallback 是在子线程中进行回调的，所以一定要切换到主线程处理UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                switch (status) {
                    case SUCCESS:
                        //播放成功
                        mPlayerStatus = PlayerStatus.SUCCESS;
                        tv_playHint.setVisibility(View.GONE);
                        tuv_playback.setKeepScreenOn(true);//保持亮屏
                        timeBar.setCurrentTime(mPlayer.getOSDTime());
                        startUpdateTime();//开始刷新回放时间
                        break;
                    case FAILED:
                        //播放失败
                        mPlayerStatus = PlayerStatus.FAILED;
                        tv_playHint.setVisibility(View.VISIBLE);
                        tv_playHint.setText(MessageFormat.format("回放失败，错误码：{0}", Integer.toHexString(errorCode)));
                        break;
                    case EXCEPTION:
                        //取流异常
                        mPlayerStatus = PlayerStatus.EXCEPTION;
                        mPlayer.stopPlay();//TODO 注意:异常时关闭取流
                        tv_playHint.setVisibility(View.VISIBLE);
                        tv_playHint.setText(MessageFormat.format("取流发生异常，错误码：{0}", Integer.toHexString(errorCode)));
                        break;
                    case FINISH:
                        //录像回放结束
                        mPlayerStatus = PlayerStatus.FINISH;
                        ToastUtils.showShort("没有录像片段了");
                        break;
                }
            }
        });
    }


    /**
     * 开始刷新回放时间
     */
    private void startUpdateTime() {
        tv_playHint.getHandler().postDelayed(mGetOSDTimeTask, 400);
    }

    /**
     * 停止刷新回放时间
     */
    private void cancelUpdateTime() {
        tv_playHint.getHandler().removeCallbacks(mGetOSDTimeTask);
    }


    /*************************TextureView.SurfaceTextureListener 接口的回调方法********************/
    //TODO 注意:APP前后台切换时 SurfaceTextureListener可能在有某些华为手机上不会回调，例如：华为P20，因此我们需要在Activity生命周期中手动调用回调方法
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mPlayerStatus == PlayerStatus.STOPPING) {
            //恢复处于暂停播放状态的窗口
            startPlayback(tuv_playback.getSurfaceTexture());
            Log.d(TAG, "onSurfaceTextureAvailable: startPlayback");
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayerStatus == PlayerStatus.SUCCESS) {
            mPlayerStatus = PlayerStatus.STOPPING;//暂停播放，再次进入时恢复播放
            mPlayer.stopPlay();
            cancelUpdateTime();
            Log.d(TAG, "onSurfaceTextureDestroyed: stopPlay");
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

}
