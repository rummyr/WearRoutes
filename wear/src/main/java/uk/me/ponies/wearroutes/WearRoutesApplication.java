package uk.me.ponies.wearroutes;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import uk.me.ponies.wearroutes.locationService.LocationPollingService;
import uk.me.ponies.wearroutes.utils.ActivityLifeCycleLogger;

/**
 * Used to provide an uncaught exception handler and other "global" like stuff
 */
public class WearRoutesApplication extends Application {
        Intent locationServiceIntent;
    private Thread.UncaughtExceptionHandler mDefaultUEHander;
    private static final String TAG = "WearRoutesApplication";
    ActivityLifeCycleLogger mActivityLifecycleLogger;

    public void onCreate ()
        {
            if (mActivityLifecycleLogger  == null) {
                mActivityLifecycleLogger = new ActivityLifeCycleLogger();
                registerActivityLifecycleCallbacks(mActivityLifecycleLogger);
            } else {
                Log.e(TAG, "onCreate called and lifecycle logger already registered!");
            }


            mDefaultUEHander = Thread.getDefaultUncaughtExceptionHandler();
            // Setup handler for uncaught exceptions.
            Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
            {
                @Override
                public void uncaughtException (Thread thread, Throwable e)
                {
                    handleUncaughtException (thread, e);
                }
            });

            locationServiceIntent = new Intent(this, LocationPollingService.class);
            startService(locationServiceIntent);
            super.onCreate();
        }

        @Override
        public void onTerminate() {
            stopService(locationServiceIntent);
            if (mActivityLifecycleLogger != null) {
                unregisterActivityLifecycleCallbacks(mActivityLifecycleLogger);
                mActivityLifecycleLogger = null;
            }
            else {
                Log.e(TAG, "onTerminate called and no activityLifecycleLogger set");
            }
            super.onTerminate();
        }

        public void handleUncaughtException (Thread thread, Throwable e)
        {
            try {
                SimpleDateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                Calendar calendar = Calendar.getInstance();
                String filename = "crash." + filenameFormatter.format(calendar.getTime()) + ".txt";
                PrintWriter out = new PrintWriter(Environment.getExternalStorageDirectory().getPath()
                        + "/wearRoutes/"
                        + filename);
                e.printStackTrace(out); // not all Android versions will print the stack trace automatically
                out.close();
            } catch (IOException ioe) {
                // drop it .. we're dead anyway!
            }
            if (mDefaultUEHander != null) {
                mDefaultUEHander.uncaughtException(thread, e);
            }
            else {
                System.exit(1); // kill off the crashed app
            }
        }
    }

