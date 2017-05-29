package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import uk.me.ponies.wearroutes.controller.Stats;

AcceptableLocationStrategyVelocityAdjust extends  AcceptableLocationStrategySimpleAccuracy {
    AcceptableLocationStrategyVelocityAdjust(float acceptableAccuracy) {
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
    public boolean isAcceptableLocation(@NonNull Location location, String src) {
        if(super.isAcceptableLocation(location, src)) {
            // if it's acceptable quite simply
            return true;
        }
        // see if a velocity adjustment helps
        if (location.hasSpeed()) {
            if (location.getAccuracy() - (location.getSpeed()/3.0) < super.getAccuracy() ) {
                return true;
            }
        }
        Stats.addUnacceptableLocation();
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
