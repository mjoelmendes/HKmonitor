/*
 *  @ProjectName: ISMS_Petrel_MCU
 *  @Copyright: 2016 HangZhou Hikvision System Technology Co., Ltd. All Right Reserved.
 *  @address: http://www.hikvision.com
 *  @Description: 本内容仅限于杭州海康威视系统技术公有限司内部使用，禁止转发.
 */

package com.example.andy.Utils;

import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import hik.common.isms.hpsclient.AbsTime;


public class CalendarUtil {

    private static final String TAG = "CalendarUtil";

    private static final String yyyy_MM_dd_T_HH_mm_ss_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String yyyy_MM_dd_T_HH_mm_ssZ = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";

    private CalendarUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 获取当天的开始时间
     *
     * @return Calendar
     */
    public static Calendar getDefaultStartCalendar() {
        Calendar currentCalendar = Calendar.getInstance();
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        currentCalendar.set(year, month, day, 0, 0, 0);
        return currentCalendar;
    }


    /**
     * 根据传入的时间点，计算当天的 （24时-1ms） 最后时刻对应的时间
     *
     * @param time 当天 最后时刻-1ms当天 最后时刻-1ms
     * @return 最后时刻
     */
    public static long getCurDayEndTime(long time) {
        if (0 >= time) {
            Log.e(TAG, "getCurDayEndTime : The long time < 0");
            return time;
        }
        Calendar curCalendar = Calendar.getInstance();
        curCalendar.setTimeInMillis(time);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(Calendar.YEAR, curCalendar.get(Calendar.YEAR));
        endCalendar.set(Calendar.MONTH, curCalendar.get(Calendar.MONTH));
        endCalendar.set(Calendar.DAY_OF_MONTH, curCalendar.get(Calendar.DAY_OF_MONTH));
        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endCalendar.set(Calendar.MINUTE, 59);
        endCalendar.set(Calendar.SECOND, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);
        return endCalendar.getTimeInMillis();
    }


    /**
     * 将"2018-05-07T14:41:57.819+03:00"型转换为Calendar,注意将转换为手机设备所在时区的Calendar
     *
     * @param formatTime "2018-05-07T14:41:57.819+03:00"型time
     * @return Calendar
     */
    public static Calendar yyyy_MM_dd_T_HH_mm_SSSZToCalendar(String formatTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (TextUtils.isEmpty(formatTime)) {
            Log.e(TAG, "convertToCalendar : The formatting time is null");
            return calendar;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(yyyy_MM_dd_T_HH_mm_ss_SSSZ, Locale.getDefault());
        try {
            Date date = sdf.parse(formatTime);
            calendar.setTime(date);
            return calendar;
        } catch (ParseException e) {
            Log.e(TAG, "convertToCalendar : The time format not 2018-05-07T14:41:57.819+03:00");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(yyyy_MM_dd_T_HH_mm_ssZ, Locale.getDefault());
            try {
                Date date = simpleDateFormat.parse(formatTime);
                calendar.setTime(date);
                return calendar;
            } catch (ParseException pw) {
                Log.e(TAG, "convertToCalendar : The time format not 2018-05-07T14:41:57+03:00");
                return calendar;
            }
        }
    }


    /**
     * 将Calendar格式化为"2018-05-07T14:41:57.819+03:00",转换为手机设备所在时区
     *
     * @param calender calenderTime
     * @return 格式化时间字符串
     */
    public static String calendarToyyyy_MM_dd_T_HH_mm_SSSZ(Calendar calender) {
        if (null == calender) {
            Log.e(TAG, "convertToString : The calender time is null");
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(yyyy_MM_dd_T_HH_mm_ss_SSSZ, Locale.getDefault());
        StringBuilder builder = new StringBuilder(sdf.format(calender.getTime()));
        builder.insert(26, ":");
        return builder.toString();
    }


    /**
     * 将Calendar转换为ABS_TIME
     *
     * @param calenderTime calenderTime
     * @return ABS_TIME
     */
    public static AbsTime calendarToABS(Calendar calenderTime) {
        if (null == calenderTime) {
            Log.e(TAG, "calendarToABS：The calender time is null");
            return null;
        }
        AbsTime startTimeST = new AbsTime();
        startTimeST.setYear(calenderTime.get(Calendar.YEAR));
        startTimeST.setMonth(calenderTime.get(Calendar.MONTH) + 1);
        startTimeST.setDay(calenderTime.get(Calendar.DAY_OF_MONTH));
        startTimeST.setHour(calenderTime.get(Calendar.HOUR_OF_DAY));
        startTimeST.setMinute(calenderTime.get(Calendar.MINUTE));
        startTimeST.setSecond(calenderTime.get(Calendar.SECOND));
        return startTimeST;
    }


    /**
     * 将 "2018-05-07T14:41:57.819+03:00" 转换为 AbsTime
     *
     * @param formatTime "2018-05-07T14:41:57.819+03:00"
     * @return AbsTime
     */
    public static AbsTime yyyy_MM_dd_T_HH_mm_SSSZToABSTime(String formatTime) {
        Calendar calendar = yyyy_MM_dd_T_HH_mm_SSSZToCalendar(formatTime);
        return calendarToABS(calendar);
    }


    /**
     * 将 "2018-05-07T14:41:57.819+03:00" 转换为 long型
     *
     * @param formatTime "2018-05-07T14:41:57.819+03:00"
     * @return long型
     */
    public static long yyyy_MM_dd_T_HH_mm_SSSZToLong(String formatTime) {
        Calendar calendar = yyyy_MM_dd_T_HH_mm_SSSZToCalendar(formatTime);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static String getPresentTime() {
        return getFormatTime("yyyy-MM-dd HH:mm:ss.SSS");
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static String getPresentTimeNoMS() {
        return getFormatTime("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    public static String getPresentTimeNoSeconds() {
        return getFormatTime("yyyy/MM/dd HH:mm");
    }


    private static String getFormatTime(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

}
