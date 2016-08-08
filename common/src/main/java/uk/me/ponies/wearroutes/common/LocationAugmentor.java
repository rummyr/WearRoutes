package uk.me.ponies.wearroutes.common;

import android.location.Location;
import android.util.Log;
import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * A class that adds speed and Bearing (if required) to a location
 */
public class LocationAugmentor {
    private static final String TAG = LocationAugmentor.class.getSimpleName();
    /** Only for the addSpeedAndBearing code */
    private Location mPreviousLocation;


    /**
     *
     * @param location the location to AUGMENT
     * @return ORIGINAL location with speed and bearing added if required and possible
     */
    public Location addSpeedAndBearing(Location location) {
        // augment the location if required
        if (!location.hasBearing() && mPreviousLocation != null) {
            // simulate a bearing
            if (tagEnabled(TAG)) Log.d(TAG, "Need to simulate a bearing");

            if (mPreviousLocation != null) {
                double bearingDegrees = mPreviousLocation.bearingTo(location);
                location.setBearing((float) bearingDegrees);
            }

        }
        if (!location.hasSpeed() && mPreviousLocation != null) {
            // simulate a speed
            if (tagEnabled(TAG)) Log.d(TAG, "Need to simulate a speed");

            double distance = 0.0;
            if (mPreviousLocation != null) {
                distance = mPreviousLocation.distanceTo(location);
            }

            double duration = location.getElapsedRealtimeNanos() - mPreviousLocation.getElapsedRealtimeNanos();
            double speed = distance / duration;
            location.setSpeed((float) speed);
        }
        mPreviousLocation = location;
        return location;
    }
}
