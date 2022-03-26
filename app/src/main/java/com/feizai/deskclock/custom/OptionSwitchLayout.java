package com.feizai.deskclock.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.feizai.deskclock.R;


/**
 * Author: chenhao
 * Date: 2022/2/11-0011 下午 07:06:18
 * Describe:
 */
public class OptionSwitchLayout extends LinearLayout {

    private TextView mOptionTitle;
    private Switch mOptionSwitch;

    public OptionSwitchLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_option_switch_item, this);
        findView();
        initAttes(context, attrs);
    }

    private void findView() {
        mOptionTitle = findViewById(R.id.option_title);
        mOptionSwitch = findViewById(R.id.option_switch);
    }

    private void initAttes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.OptionSwitchLayout);

        mOptionTitle.setText(ta.getString(R.styleable.OptionSwitchLayout_option_switch_title_text));
        mOptionTitle.getPaint().setTextSize(ta.getDimension(R.styleable.OptionSwitchLayout_option_switch_title_textSize, 12));
        mOptionTitle.setTextColor(ta.getColor(R.styleable.OptionSwitchLayout_option_switch_title_textColor, 0xFF000000));

        mOptionSwitch.setChecked(ta.getBoolean(R.styleable.OptionSwitchLayout_option_switch_value, false));

        ta.recycle();
    }

    public void setSwitchValue(Boolean value) {
        mOptionSwitch.setChecked(value);
    }

    public Boolean getSwitchValue() {
        return mOptionSwitch.isChecked();
    }

}
