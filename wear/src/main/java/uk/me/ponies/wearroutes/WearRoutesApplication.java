package uk.me.ponies.wearroutes;

import android.app.Application;
import android.os.Environment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by rummy on 02/09/2016.
 */
public class WearRoutesApplication extends Application {
    public class MyApplication extends Application
    {
        public void onCreate ()
        {
            // Setup handler for uncaught exceptions.
            Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
            {
                @Override
                public void uncaughtException (Thread thread, Throwable e)
                {
                    handleUncaughtException (thread, e);
                }
            });
            super.onCreate();
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

            System.exit(1); // kill off the crashed app
        }
    }
}
