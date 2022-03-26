package com.feizai.deskclock;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Author: chenhao
 * Date: 2022年3月26日 0026 下午 01:07:07
 * Describe:
 */
public abstract class BaseActivity extends AppCompatActivity {
    private int mLayoutRes;

    public BaseActivity(@NonNull int layoutRes) {
        this.mLayoutRes = layoutRes;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutRes);
        findView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    protected abstract void findView();

    protected abstract void initData();

    protected void startActivity(@Nullable Class activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    protected void startActivity(@Nullable Intent intent, @Nullable Class activityClass) {
        intent.setClass(this, activityClass);
        startActivity(intent);
    }
}
