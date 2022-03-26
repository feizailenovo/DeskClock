package com.feizai.deskclock.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.feizai.deskclock.R;
import com.feizai.deskclock.base.BaseActivity;
import com.feizai.deskclock.data.Alarm;
import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.LogUtil;

import java.util.Calendar;

/**
 * Author: chenhao
 * Date: 2022年3月26日 0026 下午 02:02:42
 * Describe:
 */
public class AlarmAlertFullScreen extends BaseActivity {

    /**
     * These defaults must match the values in res/xml/settings.xml
     * 这些默认值必须与 res/xml/settings.xml 中的值匹配
     */
    private static final String DEFAULT_SNOOZE = "10";
    private static final String DEFAULT_VOLUME_BEHAVIOR = "2";
    protected static final String SCREEN_OFF = "screen_off";

    protected Alarm mAlarm;
    private int mVolumeBehavior;

    private static final String PACKAGE_NAME = "com.feizai.deskclock";
    private static final String APP_NAME = "DeskClock";
    private int SNOOZE_TIME_IN_MINUTES = 5;
    private static final long SECOND = 1000 * 60;

    public AlarmAlertFullScreen() {
        super(R.layout.layout_alarm_alert);
    }

    @Override
    protected void findView() {
        findViewById(R.id.btn_snooze).setOnClickListener(v -> {
            snooze();
        });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dismiss(false);
        });

        /**
         * Register to get the alarm killed/snooze/dismiss intent.
         * 注册广播接收器以获取闹钟终止/暂停/解除意图。
         */
        IntentFilter filter = new IntentFilter(Alarms.ALARM_KILLED);
        filter.addAction(Alarms.ALARM_SNOOZE_ACTION);
        filter.addAction(Alarms.ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void myCreate() {
        mAlarm = getIntent().getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        /**
         * sign changed by reason
         * 标志因原因而改变
         */
        mAlarm = Alarms.getAlarm(getContentResolver(), mAlarm.id);

        /**
         * Get the volume/camera button behavior setting
         * 获取音量/相机按钮行为设置
         */
        mVolumeBehavior = Integer.parseInt(DEFAULT_VOLUME_BEHAVIOR);

        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        /**
         * Turn on the screen unless we are being launched from the AlarmAlert subclass.
         * 除非我们从 AlarmAlert 子类启动，否则打开屏幕。
         */
        if (!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }
    }

    @Override
    protected void initData() {
        /**
         * If the alarm was deleted at some point, disable snooze.
         * 如果警报在某个时候被删除，请禁用贪睡。
         */
        if (Alarms.getAlarm(getContentResolver(), mAlarm.id) == null) {
            findViewById(R.id.btn_snooze).setEnabled(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtil.v("AlarmAlert.OnNewIntent()");
        mAlarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
    }

    /**
     * Attempt to snooze this alert.
     * 闹钟暂停几分钟
     */
    private void snooze() {
        /**
         * Do not snooze if the snooze button is disabled.
         * 如果睡眠按钮被禁用，则不睡眠。
         */
        if (!findViewById(R.id.btn_snooze).isEnabled()) {
            dismiss(false);
            return;
        }
        final long snoozeTime = System.currentTimeMillis() + SNOOZE_TIME_IN_MINUTES * SECOND;
        /**
         * Get the display time for the snooze and update the notification.
         * 获取睡眠的显示时间并更新通知。
         */
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(snoozeTime);
        calendar.set(Calendar.SECOND, 0);
        Alarms.saveSnoozeAlert(this, mAlarm.id, calendar.getTimeInMillis());

        /**
         * Notify the user that the alarm has been snoozed.
         * 通知用户闹钟已暂停。
         */
        Intent cancelSnooze = new Intent();
        cancelSnooze.setAction(Alarms.CANCEL_SNOOZE);
        cancelSnooze.putExtra(Alarms.ALARM_ID, mAlarm.id);
        cancelSnooze.setPackage(PACKAGE_NAME);
        PendingIntent cancelSnoozePending = PendingIntent.getBroadcast(this, mAlarm.id, cancelSnooze, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(String.valueOf(mAlarm.id), APP_NAME, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, String.valueOf(mAlarm.id))
                .setContentTitle(getString(R.string.alarm))
                .setContentText(getString(R.string.alarm_notify_snooze_text, SNOOZE_TIME_IN_MINUTES))
                .setWhen(mAlarm.time)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(cancelSnoozePending)
                .build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
        manager.notify(mAlarm.id, notification);

        /**
         * Display the snooze minutes in a toast.
         * 在Toast中显示闹钟睡眠分钟数
         */
        Toast.makeText(this, getString(R.string.alarm_toast_snooze_text, SNOOZE_TIME_IN_MINUTES), Toast.LENGTH_LONG).show();
        Intent stopAlarm = new Intent();
        stopAlarm.setAction(Alarms.ALARM_ALERT_ACTION);
        stopAlarm.setPackage(PACKAGE_NAME);
        stopService(stopAlarm);
        finish();
    }

    private void dismiss(boolean killed) {
        /**
         * The service told us that the alarm has been killed, do not modify
         * the notification or stop the service.
         * 服务告诉我们警报已被杀死，不要修改通知或停止服务。
         */
        if (!killed) {
            // Cancel the notification and stop playing the alarm
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(mAlarm.id);
            Intent stopAlarm = new Intent();
            stopAlarm.setAction(Alarms.ALARM_ALERT_ACTION);
            stopAlarm.setPackage(PACKAGE_NAME);
            stopService(stopAlarm);
        }
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.v("AlarmAlert.onDestroy()");
        /**
         * No longer care about the alarm being killed.
         * 不再关心警报被杀死。
         */
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        /**
         * Do this on key down to handle a few of the system keys.
         * 在按键上执行此操作以处理一些系统键。
         */
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
            /**
             * Volume keys and camera keys dismiss the alarm
             * 音量键和拍照键将取消闹钟
             */
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (up) {
                    switch (mVolumeBehavior) {
                        case 1:
                            snooze();
                            break;
                        case 2:
                            dismiss(false);
                            break;
                        default:
                            break;
                    }
                }
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        /**
         * Don't allow back to dismiss. This method is overriden by AlarmAlert
         * so that the dialog is dismissed.
         * 不允许回退。 此方法被警报警报覆盖，以便关闭对话框。
         * 屏蔽返回键放置退出闹钟弹框
         */
        return;
    }

    /**
     * Receives the ALARM_KILLED action from the AlarmService,
     * and also ALARM_SNOOZE_ACTION / ALARM_DISMISS_ACTION from other applications
     * 接收来自 AlarmService 的 ALARM_KILLED 动作，
     * 以及来自其他应用程序的 ALARM_SNOOZE_ACTION / ALARM_DISMISS_ACTION
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Alarms.ALARM_SNOOZE_ACTION)) {
                snooze();
            } else if (action.equals(Alarms.ALARM_DISMISS_ACTION)) {
                dismiss(false);
            } else {
                Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
                if (alarm != null && mAlarm.id == alarm.id) {
                    dismiss(true);
                }
            }
        }
    };
}
