package com.feizai.deskclock.data;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.format.DateFormat;

import com.feizai.deskclock.R;
import com.feizai.deskclock.util.LogUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Author: chenhao
 * Date: 2022/3/26-0026 上午 09:51:02
 * Describe: 闹钟的数据结构
 */
public final class Alarm implements Parcelable {

    //////////////////////////////
    //序列化的Parcelable接口
    /*
     * by chenhao
     * in 2020-03-26
     * by start
     */
    //////////////////////////////
    public static final Creator<Alarm> CREATOR = new Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(hour);
        dest.writeInt(minutes);
        dest.writeInt(daysOfWeek.getCoded());
        dest.writeLong(time);
        dest.writeInt(vibrate ? 1 : 0);
        dest.writeString(label);
        dest.writeParcelable(alert, flags);
        dest.writeInt(silent ? 1 : 0);
    }
    //////////////////////////////
    // by end
    //////////////////////////////

    //////////////////////////////
    // 定义列
    //////////////////////////////
    public static class Columns implements BaseColumns {
        /**
         * The content:// 为这个表定义一个共享的Url
         */
        public static final Uri CONTENT_URI = Uri.parse("content://com.feizai.deskclock/alarm");

        /**
         * Hour in 24-hour localtime 0 - 23.
         * 小时，采用24小时制，从 0 - 23。
         * <P>Type: INTEGER</P>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59
         * 分钟，从 0 - 59。
         * <P>Type: INTEGER</P>
         */
        public static final String MINUTES = "minutes";

        /**
         * Days of week coded as integer
         * 重复，描述闹钟的重复响铃
         * <P>Type: INTEGER</P>
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * Alarm time in UTC milliseconds from the epoch.
         * 响铃时间，描述闹钟响铃的时间，采用毫秒，UTC计时
         * <P>Type: INTEGER</P>
         */
        public static final String ALARM_TIME = "alarmtime";

        /**
         * True if alarm is active
         * 闹钟是否响铃
         * <P>Type: BOOLEAN</P>
         */
        public static final String ENABLED = "enabled";

        /**
         * True if alarm should vibrate
         * 闹钟是否震动
         * <P>Type: BOOLEAN</P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Message to show when alarm triggers
         * Note: not currently used
         * <P>Type: STRING</P>
         */
        public static final String MESSAGE = "message";

        /**
         * Audio alert to play when alarm triggers
         * 闹钟响铃铃声
         * <P>Type: STRING</P>
         */
        public static final String ALERT = "alert";

        /**
         * The default sort order for this table
         * 闹钟排序升序排序
         */
        public static final String DEFAULT_SORT_ORDER =
                HOUR + ", " + MINUTES + " ASC";

        /**
         * Used when filtering enabled alarms.
         */
        public static final String WHERE_ENABLED = ENABLED + "=1";

        /**
         * 闹钟的所有字段集合
         */
        static final String[] ALARM_QUERY_COLUMNS = {
                _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME,
                ENABLED, VIBRATE, MESSAGE, ALERT
        };

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         * 这些保存对游标的调用，它们必须与上面的查询列保持同步。
         */
        public static final int ALARM_ID_INDEX = 0;
        public static final int ALARM_HOUR_INDEX = 1;
        public static final int ALARM_MINUTES_INDEX = 2;
        public static final int ALARM_DAYS_OF_WEEK_INDEX = 3;
        public static final int ALARM_TIME_INDEX = 4;
        public static final int ALARM_ENABLED_INDEX = 5;
        public static final int ALARM_VIBRATE_INDEX = 6;
        public static final int ALARM_MESSAGE_INDEX = 7;
        public static final int ALARM_ALERT_INDEX = 8;
    }
    //////////////////////////////
    // End 每一列定义结束
    //////////////////////////////

    /**
     * 对应的公共的每一列的映射
     */
    public int id;
    public boolean enabled;
    public int hour;
    public int minutes;
    public DaysOfWeek daysOfWeek;
    public long time;
    public boolean vibrate;
    public String label;
    public Uri alert;
    public boolean silent;

    protected Alarm(Parcel parcel) {
        id = parcel.readInt();
        enabled = parcel.readInt() == 1;
        hour = parcel.readInt();
        minutes = parcel.readInt();
        daysOfWeek = new DaysOfWeek(parcel.readInt());
        time = parcel.readLong();
        vibrate = parcel.readInt() == 1;
        label = parcel.readString();
        alert = (Uri) parcel.readParcelable(null);
        silent = parcel.readInt() == 1;
    }

    public Alarm(Cursor cursor) {
        id = cursor.getInt(Columns.ALARM_ID_INDEX);
        enabled = cursor.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
        hour = cursor.getInt(Columns.ALARM_HOUR_INDEX);
        minutes = cursor.getInt(Columns.ALARM_MINUTES_INDEX);
        daysOfWeek = new DaysOfWeek(cursor.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
        time = cursor.getLong(Columns.ALARM_TIME_INDEX);
        vibrate = cursor.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
        label = cursor.getString(Columns.ALARM_MESSAGE_INDEX);
        String alertString = cursor.getString(Columns.ALARM_ALERT_INDEX);
        if (Alarms.ALARM_ALERT_SILENT.equals(alertString)) {
            if (true) {
                LogUtil.v("Alarm is marked as silent");
            }
            silent = true;
        } else {
            if (alertString != null && alertString.length() != 0) {
                alert = Uri.parse(alertString);
            }

            // If the database alert is null or it failed to parse, use the
            // default alert.
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
        }
    }

    /**
     * Creates a default alarm at the current time.
     * 默认创建一个当前时间的闹钟
     */
    public Alarm() {
        id = -1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minutes = calendar.get(Calendar.MINUTE);
        vibrate = true;
        daysOfWeek = new DaysOfWeek(0x00);
        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public String getLabelOrDefault() {
        if (label == null) {
            return "";
        }
        return label;
    }

    public String toString(Context context) {
        return "Alarm{" +
                "id=" + id +
                ", enabled=" + enabled +
                ", hour=" + hour +
                ", minutes=" + minutes +
                ", daysOfWeek=" + daysOfWeek.toString(context, R.array.week) +
                ", time=" + time +
                ", vibrate=" + vibrate +
                ", label='" + label + '\'' +
                ", alert=" + alert +
                ", silent=" + silent +
                '}';
    }

    public String getAlarmTime(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        if (DateFormat.is24HourFormat(context)) {
            return new SimpleDateFormat("HH:mm").format(calendar.getTime());
        } else {
            return new SimpleDateFormat("a hh:mm").format(calendar.getTime());
        }
    }
}
