package uk.me.ponies.wearroutes;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.lang.ref.WeakReference;

import uk.me.ponies.wearroutes.common.BearingSectorizer;

/**
     * Created by rummy on 16/05/2016.
     */
    public class TrackLocationAndBearingOnMapFragment implements LocationListener {
        public static final String TAG = "TrackLocBearingOnMapFrg";
        /* Use soft reference to avoid memory pin leaks,
         * as long as the original exists then this will be valid! */
        WeakReference<MapSwipeToZoomFragment> mapRef = new WeakReference<>(null);
        private Location mCurrentLocation;
        private Location mPreviousLocation;
        private BearingSectorizer mBearingSectorizer = new BearingSectorizer(Options.BEARING_SECTORS); // 20 degree sectors

        /* Do we keep a reference to MainMobileActivity or mapSwipeToZoomFragment */
        public TrackLocationAndBearingOnMapFragment(MapSwipeToZoomFragment mapSwipeToZoomFragment) {
            mapRef = new WeakReference<>(mapSwipeToZoomFragment);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged Called with " + location);

            // quick check, if our map has dissapeared!!!
            MapSwipeToZoomFragment mapSwipeToZoomFragment = mapRef.get();

            // use a try finally block to ensure that previousLocation is updated
            try {
                if (mapSwipeToZoomFragment == null) {
                    Log.e(TAG, "Well, that's interesting, our MapView seems to have pissed off!");
                    return;
                }

                mCurrentLocation = location;
                double distance = 0.0;
                if (mPreviousLocation != null) {
                    distance = mPreviousLocation.distanceTo(mCurrentLocation);
                }
                if (!location.hasBearing() && mPreviousLocation != null) {
                    // simulate a bearing
                    Log.d(TAG, "Need to simulate a bearing");
                    if (mPreviousLocation != null) {
                        double bearingDegrees = mPreviousLocation.bearingTo(mCurrentLocation);
                        location.setBearing((float) bearingDegrees);
                    }

                }
                if (!location.hasSpeed() && mPreviousLocation != null) {
                    // simulate a speed
                    Log.d(TAG, "Need to simulate a speed");
                    double duration = location.getElapsedRealtimeNanos() - mPreviousLocation.getElapsedRealtimeNanos();
                    double speed = distance / duration;
                    location.setSpeed((float) speed);
                }
                //mMapLocationOverlay.setLocation(location); // GRR!

                //osmDMap.setMapOrientation(135.74706f + 90);
                CameraPosition originalCameraPosition = mapSwipeToZoomFragment.getCameraPosition();
                GoogleMap map = mapSwipeToZoomFragment.getMap();

                if (originalCameraPosition != null) {
                    CameraPosition.Builder targetPos = CameraPosition.builder(originalCameraPosition);

                    if (!Options.NORTH_UP) {
                        // check the bearing to see if it has changed
                        float newBearing = mBearingSectorizer.convertToSectorDegrees(mCurrentLocation.getBearing());
                        targetPos.bearing(newBearing);
                    }

                    if (map != null) {
                        LatLngBounds latLngBounds = map.getProjection().getVisibleRegion().latLngBounds;
                        // use this to see if map need fixing up!
                    }
                    // only update target cameraPosition IFF it has moved by enough
                    if (Options.I_MOVE_MAP) {
                        if (mPreviousLocation == null) {
                            // if NO previous location, then update camera location
                            targetPos.target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                        } else {
                            // only update camera position if it has changed appropriately
                            float distanceFromCam = haversineDistanceBetween(originalCameraPosition.target.latitude, originalCameraPosition.target.longitude,
                                    mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                            //float[] results = new float[3];;
                            //Location.distanceBetween(cameraPosition.target.latitude, cameraPosition.target.longitude,
                            //        mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), results);
                            //float accurateDistanceFromCam = results[0];
                            //        String.valueOf(accurateDistanceFromCam);
                            //Log.v(TAG,"My cam dis is " + distanceFromCam + " accurate:" + accurateDistanceFromCam);
                            if (distanceFromCam  > Options.MIN_MOVE_DISTANCE_METERS + location.getAccuracy()) {
                                targetPos.target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                            }
                        }
                    }

                    CameraUpdate newCam = CameraUpdateFactory.newCameraPosition(targetPos.build());
                    if (!newCam.equals(originalCameraPosition)) {
                        if (Options.ANIMATE_MOVES) {
                            mapSwipeToZoomFragment.animateCamera(newCam);
                        } else {
                            mapSwipeToZoomFragment.moveCamera(newCam);
                        }
                    }
                }
            } finally {
                mPreviousLocation = location;
            }
        }// end on Location Changed

        /** Only accurate for short distances (don't use great circles! or any other stuff)
         *
         * @param latitude
         * @param longitude
         * @param latitude1
         * @param longitude1
         * @return
         */
        static final double TORAD = Math.PI / 180.0;
        static final int REarthMeters = 6371 * 1000;
        private float shortDistanceBetween(double latitude1, double longitude1, double latitude2, double longitude2) {
            double λ1 = latitude1 * TORAD;
            double λ2 = latitude2 * TORAD;
            double φ1 = longitude1* TORAD;
            double φ2 = longitude2* TORAD;
            double Rmeters = 6371 * 1000;

            double x = (λ2-λ1) * Math.cos((φ1+φ2)/2);
            double y = (φ2-φ1);
            double d = Math.sqrt(x*x + y*y) * Rmeters;
            return (float) d;
        }


        private float haversineDistanceBetween(double lat1, double lon1, double lat2, double lon2) {
            double φ1 = lat1 * TORAD;
            double φ2 = lat2* TORAD;
            double Δφ = (lat2-lat1)* TORAD;
            double Δλ = (lon2-lon1)* TORAD;

            double  a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                    Math.cos(φ1) * Math.cos(φ2) *
                            Math.sin(Δλ/2) * Math.sin(Δλ/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

            return (float) (REarthMeters * c);
        }
    }


