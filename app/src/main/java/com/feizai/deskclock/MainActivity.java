package com.feizai.deskclock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.feizai.deskclock.activity.AlarmClockEditActivity;
import com.feizai.deskclock.adapter.AlarmClockAdapter;
import com.feizai.deskclock.base.BaseActivity;
import com.feizai.deskclock.data.Alarm;
import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.ToastUtil;
import com.feizai.deskclock.util.ToolUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private RecyclerView mRecyclerView;
    private LinearLayout mAddAlarmClock;
    private ToastUtil toastUtil;

    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void findView() {
        mRecyclerView = findViewById(R.id.alarmclock_list);
        mAddAlarmClock = findViewById(R.id.add_alarm_clock);
    }

    @Override
    protected void initData() {
        toastUtil = ToastUtil.getInstance(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<Alarm> alarmList = getAlarmList(Alarms.getAlarmsCursor(getContentResolver()));
//        List<AlarmClockBean> alarmClockBeans = getAlarmClocks();
//        alarmClockBeans.add(new AlarmClockBean("12:00", "", (byte) 0x10, false));
//        alarmClockBeans.add(new AlarmClockBean("13:00", "", (byte) 0x10, true));
        AlarmClockAdapter alarmClockAdapter = new AlarmClockAdapter(this, alarmList);
        alarmClockAdapter.setOnItemClickListener((v, position) -> {
            Intent intent = new Intent();
            intent.putExtra(Alarms.ALARM_ID, alarmList.get(position).id);
            startActivity(intent, AlarmClockEditActivity.class);
        });
        alarmClockAdapter.setOnItemChildClickListener((v, position) -> {
            Switch alarm_switch = v.findViewById(R.id.alarm_switch);
            alarm_switch.setChecked(!alarm_switch.isChecked());
            Alarms.enableAlarm(MainActivity.this, alarmList.get(position).id, alarm_switch.isChecked());
            Alarms.setNextAlert(MainActivity.this);
            long l = Alarms.calculateAlarm(alarmList.get(position));
            if (alarm_switch.isChecked()) {
                showToast("将在" + ToolUtil.timestampToString(l - System.currentTimeMillis()) + "后响铃");
            }else {
                showToast("闹钟已关闭");
            }
//            Alarm alarm = Alarms.calculateNextAlert(getContext());
//            if (alarm != null) {
//                Log.d("chenhao", "next alarm==" + alarm.toString(getContext()));
//            }else {
//                Log.d("chenhao", "alarm  nextAlarm is null");
//            }
        });
        mRecyclerView.setAdapter(alarmClockAdapter);
        mAddAlarmClock.setOnClickListener(v -> {
            startActivity(AlarmClockEditActivity.class);
//            Intent intent = new Intent();
//            Calendar calendar = Calendar.getInstance();
//            AlarmClockBean newAlarmClockBean = new AlarmClockBean(calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),"", (byte) 0x00,false);
//            intent.putExtra(FragmentNameConst.FRAGMENT_NAME, FragmentNameConst.ALARM_CLOCK_EDIT_FRAGMENT);
//            intent.putExtra("data", newAlarmClockBean);
//            startActivity(intent, ContainerActivity.class);
        });
    }

    private List<Alarm> getAlarmList(Cursor cursor) {
        List<Alarm> list = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                final Alarm alarm = new Alarm(cursor);
                list.add(alarm);
            }
            cursor.close();
        }
        return list;
    }

    public void showToast(CharSequence content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }
}