package com.feizai.deskclock.util;

import android.content.Context;

import com.feizai.deskclock.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: chenhao
 * Date: 2022/2/11-0011 下午 02:07:49
 * Describe:
 */
public class AlarmClockUtil {
    public static String getAlarmClockTimes(Context context, String alarmClockTimes) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        String[] stringArray = context.getResources().getStringArray(R.array.week);
        for (int i = 0; i < alarmClockTimes.length(); i++) {
            if (alarmClockTimes.charAt(i) == '1') {
                if (count > 0) {
                    builder.append(",");
                }
                builder.append(stringArray[i]);
                count++;
            }
        }
        return builder.toString();
    }

    public static String getAlarmClockTimes(Context context, byte alarmClockTimes) {
        if (alarmClockTimes == 0x01) {
            return "只响一次";
        }else {
            return getAlarmClockTimes(context, ByteUtil.byteToBit(alarmClockTimes));
        }
    }

    public static int[] getAlarmClockTimesArray(byte alarmClockTimes) {
        String result = ByteUtil.byteToBit(alarmClockTimes);
        List<Integer> array = new ArrayList<>();
        for (int i = 0; i < result.length() - 1; i++) {
            if (result.charAt(i) == '1') {
                array.add(i);
            }
        }
        Integer[] integers = array.toArray(new Integer[0]);
        int[] temp = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
            temp[i] = integers[i];
        }
        return temp;
    }

    public static int[] getAlarmClockTimesArray(boolean[] arr) {
        List<Integer> array = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i]) {
                array.add(i);
            }
        }
        Integer[] integers = array.toArray(new Integer[0]);
        int[] temp = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
            temp[i] = integers[i];
        }
        return temp;
    }

//    public static Calendar timeStringToCalendar(String timeStr) {
//        Calendar calendar = Calendar.getInstance();
//    }
}
