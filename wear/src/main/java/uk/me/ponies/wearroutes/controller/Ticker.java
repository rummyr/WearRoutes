package uk.me.ponies.wearroutes.controller;

import android.os.Handler;
import android.os.SystemClock;

import org.greenrobot.eventbus.EventBus;

import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;

/**
 * Created by rummy on 02/08/2016.
 */
public class Ticker {
    private Handler mTickerHandler;
    private Runnable mTicker;
    private Runnable mOnTickRunnable;
    private long mUpdateIntervalMs = 1000; // by default run every second

    public Ticker(Runnable onTickRunnable, long updateIntervalMs) {
        this.mOnTickRunnable = onTickRunnable;
        this.mUpdateIntervalMs = updateIntervalMs;
    }

    public void startTicker() {
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
                long next = nowUptime + (mUpdateIntervalMs - nowUptime % mUpdateIntervalMs);
                mTickerHandler.postAtTime(mTicker, nextInUptime);
            }
        };

        // fires an event immediately , and then waits for the next appropriate interval
        mTicker.run();
    }
    public void changeTickerFrequencyMs(long newFrequencyMs) {
        if (mTickerHandler != null) {
            mTickerHandler.removeCallbacks(mTicker);
        }
        mUpdateIntervalMs = newFrequencyMs;
        startTicker();
    }

    public void stopTicker() {
        if (mTickerHandler != null) {
            mTickerHandler.removeCallbacks(mTicker);
        }
    }

}
