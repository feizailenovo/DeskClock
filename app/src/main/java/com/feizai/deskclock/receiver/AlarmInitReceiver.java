package com.feizai.deskclock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.LogUtil;

/**
 * Author: chenhao
 * Date: 2022/3/26-0026 上午 11:59:52
 * Describe:
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.
     * Resets alarm on TIME_SET, TIMEZONE_CHANGED, LOCALE_CHANGED
     * 接受开机启动完成的广播，
     * 设置闹钟，当时区改变也设置
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.v("AlarmInitReceiver" + intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Alarms.saveSnoozeAlert(context, -1, -1);
        }
        Alarms.disableExpiredAlarms(context);
        Alarms.setNextAlert(context);
    }
}
