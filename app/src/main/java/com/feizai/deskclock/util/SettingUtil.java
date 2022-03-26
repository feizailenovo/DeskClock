package com.feizai.deskclock.util;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateFormat;

/**
 * Author: chenhao
 * Date: 2022/2/12-0012 上午 11:02:28
 * Describe:
 */
public class SettingUtil {
    public static Boolean getTime_12_24(Context context) {
        String value = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
        if (value != null)
            return "12".equals(value);
        else
            return !DateFormat.is24HourFormat(context);
    }

    public static void setTime_12_24(Context context, Boolean value) {
        Settings.System.putString(context.getContentResolver(), Settings.System.TIME_12_24, value ? "12" : "24");
    }

    public static Boolean getAutoTime(Context context) {
        int value = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
        return value == 1;
    }

    public static void setAutoTime(Context context, Boolean value) {
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, value ? 1 : 0);
    }
}
