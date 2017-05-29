package uk.me.ponies.wearroutes.locationHandling;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.Stats;
import uk.me.ponies.wearroutes.historylogger.LogEvent;

/**
 * A SlowLocationPollerBase requests a location at the "frequent" rate until a good one is received
 * then it cancels the request and starts another one at a time estimated to produce another good location
 * at about the right time.
 *
 * Really Tight COUPLING between this and the LocationHandler!!!
 */
public abstract class SlowLocationPollerBase implements LocationListener{
    //TODO: perhaps a better solution is to use a WakeFulBroadcastReceiver https://developer.android.com/reference/android/support/v4/content/WakefulBroadcastReceiver.html
    // TODO: or look into the CommonsWare Android Components: WakefulIntentService http://commonsware.com/cwac
    //TODO: though the cwac-locpoll is discontinued https://github.com/commonsguy/cwac-locpoll

    private static final String TAG = "SlowLocationPollerBase";
    private final static String WAKELOCK_NAME = "LocationPollerWakelock";

    private long lowFrequencyUpdateIntervalMs = TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS);
    private final long ACCEPTABLE_WAKEUP_INACCURACY = Options.ACCEPTABLE_WAKEUP_INACCURACY_MILLIS;
    private Runnable mWakelockTimeoutRunnable;
    private Handler mTimeoutHandler =new Handler();
    private long wakelockAquiredAt = 0;



    protected final Runnable mStartLocationUpdatePerSecond;
        /* a partial Wake lock, acquired just before the location listener is registered, released when the listener is removed.
         * has a timeout deliberately, though this is not tuned. */
        private PowerManager.WakeLock lockStatic;
        /**
         * movingAverageLag between desired time of fix and time a good fix was actually received.
         */
        long mMovingAverageLagNanos = 0;
        final PowerManager powerManager;

    /** for development purposes, to report early or late wake-ups */
    long expectedWakeupTime;

    protected final LocationRequest perSecondLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(1)) // Set the update interval to 1 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(1)) // Set the fastest update interval to 1 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed;
    LocationHandler masterHandler;
    private long mStatsPollingStartAtRealTimeMillis;


    public SlowLocationPollerBase(LocationHandler master, Context context) {
        masterHandler = master;
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mStartLocationUpdatePerSecond = new Runnable() {
            @Override
            public void run() {
                acquirePartialWakeLock();
                masterHandler.requestLocationUpdates(perSecondLocationRequest,SlowLocationPollerBase.this);
                EventBus.getDefault().post(
                        new LogEvent(
                                "SlowLocation Poller now requestingLocationUpdates at " + perSecondLocationRequest.getInterval() + "ms intervals"
                                , "GPS"));
            }
        };
        mWakelockTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(new LogEvent("Timed out waiting for a good location", "GPS"));
                Log.w(TAG, "Timed out waiting for a good location");
                Stats.addBackgroundFixTimeout();
                setupForNextSample();
            }
        };

        }

        public abstract void scheduleNext();

        private void acquirePartialWakeLock() {
            if (lockStatic == null) {
                // lockStatic doesn't exist yet, try to create it
                // have a context and no lockStatic, create one
                lockStatic = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_NAME);
                if (lockStatic != null) {
                    lockStatic.setReferenceCounted(false);
                }
            }

            if (lockStatic != null) { // redundant isNullAndLog, but what the hell!
                //TODO: proper handling of giving up if we can't acquire a good location!

                // set a timer to tell us if we have waited too long!
                // which is generally 2x the poll interval
                // we add some inaccuracy slop on and subtract a second to avoid conflicts with the wakelock timeout
                long tickerWakeupIn = TimeUnit.SECONDS.toMillis(Options.SlowPollingKeepAwakeForAtMostSecs + Options.ACCEPTABLE_WAKEUP_INACCURACY_SECS -1);
                mTimeoutHandler.postAtTime(mWakelockTimeoutRunnable,
                        SystemClock.uptimeMillis() + tickerWakeupIn);

                // acquire the wakelock, for safety reasons we let it expire after we expect the timeout handler to trigger (-1 above and +1 here)
                lockStatic.acquire(TimeUnit.SECONDS.toMillis(Options.SlowPollingKeepAwakeForAtMostSecs+ 1 + Options.ACCEPTABLE_WAKEUP_INACCURACY_SECS));
                wakelockAquiredAt = SystemClock.uptimeMillis();
            }
        }

        private void releasePartialWakeLock() {
            if (lockStatic != null) {
                lockStatic.release();
                long wakelockHeldFor = SystemClock.uptimeMillis() - wakelockAquiredAt;
                Stats.addWakeLockHeldTime(wakelockHeldFor);

            }

        }


        /**
         * removes any requests for location updates and cancels the pending timer.
         */
        public void stop() {
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "SlowLocationPollerBase stopped");
            removeListener();
            releasePartialWakeLock();
        }

    public void destroy() {
        mTimeoutHandler.removeCallbacks(mWakelockTimeoutRunnable);
        mWakelockTimeoutRunnable = null;
        mTimeoutHandler = null;
        stop();
        masterHandler = null;
    }

        private void removeListener() {
            if (masterHandler.mConnected) {
                masterHandler.removeLocationUpdates(this);
            }
        }

    public void setLowFrequencyUpdateIntervalSecs(int recordingIntervalSecs) {
        lowFrequencyUpdateIntervalMs = TimeUnit.SECONDS.toMillis(recordingIntervalSecs);
        Stats.updateBackgroundPollingInterval(recordingIntervalSecs);
    }

    /** calculates the desirable sleep time,
     * takes into account any permitted inaccuracies in case we wake up a little early.
     */
    public long sleepTimeMs() {
            long nowEpoch = System.currentTimeMillis();
            long nextWakeupAt = nowEpoch + lowFrequencyUpdateIntervalMs - nowEpoch % lowFrequencyUpdateIntervalMs;
            if ((nextWakeupAt - nowEpoch) < ACCEPTABLE_WAKEUP_INACCURACY) {
                nextWakeupAt += lowFrequencyUpdateIntervalMs;
            }
            long sleepMillis = nextWakeupAt - nowEpoch;
            expectedWakeupTime = nextWakeupAt;
            return sleepMillis;
        }



        @Override
        public void onLocationChanged(Location location) {

            String msg = "Location received:" + location
                    + "state is " + "slowPoller";
            Log.d(TAG, msg);
            EventBus.getDefault().post(new LogEvent(msg, "GPS"));

            if (location == null || !masterHandler.isAcceptableLocation(location, "slowPollingLocation")) {
                return;
            }

            // got a good location
            // send the data out
            // and schedule in a new request

            if (mStatsPollingStartAtRealTimeMillis != 0) {
                long TTF = SystemClock.elapsedRealtime() - mStatsPollingStartAtRealTimeMillis;
                EventBus.getDefault().post(new LogEvent(String.format("TTF SlowPoller:%.1f",TTF/1000.0f),"GPS"));
                if (Controller.getInstance() != null) {
                    Controller.getInstance().statsBackgroundTTFTimeMs(TTF);
                }

            }
            masterHandler.goodLocationReceived(location, getClass().getSimpleName());

            setupForNextSample();
        }

    public void setupForNextSample() {
        mTimeoutHandler.removeCallbacks(mWakelockTimeoutRunnable);
        removeListener();
        scheduleNext();
        EventBus.getDefault().post(
                new LogEvent(
                        "Scheduling a further update in:" +sleepTimeMs() + "ms"
                        , "GPS"));
        releasePartialWakeLock();

    }
    /* starts the Location Request, and acquires a wakelock */
    public void commencePollingAsync(String tag) {
        acquirePartialWakeLock();
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG,"Woke-up " + (expectedWakeupTime - System.currentTimeMillis()) + "early/late");

        mStatsPollingStartAtRealTimeMillis = SystemClock.elapsedRealtime();
        mStartLocationUpdatePerSecond.run();

    }

    public long getAcceptablePollTimeErrorMs() {
        return ACCEPTABLE_WAKEUP_INACCURACY;
    }

    public Context getContext() {
        return masterHandler.mApplicationContext;
    }
}


