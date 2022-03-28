package com.feizai.deskclock.receiver;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.feizai.deskclock.AlarmAlertWakeLock;
import com.feizai.deskclock.R;
import com.feizai.deskclock.activity.AlarmAlert;
import com.feizai.deskclock.activity.AlarmAlertFullScreen;
import com.feizai.deskclock.data.Alarm;
import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.LogUtil;

/**
 * Author: chenhao
 * Date: 2022年3月26日 0026 下午 01:17:02
 * Describe: 闹钟广播接收器
 * 将 AlarmAlert IntentReceiver 连接到 AlarmAlert 活动。 通过警报 ID。
 */
public class AlarmReceiver extends BroadcastReceiver {

    private final static String APP_NAME = "DeskClock";
    private final static String PACKAGE_NAME = "com.feizai.deskclock";

    /**
     * 如果警报早于 STALE_WINDOW，则忽略。 这可能是时间或时区更改的结果
     * If the alarm is older than STALE_WINDOW, ignore.
     * It is probably the result of a time or timezone change
     */
    private final static int STALE_WINDOW = 30 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Alarms.ALARM_KILLED.equals(action)) {
            updateNotification(context,
                    (Alarm) intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA),
                    intent.getIntExtra(Alarms.ALARM_KILLED_TIMEOUT, -1));
            return;
        } else if (Alarms.CANCEL_SNOOZE.equals(action)) {
            Alarms.saveSnoozeAlert(context, -1, -1);
            return;
        } else if (!Alarms.ALARM_ALERT_ACTION.equals(action)) {
            return;
        }

        Alarm alarm = null;
        /**
         * Grab the alarm from the intent. Since the remote AlarmManagerService
         * fills in the Intent to add some extra data, it must unparcel the
         * Alarm object. It throws a ClassNotFoundException when unparcelling.
         * To avoid this, do the marshalling ourselves.
         * 从意图中获取警报。 由于远程AlarmManagerService填写Intent添加一些额外的数据，它必须解包Alarm对象。
         * 拆包时会抛出 ClassNotFoundException。 为避免这种情况，请自己进行编组。
         */
        final byte[] date = intent.getByteArrayExtra(Alarms.ALARM_RAW_DATA);
        if (date != null) {
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(date, 0, date.length);
            parcel.setDataPosition(0);
            alarm = Alarm.CREATOR.createFromParcel(parcel);
        }

        if (alarm == null) {
            LogUtil.d("Failed to parse the alarm from the intent");
            Alarms.setNextAlert(context);
            return;
        }


        /**
         * Disable the snooze alert if this alarm is the snooze.
         * 如果此闹钟是睡眠，则禁用睡眠闹钟。
         */
        Alarms.disableSnoozeAlert(context, alarm.id);

        /**
         * Disable this alarm if it does not repeat
         * 如果不重复，则禁用此闹钟。
         */
        if (!alarm.daysOfWeek.isRepeatSet()) {
            Alarms.enableAlarm(context, alarm.id, false);
        } else {

            /**
             * Enable the next alert if there is one. The above call to
             * enableAlarm will call setNextAlert so avoid calling it twice.
             * 如果有下一个闹钟，则启用下一个闹钟。
             * 上面对 enableAlarm 的调用将调用 setNextAlert 所以避免调用它两次。
             */
            Alarms.setNextAlert(context);
        }

        // Intentionally verbose: always log the alarm time to provide useful
        // information in bug reports.
        // 故意冗长：始终记录警报时间以在错误报告中提供有用的信息。
        /**
         * Intentionally verbose: always log the alarm time to provide useful
         * information in bug reports.
         * 故意冗长：始终记录警报时间以在错误报告中提供有用的信息。
         */
        long now = System.currentTimeMillis();

        // Always verbose to track down time change problems.
        // 总是冗长地跟踪时间变化问题。
        /**
         * Always verbose to track down time change problems.
         *  总是冗长地跟踪时间变化问题。
         */
        if (now > alarm.time + STALE_WINDOW) {
            LogUtil.v("Ignoring stale alarm");
            return;
        }

        /**
         * Maintain a cpu wake lock until the AlarmAlert and AlarmKlaxon can pick it up.
         * 保持 cpu 唤醒锁定，直到 AlarmAlert 和 AlarmService 可以拾取它。
         */
        AlarmAlertWakeLock.acquireCpuWakeLock(context);

        /**
         * Close dialogs and window shade
         * 关闭系统对话框
         */
        Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        closeDialogs.setPackage(PACKAGE_NAME);
        context.sendBroadcast(closeDialogs);

        /**
         * Decide which activity to start based on the state of the keyguard.
         * 根据键盘锁的状态决定启动哪个活动。
         */
        Class alertActivityClass = AlarmAlert.class;
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            alertActivityClass = AlarmAlertFullScreen.class;
        }

        /**
         * Play the alarm alert and vibrate the device.
         * 播放闹铃和振动设备
         */
        Intent playAlarm = new Intent();
        playAlarm.setAction(Alarms.ALARM_ALERT_ACTION);
        playAlarm.setPackage(PACKAGE_NAME);
        playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        context.startService(playAlarm);

        /**
         * Trigger a notification that, when clicked, will show the alarm alert
         * dialog. No need to check for fullscreen since this will always be
         * launched from a user action.
         * 触发通知，单击该通知将显示警报警报对话框。
         * 无需检查全屏，因为这将始终从用户操作启动。
         */
        Intent alarmNotify = new Intent(context, AlarmAlert.class);
        alarmNotify.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        PendingIntent alarmNotifyPending = PendingIntent.getActivity(context, alarm.id, alarmNotify, 0);

        /**
         * Use the alarm's label or the default label as the ticker text and
         * main text of the notification.
         * 使用警报的标签或默认标签作为通知的代码文本和主要文本。
         */
        String label = alarm.getLabelOrDefault();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            LogUtil.v("Android O prohibits background popups");
//            Intent playAlarmActivity = new Intent(context, alertActivityClass);
//            playAlarmActivity.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
//            playAlarmActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                    | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
//            context.startActivity(playAlarmActivity);
//        }

        /**
         * NEW: Embed the full-screen UI here. The notification manager will
         * take care of displaying it if it's OK to do so.
         * 新：在此处嵌入全屏 UI。 如果可以，通知管理器将负责显示它。
         */
        Intent alarmAlert = new Intent(context, alertActivityClass);
        alarmAlert.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(String.valueOf(alarm.id), APP_NAME, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(context, String.valueOf(alarm.id))
                .setContentTitle(context.getString(R.string.alarm_notify_title, alarm.getAlarmTime(context)))
                .setContentText(context.getString(R.string.alarm_notify_text))
                .setWhen(alarm.time)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(alarmNotifyPending)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setFullScreenIntent(PendingIntent.getActivity(context, alarm.id, alarmAlert, 0), true)
                .build();
        notification.flags |= Notification.FLAG_SHOW_LIGHTS
                | Notification.FLAG_ONGOING_EVENT;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        manager.notify(alarm.id, notification);
    }

    private void updateNotification(Context context, Alarm alarm, int timeout) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /**
         * If the alarm is null, just cancel the notification.
         * 如果alarm为空，仅仅取消通知
         */
        if (alarm == null) {
            LogUtil.d("Cannot update notification for killer callback");
            return;
        }

        /**
         * Launch SetAlarm when clicked.
         * 启动闹钟设置当被点击的时候
         */
        Intent setAlarm = new Intent();
        setAlarm.putExtra(Alarms.ALARM_ID, alarm.id);
        PendingIntent setAlarmPending = PendingIntent.getActivity(context, alarm.id, setAlarm, 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(String.valueOf(alarm.id), APP_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(context, String.valueOf(alarm.id))
                .setContentTitle(context.getString(R.string.alarm_notify_title, alarm.getAlarmTime(context)))
                .setContentText(context.getString(R.string.alarm_alert_alert_silenced, timeout))
                .setWhen(alarm.time)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(setAlarmPending)
                .build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        /**
         * We have to cancel the original notification since it is in the
         * ongoing section and we want the "killed" notification to be a plain
         * notification.
         * 我们必须取消原始通知，因为它在正在进行的部分中，并且我们希望“已终止”通知成为普通通知。
         */
        manager.cancel(alarm.id);
        manager.notify(alarm.id, notification);
    }
}
