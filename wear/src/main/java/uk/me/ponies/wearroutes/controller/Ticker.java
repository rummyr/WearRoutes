package uk.me.ponies.wearroutes.controller;

import android.os.Handler;
import android.os.SystemClock;

/**
 * calls the runnable passed in at construction time at periodic intervals
 * using post at time (android way)
 */
public class Ticker {
    private Handler mTickerHandler;
    private Runnable mTicker;
    private Runnable mOnTickRunnable;
    private long mUpdateIntervalMs = 1000; // by default run every second

    Ticker(Runnable onTickRunnable, long updateIntervalMs) {
        this.mOnTickRunnable = onTickRunnable;
        this.mUpdateIntervalMs = updateIntervalMs;
    }

    void startTicker() {
        if (mTickerHandler == null) {
            mTickerHandler =new Handler();
        }


        mTicker = new Runnable() {
            public void run() {
                if (mOnTickRunnable != null) {
                    mOnTickRunnable.run();
                }


                // we want to run *just* after the specified whatnot
                // e.g. 30,000 millis would run @01:00 and 01:30
                long nowEpoch = System.currentTimeMillis();
                long sleepMillis =  (mUpdateIntervalMs - nowEpoch % mUpdateIntervalMs);
                long nowUptime = SystemClock.uptimeMillis();
                long nextInUptime = nowUptime + sleepMillis;
                // long next = nowUptime + (mUpdateIntervalMs - nowUptime % mUpdateIntervalMs);
                mTickerHandler.postAtTime(mTicker, nextInUptime);
            }
        };

        // fires an event immediately , and then waits for the next appropriate interval
        mTicker.run();
    }
    void changeTickerFrequencyMs(long newFrequencyMs) {
        if (mTickerHandler != null) {
            mTickerHandler.removeCallbacks(mTicker);
        }
        mUpdateIntervalMs = newFrequencyMs;
        startTicker();
    }

    void stopTicker() {
        if (mTickerHandler != null) {
            mTickerHandler.removeCallbacks(mTicker);
        }
    }

}
