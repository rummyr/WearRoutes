package uk.me.ponies.wearroutes.historylogger;

import android.location.Location;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import uk.me.ponies.wearroutes.eventBusEvents.FlushLogsEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LogEvent;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Logs to an internal backlog and writes to a GPX file on an EventBus FlushLogsEvent
 */

public class SimpleTextLogger {
    private final static String TAG = "SimpleTextLogger";
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);
    private boolean mIsLogging = false;
    private final File mBaseDir;
    private final String mBaseName;

    private File mLogFile;
    private final SimpleDateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final SimpleDateFormat ISODateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final List<LogEvent> backlog = new ArrayList<>();

    // possibly add in metadata e.g.
    // <metadata>
    // <name>2015-04-19-08-53-40</name>
    // <desc></desc>
    // <author>
    //    <name>TrackLogs Digital Mapping</name>
    //    <email domain="tracklogs.co.uk" id="support"/>
    //    <link href="www.tracklogs.co.uk"/>
    // </author>
    // <time>2015-05-16T15:39:18Z</time>
    // <bounds maxlat="53.7627669706316" maxlon="-1.39222627477149" minlat="53.7406749190526" minlon="-1.47154661312931"/>
    // </metadata>
    // <trk><name>2015-04-19-08-53-40</name><desc></desc><trkseg>

    public SimpleTextLogger(File baseDir, String baseName) {
        this.mBaseDir = baseDir;
        this.mBaseName = baseName;
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
            logFileName = mBaseName + "." + filenameFormatter.format(calendar.getTime()) + ".txt";
        }
        mLogFile = new File(mBaseDir, logFileName);
        try {
            RandomAccessFile out = new RandomAccessFile(mLogFile,"rw");
            out.writeBytes("Logging Started: " + dateFormatter.format(calendar.getTime()) + "\n");
            out.close();
        }
        catch (IOException ioe) {
            Log.e(TAG, "Exception writing SimpleTextLogger header!" + ioe);
        }
    }

    private void stopLogging() {
        // force a flush (we should get one anyway, but I like to be sure!
        flushBackLog(new FlushLogsEvent());
    }


    @Subscribe
    public void onLogEvent(LogEvent logEvent) {
        if (!mIsLogging) {
            return;
        }

        synchronized (backlog) {
            backlog.add(logEvent);
        }
    }

    /* takes a copy of the backlog and writes them out */
    @Subscribe
    public void flushBackLog(FlushLogsEvent marker) {
        List<LogEvent> copyOfBackLog;
        synchronized (backlog) {
            if (backlog.isEmpty()) {
                return;
            }
            copyOfBackLog = new ArrayList<>(backlog);
            backlog.clear();
        }
        writeLocations(copyOfBackLog);
    }

    private void writeLocations(List<LogEvent> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        RandomAccessFile out;
        try {
            out = new RandomAccessFile(mLogFile, "rw");
            out.seek(out.length()); // seek to end

        } catch (IOException ioe) {
            Log.e(TAG, "IOException trying to seek to near end of output." + ioe);
            return; // nothing more we can do!
        }

        for (LogEvent l : lines) {
            String line;
            synchronized (dateFormatter) {
                line = String.format("%s: %s: %s\n"
                        , dateFormatter.format(new Date(l.getTimeMs()))
                        , l.getType()
                        , l.getLine()
                );
            }
            try {
                out.writeBytes(line);
            } catch (IOException ioe) {
                //TODO: what do we want to do with a failed write?
                Log.e(TAG, "failed to log " + line);
            }
        }
        try {
            out.close();
        } catch (IOException ioe) {
            //TODO: what do we want to do with a failed write?
            Log.e(TAG, "IOException trying to write gpx end tag to output gpx." + ioe);
            return; // nothing more we can do!
        }
    } // end writeLocations

}
