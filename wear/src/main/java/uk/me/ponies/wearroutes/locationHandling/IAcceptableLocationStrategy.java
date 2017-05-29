package uk.me.ponies.wearroutes.locationHandling;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Interface for the is Acceptable Location strategies.
 */
public interface IAcceptableLocationStrategy {
    /** Called when system needs to cleanup.*/
    void destroy();
    /** called when a new poll is starting -- reset transient internal state */
    void newPollStarting();
    /** Check to see if location is acceptable */
    boolean isAcceptableLocation(@NonNull Location location, String source);
    /** Get the best *nearly* acceptable location so far */
    @Nullable Location getNearlyAcceptableLocation();

    /** accuracy has been adjusted, not expected to adjust nearlyAcceptableLocation.*/
    void setAccuracy(float minAllowedAccuracyRecording);
}
