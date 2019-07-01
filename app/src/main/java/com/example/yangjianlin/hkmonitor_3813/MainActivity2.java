package com.example.yangjianlin.hkmonitor_3813;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;

public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "MainActivity2";
    //----------------------------------------------------------------------------------------------
    SurfaceView video_surfaceview;
    //----------------------------------------------------------------------------------------------
    private static final int PLAY_HIK_STREAM_CODE = 1001;
    private static final int PLAY_HIK_STREAM_CODE_2 = 1002;
    private String IP_ADDRESS = "172.18.36.157";
    private int PORT = 8000;
    private String USER_NAME = "admin";
    private String PASSWORD = "ketan123";
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
    private HikUtil hikUtil;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sxt);

        //摄像头向上转动
        FloatingActionButton actionA = findViewById(R.id.action_up);
        actionA.setOnTouchListener(new View.OnTouchListener() {
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
        FloatingActionButton actionB = findViewById(R.id.action_down);
        actionB.setOnTouchListener(new View.OnTouchListener() {
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
        FloatingActionButton actionC = findViewById(R.id.action_left);
        actionC.setOnTouchListener(new View.OnTouchListener() {
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
        FloatingActionButton actionD = findViewById(R.id.action_right);
        actionD.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HikUtil.startright();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    HikUtil.stopright();
                }
                return false;
            }
        });

        video_surfaceview = findViewById(R.id.video_surfaceview);
        HikUtil.initSDK();
        hikUtil = new HikUtil();
        hikUtil.initView(video_surfaceview);
        hikUtil.setDeviceData(IP_ADDRESS, PORT, USER_NAME, PASSWORD);
        //登录设备
        hikUtil.loginDevice(mHandler, PLAY_HIK_STREAM_CODE);
        //播放设备
        hikUtil.playOrStopStream();
    }
}
