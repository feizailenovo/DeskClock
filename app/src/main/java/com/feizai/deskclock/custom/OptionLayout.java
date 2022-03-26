package com.feizai.deskclock.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.feizai.deskclock.R;


/**
 * Author: chenhao
 * Date: 2022/2/11-0011 下午 07:06:18
 * Describe:
 */
public class OptionLayout extends LinearLayout {

    private TextView mOptionTitle;
    private TextView mOptionResult;
    private ImageView mOptionImg;

    public OptionLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_option_item, this);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_option_item, this);
        findView();
        initAttes(context, attrs);
    }

    private void findView() {
        mOptionTitle = findViewById(R.id.option_title);
        mOptionResult = findViewById(R.id.option_result);
        mOptionImg = findViewById(R.id.option_img);
    }

    private void initAttes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.OptionLayout);
        mOptionImg.setImageResource(ta.getResourceId(R.styleable.OptionLayout_option_img_src, 0));
        mOptionImg.setRotation(ta.getInt(R.styleable.OptionLayout_option_img_rotation, 0));

        mOptionTitle.setText(ta.getString(R.styleable.OptionLayout_option_title_text));
        mOptionTitle.getPaint().setTextSize(ta.getDimension(R.styleable.OptionLayout_option_title_textSize, 12));
        mOptionTitle.setTextColor(ta.getColor(R.styleable.OptionLayout_option_title_textColor, 0xFF000000));

        mOptionResult.setText(ta.getString(R.styleable.OptionLayout_option_result_text));
        mOptionResult.setHint(ta.getString(R.styleable.OptionLayout_option_result_hint));
        mOptionResult.getPaint().setTextSize(ta.getDimension(R.styleable.OptionLayout_option_result_textSize, 12));
        mOptionTitle.setTextColor(ta.getColor(R.styleable.OptionLayout_option_result_textColor, 0xFF000000));

        ta.recycle();
    }

    public void setOptionResult(CharSequence content) {
        mOptionResult.setText(content);
    }

    public void setOptionResultHint(CharSequence content) {
        mOptionResult.setHint(content);
    }

    public String getOptionResult() {
        return mOptionResult.getText().toString();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mOptionTitle.setEnabled(enabled);
        mOptionResult.setEnabled(enabled);
        mOptionImg.setEnabled(enabled);
    }
}
