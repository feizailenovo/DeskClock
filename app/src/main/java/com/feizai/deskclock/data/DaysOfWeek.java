package com.feizai.deskclock.data;

import android.content.Context;

import com.feizai.deskclock.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Author: chenhao
 * Date: 2022/3/26-0026 上午 09:51:35
 * Describe: 用于记录闹钟重复响铃的周期
 */
/*
 * Days of week code as a single int.
 * 0x00: no day
 * 0x01: Monday
 * 0x02: Tuesday
 * 0x04: Wednesday
 * 0x08: Thursday
 * 0x10: Friday
 * 0x20: Saturday
 * 0x40: Sunday
 */
public final class DaysOfWeek {
    private static int[] DAY_MAP = new int[]{
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY,
    };

    /**
     * Bitmask of all repeating days.
     * 采用8位二进制的形式记录
     */
    private int mDays;

    DaysOfWeek(int days) {
        mDays = days;
    }

    /**
     * 返回闹钟的重复时间
     * @param context
     * @param weekStringArrayRes 字符串数组
     * @return
     */
    public String toString(Context context, int weekStringArrayRes) {
        StringBuilder builder = new StringBuilder();

        // no days
        if (mDays == 0) {
            return "只响一次";
        }

        // every day
        if (mDays == 0x7f) {
            return "每天";
        }

        // count selected days
        int dayCount = 0;
        int days = mDays;
        String[] dayArray = context.getResources().getStringArray(weekStringArrayRes);
        for (int i = 0; i < 7 && days > 0; i++) {
            if ((days & 1) == 1) {
                if (dayCount > 0) {
                    builder.append(",");
                }
                builder.append(dayArray[i]);
                dayCount++;
            }
            days >>= 1;
        }
        return builder.toString();
    }

    private boolean isSet(int day) {
        return ((mDays & (1 << day)) > 0);
    }

    public void set(int day, boolean set) {
        if (set) {
            mDays |= (1 << day);
        } else {
            mDays &= ~(1 << day);
        }
    }

    public void set(DaysOfWeek dow) {
        mDays = dow.mDays;
    }

    public int getCoded() {
        return mDays;
    }

    public void setOnlyOne() {
        mDays = 0x00;
    }

    // Returns days of week encoded in an array of booleans.
    public boolean[] getBooleanArray() {
        boolean[] ret = new boolean[7];
        for (int i = 0; i < 7; i++) {
            ret[i] = isSet(i);
        }
        return ret;
    }

    public boolean isRepeatSet() {
        return mDays != 0;
    }

    /**
     * returns number of days from today until next alarm
     * 返回从今天到下一个闹钟的天数
     * @param c must be set to today
     */
    public int getNextAlarm(Calendar c) {
        if (mDays == 0) {
            return -1;
        }

        int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        int day = 0;
        int dayCount = 0;
        for (; dayCount < 7; dayCount++) {
            day = (today + dayCount) % 7;
            if (isSet(day)) {
                break;
            }
        }
        return dayCount;
    }
}
