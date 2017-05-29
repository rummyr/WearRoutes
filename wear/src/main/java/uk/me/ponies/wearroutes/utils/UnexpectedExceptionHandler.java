package uk.me.ponies.wearroutes.utils;

import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Custom exception logging
 */
public class UnexpectedExceptionHandler implements Thread.UncaughtExceptionHandler {
    static String TAG = "UnexpectedExceptionHdlr";
    private static Thread.UncaughtExceptionHandler androidDefaultUEH;
    private static UnexpectedExceptionHandler singleton;

    public static synchronized void register() {
        Log.i(TAG, "registering unexpectedException Handler on thread" + Thread.currentThread().getName());
        androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        if (singleton == null) {
            singleton = new UnexpectedExceptionHandler();
        }
        Thread.setDefaultUncaughtExceptionHandler(singleton);
    }

    public static synchronized void unregister() {
        Log.i(TAG, "UNRegistering unexpectedException Handler on thread" + Thread.currentThread().getName());
        if (androidDefaultUEH != null) {
            Thread.setDefaultUncaughtExceptionHandler(null);
            androidDefaultUEH = null;
        }
        singleton = null;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e(TAG, "Exception thrown:" + ex);
        SimpleDateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Calendar calendar = Calendar.getInstance();
        try {
            String filename = filenameFormatter.format(calendar.getTime()) + ".exception.txt";
            PrintWriter out = new PrintWriter(Environment.getExternalStorageDirectory().getPath()
                    + "/wearRoutes/"
                    + filename);
            ex.printStackTrace(out); // not all Android versions will print the stack trace automatically
            out.close();
        } catch (IOException ioe) {
            // drop it .. we're dead anyway!
        }

        Log.w("before","Logcat save");
        // File logFile = new File( + "/log.txt" );
        try {
            // Process process = Runtime.getRuntime().exec("logcat -d");
            String filename = Environment.getExternalStorageDirectory().getPath()
                    + "/wearRoutes/"
                    + filenameFormatter.format(calendar.getTime()) + ".logcat.txt";
            Runtime.getRuntime().exec( "logcat -f " + filename);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        androidDefaultUEH.uncaughtException(thread,ex);
    }
}
