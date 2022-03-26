package com.feizai.deskclock.util;

import android.util.Log;

/**
 * Author: chenhao
 * Date: 2021/12/13-0013 上午 10:47:49
 * Describe:
 */
public class LogUtil {

    private static final String TAG_VERBOSE         = "<CHENHAO-FRAME> VERBOSE";
    private static final String TAG_DEBUG           = "<CHENHAO-FRAME> DEBUG";
    private static final String TAG_INFO            = "<CHENHAO-FRAME> INFO";
    private static final String TAG_WARN            = "<CHENHAO-FRAME> WARN";
    private static final String TAG_ERROR           = "<CHENHAO-FRAME> ERROR";
    private static final String TAG_METHOD_IN       = "<CHENHAO-FRAME> IN ";
    private static final String TAG_METHOD_OUT      = "<CHENHAO-FRAME> OUT";
    private static final String TAG_LINE           	= "——————————————————————————";

    //是否打印Verbose日志
    private static boolean isShowVerboseLog = true;

    //是否打印Debug日志
    private static boolean isShowDebugLog = true;

    //是否打印Info日志
    private static boolean isShowInfoLog = true;

    //是否打印Warn日志
    private static boolean isShowWarnLog = true;

    //是否打印error日志
    private static boolean isShowErrorLog = true;

    /**
     * 打印Verbose日志
     * @param content 内容
     */
    public static void v(String content) {
        v(TAG_VERBOSE, content);
    }

    /**
     * 打印Verbose日志
     * @param content 内容
     * @param tag 自定义tag
     */
    public static void v(String tag, String content) {
        if(isShowVerboseLog) log(false, false, false, tag, content);
    }

    /**
     * 打印Debug日志
     * @param content 内容
     */
    public static void d(String content) {
        d(TAG_DEBUG, content);
    }

    /**
     * 打印Debug日志
     * @param content 内容
     * @param tag 自定义tag
     */
    public static void d(String tag, String content) {
        if(isShowDebugLog) log(false, false, false, tag, content);
    }

    /**
     * 打印Info日志
     * @param content 内容
     */
    public static void i(String content) {
        i(TAG_INFO, content);
    }

    /**
     * 打印Info日志
     * @param content 内容
     * @param tag 自定义tag
     */
    public static void i(String tag, String content) {
        if(isShowInfoLog) log(false, false, false, tag, content);
    }

    /**
     * 打印Warn日志
     * @param content 内容
     */
    public static void w(String content) {
        w(TAG_WARN, content);
    }

    /**
     * 打印Warn日志
     * @param content 内容
     * @param tag 自定义tag
     */
    public static void w(String tag, String content) {
        if(isShowWarnLog) log(false, false, false, tag, content);
    }

    /**
     * 打印Error日志
     * @param content 内容
     */
    public static void e(String content) {
        e(TAG_ERROR, content);
    }

    /**
     * 打印Error日志
     * @param content 内容
     * @param tag 自定义tag
     */
    public static void e(String tag, String content) {
        if(isShowErrorLog) log(false, false, false, tag, content);
    }

    /**
     * 打印方法底部输入日志，用于Debug
     */
    public static void in() {
        logMethod(TAG_METHOD_IN, null);
    }

    /**
     * 打印方法底部输入日志，用于Debug
     * @param content 内容
     */
    public static void in(String content) {
        logMethod(TAG_METHOD_IN, content);
    }

    /**
     * 打印方法底部输出日志，用于Debug
     */
    public static void out() {
        logMethod(TAG_METHOD_OUT, null);
    }

    /**
     * 打印方法底部输出日志，用于Debug
     * @param content 内容
     */
    public static void out(String content) {
        logMethod(TAG_METHOD_OUT, content);
    }

    /**
     * 打印方法
     * @param methodTag 标签
     * @param content 内容
     */
    private static void logMethod(String methodTag, String content) {
        try {
            StringBuilder strBuilder = new StringBuilder();
            StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
            String currentClassName = traceElements[4].getClassName();
            String currentMethodName = traceElements[4].getMethodName();
            int lastIndex = currentClassName.lastIndexOf(".") + 1;
            currentClassName = currentClassName.substring(lastIndex);
            strBuilder.append("method: ")
                    .append(currentClassName)
                    .append(".")
                    .append(currentMethodName)
                    .append("()")
                    .append("\nthread: ")
                    .append(Thread.currentThread().getName());
            if(content != null) {
                strBuilder.append("\n\n")
                        .append(content);
            }
            method(methodTag, methodTag.contains("IN") ? "METHOD IN" : "METHOD OUT", strBuilder.toString());
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印方法
     * @param showClassName 是否显示类名
     * @param showMethodName 是否显示方法名
     * @param showThreadName 是否显示线程名称
     * @param logTag 日志TAG
     * @param content 内容
     */
    private static void log(
            boolean showClassName,
            boolean showMethodName,
            boolean showThreadName,
            String logTag,
            String content) {
        String currentClassName = null;
        String currentMethodName = null;
        StringBuilder strBuilder = new StringBuilder();

        try {
            StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
            currentClassName = traceElements[5].getClassName();
            currentMethodName = traceElements[5].getMethodName();
            int lastIndex = currentClassName.lastIndexOf(".") + 1;
            currentClassName = currentClassName.substring(lastIndex);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        if(showClassName) {
            strBuilder.append(currentClassName);
        }

        if(showMethodName) {
            if(showClassName) {
                strBuilder.append(".");
            }
            strBuilder.append(currentMethodName)
                    .append("()");
        }

        if(showThreadName) {
            strBuilder.append(" >> Thread name: ")
                    .append(Thread.currentThread().getName());
            if(content != null) {
                strBuilder.append(" >> Content: ");
            }
        }

        if(content != null) {
            strBuilder.append(content);
        }

        switch (logTag) {
            case TAG_VERBOSE:
                Log.v(logTag, strBuilder.toString());
                break;
            case TAG_INFO:
                Log.i(logTag, strBuilder.toString());
                break;
            case TAG_WARN:
                Log.w(logTag, strBuilder.toString());
                break;
            case TAG_ERROR:
                Log.e(logTag, strBuilder.toString());
                break;
            default:
                Log.d(logTag, strBuilder.toString());
                break;
        }
    }

    /**
     * 打印方法in/out格式
     * @param title 打印标题
     * @param content 内容
     */
    public static void method(String tag, String title, String content) {
        StringBuilder sbContent = new StringBuilder();
        StringBuilder sbEndLine = new StringBuilder();
        StringBuilder sbBody = new StringBuilder();

        //拼接尾部线
        int twoLineLength = (TAG_LINE + TAG_LINE).length();
        int endLineLength = (TAG_LINE + title + TAG_LINE).length();
        int needAddLength = endLineLength - twoLineLength;
        sbEndLine.append(TAG_LINE)
                .append(TAG_LINE);
        if(needAddLength > 0) {
            for (int i = 0; i < needAddLength; i++) {
                sbEndLine.append("—");
            }
        }

        //判断内容是否有换行操作
        if(content.contains("\n")) {
            String[] strs = content.split("");
            for (String str : strs) {
                if(str.equals("\n")) {
                    sbContent.append(str).append("|    ");
                    continue;
                }
                sbContent.append(str);
            }
        }

        sbBody.append(TAG_LINE)
                .append(title)
                .append(TAG_LINE)
                .append("\n|")
                .append("\n|    ")
                .append(sbContent.toString())
                .append("\n|\n")
                .append(sbEndLine);

        Log.d(tag, sbBody.toString());
    }

    /**
     * 打印dump格式
     * @param title 打印标题
     * @param content 内容
     */
    public static void dump(String title, String content) {
        StringBuilder sbContent = new StringBuilder();
        StringBuilder sbEndLine = new StringBuilder();
        StringBuilder sbBody = new StringBuilder();

        //拼接尾部线
        int twoLineLength = (TAG_LINE + TAG_LINE).length();
        int endLineLength = (TAG_LINE + title + TAG_LINE).length();
        int needAddLength = endLineLength - twoLineLength;
        sbEndLine.append(TAG_LINE)
                .append(TAG_LINE);
        if(needAddLength > 0) {
            for (int i = 0; i < needAddLength; i++) {
                sbEndLine.append("—");
            }
        }

        //判断内容是否有换行操作
        if(content.contains("\n")) {
            String[] strs = content.split("");
            for (String str : strs) {
                if(str.equals("\n")) {
                    sbContent.append(str).append("|    ");
                    continue;
                }
                sbContent.append(str);
            }
        } else {
            sbContent.append(content);
        }

        sbBody.append("\n")
                .append(TAG_LINE)
                .append(title)
                .append(TAG_LINE)
                .append("\n|")
                .append("\n|    ")
                .append(sbContent.toString())
                .append("\n|\n")
                .append(sbEndLine);

        d(sbBody.toString());
    }
}
