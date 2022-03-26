package com.feizai.deskclock.activity;

import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TimePicker;

import androidx.annotation.RequiresApi;


import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.ModalDialog;
import com.afollestad.materialdialogs.input.DialogInputExtKt;
import com.afollestad.materialdialogs.list.DialogListExtKt;
import com.afollestad.materialdialogs.list.DialogMultiChoiceExtKt;
import com.feizai.deskclock.R;
import com.feizai.deskclock.base.BaseActivity;
import com.feizai.deskclock.custom.OptionLayout;
import com.feizai.deskclock.data.Alarm;
import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.AlarmClockUtil;
import com.feizai.deskclock.util.SettingUtil;
import com.feizai.deskclock.util.ToastUtil;
import com.feizai.deskclock.util.ToolUtil;

import java.util.Calendar;

/**
 * Author: chenhao
 * Date: 2022/2/15-0015 下午 06:31:29
 * Describe:
 */
public class AlarmClockEditActivity extends BaseActivity {

    private Button mDelete;
    private ImageView mCancelImg;
    private ImageView mSureImg;
    private TimePicker mTimePicker;
    private OptionLayout mRemark;
    private OptionLayout mTimes;
    private int mId;
    private Alarm mAlarm;
    private ToastUtil mToast;

    public AlarmClockEditActivity() {
        super(R.layout.activity_alarm_clock_edit);
    }

    @Override
    protected void findView() {
        mTimePicker = findViewById(R.id.timePicker);
        mCancelImg = findViewById(R.id.cancel_img);
        mSureImg = findViewById(R.id.sure_img);
        mRemark = findViewById(R.id.remark);
        mTimes = findViewById(R.id.times);
        mDelete = findViewById(R.id.delete_alarm);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void initData() {
        mToast = ToastUtil.getInstance(this);
        Intent intent = getIntent();
        mId = intent.getIntExtra(Alarms.ALARM_ID, -1);
        Log.d("chenhao", "In AlarmClockEditActivity, alarm id = " + mId);
        mAlarm = null;
        if (mId == -1) {
            mAlarm = new Alarm();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.SECOND, 0);
            mAlarm.hour = calendar.get(Calendar.HOUR_OF_DAY);
            mAlarm.minutes = calendar.get(Calendar.MINUTE);
//            mAlarm.time = calendar.getTimeInMillis();
            mAlarm.daysOfWeek.setOnlyOne();
            mAlarm.silent = false;
            mAlarm.vibrate = true;
            mAlarm.enabled = true;
            mAlarm.label = "";
            mAlarm.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            mDelete.setVisibility(View.VISIBLE);
            mAlarm = Alarms.getAlarm(getContentResolver(), mId);
            if (mAlarm == null) {
                finish();
                return;
            }
        }

        updateData(mAlarm);

        mTimePicker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);
        mTimePicker.setIs24HourView(!SettingUtil.getTime_12_24(this));
        mTimePicker.setHour(mAlarm.hour);
        mTimePicker.setMinute(mAlarm.minutes);
        mCancelImg.setOnClickListener(v -> {
            finish();
        });
        mSureImg.setOnClickListener(v -> {
            int hour = mTimePicker.getHour();
            int minute = mTimePicker.getMinute();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            mAlarm.hour = hour;
            mAlarm.minutes = minute;
            mAlarm.enabled = true;
            mAlarm.time = 0;
//            mAlarm.time = calendar.getTimeInMillis();
            Log.d("chenhao", "aa" + mAlarm.toString(this));
            long l = saveAlarm();
            mToast.showToast("将在" + ToolUtil.timestampToString(l - System.currentTimeMillis()) + "后响铃");
            finish();
        });
        mRemark.setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog(AlarmClockEditActivity.this, ModalDialog.INSTANCE);
            DialogInputExtKt.input(dialog, "添加备注", 0, "",
                    0, 0, 10,
                    true, true, (materialDialog, charSequence) -> {
                        Log.d("chenhaoedit", charSequence.toString());
                        mAlarm.label = charSequence.toString();
                        updateData(mAlarm);
                        return null;
                    });
            dialog.negativeButton(0, "取消", materialDialog -> null);
            dialog.title(0, "添加备注");
            dialog.show();
        });
        mTimes.setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog(AlarmClockEditActivity.this, ModalDialog.INSTANCE);
            int[] choose = AlarmClockUtil.getAlarmClockTimesArray(mAlarm.daysOfWeek.getBooleanArray());
            DialogListExtKt.listItems(dialog, R.array.type, null, new int[]{}, false,
                    (materialDialog, integer, charSequence) -> {
                        if (integer == 1) {
                            MaterialDialog listDialog = new MaterialDialog(AlarmClockEditActivity.this, ModalDialog.INSTANCE);
                            DialogMultiChoiceExtKt.listItemsMultiChoice(listDialog, R.array.week, null,
                                    new int[]{}, choose, true,
                                    false, (materialListDialog, ints, charSequences) -> {
                                        mAlarm.daysOfWeek.setOnlyOne();
                                        for (int anInt : ints) {
                                            mAlarm.daysOfWeek.set(anInt, true);
                                        }
                                        updateData(mAlarm);
                                        return null;
                                    });
                            listDialog.positiveButton(0, "确认", materialListDialog -> null);
                            listDialog.negativeButton(0, "取消", materialListDialog -> null);
                            listDialog.title(0, "响铃时间");
                            listDialog.show();
                        } else {
                            mAlarm.daysOfWeek.setOnlyOne();
                            updateData(mAlarm);
                        }
                        mAlarm.enabled = true;
                        return null;
                    });
            dialog.title(0, "响铃方式");
            dialog.show();
        });
        mDelete.setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog(AlarmClockEditActivity.this, MaterialDialog.getDEFAULT_BEHAVIOR());
            dialog.title(0, "删除闹钟");
            dialog.message(0, "确定删除此闹钟？", dialogMessageSettings -> null);
            dialog.positiveButton(0, "确定", materialDialog -> {
                Alarms.deleteAlarm(AlarmClockEditActivity.this, mId);
                mToast.showToast("已删除闹钟");
                finish();
                return null;
            });
            dialog.negativeButton(0, "取消", materialDialog -> null);
            dialog.show();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateData(Alarm alarm) {
        mId = alarm.id;
        if (!TextUtils.isEmpty(alarm.label)) {
            mRemark.setOptionResult(alarm.label);
        } else {
            mRemark.setOptionResult("");
            mRemark.setOptionResultHint("添加备注");
        }
        mTimes.setOptionResult(alarm.daysOfWeek.toString(this,R.array.week));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private long saveAlarm() {
        long time;
        if (mAlarm.id == -1) {
            time = Alarms.addAlarm(this, mAlarm);
            // addAlarm populates the alarm with the new id. Update mId so that
            // changes to other preferences update the new alarm.
            mId = mAlarm.id;
        } else {
            time = Alarms.setAlarm(this, mAlarm);
        }
        return time;
    }

}
