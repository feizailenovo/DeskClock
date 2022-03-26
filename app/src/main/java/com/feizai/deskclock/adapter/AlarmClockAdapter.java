package com.feizai.deskclock.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.feizai.deskclock.R;
import com.feizai.deskclock.constant.AlarmClockConst;
import com.feizai.deskclock.data.Alarm;
import com.feizai.deskclock.util.SettingUtil;
import com.feizai.deskclock.util.ToolUtil;

import java.util.List;

/**
 * Author: chenhao
 * Date: 2022/2/11-0011 上午 11:50:24
 * Describe:
 */
public class AlarmClockAdapter extends RecyclerView.Adapter<AlarmClockAdapter.AlarmClockViewHolder> {

    private Context mContext;
    private List<Alarm> mAlarmList;
    private onItemClickListener mItemClickListener;
    private onItemChildClickListener mItemChildClickListener;

    public AlarmClockAdapter(Context context, List<Alarm> alarms) {
        this.mContext = context;
        this.mAlarmList = alarms;
    }

    @NonNull
    @Override
    public AlarmClockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_alarm_clock_item, parent, false);
        AlarmClockViewHolder alarmClockViewHolder = new AlarmClockViewHolder(view);
        return alarmClockViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmClockViewHolder holder, int position) {
        Alarm alarm = mAlarmList.get(position);
        holder.mAlarmTime.setText(ToolUtil.getTimeShortSimpleFormat(SettingUtil.getTime_12_24(mContext)).format(ToolUtil.stringToDate(alarm.hour+":"+alarm.minutes, AlarmClockConst.TWENTY_FOUR_HOUR_SHORT_FORMAT)));
        holder.mAlarmRemark.setText(alarm.label);
        holder.mAlarmTimes.setText(alarm.daysOfWeek.toString(mContext,R.array.week));
        holder.mAlarmSwitch.setChecked(alarm.enabled);
        holder.view.setOnClickListener(v -> {
            mItemClickListener.onItemClick(v,position);
        });
        holder.mAlarmSwitchLayout.setOnClickListener(v->{
            mItemChildClickListener.onItemChildClick(v,position);
        });
    }

    @Override
    public int getItemCount() {
        return mAlarmList.size();
    }

    protected class AlarmClockViewHolder extends RecyclerView.ViewHolder {

        private TextView mAlarmTime;
        private TextView mAlarmRemark;
        private TextView mAlarmTimes;
        private Switch mAlarmSwitch;
        private LinearLayout mAlarmItemLayout;
        private LinearLayout mAlarmSwitchLayout;
        private View view;

        public AlarmClockViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            mAlarmTime = itemView.findViewById(R.id.alarm_time);
            mAlarmRemark = itemView.findViewById(R.id.alarm_label);
            mAlarmTimes = itemView.findViewById(R.id.alarm_times);
            mAlarmSwitch = itemView.findViewById(R.id.alarm_switch);
            mAlarmItemLayout = itemView.findViewById(R.id.alarm_item_layout);
            mAlarmSwitchLayout = itemView.findViewById(R.id.alarm_switch_layout);
        }
    }

    public void setData(@NonNull List<Alarm> alarms) {
        this.mAlarmList = alarms;
        notifyDataSetChanged();
    }

    public void clear() {
        this.mAlarmList.clear();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(@NonNull onItemClickListener itemClickListener){
        mItemClickListener = itemClickListener;
    }

    public void setOnItemChildClickListener(@NonNull onItemChildClickListener itemChildClickListener) {
        mItemChildClickListener = itemChildClickListener;
    }

    public interface onItemClickListener{
        void onItemClick(View view, int position);
    }

    public interface onItemChildClickListener{
        void onItemChildClick(View view, int position);
    }
}
