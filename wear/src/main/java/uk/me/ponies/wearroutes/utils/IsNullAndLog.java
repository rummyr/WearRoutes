package uk.me.ponies.wearroutes.utils;

import android.util.Log;

/**
 * Logging utility
 */
public abstract class IsNullAndLog {
    public static boolean isNullAndLog(String tag, String thing, Object o) {
        if (o == null) {
            Log.w(tag, "Object " + thing + " is unexpectedly null");
        }
        return (o == null);
    }

    public static void logNull(String tag, String thing, Object o) {
        if (o == null) {
            Log.w(tag, new Exception("Object " + thing + " is unexpectedly null"));
        }
    }
}
