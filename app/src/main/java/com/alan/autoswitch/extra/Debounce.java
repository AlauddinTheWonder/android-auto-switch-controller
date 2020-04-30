package com.alan.autoswitch.extra;

import android.os.Handler;

public class Debounce {

    private Handler mHandler = new Handler();
    private long mInterval;

    public Debounce(long interval) {
        mInterval = interval;
    }

    public void attempt(Runnable runnable) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(runnable, mInterval);
    }
}
