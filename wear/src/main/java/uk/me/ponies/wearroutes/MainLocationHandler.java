package uk.me.ponies.wearroutes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.me.ponies.wearroutes.common.LocationAugmentor;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * A Class that handles "requesting" location updates from the FusedLocationApi,
 * augmenting with speed and distance and then broadcasting the augmented location.
 * It handles changing the frequency on entering/leaving Ambient mode (though this may be poor design)
 * it may in future also manage the internal GPS, but that is not guaranteed to happen
 * due to the battery drain on a watch.
 */
public class MainLocationHandler implements LocationListener {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private final Context mContext;
    private final String mAppName; // used only for displaying an error dialog!
    private final String TAG = getClass().getSimpleName();
    private Location prevLocation;

    private final LocationRequest whenOnLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the update interval to 2 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the fastest update interval to 2 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed;
    private final LocationRequest whenAmbientLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS)) // Set the update interval to 2 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS)) // Set the fastest update interval to 2 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed
    private final LocationRequest tryForMoreAccurateLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_RETRYING_SECS)) // Set the update interval to 2 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_RETRYING_SECS)) // Set the fastest update interval to 2 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed

    private final LocationAugmentor mLocationAugmentor = new LocationAugmentor();


    // api and callbacks
    private final GoogleApiClient mGoogleApiClient;
    @SuppressWarnings("FieldCanBeLocal")
    private final ConnectionCallBacksHandler mConnectionCallBacksHandler;
    private boolean mConnected = false;

    private LocationRequest mCurrentlyActiveLocRequest = whenOnLocationRequest; // start with this
    private AtomicBoolean mIsCurrentlyTryingForABetterLocation = new AtomicBoolean(false);
    private LocationListener mRetryLocationListener = new RetryLocationListener();

    public MainLocationHandler(Activity activity) {
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


    /** stops the main poller, and cancels the retryer */
    private void stop() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        cancelTryingForABetterLocation();
    }

    /** after some permission checks cancels any previous requestforlocations and starts it again
     * does NOT cancel the retryer if its running.
     * @param locRequest the locationRequest to use
     */
    private void start(LocationRequest locRequest) {
        if (!mConnected) {
            return;
        }
        if (android.content.pm.PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {

                // to handle changing of update frequency we have to remove and add
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locRequest, this);

            } catch (java.lang.SecurityException se) {
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

    /** Request just a single location, only works if connected, keep going until either
     * a) we've waited too long
     * b) we have a location that has .. speed, elevation, accuracy meeting internal requirements
     *
     * requests at a faster interval,
     * cancels when a good enough reading is received
     * automatically when it expires (set to roughly just before the next location is expected to land)
     *
     */
    private void tryForABetterLocation() {
        boolean currentlyTrying = mIsCurrentlyTryingForABetterLocation.getAndSet(true);
        if (currentlyTrying) {
            return;
        }
        if (!mConnected) {
            return;
        }

        // a tryForMoreAccurateLocationRequest requests more frequently, but only for a limited length of time
        //TODOLOW: retry expiry isn't absolutely accurate
        // set the expiry to roughly 1 second before the next one is due to land (probably)
        //BUG: when it expires we don't get a notification! so we still think it's RUNNING! This needs a serious rethink!
        long mCurrentlyActiveLocRequestIntervalMillis = mCurrentlyActiveLocRequest.getInterval();
        tryForMoreAccurateLocationRequest.setExpirationDuration(mCurrentlyActiveLocRequestIntervalMillis - TimeUnit.SECONDS.toMillis(1));

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, tryForMoreAccurateLocationRequest, mRetryLocationListener);
    }
    private void cancelTryingForABetterLocation() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,mRetryLocationListener);
        mIsCurrentlyTryingForABetterLocation.getAndSet(false);

    }

    @Override
    public void onLocationChanged(Location location) {
        boolean unusableLocation = false;
        if (location == null) { // might as well handle it though it should never happen
            return;
        }

        // perform some simple filtering on the data
        if (prevLocation != null
                && location.getLatitude() == prevLocation.getLatitude()
                && location.getLongitude() == location.getLongitude()) {
                Log.w(TAG, "Location seen with duplicate location (different timestamps)");
        }

        if (!isAcceptableLocation(location)) {
            unusableLocation = true;
        }

        if (!unusableLocation) {
            EventBus.getDefault().post(new LocationEvent(location));
            mLocationAugmentor.addSpeedAndBearing(location);
            cancelTryingForABetterLocation(); // just in case
        }
        else {
            tryForABetterLocation();
        }
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
    private boolean isAcceptableLocation(Location location) {
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
        if (AmbientEvent.ENTER_AMBIENT == evt.getType()) {
            boolean updateInfrequentlyInAmbient = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Keys.KEY_GPS_UPDATES_LESS_IN_AMBIENT, true);
            if (updateInfrequentlyInAmbient) {
                mCurrentlyActiveLocRequest = whenAmbientLocationRequest;
            } else {
                mCurrentlyActiveLocRequest = whenOnLocationRequest;
            }
            start(mCurrentlyActiveLocRequest);
        } else if (AmbientEvent.LEAVE_AMBIENT == evt.getType()) {
            mCurrentlyActiveLocRequest = whenOnLocationRequest;
            start(mCurrentlyActiveLocRequest);
        }
    }


    private class ConnectionCallBacksHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (tagEnabled(TAG)) Log.d(TAG, "onConnected(): Successfully connected to Google API client");
            mConnected = true;
            //TODO: should reflect ambient state!
            MainLocationHandler.this.start(whenOnLocationRequest);
        } // end onConnected


        @Override
        public void onConnectionSuspended(int cause) {
            //TODO: check suspended = NOT Connected
            mConnected = false;
            MainLocationHandler.this.stop();
            cancelTryingForABetterLocation();
            if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mConnected = false;
            Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
        }
    }

    private class RetryLocationListener implements LocationListener{
            @Override
            public void onLocationChanged(Location location) {
                if (isAcceptableLocation(location)) { // is good enough
                    // mark as good
                    mIsCurrentlyTryingForABetterLocation.set(false);
                    // cancel these frequent retries
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
                    // do usual processing on the data
                    MainLocationHandler.this.onLocationChanged(location);
                }
            }
        }

}
