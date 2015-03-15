package android.weiweiwang.github.toolkit;

import android.util.Log;

/**
 * 对android的log的简单封装，通过设置LEVEL可以控制日志的输出
 * @see Logger#LEVEL
 * @author  wangweiwei
 */
public class Logger {
    /**
     * 日志级别，不小于这个级别的日志才会被输出
     */
    public static int LEVEL = Log.DEBUG;

    static public void v(String tag, String msg) {
        if (LEVEL <= Log.VERBOSE) {
            Log.v(tag, msg);
        }
    }

    static public void v(String tag, String msg, Throwable t) {
        if (LEVEL <= Log.VERBOSE) {
            Log.v(tag, msg, t);
        }
    }

    static public void d(String tag, String msg) {
        if (LEVEL <= Log.DEBUG) {
            Log.d(tag, msg);
        }
    }

    static public void d(String tag, String msg, Throwable t) {
        if (LEVEL <= Log.DEBUG) {
            Log.d(tag, msg, t);
        }
    }
    static public void i(String tag, String msg) {
        if (LEVEL <= Log.INFO) {
            Log.i(tag, msg);
        }
    }

    static public void i(String tag, String msg, Throwable t) {
        if (LEVEL <= Log.INFO) {
            Log.i(tag, msg, t);
        }
    }


    static public void w(String tag, String msg) {
        if (LEVEL <= Log.WARN) {
            Log.w(tag, msg);
        }
    }

    static public void w(String tag, String msg, Throwable t) {
        if (LEVEL <= Log.WARN) {
            Log.w(tag, msg, t);
        }
    }
}