package uk.me.ponies.wearroutes.locationHandling;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.common.LocationAugmentor;
import uk.me.ponies.wearroutes.common.locationUtils.Utils;
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
public class LocationHandler implements LocationListener,ILocationHandler, SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);


    private final static String TAG = LocationHandler.class.getSimpleName();
    private final String mAppName; // used only for displaying an error dialog!
    private final LocationAugmentor mLocationAugmentor = new LocationAugmentor();
    Context mContext; //TODO: Coupling!
    private GoogleApiClient mGoogleApiClient;
    boolean mConnected = false; //TODO: Coupling!

    float minAllowedAccuracyRecording = 100;


    private SlowLocationPollerBase mSlowLocationPoller;

    //BUG: accept lower accuracy when not recording
    private final LocationRequest whenOnLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Use high accuracy
            .setInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the update interval to 2 seconds
            .setFastestInterval(TimeUnit.SECONDS.toMillis(Options.LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS)) // Set the fastest update interval to 2 seconds
            .setSmallestDisplacement(2); // Set the minimum displacement to quite small indeed;


    // api and callbacks
    @SuppressWarnings("FieldCanBeLocal")
    private ConnectionCallBacksHandler mConnectionCallBacksHandler;
    /**
     * used to determine which poller to use when the API client is connected/disconnected/suspended.
     */
    private boolean isSlowPolling = false;

    public static final int POLL_USING_POST_AT_TIME = 1;
    public static final int POLL_USING_ALARMS = 2;
    public static final int ACCURACY_SIMPLE = 100;
    public static final int ACCURACY_VELOCITY_ADJUST = 101;
    private IAcceptableLocationStrategy mAccuracyStratey;


    public LocationHandler(Activity activity, int pollingStrategy, int accuracyStrategy) {
        this.mContext = activity.getApplicationContext();


        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(this);
        setMinAllowedAccuracyRecordingFromSharedPrefs(PreferenceManager.getDefaultSharedPreferences(mContext));


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

        switch (pollingStrategy) {
            case POLL_USING_ALARMS: {
                mSlowLocationPoller = new SlowLocationPollerAlarmWakeup(this, mContext);
                break;
            }
            case POLL_USING_POST_AT_TIME: {
                mSlowLocationPoller = new SlowLocationPollerPostAtTime(this, mContext);
                break;
            }
            default: {
                Log.e(TAG, "Unknown scheduling pollingStrategy " + pollingStrategy);
                mSlowLocationPoller = new SlowLocationPollerNoOp(this, mContext);
                break;
            }

        }

        assignAccuracyStrategy(accuracyStrategy);

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

    private void assignAccuracyStrategy(int accuracyStrategy) {
        switch (accuracyStrategy){
            case ACCURACY_SIMPLE: {
                mAccuracyStratey = new AcceptableLocationStrategySimpleAccuracy(minAllowedAccuracyRecording);
                break;
            }
            case ACCURACY_VELOCITY_ADJUST: {
                mAccuracyStratey = new AcceptableLocationStrategyVelocityAdjust(minAllowedAccuracyRecording);
                break;
            }
            default: {
                Log.e(TAG, "accuracy strategy not recognised!");
                mAccuracyStratey = new AcceptableLocationStrategyAll();
            }
        }
    }


    /**
     * stops requesting location updates.
     */
    private void stopNormalLocationRequest() {
        if (mConnected) {
            removeLocationUpdates(this);
        }
    }

    /**
     * after some permission checks cancels any previous requestforlocations and starts it again
     *
     * @param locRequest the locationRequest to use
     */
    private void startNormalLocationRequest(LocationRequest locRequest) {
        if (!mConnected) {
            return;
        }
        if (PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                removeLocationUpdates(this);
                EventBus.getDefault().post(
                        new LogEvent(
                                "Normal Poller now requestingLocationUpdates at " + locRequest.getInterval() + "ms intervals"
                                , "GPS"));
                requestLocationUpdates(locRequest, this);
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
                        "Location received:" + location
                                + "state is " + "normallPoller"
                        , "GPS"));

        if (location == null || !isAcceptableLocation(location, "whenOnLocation")) {
            return;
        }


        goodLocationReceived(location);
    }

    void goodLocationReceived(Location location) {
        EventBus.getDefault().post(
                new LogEvent(
                        "Good Location received:" + location
                        , "GPS"));
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "Good location received " + location);
        mLocationAugmentor.addSpeedAndBearing(location);
        EventBus.getDefault().post(new LocationEvent(location));
    }

    /**
     * Checks to see if the location result is suitable ..
     * needs to be
     * accurate enough aka <MIN_ALLOWED_ACCURACY_METERS_RECORDING or MIN_ALLOWED_ACCURACY_METERS_NOT_RECORDING
     * has an elevation not 0.000000
     * -- should we check speed? Probably not
     * and not the same time as the previous location
     *
     * @param location as received from google
     * @return true if the location seems good
     */
    boolean isAcceptableLocation(Location location, final String src) {
        if (location == null) {
            return false;
        }
        return mAccuracyStratey.isAcceptableLocation(location);
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
                // although it seems sensible to stop the slow and start the normal
                // if we do it the other way around we make sure that there is never
                // a gap which *MIGHT* stop any unknown shutdown GPS while it's not needed behaviour
                mAccuracyStratey.newPollStarting();
                startNormalLocationRequest(whenOnLocationRequest);
                mSlowLocationPoller.stop();


                break;
            }
            case AmbientEvent.ENTER_AMBIENT: {
                boolean updateLessOftenInAmbient = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Keys.KEY_GPS_UPDATES_LESS_IN_AMBIENT, true);
                if (updateLessOftenInAmbient) {
                    isSlowPolling = true;
                    mAccuracyStratey.newPollStarting();
                    mSlowLocationPoller.scheduleNext();
                    stopNormalLocationRequest();
                } else {
                    isSlowPolling = false;
                    mAccuracyStratey.newPollStarting();
                    startNormalLocationRequest(whenOnLocationRequest);
                    mSlowLocationPoller.stop();
                }
                break;
            }
        }
    }

    /** to detect changes to the accuracy preference */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Keys.KEY_PREF_WEAR_RECORDING_LOCATION_ACCURACY . equals(key)) {
            setMinAllowedAccuracyRecordingFromSharedPrefs(sharedPreferences);
        }
    }

    public void setMinAllowedAccuracyRecordingFromSharedPrefs(SharedPreferences prefs) {
        String minAllowedAccuracyRecordingStr = prefs.getString(Keys.KEY_PREF_WEAR_RECORDING_LOCATION_ACCURACY,
                String.valueOf(Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING));
        setMinAllowedAccuracyRecording(minAllowedAccuracyRecordingStr);
    }

    public void setMinAllowedAccuracyRecording(String minAllowedAccuracyRecordingStr) {
        try {
            minAllowedAccuracyRecording = Float.parseFloat(minAllowedAccuracyRecordingStr);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "minAllowedAccuracyRecording is NOT a sensible float '" + minAllowedAccuracyRecordingStr + "'"
                    + " defaulting to " + Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING);
            minAllowedAccuracyRecording = Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING; // so we have something if it fails to parse
            mAccuracyStratey.setAccuracy(minAllowedAccuracyRecording);
        }
    }


    private class ConnectionCallBacksHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (tagEnabled(TAG))  Log.d(TAG, "onConnected(): Successfully connected to Google API client");
            mConnected = true;
            if (isSlowPolling) {
                stopNormalLocationRequest();
                mAccuracyStratey.newPollStarting();
                mSlowLocationPoller.scheduleNext();
                //TODOLOW: should we consider an "immediate" request anyway?
            } else {
                mSlowLocationPoller.stop();
                mAccuracyStratey.newPollStarting();
                startNormalLocationRequest(whenOnLocationRequest);
            }
        } // end onConnected


        @Override
        public void onConnectionSuspended(int cause) {
            //TODO: check suspended = NOT Connected
            mConnected = false;
            stopNormalLocationRequest();
            mSlowLocationPoller.stop();
            if (tagEnabled(TAG))
                Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mConnected = false;
            Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
        }
    }



    /**
     * Shutdown!
     */
    public void shutdown() {
        stopNormalLocationRequest();
        mSlowLocationPoller.destroy();
        mSlowLocationPoller = null;
        mGoogleApiClient.unregisterConnectionCallbacks(mConnectionCallBacksHandler);
        mGoogleApiClient.unregisterConnectionFailedListener(mConnectionCallBacksHandler);
        mGoogleApiClient.disconnect();
        mGoogleApiClient = null;
        mConnectionCallBacksHandler = null;
        EventBus.getDefault().unregister(this);
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
        mAccuracyStratey.destroy();
        mAccuracyStratey = null;
        mContext = null;
    }

    /**
     * try to force all locationUpdateRequests through here so we can watch them
     */
    @Override
    public PendingResult<Status> requestLocationUpdates(LocationRequest locRequest, LocationListener locationListener) {
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "requestingLocationUpdates for listener " + locationListener);
        return LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                locRequest,
                locationListener);
    }

    public PendingResult<Status> removeLocationUpdates(LocationListener locationListener) {
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "removingLocationUpdates for listener " + locationListener);
        return LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, locationListener);
    }
}



