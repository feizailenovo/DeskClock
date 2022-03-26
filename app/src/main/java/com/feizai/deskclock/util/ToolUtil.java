package com.feizai.deskclock.util;

import com.feizai.deskclock.constant.AlarmClockConst;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: chenhao
 * Date: 2022/2/12-0012 下午 12:12:34
 * Describe:
 */
public class ToolUtil {
    public static SimpleDateFormat getTimeLongSimpleFormat(Boolean is12Hour) {
        if (is12Hour) {
            return new SimpleDateFormat(AlarmClockConst.TWELVE_HOUR_LONG_FORMAT);
        } else {
            return new SimpleDateFormat(AlarmClockConst.TWENTY_FOUR_HOUR_LONG_FORMAT);
        }
    }

    public static SimpleDateFormat getTimeShortSimpleFormat(Boolean is12Hour) {
        if (is12Hour) {
            return new SimpleDateFormat(AlarmClockConst.TWELVE_HOUR_SHORT_FORMAT);
        } else {
            return new SimpleDateFormat(AlarmClockConst.TWENTY_FOUR_HOUR_SHORT_FORMAT);
        }
    }

    public static String getNowTimeShortFormat() {
        return new SimpleDateFormat(AlarmClockConst.TWENTY_FOUR_HOUR_SHORT_FORMAT).format(System.currentTimeMillis());
    }

    public static String getNowTimeLongFormat() {
        return new SimpleDateFormat(AlarmClockConst.TWENTY_FOUR_HOUR_LONG_FORMAT).format(System.currentTimeMillis());
    }

    public static Date stringToDate(String dateString, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String timestampToString(long timestamp) {
        long nd = 1000 * 24 * 60 * 60;//1天
        long nh = 1000 * 60 * 60;//1小时
        long nm = 1000 * 60; //1分钟
        long ns = 1000;
        long day = timestamp / nd;// 计算差多少天
        long hour = timestamp % nd / nh;// 计算差多少小时
        long min = timestamp % nd % nh / nm;// 计算差多少分钟
        long second = timestamp % nd % nh % nm / ns;// 计算差多少秒
        if (day > 0) {
            return day + "天" + hour + "时" + min + "分" + second + "秒";
        } else if (hour > 0) {
            return hour + "时" + min + "分" + second + "秒";
        } else if (min > 0) {
            return min + "分" + second + "秒";
        } else {
            return "小于1分钟";
        }
    }
}
