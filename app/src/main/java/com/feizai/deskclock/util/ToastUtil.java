package com.feizai.deskclock.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * Author: feizai
 * Date: 2021/12/1-0001 上午 11:11:11
 * Describe:
 */
public class ToastUtil {
    private static Context mContext;
    private Toast toast;

    private ToastUtil() {
        toast = new Toast(mContext);
    }

    public static ToastUtil getInstance(@NonNull Context context) {
        mContext = context;
        return ToastUtilHolder.sInstance;
    }

    private static class ToastUtilHolder {
        private final static ToastUtil sInstance = new ToastUtil();
    }

    public void showToast(CharSequence content) {
        toast.makeText(mContext, content, Toast.LENGTH_SHORT).show();
    }

    public void showToast(CharSequence content, Integer duration) {
        toast.makeText(mContext, content, duration).show();
    }

}
