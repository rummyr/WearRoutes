package uk.me.ponies.wearroutes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.common.LocationAugmentor;
import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LogEvent;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * A Class that handles "requesting" location updates from the FusedLocationApi,
 * augmenting with speed and distance and then broadcasting the augmented location.
 * When in "frequent" mode it uses the standard continuous updates approach to getting location
 * But when in less frequent mode (undefined, but 30s is definitely less frequent) it toggles between
 * continuous and &quot;off&quot; mode with possibly some heuristics to try to meet the desired frequency.
 */
public class PollingLocationHandler implements LocationListener {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);


    private final String TAG = getClass().getSimpleName();
    private final Context mContext;
    private final String mAppName; // used only for displaying an error dialog!
    private final LocationAugmentor mLocationAugmentor = new LocationAugmentor();
    private final GoogleApiClient mGoogleApiClient;
    private final SlowLocationPoller mSlowLocationPoller = new SlowLocationPoller();
    private long lowFrequencyUpdateIntervalMs = TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS);

    //BUG: accept lower accuracy when not recording
    private final LocationRequest whenOnLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the update interval to 2 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the fastest update interval to 2 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed;

    private final LocationRequest perSecondLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(1)) // Set the update interval to 1 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(1)) // Set the fastest update interval to 1 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed;

    private Location prevLocation;


    // api and callbacks
    @SuppressWarnings("FieldCanBeLocal")
    private final ConnectionCallBacksHandler mConnectionCallBacksHandler;
    private boolean mConnected = false;
    /** used to determine which poller to use when the API client is connected/disconnected/suspended. */
    private boolean isSlowPolling = false;


    public PollingLocationHandler(Activity activity) {
        this.mContext = activity.getApplicationContext();

        String appName;
        try {
            ActivityInfo activityInfo = activity.getPackageManager().getActivityInfo(
                    activity.getComponentName(), PackageManager.GET_META_DATA);
            appName = activityInfo.loadLabel(activity.getPackageManager())
                    .toString();
        } catch (PackageManager.NameNotFoundException nnfe) {
            appName = "No App Name";
        }
        mAppName = appName;

        mConnectionCallBacksHandler = new ConnectionCallBacksHandler();

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallBacksHandler)
                .addOnConnectionFailedListener(mConnectionCallBacksHandler)
                .build();
        mGoogleApiClient.connect();

        EventBus.getDefault().register(this);
    }


    /** stops requesting location updates. */
    private void stopNormalLocationRequest() {
        if (mConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    /** after some permission checks cancels any previous requestforlocations and starts it again
     * @param locRequest the locationRequest to use
     */
    private void startNormalLocationRequest(LocationRequest locRequest) {
        if (!mConnected) {
            return;
        }
        if (PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locRequest, this);
            } catch (SecurityException se) {
                Log.e(TAG, "WTF! I was told we had this permission!, time to moan to the user!");

                AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setTitle("Permission Problem")
                        .setMessage(mAppName + " has been denied access to your location, please enable it in Settings>Permissions.")
                        .setPositiveButton("I'll do that", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //Your action here
                            }
                        })
                        .show();
            }
        } else {
            //TODO: handle FINE_LOCATION permission denied .. not that it should happen, but you never know
            Log.e(TAG, "FINE_LOCATION permission is denied, this need handling!");
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        EventBus.getDefault().post(
                new LogEvent(
                        "Location received:"  + location
                        + "state is " + "normallPoller"
                        , "GPS"));

        if (location == null || !isAcceptableLocation(location, "whenOnLocation")) {
            return;
        }

        // double check level logging
        if (prevLocation != null
                && location.getLatitude() == prevLocation.getLatitude()
                && location.getLongitude() == location.getLongitude()) {
                Log.w(TAG, "Location seen with duplicate location (different timestamps)");
        }

        goodLocationReceived(location);
    }

    private void goodLocationReceived(Location location) {
        mLocationAugmentor.addSpeedAndBearing(location);
        EventBus.getDefault().post(new LocationEvent(location));
        prevLocation = location;
    }

    /** Checks to see if the location result is suitable ..
     * needs to be
     * accurate enough
     * has an elevation not 0.000000
     * -- should we check speed? Probably not
     * and not the same time as the previous location
     * @param location as received from google
     * @return true if the location seems good
     */
    private boolean isAcceptableLocation(Location location, final String src) {
        if (location == null) {
            return false;
        }

        if (Controller.getInstance().isRecording()) {
            if (location.getAccuracy() > Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING) {
                Log.w(TAG, "Location seen with poor accuracy " + location.getAccuracy());
                return false;
            }
            if (location.getAltitude() == 0.0000 && !location.isFromMockProvider()) {
                Log.w(TAG, "Location seen with zero elevation");
                return false;
            }
            if (prevLocation != null && location.getTime() == prevLocation.getTime()) {
                Log.w(TAG, "Location seen with duplicate timestamp");
                return false;
            }
        }
        else {
            // not recording, we can be more lenient
            if (location.getAccuracy() > Options.MIN_ALLOWED_ACCURACY_METERS_NOT_RECORDING) {
                Log.w(TAG, "Location seen with poor accuracy " + location.getAccuracy());
                return false;
            }
            // we also accept no elevation data when NOT recording
            // and also duplicate timestamps
        }

        return true;
    }

    /**
     * change the update frequency when in/out of ambient mode
     */
    @Subscribe
    public void onAmbientEvent(AmbientEvent evt) {

        switch (evt.getType()) {
            case AmbientEvent.LEAVE_AMBIENT: {
                // transition to "on" state
                // simply request data at the "frequent" rate, no polling required
                isSlowPolling = false;
                mSlowLocationPoller.stop();
                startNormalLocationRequest(whenOnLocationRequest);
                break;
            }
            case AmbientEvent.ENTER_AMBIENT: {
                boolean updateLessOftenInAmbient = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Keys.KEY_GPS_UPDATES_LESS_IN_AMBIENT, true);
                if (updateLessOftenInAmbient) {
                    isSlowPolling = true;
                    stopNormalLocationRequest();
                    mSlowLocationPoller.scheduleNext();
                } else {
                    isSlowPolling = false;
                    mSlowLocationPoller.stop();
                    startNormalLocationRequest(whenOnLocationRequest);
                }
                break;
            }
        }
    }


    private class ConnectionCallBacksHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (tagEnabled(TAG)) Log.d(TAG, "onConnected(): Successfully connected to Google API client");
            mConnected = true;
            //TODO: should reflect ambient state!
            if (isSlowPolling) {
                stopNormalLocationRequest();
                mSlowLocationPoller.scheduleNext();
                //TODOLOW: should we consider an "immediate" request anyway?
            }
            else {
                mSlowLocationPoller.stop();
                PollingLocationHandler.this.startNormalLocationRequest(whenOnLocationRequest);
            }
        } // end onConnected


        @Override
        public void onConnectionSuspended(int cause) {
            //TODO: check suspended = NOT Connected
            mConnected = false;
            PollingLocationHandler.this.stopNormalLocationRequest();
            mSlowLocationPoller.stop();
            if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mConnected = false;
            Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
        }
    }

    /** A SlowLocationPoller requests a location at the "frequent" rate until a good one is received
     * then it cancels the request and starts another one at a time estimated to produce another good location
     * at about the right time.
      */
    private  class SlowLocationPoller implements LocationListener{
        private final Handler mTickerHandler = new Handler();
        private final Runnable mStartLocationUpdatePerSecond;

        /** movingAverageLag between desired time of fix and time a good fix was actually received.  */
        long mMovingAverageLagNanos = 0;

        public SlowLocationPoller() {
            mStartLocationUpdatePerSecond = new Runnable() {
                @Override
                public void run() {
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            PollingLocationHandler.this.mGoogleApiClient,
                            perSecondLocationRequest,
                            SlowLocationPoller.this);
                }
            };
        }

        /** removes any requests for location updates and cancels the pending timer. */
        public void stop() {
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "SlowLocationPoller stopped");
            removeListener();
            if (mTickerHandler != null) {
                mTickerHandler.removeCallbacks(mStartLocationUpdatePerSecond);
            }
        }

        private void removeListener() {
            LocationServices.FusedLocationApi.removeLocationUpdates(PollingLocationHandler.this.mGoogleApiClient, this);
        }

        public void scheduleNext() {
            // schedule a new Request at the appropriate time based on when we want an update and how long it takes to get an accurate fix
            // we want to run *just* after the specified whatnot
            // e.g. 30,000 millis would run @01:00 and 01:30
            long nowEpoch = System.currentTimeMillis();
            long sleepMillis =  (lowFrequencyUpdateIntervalMs - nowEpoch % lowFrequencyUpdateIntervalMs);
            long nowUptime = SystemClock.uptimeMillis();
            long nextInUptime = nowUptime + sleepMillis;
            mTickerHandler.postAtTime(mStartLocationUpdatePerSecond, nextInUptime);
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "SlowLocationPoller scheduled");
            EventBus.getDefault().post(
                    new LogEvent(
                            "SlowLocation Poller Scheduled for "  + sleepMillis + "ms time."
                            , "GPS"));

        }

        @Override
        public void onLocationChanged(Location location) {

            EventBus.getDefault().post(
                    new LogEvent(
                            "Location received:"  + location
                                    + "state is " + "slowPoller"
                            , "GPS"));

            if (location == null || !isAcceptableLocation(location, "slowPollingLocation")) {
                return;
            }

            // got a good location
            // update the lag (probably not used)
            // send the data out
            // and schedule in a new request

            // update the moving average lag
            final long nanosSinceLastGoodFix;
            if (prevLocation != null) {
                nanosSinceLastGoodFix = location.getElapsedRealtimeNanos() - prevLocation.getElapsedRealtimeNanos();
            }
            else {
                nanosSinceLastGoodFix = 0; // assume fix came in bang on time?
            }
            mMovingAverageLagNanos = mMovingAverageLagNanos /2 + nanosSinceLastGoodFix;

            goodLocationReceived(location);
            removeListener();
            scheduleNext();
        }
    }


}
