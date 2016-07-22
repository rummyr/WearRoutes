package uk.me.ponies.wearroutes;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;

/**
 * Created by rummy on 16/05/2016.
 */
public class TrackLocationAndBearingOnMap implements LocationListener {
    public static String TAG = "TrackLocation+Bearing";
    /* Use soft reference to avoid memory pin leaks,
     * as long as the original exists then this will be valid! */
    WeakReference<GoogleMap> mapRef = new WeakReference<GoogleMap>(null);
    private Location mCurrentLocation;
    private Location mPreviousLocation;

    /* Do we keep a reference to MainMobileActivity or osmDMap */
    public TrackLocationAndBearingOnMap(GoogleMap osmDMap) {
        mapRef = new WeakReference<>(osmDMap);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged Called with " + location);

        // quick check, if our map has dissapeared!!!
        GoogleMap osmDMap = mapRef.get();
        if (osmDMap == null) {
            Log.e(TAG, "Well, thats interesting, our MapView seems to have pissed off!");
            mPreviousLocation = location;
            return;
        }



        mCurrentLocation = location;
        double distance = 0.0;
        if (mPreviousLocation != null) {
            distance = mPreviousLocation.distanceTo(mCurrentLocation);
        }
        if (!location.hasBearing() && mPreviousLocation != null) {
            // simulate a bearing
            if (mPreviousLocation != null) {
                double bearingDegrees = mPreviousLocation.bearingTo(mCurrentLocation);
                location.setBearing((float)bearingDegrees);
            }

        }
        if (!location.hasSpeed() && mPreviousLocation != null) {
            // simulate a speed
            double duration = location.getElapsedRealtimeNanos() - mPreviousLocation.getElapsedRealtimeNanos();
            double speed = distance/duration;
            location.setSpeed((float) speed);
        }
        //mMapLocationOverlay.setLocation(location); // GRR!

        //osmDMap.setMapOrientation(135.74706f + 90);
        CameraPosition.Builder targetPos = CameraPosition.builder(osmDMap.getCameraPosition());

        if (!Options.NORTH_UP) {
            targetPos.bearing(mCurrentLocation.getBearing());
        }

        // only rotate/animate IFF has really moved!
        if (mPreviousLocation == null) {
            targetPos.target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        }
        else if (distance > Options.MIN_MOVE_DISTANCE_METERS + location.getAccuracy()) {
            targetPos.target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        }
        if (Options.ANIMATE_MOVES) {
            osmDMap.animateCamera(CameraUpdateFactory.newCameraPosition(targetPos.build()));
        }
        else {
            osmDMap.moveCamera(CameraUpdateFactory.newCameraPosition(targetPos.build()));
        }


        mPreviousLocation = location;
    }
}
