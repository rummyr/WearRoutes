package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by rummy on 02/09/2016.
 */
public interface IAcceptableLocationStrategy {
    /** Called when system needs to cleanup.*/
    public void destroy();
    /** called when a new poll is starting -- reset transient internal state */
    public void newPollStarting();
    /** Check to see if location is acceptable */
    public boolean isAcceptableLocation(@NonNull Location location);
    /** Get the best *nearly* acceptable location so far */
    @Nullable public Location getNearlyAcceptableLocation();

    /** accuracy has been adjusted, not expected to adjust nearlyAcceptableLocation.*/
    void setAccuracy(float minAllowedAccuracyRecording);
}
