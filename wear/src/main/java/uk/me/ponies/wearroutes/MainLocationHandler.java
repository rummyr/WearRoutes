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

import uk.me.ponies.wearroutes.common.LocationAugmentor;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.prefs.Keys;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * A Class that handles "requesting" location updates from the FusedLocationApi,
 * augmenting with speed and distance and then broadcasting the augmented location.
 * It handles changing the frequency on entering/leaving Ambient mode (though this may be poor design)
 * it may in future also manage the internal GPS, but that is not guaranteed to happen
 * due to the battery drain on a watch.
 */
public class MainLocationHandler implements LocationListener {
    //TODO: temporary
    //private TrackLocationAndBearingOnMapFragment mMapLocationAndBearingTracker; // here to stop garbage collection
    //mMapLocationAndBearingTracker = new TrackLocationAndBearingOnMapFragment(mMapFragmentContainer);
    private final Context mContext;
    private final String mAppName; // used only for displaying an error dialog!
    private final String TAG = getClass().getSimpleName();

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

    private final LocationAugmentor mLocationAugmentor = new LocationAugmentor();


    // api and callbacks
    private final GoogleApiClient mGoogleApiClient;
    private final ConnectionCallBacksHandler mConnectionCallBacksHandler;
    private boolean mConnected = false;

    private LocationRequest updateFrequency = whenOnLocationRequest;


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

    public void start(LocationRequest locRequest) {
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
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        mLocationAugmentor.addSpeedAndBearing(location);

        EventBus.getDefault().post(new LocationEvent(location));
    }


    /**
     * change the update frequency when in/out of ambient mode
     */
    @Subscribe
    public void onAmbientEvent(AmbientEvent evt) {
        if (AmbientEvent.ENTER == evt.getType()) {
            boolean updateInfrequentlyInAmbient = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Keys.KEY_GPS_UPDATES_LESS_IN_AMBIENT, true);
            if (updateInfrequentlyInAmbient) {
                updateFrequency = whenAmbientLocationRequest;
            } else {
                updateFrequency = whenOnLocationRequest;
            }
            start(updateFrequency);
        } else if (AmbientEvent.LEAVE == evt.getType()) {
            updateFrequency = whenOnLocationRequest;
            start(updateFrequency);
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
            if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mConnected = false;
            Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
        }
    }
}
