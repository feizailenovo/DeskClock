package com.feizai.deskclock.activity;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.LogUtil;

/**
 * Author: chenhao
 * Date: 2022年3月26日 0026 下午 02:02:00
 * Describe:
 */
public class AlarmAlert extends AlarmAlertFullScreen {

    /**
     * If we try to check the keyguard more than 5 times, just launch the full screen activity.
     * 如果我们尝试检查键盘锁超过 5 次，只需启动全屏活动。
     */
    private int mKeyguardRetryCount;
    private final int MAX_KEYGUARD_CHECKS = 5;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleScreenOff((KeyguardManager) msg.obj);
        }
    };

    private final BroadcastReceiver mScreenOffReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    KeyguardManager km = (KeyguardManager) context
                            .getSystemService(Context.KEYGUARD_SERVICE);
                    handleScreenOff(km);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * Listen for the screen turning off so that when the screen comes back on,
         * the user does not need to unlock the phone to dismiss the alarm.
         * 监听屏幕是否关闭，这样当屏幕重新亮起时，用户无需解锁手机即可解除警报。
         */
        registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOffReceiver);
        /**
         * Remove any of the keyguard messages just in case
         * 删除任何键盘保护消息以防万一
         */
        mHandler.removeMessages(0);
    }

    @Override
    public void onBackPressed() {
        return;
    }

    private boolean checkRetryCount() {
        if (mKeyguardRetryCount++ >= MAX_KEYGUARD_CHECKS) {
            LogUtil.v("Tried to read keyguard status too many times, bailing...");
            return false;
        }
        return true;
    }

    private void handleScreenOff(final KeyguardManager km) {
        if (!km.inKeyguardRestrictedInputMode() && checkRetryCount()) {
            if (checkRetryCount()) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(0, km), 500);
            }
        } else {
            /**
             * Launch the full screen activity but do not turn the screen on.
             * 启动全屏活动，但不要打开屏幕。
             */
            Intent intent = new Intent(this, AlarmAlertFullScreen.class);
            intent.putExtra(Alarms.ALARM_INTENT_EXTRA, mAlarm);
            intent.putExtra(SCREEN_OFF, true);
            startActivity(intent);
            finish();
        }
    }
}
