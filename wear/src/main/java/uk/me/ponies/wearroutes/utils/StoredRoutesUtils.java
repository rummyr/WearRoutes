package uk.me.ponies.wearroutes.utils;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.me.ponies.wearroutes.keys.WearUIKeys;
import uk.me.ponies.wearroutes.common.StoredRoute;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


/**
 * Created by rummy on 04/07/2016.
 */
public class StoredRoutesUtils {
    private static String TAG = StoredRoutesUtils.class.getSimpleName();

    private StoredRoutesUtils() {}

    public static List<StoredRoute> readStoredRoutes(Context context, File storedRoutesDirectory) {
        List<StoredRoute> rv = new ArrayList<>();

        String[] storedRouteFileList = storedRoutesDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".route");
            }
        });
        for (String storedRouteName : storedRouteFileList) {
            File routeFile = new File(storedRoutesDirectory, storedRouteName);
            try {
                BufferedReader r = new BufferedReader(new FileReader(routeFile));
                String line;
                StringBuffer sb = new StringBuffer((int) routeFile.length());
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
                r.close();
                StoredRoute route = StoredRoute.fromJSON(sb.toString());
                route.setTFile(routeFile);

                boolean isHidden = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(WearUIKeys.HIDE_PREFIX + route.getName(),false);
                route.setTHidden(isHidden);
                if (tagEnabled(TAG))Log.d(TAG, "Read " + route);
                rv.add(route);

            } catch (IOException ioe) {
                Log.e(TAG, "Failed to read " + storedRouteName + "IOE" + ioe);
            } catch (JSONException jse) {
                Log.e(TAG, "Failed to read " + storedRouteName + "JSE" + jse);
            }
        }
        return rv;
    }
}


