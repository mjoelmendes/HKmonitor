package com.example.andy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.blankj.utilcode.util.SizeUtils;
import com.example.andy.Utils.CalendarUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * <p>时间条View </p>
 */
public class TimeBarView extends View {
    /*小时模式*/
    private static final int MODE_HOUR = 1;
    /*分钟模式*/
    private static final int MODE_MINUTE = 2;
    /*10分钟代表的毫秒值*/
    private static final int TEN_MINUTE = 10 * 60 * 1000;
    /*移动敏感度，过小会导致刷新太频繁*/
    private static final float MOVE_SENSITIVE = 0.2f;
    /*刻度颜色*/
    private int mScaleColor = 0x4cffffff;
    /*时间文字颜色*/
    private int mTextColor = 0x9effffff;
    /*中线图*/
    private Drawable mMiddleLineDrawable;
    /*时间条宽度*/
    private int mViewWidth;
    /*时间条高度*/
    private int mViewHeight;
    /*每一小格的宽度*/
    private float mDivisorWidth;
    /*每一小格代表的时长模式*/
    private int mDivisorMode;
    /*画笔*/
    private Paint mPaint;
    /*刻度的宽度*/
    private int mScaleWidth;
    /*刻度的高度*/
    private int mScaleHeight;
    /*上下两条线的宽度*/
    private int mUpAndDownLineWidth = SizeUtils.dp2px(1);
    /*视频区域画笔*/
    private Paint mVideoPaint;
    /*视频区域*/
    private RectF mVideoAreaRect;
    /*文本画笔*/
    private TextPaint mTextPaint;
    private int mTextSize = SizeUtils.dp2px(10);//文本大小
    /*中线代表的时间*/
    private long mMiddleLineMillis;
    /*上一次触摸的X坐标*/
    private float mLastTouchX = 0f;
    /*是否有移动*/
    private boolean mTouchMoved = false;
    /*按下标志,按下时相应外部输入*/
    private boolean mTouchDownFlag = false;
    /*时间条被冻结标志，冻结后时间条不能被移动*/
    private boolean mIsFrozen = false;
    /*是否双击标识*/
    private boolean isDouble;
    /*双指之前的距离*/
    private float beforeLength;
    /*双指移动后的距离*/
    private float afterLength;
    /*计算后的缩放比例*/
    private float mScale;
    private TimePickedCallBack onTimeBarMoveListener;
    private List<RecordSegment> mFileInfoList;
    private long mLeftTime;
    private long mMiddleLineDuration;


    public TimeBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            return;
        }
        initView();
    }

    private void initView() {
        //下面的值是初始化后就能确定的
        mMiddleLineDrawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.icon_time_bar_center_line, getContext().getTheme());
        //小刻度大小，大刻度高度 = 小刻度高度*2
        mScaleWidth = SizeUtils.dp2px(1f);
        mScaleHeight = SizeUtils.dp2px(8);
        mUpAndDownLineWidth = SizeUtils.dp2px(1);
        //文字大小
        mTextSize = SizeUtils.dp2px(10);
        //初始化画笔
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mPaint = new Paint();
        //初始化视频区域
        mVideoPaint = new Paint();
        mVideoPaint.setAntiAlias(true);
        mVideoAreaRect = new RectF();
        //初始化录像片段集合
        mFileInfoList = new ArrayList<>();
        //初始化时，设置每一时间格宽度为30像素,每一小格代表10分钟时间
        mDivisorWidth = 30f;
        mDivisorMode = MODE_HOUR;
        //初始化时，设置中线的时间为当天的00:00
        mMiddleLineMillis = getTodayStart(Calendar.getInstance().getTimeInMillis());

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawUpAndDownLine(canvas);
        drawScales(canvas);
        drawVideoArea(canvas);
        drawMiddleLine(canvas);
    }


    /**
     * 绘制上下线
     */
    private void drawUpAndDownLine(Canvas canvas) {
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mUpAndDownLineWidth);
        mPaint.setColor(mScaleColor);
    }

    /**
     * 绘制刻度
     */
    private void drawScales(Canvas canvas) {
        //设置刻度线宽度
        mPaint.setStrokeWidth(mScaleWidth);
        //计算需要画的格子数量
        int scaleNum = (int) (mViewWidth / mDivisorWidth) + 2;
        //中心点距离左边所占用的时长
        mMiddleLineDuration = (long) ((mViewWidth / 2f) * (TEN_MINUTE / mDivisorWidth));
        //计算屏幕最左边的时间
        mLeftTime = mMiddleLineMillis - mMiddleLineDuration;
        //计算左边第一个刻度线的位置
        long minuteNum = (long) Math.ceil(mLeftTime / TEN_MINUTE);
        float xPosition = (minuteNum * TEN_MINUTE - mLeftTime) * (mDivisorWidth / TEN_MINUTE);
        //计算左边第一个刻度线是今天的第多少个分钟数
        for (int i = 0; i < scaleNum; i++) {
            if (mDivisorMode == MODE_HOUR) {
                //小时级别的画法
                if (minuteNum % 6 == 0) { //大刻度
                    //画上面的大刻度
                    canvas.drawLine(xPosition + i * mDivisorWidth, 0, xPosition + i * mDivisorWidth, mScaleHeight * 2, mPaint);
                    String hourMinute = getHourMinute(minuteNum);
                    float timeStrWidth = mTextPaint.measureText(hourMinute);
                    //画出时间文字
                    canvas.drawText(hourMinute, xPosition + i * mDivisorWidth - timeStrWidth / 2, mScaleHeight * 2 + mTextSize, mTextPaint);
                } else {//小刻度
                    //画上面的小刻度
                    canvas.drawLine(xPosition + i * mDivisorWidth, 0, xPosition + i * mDivisorWidth, mScaleHeight, mPaint);
                }
            } else if (mDivisorMode == MODE_MINUTE) {
                for (int j = 0; j < 10; j++) {
                    float startX = xPosition + i * mDivisorWidth;
                    //画上面的大刻度
                    if (j == 0) {
                        canvas.drawLine(startX, 0, startX, mScaleHeight * 2, mPaint);
                        String hourMinute = getHourMinute(minuteNum);
                        float timeStrWidth = mTextPaint.measureText(hourMinute);
                        //画出时间文字
                        canvas.drawText(hourMinute, startX - timeStrWidth / 2, mScaleHeight * 2 + mTextSize, mTextPaint);
                    } else {
                        //画上面的小刻度
                        canvas.drawLine(startX + j * mDivisorWidth * 0.1f, 0, startX + j * mDivisorWidth * 0.1f, mScaleHeight, mPaint);
                    }
                }
            }
            //每一格刻度代表10分钟
            minuteNum++;
        }

    }

    /**
     * 绘制视频区域
     */
    private void drawVideoArea(Canvas canvas) {
        if (mFileInfoList.isEmpty()) {
            return;
        }
        long rightTime = mLeftTime + mMiddleLineDuration * 2;
        for (RecordSegment recordSegment : mFileInfoList) {
            //给画笔设置颜色
            mVideoPaint.setColor(ContextCompat.getColor(getContext(), R.color.playback_timebar_color));
            //先将录像片段的开始和结束时间转换为时间戳
            long segmentBeginTime = CalendarUtil.yyyy_MM_dd_T_HH_mm_SSSZToCalendar(recordSegment.getBeginTime()).getTimeInMillis();
            long segmentEndTime = CalendarUtil.yyyy_MM_dd_T_HH_mm_SSSZToCalendar(recordSegment.getEndTime()).getTimeInMillis();
            //1、首先判断是否全部包含了从左到右的时间段
            boolean isContainTime = segmentBeginTime <= mLeftTime && segmentEndTime >= rightTime;
            boolean isLeftTime = isCurrentTimeArea(segmentBeginTime, mLeftTime, rightTime);
            boolean isRightTime = isCurrentTimeArea(segmentEndTime, mLeftTime, rightTime);
            //视频区域的top
            int areaHeightTop = mScaleHeight * 2 + mTextSize + SizeUtils.dp2px(4.0f);
            float leftX = 0f;
            float rightX = 0f;
            if (isContainTime) {
                //录像片段超过整个屏幕
                leftX = 0;
                rightX = mViewWidth;
            } else if (isLeftTime && isRightTime) {
                //录像片段在屏幕包含范围内
                leftX = (segmentBeginTime - mLeftTime) * (mDivisorWidth / TEN_MINUTE);
                rightX = (segmentEndTime - mLeftTime) * (mDivisorWidth / TEN_MINUTE);
            } else if (isLeftTime) {
                //录像片段只有左边在(需要从左边，画到屏幕右侧)
                leftX = (segmentBeginTime - mLeftTime) * (mDivisorWidth / TEN_MINUTE);
                rightX = mViewWidth;
            } else if (isRightTime) {
                //录像片段只有右边在（画从头开始到右边时刻）
                leftX = 0;
                rightX = (segmentEndTime - mLeftTime) * (mDivisorWidth / TEN_MINUTE);
            }
            mVideoAreaRect.set(leftX, areaHeightTop, rightX, mViewHeight - SizeUtils.dp2px(4.0f));
            canvas.drawRect(mVideoAreaRect, mVideoPaint);
        }
    }

    /**
     * 绘制中线
     */
    private void drawMiddleLine(Canvas canvas) {
        int minimumWidth = mMiddleLineDrawable.getMinimumWidth();
        mMiddleLineDrawable.setBounds(mViewWidth / 2 - minimumWidth / 2, 0, mViewWidth / 2 + minimumWidth / 2, mViewHeight);
        mMiddleLineDrawable.draw(canvas);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mTouchDownFlag = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 2 && isDouble) {
                    //双指移动
                    afterLength = getDistance(event);// 获取两点的距离
                    if (beforeLength == 0) {
                        beforeLength = afterLength;
                    }
                    float gapLength = afterLength - beforeLength;// 变化的长度
                    if (Math.abs(gapLength) > 5f) {
                        mScale = afterLength / beforeLength;// 求的缩放的比例
                        //双指缩放了
                        beforeLength = afterLength;
                        onZooming();
                    }
                } else if (event.getPointerCount() == 1) {
                    onActionMove(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDouble) {
                    //双指抬起
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mIsFrozen = false;
                        }
                    }, 100);
                }
                if (!isDouble && mTouchMoved) {
                    //拖动结束后回调当前中线的时间戳
                    if (onTimeBarMoveListener != null) { // 回调
                        onTimeBarMoveListener.onTimePickedCallback(mMiddleLineMillis);
                    }
                }
                mTouchMoved = false;
                mTouchDownFlag = false;
                isDouble = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    //双指按下
                    mIsFrozen = true;
                    beforeLength = getDistance(event);
                    isDouble = true;
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 执行缩放方法
     */
    private void onZooming() {
        if (mScale > 1) {
            mDivisorWidth += 30f;
        } else {
            mDivisorWidth -= 30f;
        }
        if (mDivisorWidth < 160f) {
            mDivisorMode = MODE_HOUR;
            //小时级别了
            if (Math.abs(mDivisorWidth) < 30) {//不能小于10dp
                // 强制设置为小时级别的
                mDivisorWidth = 30f;
                if (onTimeBarMoveListener != null) {
                    onTimeBarMoveListener.onMinScale();
                }
            }
        } else if (mDivisorWidth < 320 * 1.5) {//不能超过1.5倍
            mDivisorMode = MODE_MINUTE;
            //分钟级别了
        } else {
            //已经是最大刻度
            mDivisorMode = MODE_MINUTE;
            mDivisorWidth = 320 * 1.5f;
            if (onTimeBarMoveListener != null) {
                onTimeBarMoveListener.onMaxScale();
            }
        }
        //重新绘制
        invalidate();
    }


    private void onActionMove(MotionEvent event) {
        // 判断移动距离，如果距离很小，不认为在移动（手指按下后，没有移动也会触发move事件）
        float movedX = event.getX() - mLastTouchX;
        if (Math.abs(movedX) < MOVE_SENSITIVE) {
            return;
        }
        // 更新上一次按下的X坐标
        mLastTouchX = event.getX();

        //禁止移动时，返回
        if (mIsFrozen) {
            return;
        }
        mTouchMoved = true;
        //更新当前中线代表的时间
        mMiddleLineMillis = mMiddleLineMillis - (long) (movedX * (TEN_MINUTE / mDivisorWidth));
        // 让窗口重绘
        invalidate();
        //时间轴被拖动的时候回调
        if (onTimeBarMoveListener != null) {
            onTimeBarMoveListener.onMoveTimeCallback(mMiddleLineMillis);
        }
    }

    /**
     * 计算两点的距离
     **/
    private float getDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 设置当前时间
     *
     * @param currentTimeMillis 当前时间戳
     */
    public void setCurrentTime(long currentTimeMillis) {
        //时间不符或者正在拖动时，不更新
        if (currentTimeMillis <= 0 || mTouchDownFlag) {
            return;
        }

        this.mMiddleLineMillis = currentTimeMillis;
        //时间轴自动移动的时候回调
        if (onTimeBarMoveListener != null) {
            onTimeBarMoveListener.onBarMoving(mMiddleLineMillis);
        }
        postInvalidate();
    }

    /**
     * 添加文件信息列表
     *
     * @param fileInfoList 文件信息列表
     */
    public void addFileInfoList(List<RecordSegment> fileInfoList) {
        if (null == fileInfoList) {
            return;
        }
        mFileInfoList.clear();
        mFileInfoList.addAll(fileInfoList);
        postInvalidate();
    }

    /**
     * 重置时间条
     *
     * @param d 日期
     */
    public void reset(Date d) {
        mFileInfoList.clear();
        setCurrentTime(d.getTime());
    }


    /**
     * 获取当前时间的起点（00:00:00）
     */
    public long getTodayStart(long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(currentTime));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 根据下标获取HH:mm格式的时间
     */
    public String getHourMinute(long timeIndex) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return format.format(new Date(timeIndex * 10 * 60 * 1000));
    }

    /**
     * 判断时间是否在时间段内
     */
    public boolean isCurrentTimeArea(long nowTime, long beginTime, long endTime) {
        return nowTime >= beginTime && nowTime <= endTime;
    }

    /**
     * 设置移动监听
     */
    public void setTimeBarCallback(TimePickedCallBack onBarMoveListener) {
        onTimeBarMoveListener = onBarMoveListener;
    }

    /**
     * 时间轴移动、拖动的回调
     */
    public interface TimePickedCallBack {
        /**
         * 当时间轴被拖动的时候回调
         *
         * @param currentTime 拖动到的中线时间
         */
        void onMoveTimeCallback(long currentTime);

        /**
         * 当时间轴自动移动的时候回调
         *
         * @param currentTime 当前时间
         */
        void onBarMoving(long currentTime);

        /**
         * 当拖动完成时回调
         *
         * @param currentTime 拖动结束时的时间
         */
        void onTimePickedCallback(long currentTime);


        /**
         * 超过最大缩放值
         */
        void onMaxScale();

        /**
         * 超过最小缩放值
         */
        void onMinScale();
    }


}
