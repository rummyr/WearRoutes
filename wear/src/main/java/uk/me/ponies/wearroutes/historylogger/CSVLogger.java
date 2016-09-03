package uk.me.ponies.wearroutes.historylogger;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Created by rummy on 30/07/2016.
 */

// TODO: only write on a timed interval
public class CSVLogger {
    private String TAG = getClass().getSimpleName();
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);
    private boolean mIsLogging = false;
    private final File mBaseDir;
    private File mLogFile;
    private final SimpleDateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final String CSVHEADER = "Time, Latitude, Longitude, Accuracy, Speed, Bearing, Provider"
            +",Satellites, hdop, vdop, pdop, geoidHeight, ageOfGPSData, dgpsid";


    public CSVLogger(File baseDir) {
        this.mBaseDir = baseDir;
    }



    public boolean isLogging() {
        return mIsLogging;
    }

    public void setLogging(boolean logging) {
        if (logging && !mIsLogging) { // starting to log
            startLogging();
        }
        else if (!logging && mIsLogging) { // stopping logging
            stopLogging();
        }


        this.mIsLogging = logging;
    }


    private void startLogging() {
        Calendar calendar = Calendar.getInstance();
        String logFileName;
        synchronized (dateFormatter) {
            logFileName = "wearRoutes." + filenameFormatter.format(calendar.getTime()) + ".history.csv";
        }
        mLogFile = new File(mBaseDir, logFileName);
        try {
            PrintWriter out = new PrintWriter(new FileWriter(mLogFile, true)); // true = append
            out.println(CSVHEADER);
            out.close();
        }
        catch (IOException ioe) {
            Log.e(TAG, "Exception writing csv header!" + ioe);
        }
    }

    private void stopLogging() {
        //TODO: is anything needed when we shutdown logging to a csv file?
    }






    @Subscribe
    public void newLocation(LocationEvent locationEvent) {
        if (!mIsLogging) {
            return;
        }
        Location l = locationEvent.getLocation();
        if (l == null) {
            return;
        }

        Bundle b = l.getExtras();
        String satellites = "";
        String hdop = "";
        String vdop = "";
        String pdop = "";
        String geoidHeight = "";
        String ageOfGPSData = "";
        String dgpsid = "";

        if (b != null) {
            satellites = b.getString("satellites", "");
            if ("".equals(satellites)) {
                satellites = b.getString("SATELLITES_FIX", "");
            }

            hdop = zeroIsEmpty(b.getString("HDOP"));
            vdop = zeroIsEmpty(b.getString("VDOP"));
            pdop = zeroIsEmpty(b.getString("PDOP"));
            geoidHeight = zeroIsEmpty(b.getString("GEOIDHEIGHT"));
            ageOfGPSData = zeroIsEmpty(b.getString("AGEOFDGPSDATA"));
            dgpsid = zeroIsEmpty(b.getString("DGPSID"));
        }

        String line;
        synchronized (dateFormatter) {
            line = String.format(Locale.UK, "\"%s\",%f,%f,%f,%f,%f,\"%s\"" + "%s, %s, %s, %s, %s, %s, \"%s\""
                    , dateFormatter.format(l.getTime())
                    ,l.getLatitude()
                    ,l.getLongitude()
                    ,l.getAccuracy()
                    ,l.getSpeed()
                    ,l.getBearing()
                    ,l.getProvider()
                    // next block
                    ,satellites
                    , hdop
                    , vdop
                    ,pdop
                    ,geoidHeight
                    ,ageOfGPSData
                    ,dgpsid
                    );
            }
        try {
            PrintWriter out = new PrintWriter(new FileWriter(mLogFile, true)); // true = append
            out.println(line);
            if (tagEnabled(TAG))Log.d(TAG, "Written " + line);
            out.close();
        } catch (IOException ioe) {
            //TODO: what do we want to do with a failed write?
            Log.e("CSVLogger", "failed to log " + line);
        }

    }

    @NonNull
    private static String zeroIsEmpty(String value) {
        if ("0".equals(value)
                || "".equals(value)
                || null == value) {
            return "";
        }
        else return String.valueOf(value);
    }
}
