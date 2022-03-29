package com.feizai.deskclock.data;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.feizai.deskclock.util.LogUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Author: chenhao
 * Date: 2022/3/26-0026 上午 10:19:42
 * Describe: The Alarms provider supplies info about Alarm Clock settings
 * 核心类，对Clock设置提供支持信息
 */
public class Alarms {

    /**
     * package name
     * 包名
     */
    private static final String PACKAGE_NAME = "com.feizai.deskclock";

    private static final String PREFERENCES = "AlarmClock";

    /**
     * This action triggers the AlarmReceiver as well as the AlarmService. It
     * is a public action used in the manifest for receiving Alarm broadcasts
     * from the alarm manager.
     * 这个动作触发了AlarmReceiver和AlarmService。它是在清单中使用的公共操作，用于接收警报管理器的警报广播。
     */
    public static final String ALARM_ALERT_ACTION = PACKAGE_NAME + ".ALARM_ALERT";

    /**
     * A public action sent by AlarmKlaxon when the alarm has stopped sounding
     * for any reason (e.g. because it has been dismissed from AlarmAlertFullScreen,
     * or killed due to an incoming phone call, etc).
     * 当警报因任何原因停止发出时(例如，因为它已经从AlarmAlertFullScreen 中被排除，或由于一个来电而被杀死，等等)，
     * 由AlarmService发送的公共行动。
     */
    public static final String ALARM_DONE_ACTION = PACKAGE_NAME + ".ALARM_DONE";

    /**
     * AlarmAlertFullScreen listens for this broadcast intent, so that other applications
     * can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     * AlarmAlertFullScreen 侦听此广播意图，以便其他应用程序可以使警报停止(在ALARM_ALERT_ACTION之后和在ALARM_DONE_ACTION之前)。
     */
    public static final String ALARM_SNOOZE_ACTION = PACKAGE_NAME + ".ALARM_SNOOZE";

    /**
     * AlarmAlertFullScreen listens for this broadcast intent, so that other applications
     * can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     * AlarmAlertFullScreen 侦听此广播意图，以便其他应用程序可以解散警报(在ALARM_ALERT_ACTION之后和在ALARM_DONE_ACTION之前)。
     */
    public static final String ALARM_DISMISS_ACTION = PACKAGE_NAME + ".ALARM_DISMISS";

    /**
     * This is a private action used by the AlarmService to update the UI to
     * show the alarm has been killed.
     * 这是一个私有操作，由AlarmService 用来更新UI以显示警报已被杀死。
     */
    public static final String ALARM_KILLED = "alarm_killed";

    /**
     * Extra in the ALARM_KILLED intent to indicate to the user how long the
     * alarm played before being killed.
     * ALARM_KILLED中的Extra意图告诉用户在被杀死之前警报播放了多长时间。
     */
    public static final String ALARM_KILLED_TIMEOUT = "alarm_killed_timeout";

    /**
     * This string is used to indicate a silent alarm in the db.
     * 闹钟静音字段
     */
    public static final String ALARM_ALERT_SILENT = "silent";

    /**
     * This intent is sent from the notification when the user cancels the
     * snooze alert.
     * 这个意图在用户取消睡眠提醒时从通知中发送。
     */
    public static final String CANCEL_SNOOZE = "cancel_snooze";

    /**
     * ALARM_KILLED_TIMEOUT
     * 这个意图是闹钟超时取消报警
     */
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    /**
     * This extra is the raw Alarm object data. It is used in the
     * AlarmManagerService to avoid a ClassNotFoundException when filling in
     * the Intent extras.
     * 这个额外的是原始Alarm对象数据。它在AlarmManagerService中被使用，以避免在填充Intent额外内容时出现ClassNotFoundException异常。
     */
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    /**
     * This string is used to identify the alarm id passed to SetAlarm from the
     * list of alarms.
     * 该字符串用于标识从闹钟列表中传递给SetAlarm的闹钟id。
     */
    public static final String ALARM_ID = "alarm_id";

    final static String PREF_SNOOZE_ID = "snooze_id";
    final static String PREF_SNOOZE_TIME = "snooze_time";
    final static String PREF_AT_THE_SAME_TIME = "at_the_same_time";
    final static String PREF_MIN_TIME = "min_time";

    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E h:mm";

    private final static String M12 = "h:mm aa";
    // Shared with DigitalClock
    private final static String M24 = "hh:mm";


    /**
     * Creates a new Alarm and fills in the given alarm's id.
     * 创建一个新的Alarm并填写给定的Alarm的id。
     */
    public static long addAlarm(Context context, Alarm alarm) {
        long timeInMillis = calculateAlarm(alarm);
        alarm.time = timeInMillis;

        ContentValues values = createContentValues(alarm);
        Uri uri = context.getContentResolver().insert(Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);

        if (alarm.enabled) {
            clearSnoozeIfNeeded(context, timeInMillis);
        }
        setNextAlert(context);
        return timeInMillis;
    }

    /**
     * Removes an existing Alarm. If this alarm is snoozing, disables snooze.
     * Sets next alert.
     * 删除已存在的闹钟。如果这个闹钟正在睡眠，禁用睡眠功能。设置下一个警报。
     */
    public static void deleteAlarm(Context context, int alarmId) {
        if (alarmId == -1) return;

        enableAlarm(context, alarmId, false);
        setNextAlert(context);

        ContentResolver contentResolver = context.getContentResolver();
        /* If alarm is snoozing, lose it */
        /* 取消正在睡眠的闹钟 */
        disableSnoozeAlert(context, alarmId);

        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

        setNextAlert(context);
    }

    /**
     * Queries all alarms
     * 获取闹钟的游标
     *
     * @return cursor over all alarms
     */
    public static Cursor getAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(
                Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    /**
     * Private method to get a more limited set of alarms from the database.
     * 私有方法来从数据库中获取更有限的一组警报。
     *
     * @param contentResolver
     * @return
     */
    private static Cursor getFilteredAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI,
                Alarm.Columns.ALARM_QUERY_COLUMNS, Alarm.Columns.WHERE_ENABLED,
                null, null);
    }

    private static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(8);
        /**
         * Set the alarm_time value if this alarm does not repeat. This will be
         * used later to disable expire alarms.
         * 如果此闹钟不重复，请设置alarm_time的值。这将用于禁用过期闹钟。
         */
        long time = 0;
        if (!alarm.daysOfWeek.isRepeatSet()) {
            time = calculateAlarm(alarm);
        }

        values.put(Alarm.Columns.ENABLED, alarm.enabled ? 1 : 0);
        values.put(Alarm.Columns.HOUR, alarm.hour);
        values.put(Alarm.Columns.MINUTES, alarm.minutes);
        values.put(Alarm.Columns.ALARM_TIME, alarm.time);
        values.put(Alarm.Columns.DAYS_OF_WEEK, alarm.daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, alarm.vibrate);
        values.put(Alarm.Columns.MESSAGE, alarm.label);

        // A null alert Uri indicates a silent alarm.
        // null alert Uri表示静默闹钟。
        values.put(Alarm.Columns.ALERT, alarm.alert == null ?
                ALARM_ALERT_SILENT : alarm.alert.toString());

        return values;
    }

    private static void clearSnoozeIfNeeded(Context context, long alarmTime) {

        /**
         * If this alarm fires before the next snooze, clear the snooze to
         * enable this alarm.
         * 如果此闹钟在下次睡眠之前触发，请清除睡眠功能以启用此闹钟。
         */
        SharedPreferences prefs =
                context.getSharedPreferences(PREFERENCES, 0);
        long snoozeTime = prefs.getLong(PREF_SNOOZE_TIME, 0);
        if (alarmTime < snoozeTime) {
            clearSnoozePreference(context, prefs);
        }
    }

    /**
     * Return an Alarm object representing the alarm id in the database.
     * Returns null if no alarm exists.
     * 返回一个Alarm对象，表示数据库中的告警id。如果不存在告警，则返回null。
     */
    public static Alarm getAlarm(ContentResolver contentResolver, int alarmId) {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId),
                Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, null);
        Alarm alarm = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                alarm = new Alarm(cursor);
            }
            cursor.close();
        }
        return alarm;
    }


    /**
     * A convenience method to set an alarm in the Alarms
     * content provider.
     * 在Alarms内容提供程序中设置闹钟方法。
     *
     * @return Time when the alarm will fire.
     */
    public static long setAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        ContentResolver resolver = context.getContentResolver();
        resolver.update(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id),
                values, null, null);

        long timeInMillis = calculateAlarm(alarm);

        if (alarm.enabled) {
            /**
             * Disable the snooze if we just changed the snoozed alarm. This
             * only does work if the snoozed alarm is the same as the given alarm.
             * TODO: disableSnoozeAlert should have a better name.
             * 如果我们刚刚更改了睡眠闹钟，禁用睡眠闹钟。
             * 这只在睡眠闹钟和给定闹钟相同的情况下才有效。
             * TODO: disableSnoozeAlert 应该有一个更好的名称。
             */
            disableSnoozeAlert(context, alarm.id);

            /**
             * Disable the snooze if this alarm fires before the snoozed alarm.
             * This works on every alarm since the user most likely intends to
             * have the modified alarm fire next.
             * 如果此闹钟在贪睡闹钟之前触发，则禁用贪睡闹钟。这对每个警报都有效，
             * 因为用户最有可能打算让修改后的警报下次触发。
             */
            clearSnoozeIfNeeded(context, timeInMillis);
        }

        setNextAlert(context);

        return timeInMillis;
    }

    /**
     * A convenience method to enable or disable an alarm.
     * 禁用或启用闹钟
     *
     * @param id      corresponds to the _id column
     * @param enabled corresponds to the ENABLED column
     */
    public static void enableAlarm(final Context context, final int id, boolean enabled) {
        enableAlarmInternal(context, id, enabled);
//        setNextAlert(context);
    }

    private static void enableAlarmInternal(final Context context,
                                            final int id, boolean enabled) {
        enableAlarmInternal(context, getAlarm(context.getContentResolver(), id),
                enabled);
    }

    private static void enableAlarmInternal(final Context context,
                                            final Alarm alarm, boolean enabled) {
        if (alarm == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);

        /**
         * If we are enabling the alarm, calculate alarm time since the time
         * value in Alarm may be old.
         * 如果我们启用闹钟，计算闹钟时间，因为闹钟的时间值可能是旧的。
         */
        if (enabled) {
            long time = 0;
            if (alarm.daysOfWeek.isRepeatSet()) {
                time = calculateAlarm(alarm);
            }
            values.put(Alarm.Columns.ALARM_TIME, time);
        } else {
            // Clear the snooze if the id matches.
            // 清除ID相同的睡眠闹钟
            disableSnoozeAlert(context, alarm.id);
        }

        resolver.update(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id),
                values, null, null);
    }

    public static Alarm calculateNextAlert(final Context context) {
        List<Alarm> alarms = new ArrayList<>();
        Alarm alarm = null;
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        Cursor cursor = getFilteredAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Alarm a = new Alarm(cursor);
                    LogUtil.d(a.toString(context));
                    /**
                     * A time of 0 indicates this is a repeating alarm, so
                     * calculate the time to get the next alert.
                     * 时间为O表示这是一个重复的警报，因此计算获得下一个警报的时间。
                     */
                    if (a.time == 0 && a.enabled) {
                        a.time = calculateAlarm(a);
                    }
                    if (a.time < now) {
                        LogUtil.v("Disabling expired alarm set for ");
                        // Expired alarm, disable it and move along.
                        // 警报器过期了，解除警报，然后离开。
                        enableAlarmInternal(context, a, false);
//                        continue;
                    }
                    if (a.time < minTime) {
                        alarms.clear();
                        minTime = a.time;
                        saveAtTheSameTimeAlarm(context, a.id, minTime);
                        alarm = a;
                        alarms.add(a);
                    } else if (a.time == minTime) {
                        saveAtTheSameTimeAlarm(context, a.id, minTime);
                        alarms.add(a);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return alarm;
    }

    /**
     * Disables non-repeating alarms that have passed.  Called at
     * boot.
     * 禁用已通过的不重复告警。在引导。
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getFilteredAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        if (cur.moveToFirst()) {
            do {
                Alarm alarm = new Alarm(cur);
                /**
                 * A time of 0 means this alarm repeats. If the time is
                 * non-zero, check if the time is before now.
                 * O的时间表示重复这个警报。如果时间非零，检查时间是否在现在之前。
                 */
                if (alarm.time != 0 && alarm.time < now) {
                    enableAlarmInternal(context, alarm, false);
                }
            } while (cur.moveToNext());
        }
        cur.close();
    }

    /**
     * Called at system startup, on time/timezone change, and whenever
     * the user changes alarm settings.  Activates snooze if set,
     * otherwise loads all alarms, activates next alert.
     * 在系统启动时调用，在时间/时区更改时调用，以及当用户更改告警设置时调用。
     * 如果设置了则激活小睡，否则加载所有闹钟，激活下一次警报。
     */
    public static void setNextAlert(final Context context) {
//        if (!enableSnoozeAlert(context)) {
            Alarm alarm = calculateNextAlert(context);
            if (alarm != null) {
                LogUtil.d("setNextAlert alarm not null");
                enableAlert(context, alarm, alarm.time);
            } else {
                LogUtil.d("setNextAlert alarm null");
                disableAlert(context);
            }
//        }
    }

    /**
     * Sets alert in AlarmManger and StatusBar.  This is what will
     * actually launch the alert when the alarm triggers.
     * 在“告警管理器”和“状态栏”中设置告警。当警报触发时，这将实际启动警报。
     *
     * @param alarm          Alarm.
     * @param atTimeInMillis milliseconds since epoch
     */
    private static void enableAlert(Context context, final Alarm alarm,
                                    final long atTimeInMillis) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        LogUtil.v("** enableAlert id " + alarm.id + " atTime " + atTimeInMillis);

        Intent intent = new Intent();
        intent.setAction(ALARM_ALERT_ACTION);
        intent.setPackage(PACKAGE_NAME);

        // XXX: This is a slight hack to avoid an exception in the remote
        // AlarmManagerService process. The AlarmManager adds extra data to
        // this Intent which causes it to inflate. Since the remote process
        // does not know about the Alarm class, it throws a
        // ClassNotFoundException.
        //
        // To avoid this, we marshall the data ourselves and then parcel a plain
        // byte[] array. The AlarmReceiver class knows to build the Alarm
        // object from the byte[] array.
        // XXX:这是为了避免远程AlarmManagerService进程中出现异常而进行的一种轻微修改。
        // AlarmManager向这个Intent添加额外的数据，导致它膨胀。
        // 由于远程进程不知道Alarm类，它将抛出ClassNotFoundException。
        // 为了避免这种情况，我们自己打包数据，然后打包一个普通的byte[]数组。
        // AlarmReceiver类知道从byte[]数组构建Alarm对象
        Parcel out = Parcel.obtain();
        alarm.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(ALARM_RAW_DATA, out.marshall());

        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

//        am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        //不同Android 版本的设置闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(atTimeInMillis, sender);
            am.setAlarmClock(alarmClockInfo, sender);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        }

//        AlarmManager.AlarmClockInfo nextAlarmClock = am.getNextAlarmClock();
        setStatusBarIcon(context, true);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(atTimeInMillis);
        String timeString = formatDayAndTime(context, c);
        saveNextAlarm(context, timeString);
    }

    /**
     * Disables alert in AlarmManger and StatusBar.
     * 禁用告警管理器和状态栏中的告警。
     *
     * @param context
     */
    static void disableAlert(Context context) {
        Intent intent = new Intent();
        intent.setAction(ALARM_ALERT_ACTION);
        intent.setPackage(PACKAGE_NAME);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
        setStatusBarIcon(context, false);
        saveNextAlarm(context, "");
    }

    public static void saveSnoozeAlert(final Context context, final int id,
                                       final long time) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        if (id == -1) {
            clearSnoozePreference(context, prefs);
        } else {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putInt(PREF_SNOOZE_ID, id);
            ed.putLong(PREF_SNOOZE_TIME, time);
            ed.apply();
        }
        // Set the next alert after updating the snooze.
        // 在更新睡眠之后设置下一次提醒。
        setNextAlert(context);
    }

    /**
     * Disable the snooze alert if the given id matches the snooze id.
     * 如果给定的id与贪睡id匹配，则禁用贪睡警报。
     */
    public static void disableSnoozeAlert(final Context context, final int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        int snoozeId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (snoozeId == -1) {
            // No snooze set, do nothing.
            return;
        } else if (snoozeId == id) {
            // This is the same id so clear the shared prefs.
            // 这是相同的id，共享的prefs。
            clearSnoozePreference(context, prefs);
        }
    }


    /**
     * Helper to remove the snooze preference. Do not use clear because that
     * will erase the clock preferences. Also clear the snooze notification in
     * the window shade.
     * 助手来移除睡眠的preferences。不要使用clear，因为那会擦除时钟的preferences。也清除视窗上的睡眠通知。
     *
     * @param context
     * @param prefs
     */
    private static void clearSnoozePreference(final Context context, final SharedPreferences prefs) {
        final int alarmId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (alarmId != -1) {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(alarmId);
        }

        final SharedPreferences.Editor ed = prefs.edit();
        ed.remove(PREF_SNOOZE_ID);
        ed.remove(PREF_SNOOZE_TIME);
        ed.apply();
    }

    ;

    /**
     * If there is a snooze set, enable it in AlarmManager
     * 如果设置了贪睡功能，请在AlarmManager中启用
     *
     * @return true if snooze is set
     */
    private static boolean enableSnoozeAlert(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);

        int id = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (id == -1) {
            return false;
        }
        long time = prefs.getLong(PREF_SNOOZE_TIME, -1);

        // Get the alarm from the db.
        final Alarm alarm = getAlarm(context.getContentResolver(), id);
        if (alarm == null) {
            return false;
        }
        // The time in the database is either 0 (repeating) or a specific time
        // for a non-repeating alarm. Update this value so the AlarmReceiver
        // has the right time to compare.
        //数据库中的时间可以是O(重复)，也可以是不重复告警的具体时间。
        // 更新此值，以便AlarmReceiver有正确的时间进行比较。
        alarm.time = time;

        enableAlert(context, alarm, time);
        return true;
    }

    private static void saveAtTheSameTimeAlarm(final Context context, int alarmId, long minTime) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = prefs.edit();
        String idStr = prefs.getString(PREF_AT_THE_SAME_TIME, "");
        long prefMinTime = prefs.getLong(PREF_MIN_TIME, 0L);
        if (prefMinTime == 0) {
            editor.putString(PREF_AT_THE_SAME_TIME, String.valueOf(alarmId));
            editor.putLong(PREF_MIN_TIME, minTime);
            editor.apply();
            editor.commit();
        } else if (prefMinTime == minTime) {
            if (TextUtils.isEmpty(idStr)) {
                editor.putString(PREF_AT_THE_SAME_TIME, String.valueOf(alarmId));
            } else {
                String[] split = idStr.split(",");
                for (String s : split) {
                    if (s.equals(String.valueOf(alarmId))) {
                        return;
                    }
                }
                idStr = idStr + "," + String.valueOf(alarmId);
                editor.putString(PREF_AT_THE_SAME_TIME, idStr);
            }
            editor.apply();
            editor.commit();
        } else {
            editor.putString(PREF_AT_THE_SAME_TIME, String.valueOf(alarmId));
            editor.putLong(PREF_MIN_TIME, minTime);
            editor.apply();
            editor.commit();
        }
    }

    /**
     * Tells the StatusBar whether the alarm is enabled or disabled
     * 告诉StatusBar闹钟图标是否开启或关闭
     */
    private static void setStatusBarIcon(Context context, boolean enabled) {
        Intent alarmChanged = new Intent();
        alarmChanged.setAction("android.intent.action.ALARM_CHANGED");
//        alarmChanged.setAction(Intent.ACTION_ALARM_CHANGED);
        alarmChanged.putExtra("alarmSet", enabled);
        alarmChanged.setPackage(PACKAGE_NAME);
        context.sendBroadcast(alarmChanged);
    }

    /**
     * 计算闹钟响铃时间
     *
     * @param alarm
     * @return
     */
    public static long calculateAlarm(Alarm alarm) {
        return calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek)
                .getTimeInMillis();
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for
     * setting in AlarmManager.
     * 给定一个以小时和分钟为单位的闹钟，返回一个适合在AlarmManager中设置的时间。
     */
    static Calendar calculateAlarm(int hour, int minute, DaysOfWeek daysOfWeek) {

        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        // 如果报警时间晚于当前时间，则延后一天
        if (hour < nowHour ||
                hour == nowHour && minute <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.getNextAlarm(c);
        if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
        return c;
    }

    static String formatTime(final Context context, int hour, int minute, DaysOfWeek daysOfWeek) {
        Calendar c = calculateAlarm(hour, minute, daysOfWeek);
        return formatTime(context, c);
    }

    /* used by AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String) DateFormat.format(format, c);
    }

    /**
     * Shows day and time -- used for lock screen
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String) DateFormat.format(format, c);
    }

    /**
     * Save time of the next alarm, as a formatted string, into the system
     * settings so those who care can make use of it.
     * 保存下一次警报的时间，作为一个格式化的字符串，进入系统设置，以便那些关心的人可以利用它。
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED,
                timeString);
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    static boolean get24HourMode(final Context context) {
        return DateFormat.is24HourFormat(context);
    }
}
