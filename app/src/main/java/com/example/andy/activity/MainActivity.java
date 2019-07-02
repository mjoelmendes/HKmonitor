package com.example.andy.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.andy.Utils.HikUtil;
import com.example.andy.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    //----------------------------------------------------------------------------------------------
    private SurfaceView video_surfaceview;
    //----------------------------------------------------------------------------------------------
    private static final int PLAY_HIK_STREAM_CODE = 1001;
    private static final int PLAY_HIK_STREAM_CODE_2 = 1002;
    private String IP_ADDRESS = "172.18.36.157";
    private int PORT = 8000;
    private String USER_NAME = "admin";
    private String PASSWORD = "ketan123";
    private HikUtil hikUtil;
    private int channel = 0;
    //    private FloatingActionButton btn_up, btn_down, btn_left, btn_right;
    private Button btn_up, btn_down, btn_left, btn_right, btn_in, btn_out, btn_switch, btn_stop, btn_playback;
    //----------------------------------------------------------------------------------------------

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case PLAY_HIK_STREAM_CODE:
                    hikUtil.playOrStopStream();
                    break;
                case PLAY_HIK_STREAM_CODE_2:
//                    hikUtil2.playOrStopStream();
                    break;
                default:
                    break;
            }
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setOnTouchListener();
        startVedio();
    }

    private void startVedio() {
        HikUtil.initSDK();
        hikUtil = new HikUtil();
        hikUtil.initView(video_surfaceview);

        //配置网络摄像头参数
        hikUtil.setDeviceData(IP_ADDRESS, PORT, USER_NAME, PASSWORD);
        //登录设备
        hikUtil.loginDevice(mHandler, PLAY_HIK_STREAM_CODE);
        //播放设备
        hikUtil.playOrStopStream();
    }

    private void switchVedio() {
        if (channel == 0) {
            channel = 1;
            IP_ADDRESS = "172.18.36.157";
        } else {
            channel = 0;
            IP_ADDRESS = "172.18.36.158";
        }

        HikUtil.initSDK();
        hikUtil = new HikUtil();
        hikUtil.initView(video_surfaceview);
        //配置网络摄像头参数
        hikUtil.setDeviceData(IP_ADDRESS, PORT, USER_NAME, PASSWORD);
        //登录设备
        hikUtil.loginDevice(mHandler, PLAY_HIK_STREAM_CODE);
        //播放设备
        hikUtil.playOrStopStream();
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setOnTouchListener() {
        //摄像头向上转动
        btn_up.setOnTouchListener(new View.OnTouchListener() {
            //由于调用向左转动的方法，会停不下来，所以这里我加了监听点击按键不放即调用启动向左转动方法，松开按键即调用停止向左转动的方法
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startup();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopup();
                }
                return false;
            }
        });

        //摄像头向下转动
        btn_down.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startdown();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopdown();
                }
                return false;
            }
        });

        //摄像头左转
        btn_left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startleft();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopleft();
                }
                return false;
            }
        });

        //摄像头右转
        btn_right.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startright();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopright();
                }
                return false;
            }
        });

        //焦距变大
        btn_in.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startfd();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopfd();
                }
                return false;
            }
        });

        //焦距变小
        btn_out.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startsx();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopsx();
                }
                return false;
            }
        });

        btn_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hikUtil.stopPlay();
                //注销设备
                hikUtil.logoutDevice();
                //释放sdk资源
                hikUtil.freeSDK();
                switchVedio();
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (HikUtil.playId < 0) {
                    btn_stop.setText("暂停");
                } else {
                    btn_stop.setText("播放");
                }
                //登录设备
                hikUtil.loginDevice(mHandler, PLAY_HIK_STREAM_CODE);
                //播放设备
                hikUtil.playOrStopStream();
            }
        });

        btn_playback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlaybackActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initView() {
        btn_up = findViewById(R.id.btn_up);
        btn_down = findViewById(R.id.btn_down);
        btn_left = findViewById(R.id.btn_left);
        btn_right = findViewById(R.id.btn_right);
        btn_in = findViewById(R.id.btn_in);
        btn_out = findViewById(R.id.btn_out);
        video_surfaceview = findViewById(R.id.video_surfaceview);
        btn_switch = findViewById(R.id.btn_switch);
        btn_stop = findViewById(R.id.btn_stop);
        btn_playback = findViewById(R.id.btn_playback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止播放
        hikUtil.stopPlay();
        //注销设备
        hikUtil.logoutDevice();
        //释放sdk资源
        hikUtil.freeSDK();
    }
}
