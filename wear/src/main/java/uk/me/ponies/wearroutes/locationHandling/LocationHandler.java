package uk.me.ponies.wearroutes.locationHandling;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.common.LocationAugmentor;
import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.controller.Stats;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationPollingStateEvent;
import uk.me.ponies.wearroutes.historylogger.LogEvent;
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
public class LocationHandler implements ILocationHandler, SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);


    private final static String TAG = LocationHandler.class.getSimpleName();
    // not used, but kept just in case private final CharSequence mAppName; // used only for displaying an error dialog!
    private final LocationAugmentor mLocationAugmentor = new LocationAugmentor();
    Context mApplicationContext; //TODO: Coupling!
    private GoogleApiClient mGoogleApiClient;
    boolean mConnected = false; //TODO: Coupling!

    private float minAllowedAccuracyRecording = 100;


    private SlowLocationPollerBase mSlowLocationPoller;
    private NormalLocationPoller mNormalLocationPoller;




    // api and callbacks
    @SuppressWarnings("FieldCanBeLocal")
    private ConnectionCallBacksHandler mConnectionCallBacksHandler;


    private static final int POLL_USING_POST_AT_TIME = 1;
    public static final int POLL_USING_ALARMS = 2;
    private static final int ACCURACY_SIMPLE = 100;
    public static final int ACCURACY_VELOCITY_ADJUST = 101;
    private IAcceptableLocationStrategy mAccuracyStratey;

    private boolean mFastPolling;
    private boolean mSlowPolling = false;

    private static final int NO_POLLING = 0;
    private static final int VERY_SLOW_POLLING = 1;
    private static final int SLOW_POLLING = 2;
    private static final int FAST_POLLING = 3;
    private String[] pollingModes = { "NO", "Very Slow", "Slow", "Fast"};

    private int pollingMode;
    private LocationListener currentlyRegisteredListener;

    public LocationHandler(Context context, int pollingStrategy, int accuracyStrategy) {
        this.mApplicationContext = context;


        PreferenceManager.getDefaultSharedPreferences(mApplicationContext).registerOnSharedPreferenceChangeListener(this);
        setMinAllowedAccuracyRecordingFromSharedPrefs(PreferenceManager.getDefaultSharedPreferences(mApplicationContext));



    /* not required, but kept in case I need the code again
        CharSequence appName;

        ApplicationInfo applicationInfo = mApplicationContext.getApplicationInfo();
        appName = applicationInfo.loadLabel(mApplicationContext.getPackageManager());

        mAppName = appName;
    */
        switch (pollingStrategy) {
            case POLL_USING_ALARMS: {
                mSlowLocationPoller = new SlowLocationPollerAlarmWakeup(this, mApplicationContext);
                break;
            }
            case POLL_USING_POST_AT_TIME: {
                mSlowLocationPoller = new SlowLocationPollerPostAtTime(this, mApplicationContext);
                break;
            }
            default: {
                Log.e(TAG, "Unknown scheduling pollingStrategy " + pollingStrategy);
                mSlowLocationPoller = new SlowLocationPollerNoOp(this, mApplicationContext);
                break;
            }
        }
        mNormalLocationPoller = new NormalLocationPoller(this);

        setSlowPollerRecordingIntervalFromSharedPrefs(PreferenceManager.getDefaultSharedPreferences(mApplicationContext));

        assignAccuracyStrategy(accuracyStrategy);

        mConnectionCallBacksHandler = new ConnectionCallBacksHandler();

        mGoogleApiClient = new GoogleApiClient.Builder(mApplicationContext)
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

    void goodLocationReceived(Location location, String pollerType) {
        if (mSlowPolling && mFastPolling) {
            Log.e(TAG, "Slow AND Fast polling are enabled!");
            EventBus.getDefault().post(
                    new LogEvent(
                            "Slow AND Fast polling are enabled!","GPS"));

        }
        EventBus.getDefault().post(
                new LogEvent(
                        "Good Location received:" + location +" from" + pollerType
                        , "GPS"));
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "Good location received " + location + " from " + pollerType);
        mLocationAugmentor.addSpeedAndBearing(location);
        EventBus.getDefault().post(new LocationEvent(location, pollerType));
    }

    /**
     * Checks to see if the location result is suitable ..
     * needs to be
     * accurate enough aka <MIN_ALLOWED_ACCURACY_METERS_RECORDING or MIN_ALLOWED_ACCURACY_METERS_NOT_RECORDING
     * has an elevation not 0.000000
     * -- should we isNullAndLog speed? Probably not
     * and not the same time as the previous location
     *
     * @param location as received from google
     * @return true if the location seems good
     */
    boolean isAcceptableLocation(@NonNull Location location, final String src) {
        return mAccuracyStratey.isAcceptableLocation(location, src);
    }




    /** to detect changes to the accuracy preference */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Keys.KEY_PREF_WEAR_RECORDING_LOCATION_ACCURACY . equals(key)) {
            setMinAllowedAccuracyRecordingFromSharedPrefs(sharedPreferences);
        }
        if (Keys.KEY_PREF_WEAR_RECORDING_LOCATION_INTERVAL.equals(key)) {
            setSlowPollerRecordingIntervalFromSharedPrefs(sharedPreferences);
        }
    }

    private void setMinAllowedAccuracyRecordingFromSharedPrefs(SharedPreferences prefs) {
        String minAllowedAccuracyRecordingStr = prefs.getString(Keys.KEY_PREF_WEAR_RECORDING_LOCATION_ACCURACY,
                String.valueOf(Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING));
        setMinAllowedAccuracyRecording(minAllowedAccuracyRecordingStr);
    }

    private void setSlowPollerRecordingIntervalFromSharedPrefs(SharedPreferences prefs) {
        String minRecordingLocationInterval = prefs.getString(Keys.KEY_PREF_WEAR_RECORDING_LOCATION_INTERVAL,
                String.valueOf(Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS));
        setSlowPollerRecordingInterval(minRecordingLocationInterval );
    }

    private void setMinAllowedAccuracyRecording(String minAllowedAccuracyRecordingStr) {
        try {
            minAllowedAccuracyRecording = Float.parseFloat(minAllowedAccuracyRecordingStr);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "minAllowedAccuracyRecording is NOT a sensible float '" + minAllowedAccuracyRecordingStr + "'"
                    + " defaulting to " + Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING);
            minAllowedAccuracyRecording = Options.MIN_ALLOWED_ACCURACY_METERS_RECORDING; // so we have something if it fails to parse
            mAccuracyStratey.setAccuracy(minAllowedAccuracyRecording);
        }
    }

    private void setSlowPollerRecordingInterval(String recordingIntervalStr) {
        int recordingInterval;
        try {
            recordingInterval = Integer.parseInt(recordingIntervalStr);
            mSlowLocationPoller.setLowFrequencyUpdateIntervalSecs(recordingInterval);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "recordingInterval is NOT a sensible int '" + recordingIntervalStr + "'"
                    + " defaulting to " + Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS);
            recordingInterval = Options.LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS; // so we have something if it fails to parse
            mSlowLocationPoller.setLowFrequencyUpdateIntervalSecs(recordingInterval);
        }
    }

    private class ConnectionCallBacksHandler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (tagEnabled(TAG))  Log.d(TAG, "onConnected(): Successfully connected to Google API client");
            mConnected = true;
            if (pollingMode == NO_POLLING) {
                switchToNoPolling();
            } else if (pollingMode == VERY_SLOW_POLLING) {
                switchToVerySlowPolling();
            }  else if (pollingMode == SLOW_POLLING) {
                switchToSlowPolling();
            } else if (pollingMode == FAST_POLLING) {
                switchToFastPolling();
            }
       } // end onConnected


        @Override
        public void onConnectionSuspended(int cause) {
            int previousPollingMode = pollingMode;
            switchToNoPolling();
            // and restore
            pollingMode = previousPollingMode;
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
        mSlowLocationPoller.destroy();
        mSlowLocationPoller = null;
        mNormalLocationPoller.destroy();
        mNormalLocationPoller = null;
        mGoogleApiClient.disconnect();
        mGoogleApiClient.unregisterConnectionCallbacks(mConnectionCallBacksHandler);
        mGoogleApiClient.unregisterConnectionFailedListener(mConnectionCallBacksHandler);
        mConnected = false; // or it very soon will be
        mGoogleApiClient = null;
        mConnectionCallBacksHandler = null;
        EventBus.getDefault().unregister(this);
        PreferenceManager.getDefaultSharedPreferences(mApplicationContext).unregisterOnSharedPreferenceChangeListener(this);
        mAccuracyStratey.destroy();
        mAccuracyStratey = null;
        mApplicationContext = null;
    }

    /**
     * try to force all locationUpdateRequests through here so we can watch them
     */
    @Override
    public PendingResult<Status> requestLocationUpdates(LocationRequest locRequest, LocationListener locationListener) {
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "requestingLocationUpdates for listener " + locationListener);
        if (currentlyRegisteredListener != null) {
            String msg = "Registered a locationListener when there was one already active old:" + currentlyRegisteredListener + " new:" + locationListener;
            Log.e(TAG, msg);
            EventBus.getDefault().post(new LogEvent(msg, "GPS"));
        }

        if (!mConnected) {
            String msg = "Can't request location updates @ (" + locRequest.getInterval() + ")ms because we aren't connected"
            Log.w(TAG, msg);
            EventBus.getDefault().post(new LogEvent(msg, "GPS"));
            return null;
        }
        try {
            PendingResult<Status> rv = LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    locRequest,
                    locationListener);
            currentlyRegisteredListener = locationListener;
            String msg = "Registering a locationListener:  " + locationListener + "updates @ (" + locRequest.getInterval() + ")ms";
            Log.w(TAG, msg);
            EventBus.getDefault().post(new LogEvent(msg, "GPS"));
            return rv;
        }
        catch (SecurityException se) {
            //TODO: fix properly!
            Log.e(TAG, "SecurityException thrown requestingLocationUpdates", se);
        }
        catch (Exception e) {
            return null;
        }
    }

    public @Nullable  PendingResult<Status> removeLocationUpdates(LocationListener locationListener) {
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "removingLocationUpdates for listener " + locationListener);
        if (currentlyRegisteredListener != locationListener) {
            String msg = "removing a locationListener that is NOT the last registered one! old:" + currentlyRegisteredListener + " new:" + locationListener;
            Log.e(TAG, msg);
            EventBus.getDefault().post(new LogEvent(msg, "GPS"));
        }
        if (currentlyRegisteredListener != null) {
            PendingResult<Status> rv = LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, locationListener);
            currentlyRegisteredListener = null;
            return  rv;
        }
        else {
            String msg = "Removing a locationListener (" + locationListener + "(when we aren't listening for updates!";
            Log.e(TAG, msg);
            EventBus.getDefault().post(new LogEvent(msg, "GPS"));
            return null;
        }
    }

    private void switchToFastPolling() {
        Stats.setCurrentPollingMode("Fast");
        if (pollingMode == NO_POLLING) {
            // nothing to stop!
            Defeat.noop();
        } else if (pollingMode == VERY_SLOW_POLLING) {
            mSlowLocationPoller.stop();
            mSlowPolling = false;
        } else if (pollingMode == SLOW_POLLING) {
            mSlowLocationPoller.stop();
            mSlowPolling = false;
        }

        if (!mNormalLocationPoller.isActive()) {
            mAccuracyStratey.newPollStarting();
            mNormalLocationPoller.scheduleNext();
            mFastPolling = true;
        }
        pollingMode = FAST_POLLING;
    }

    private void switchToSlowPolling() {
        Stats.setCurrentPollingMode("Slow");
        if (pollingMode == NO_POLLING) {
            // nothing to stop!
            Defeat.noop();
        } else if (pollingMode == VERY_SLOW_POLLING) {
            // nothing to do until we implement v slow polling
            Defeat.noop();
        } else if (pollingMode == SLOW_POLLING) {
            // already in the right mode
            // though we might want to consider restarting -- e.g. if is a re-connected event!!
            Defeat.noop();
        } if (pollingMode == FAST_POLLING) {
            mNormalLocationPoller.stop();
            mFastPolling = false;
        }

        //BUG: need to switch on slow poller if it isn't already active!
        if (!mSlowPolling) {
            mAccuracyStratey.newPollStarting();
            mSlowLocationPoller.scheduleNext();
            mSlowPolling = true;
        }
        pollingMode = SLOW_POLLING;
    }

    //TODO: implement very slow polling
    private void switchToVerySlowPolling() {
        Stats.setCurrentPollingMode("Very Slow");

        if (pollingMode == NO_POLLING) {
            // nothing to stop!
            Defeat.noop();
        } else if (pollingMode == VERY_SLOW_POLLING) {
            // nothing to do until we implement v slow polling
            Defeat.noop();
        } else if (pollingMode == SLOW_POLLING) {
            // already in the right mode
            // though we might want to consider restarting -- e.g. if is a re-connected event!!
            Defeat.noop();
        } if (pollingMode == FAST_POLLING) {
            mNormalLocationPoller.stop();
            mFastPolling = false;
        }

        //BUG: need to switch on slow poller if it isn't already active!
        if (!mSlowPolling) {
            mAccuracyStratey.newPollStarting();
            mSlowLocationPoller.scheduleNext();
            mSlowPolling = true;
        }
        pollingMode = VERY_SLOW_POLLING;

    }

    private void switchToNoPolling() {
        Stats.setCurrentPollingMode("Stopped/No");

        if (pollingMode == VERY_SLOW_POLLING) {
            mSlowLocationPoller.stop();
            mSlowPolling = false;
        }
        if (pollingMode == SLOW_POLLING) {
            mSlowLocationPoller.stop();
            mSlowPolling = false;
        }
        else if (pollingMode == FAST_POLLING) {
            mNormalLocationPoller.stop();
            mFastPolling = false;
        }
        pollingMode = NO_POLLING;

    }

    /** polling state change .. check against current state to avoid stopping/starting unnecessarily */
    @Subscribe
    public void onPollingStateChange(LocationPollingStateEvent plse) {
        Log.d(TAG, "PollingStateChange Event received, state is " + plse.toString());
        Log.d(TAG, "Polling mode switching from " + pollingModes[pollingMode]);

        if (plse.getState() == LocationPollingStateEvent.OFF) {
            switchToNoPolling();
        }

        if (plse.getState() == LocationPollingStateEvent.FAST) {
            // fast polling, recording or not
            switchToFastPolling();
        }

        if (plse.getState() == LocationPollingStateEvent.SLOW) {
            // fast polling, recording or not
            switchToSlowPolling();
        }
        if (plse.getState() == LocationPollingStateEvent.VERY_SLOW) {
            // fast polling, recording or not
            switchToVerySlowPolling();
        }
    }
}



