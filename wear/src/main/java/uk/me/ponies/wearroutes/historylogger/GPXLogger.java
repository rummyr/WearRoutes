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
import java.util.List;

import uk.me.ponies.wearroutes.eventBusEvents.FlushLogsEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Logs to an internal backlog and writes to a GPX file on an EventBus FlushLogsEvent
 */

public class GPXLogger {
    private final static String TAG = "GPXLogger";
    private boolean mIsLogging = false;
    private final File mBaseDir;
    private File mLogFile;
    private final SimpleDateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final SimpleDateFormat ISODateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final String GPXHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"\n"
            + " version=\"1.1\"\n"
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"\n"
            + " creator=\"wearRoutes\"\n>\n"
            ;
    private static final String TRK_FOOTER = "</trkseg></trk></gpx>\n";


    private final List<Location> backlog = new ArrayList<>();

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

    public GPXLogger(File baseDir) {
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
            logFileName = "wearRoutes." + filenameFormatter.format(calendar.getTime()) + ".track.gpx";
        }
        mLogFile = new File(mBaseDir, logFileName);
        try {
            RandomAccessFile out = new RandomAccessFile(mLogFile,"rw");
            out.writeBytes(GPXHEADER);
            out.writeBytes("<trk><name>"+filenameFormatter.format(calendar.getTime()) +"</name><desc></desc>\n");
            out.writeBytes("<trkseg>\n");
            out.writeBytes(TRK_FOOTER);
            out.close();
        }
        catch (IOException ioe) {
            Log.e(TAG, "Exception writing gpx header!" + ioe);
        }
    }

    private void stopLogging() {
        // force a flush (we should get one anyway, but I like to be sure!
        flushBackLog(new FlushLogsEvent());
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
        synchronized (backlog) {
            backlog.add(locationEvent.getLocation());
        }
    }

    /* takes a copy of the backlog and writes them out */
    @Subscribe
    public void flushBackLog(FlushLogsEvent marker) {
        List<Location> copyOfBackLog;
        synchronized (backlog) {
            if (backlog.isEmpty()) {
                return;
            }
            copyOfBackLog = new ArrayList<>(backlog);
            backlog.clear();
        }
        writeLocations(copyOfBackLog);
    }

    private void writeLocations(List<Location> locations) {
        if (locations == null || locations.isEmpty()) {
            return;
        }

        RandomAccessFile out;
        try {
            out = new RandomAccessFile(mLogFile, "rw");
            out.seek(out.length() - TRK_FOOTER.length()); // seek to before the footer
        } catch (IOException ioe) {
            Log.e(TAG, "IOException trying to seek to near end of output gpx." + ioe);
            return; // nothing more we can do!
        }

        for (Location l : locations) {
            String line;
            synchronized (ISODateFormatter) {
                line = String.format("<trkpt lat=\"%f\" lon=\"%f\"><ele>%f</ele><time>%s</time><hdop>%.2f</hdop><course>%.1f</course><speed>%.2f</speed></trkpt>\n"
                        , l.getLatitude()
                        , l.getLongitude()
                        , l.getAltitude()
                        , ISODateFormatter.format(l.getTime()),
                        l.getAccuracy() / 4.0, // CLUDGE, was 5, but accuracy seemed to be a minimum of 4m (not 5)
                        l.getBearing(),
                        l.getSpeed()
                );
            }
            try {
                out.writeBytes(line);
                if (tagEnabled(TAG))Log.d(TAG, "Written " + line);
            } catch (IOException ioe) {
                //TODO: what do we want to do with a failed write?
                Log.e(TAG, "failed to log " + line);
            }
        }
        try {
            out.writeBytes(TRK_FOOTER);
            out.close();
        } catch (IOException ioe) {
            //TODO: what do we want to do with a failed write?
            Log.e(TAG, "IOException trying to write gpx end tag to output gpx." + ioe);
            return; // nothing more we can do!
        }
    } // end writeLocations

}
