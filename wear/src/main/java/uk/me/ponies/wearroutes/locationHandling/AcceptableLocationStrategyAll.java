package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by rummy on 02/09/2016.
 */
public class AcceptableLocationStrategyAll implements IAcceptableLocationStrategy {
    @Override
    public void destroy() {
    }

    @Override
    public void newPollStarting() {
    }

    @Override
    public boolean isAcceptableLocation(@NonNull Location location) {
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
