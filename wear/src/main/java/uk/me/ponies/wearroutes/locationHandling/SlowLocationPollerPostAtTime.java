package uk.me.ponies.wearroutes.locationHandling;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.eventBusEvents.LogEvent;

/**
 * Created by rummy on 25/08/2016.
 */
public class SlowLocationPollerPostAtTime extends SlowLocationPollerBase {
        private final static String TAG = "PostAtTimeSlowLocPoller";
        private final Handler mTickerHandler;

        public SlowLocationPollerPostAtTime(LocationHandler master, Context context) {
            super(master, context);
            mTickerHandler = new Handler();
        }

        @Override
        public void scheduleNext() {
            // schedule a new Request at the appropriate time based on when we want an update and how long it takes to get an accurate fix
            // we want to run *just* after the specified whatnot
            // e.g. 30,000 millis would run @01:00 and 01:30
            long sleepMillis = super.sleepTimeMs();

            long nextInUptime = SystemClock.uptimeMillis() + sleepMillis;
            //NOTE: rather annoyingly it seems that the watch can enter deep sleep
            //NOTE: and once that happens the scheduled service doesn't run for quite a lot longer
            //NOTE: than expected. Also there I have a suspicion that the watch can go into deep sleep
            //NOTE: even while it is waiting for a good location.


            mTickerHandler.postAtTime(super.mStartLocationUpdatePerSecond, nextInUptime);
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "SlowLocationPollerBase scheduled");
            EventBus.getDefault().post(
                    new LogEvent(
                            "SlowLocation Poller Scheduled for " + sleepMillis + "ms time."
                            , "GPS"));

        }

        @Override
        public void stop() {
            if (mTickerHandler != null) {
                mTickerHandler.removeCallbacks(super.mStartLocationUpdatePerSecond);
            }
            super.stop();
        }

    }
