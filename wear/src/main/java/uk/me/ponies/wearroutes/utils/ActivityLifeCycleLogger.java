package uk.me.ponies.wearroutes.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Simple class just to log activity lifecycle changes
 */
public class ActivityLifeCycleLogger implements Application.ActivityLifecycleCallbacks {
    // I use four separate variables here. You can, of course, just use two and
    // increment/decrement them instead of using four and incrementing them all.
    private int resumed;
    private int paused;
    private int started;
    private int stopped;
    private static final String TAG = "ActivityLifeCycleLogger";

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivityCreated called");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivityDestroyed called");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivityResumed called");

    }

    @Override
    public void onActivityPaused(Activity activity) {
        ++paused;
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivityPaused called");

        Log.w("test", "application is in foreground: " + (resumed > paused));
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivitySaveInstance called");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivityStarted called");

    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        Log.w("test", "application is visible: " + (started > stopped));
        if (tagEnabled(TAG))Log.d(TAG, activity.getLocalClassName() + " onActivityStopped called");

    }
}
