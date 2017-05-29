package uk.me.ponies.wearroutes.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.LoggingEventBus;

import uk.me.ponies.wearroutes.MainWearActivity;
import uk.me.ponies.wearroutes.Options;

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

    public ActivityLifeCycleLogger() {
        Log.d(TAG, "created");
    }
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivityCreated called");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivityDestroyed called");
        if (Options.DEVELOPER_MODE && activity.getClass() == MainWearActivity.class) {
            LoggingEventBus leb = (LoggingEventBus)(EventBus.getDefault());
            leb.dump();
            SingleInstanceChecker.dumpRetainedReferences();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivityResumed called");

    }

    @Override
    public void onActivityPaused(Activity activity) {
        ++paused;
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivityPaused called");

        Log.w("test", "application is in foreground: " + (resumed > paused));
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivitySaveInstance called");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivityStarted called");

    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        Log.w("test", "application is visible: " + (started > stopped));
        if (tagEnabled(TAG))Log.d(TAG, activity + " onActivityStopped called");

    }
}
