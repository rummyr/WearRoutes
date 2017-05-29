package uk.me.ponies.wearroutes.managehistory;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.common.GPXFileParser;
import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.keys.WearUIKeys;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class HistoricalRoutesUtils {

    private static String TAG = HistoricalRoutesUtils.class.getSimpleName();

    private HistoricalRoutesUtils() {
    }

    public static List<StoredRoute> readHistoricalRoutes(Context context, File storedRoutesDirectory) {
        List<StoredRoute> rv = new ArrayList<>();

        String[] storedRouteFileList = storedRoutesDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".gpx");
            }
        });
        for (String storedRouteName : storedRouteFileList) {
            File routeFile = new File(storedRoutesDirectory, storedRouteName);
            try {
                StoredRoute route = new StoredRoute(storedRouteName, new Date(routeFile.lastModified()), "");
                List<LatLng> routePoints = GPXFileParser.decodeGPX(routeFile);
                route.setTFile(routeFile);
                if (routePoints.isEmpty()) {
                    routePoints = new ArrayList<>();
                    routePoints.add(new LatLng(0,0));
                }
                route.setPoints(routePoints);

                boolean isHidden = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(WearUIKeys.HIDE_PREFIX + route.getName(), false);
                route.setTHidden(isHidden);
                if (tagEnabled(TAG)) Log.d(TAG, "Read " + route);
                rv.add(route);

            }
            finally {
                Defeat.noop();
            }
        }
        return rv;
    }
}


