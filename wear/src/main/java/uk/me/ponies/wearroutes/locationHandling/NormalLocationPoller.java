package uk.me.ponies.wearroutes.locationHandling;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.controller.Stats;
import uk.me.ponies.wearroutes.historylogger.LogEvent;

/**
 * A NormalLocationPoller requests a location at the "frequent" rate , no timers or alarms are used
 */
public class NormalLocationPoller implements LocationListener{


    private static final String TAG = "NormalLocationPoller";
    private final LocationRequest whenOnLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the update interval to 2 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the fastest update interval to 2 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed;




    private final Runnable mStartLocationUpdatePerSecond;



    private LocationHandler masterHandler;
    private long mStatsPollingStartAtRealTimeMillis;
    private boolean mActive = false;


    NormalLocationPoller(LocationHandler master) {
        masterHandler = master;
        mStartLocationUpdatePerSecond = new Runnable() {
            @Override
            public void run() {
                PendingResult<Status> pr = masterHandler.requestLocationUpdates(whenOnLocationRequest,NormalLocationPoller.this);
                if (pr != null) {
                    mActive = true;
                }
                EventBus.getDefault().post(
                        new LogEvent(
                                "NormalLocation Poller now requestingLocationUpdates at " + whenOnLocationRequest.getInterval() + "ms intervals"
                                , "GPS"));
            }
        };
        }

    /** schedule next for normal, simply runs.. */
    void scheduleNext() {
        commencePollingAsync();
    }

    /* starts the Location Request, and acquires a wakelock */
    private void commencePollingAsync() {
        mStatsPollingStartAtRealTimeMillis = SystemClock.elapsedRealtime();
        mStartLocationUpdatePerSecond.run();

    }
    /**
         * removes any requests for location updates and cancels the pending timer.
         */
        public void stop() {
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "NormalLocationPoller stopped");
            removeListener();
        }

    public void destroy() {
        stop();
        masterHandler = null;
    }

        private void removeListener() {
            if (masterHandler.mConnected) {
                masterHandler.removeLocationUpdates(this);
            }
            mActive = false;
        }


        @Override
        public void onLocationChanged(@NonNull Location location) {

            EventBus.getDefault().post(
                    new LogEvent(
                            "Location received:" + location
                                    + "state is " + "NormalPoller"
                            , "GPS"));

            // check we have a master handler, if not we've been destroyed
            if (masterHandler == null) {
                return;
            }
            if (!masterHandler.isAcceptableLocation(location, "normalPollingLocation")) {
                return;
            }

            // got a good location
            // send the data out
            // and schedule in a new request

            if (mStatsPollingStartAtRealTimeMillis != 0) {
                long TTF = SystemClock.elapsedRealtime() - mStatsPollingStartAtRealTimeMillis;
                EventBus.getDefault().post(new LogEvent(String.format("TTF NormalPoller:%.1f",TTF/1000.0f),"GPS"));
                Stats.addLastForegroundTTFMs(0); // TODO: calculate properly
            }
            masterHandler.goodLocationReceived(location, getClass().getSimpleName());
        }

    public Context getContext() {
        return masterHandler.mApplicationContext;
    }

    boolean isActive() {
        return mActive;
    }
}


