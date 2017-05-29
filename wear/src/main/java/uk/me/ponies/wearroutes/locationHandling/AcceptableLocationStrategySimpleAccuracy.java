package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.common.locationUtils.Utils;
import uk.me.ponies.wearroutes.controller.Controller;


class AcceptableLocationStrategySimpleAccuracy implements IAcceptableLocationStrategy{
    private float minAllowedAccuracyRecording;
    private final String TAG = "SimpleLocStrategy";
    private Location prevLocation;


    AcceptableLocationStrategySimpleAccuracy(float acceptableAccuracy) {
        super();
        setAccuracy(acceptableAccuracy);
    }

    @Override
    public void destroy() {
        prevLocation = null;
    }

    @Override
    public void newPollStarting() {
        prevLocation = null;
    }

    @Override
    public boolean isAcceptableLocation(@NonNull Location location, String src) {


        try { // finally to set prevLocation wherever we return from

            if (Controller.getInstance() != null // some odd cases can get here
            && Controller.getInstance().isRecording()) {
                // just too inaccurate
                if (location.getAccuracy() > minAllowedAccuracyRecording) {
                    Log.w(TAG, "Location seen with poor accuracy " + location.getAccuracy() + " from " + src);
                    return false;
                }
                // no altitude data (usually means phone mast or wifi)
                // BUT we will accept if it's fairly accurate
                if (location.getAltitude() == 0.0000 && !location.isFromMockProvider()) {
                    // this could be a poor reading isNullAndLog that it is *fairly accurate* aka < 2/3 the MIN_ALLOWED_ACCURACY_METERS_RECORDING
                    if (location.getAccuracy() * 1.5 >= minAllowedAccuracyRecording) {
                        Log.w(TAG, "Location seen with zero elevation, not mocked and more than half the normal acceptable" + " from " + src);
                        return false;
                    }
                    if (prevLocation == null) {
                        // no previous location, use it, best we have.
                        return true;
                    }
                    // passable accuracy, is it actually far enough from the previous one
                    float distanceFromPrev = Utils.haversineDistanceBetween(location.getLatitude(), location.getLongitude(),
                            prevLocation.getLatitude(), prevLocation.getLongitude());
                    if (distanceFromPrev <= 4 * location.getAccuracy()) {
                        // moderately far from previous location, lets use it!
                        return true;
                    } else {
                        Log.w(TAG, "Location seen with zero elevation, not mocked, just OK on stricter accuracy check, but too close to previous" + " from " + src);
                        return false;
                    }
                }
                if (prevLocation != null && location.getTime() == prevLocation.getTime()) {
                    Log.w(TAG, "Location seen with duplicate timestamp"+ " from " + src);
                    return false;
                }
            } else {
                // not recording, we can be more lenient, accept no altitude and poorer accuracy
                if (location.getAccuracy() > Options.MIN_ALLOWED_ACCURACY_METERS_NOT_RECORDING) {
                    Log.w(TAG, "Location seen with poor accuracy " + location.getAccuracy()+ " from " + src);
                    return false;
                }
                // we also accept no elevation data when NOT recording
                // and also duplicate timestamps
            }

            return true;
        } finally {
                prevLocation = null;
        }
    }

    @Nullable
    @Override
    public Location getNearlyAcceptableLocation() {
        return null;
    }

    @Override
    public void setAccuracy(float minAllowedAccuracyRecording) {
        this.minAllowedAccuracyRecording = minAllowedAccuracyRecording;
    }

    /** protected because I don't see why anyone else needs to know. */
    protected float getAccuracy() {
        return this.minAllowedAccuracyRecording;
    }
}
