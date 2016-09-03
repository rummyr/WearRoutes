package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by rummy on 02/09/2016.
 */
public class AcceptableLocationStrategyVelocityAdjust extends  AcceptableLocationStrategySimpleAccuracy {
    public AcceptableLocationStrategyVelocityAdjust(float acceptableAccuracy) {
        super(acceptableAccuracy);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void newPollStarting() {
        super.newPollStarting();
    }

    @Override
    public boolean isAcceptableLocation(@NonNull Location location) {
        if(super.isAcceptableLocation(location)) {
            // if it's acceptable quite simply
            return true;
        }
        // see if a velocity adjustment helps
        if (location.hasSpeed()) {
            if (location.getAccuracy() - (location.getSpeed()/3.0) < super.getAccuracy() ) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Location getNearlyAcceptableLocation() {
        return super.getNearlyAcceptableLocation();
    }

    @Override
    public void setAccuracy(float minAllowedAccuracyRecording) {
        super.setAccuracy(minAllowedAccuracyRecording);
    }
}
