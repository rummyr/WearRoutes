package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Accepts *all* locations
 */
class AcceptableLocationStrategyAll implements IAcceptableLocationStrategy {
    @Override
    public void destroy() {
    }

    @Override
    public void newPollStarting() {
    }

    @Override
    public boolean isAcceptableLocation(@NonNull Location location, String src) {
        return true;
    }

    @Nullable
    @Override
    public Location getNearlyAcceptableLocation() {
        return null;
    }

    @Override
    public void setAccuracy(float minAllowedAccuracyRecording) {
        // do nothing
    }
}
