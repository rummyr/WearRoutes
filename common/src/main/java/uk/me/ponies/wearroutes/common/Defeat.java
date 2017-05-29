package uk.me.ponies.wearroutes.common;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

/**
 * Stupid class to hack around some of the code inspections that I really don't want to actually disable
 * Created by rummy on 08/08/2016.
 */
public class Defeat {
    public static boolean TRUE() {
        return 1 == 1;
    }
    public static boolean FALSE() {
        return 1 == 0;
    }

    @SuppressWarnings("UnusedParameters")
    public static void noop(Object o) {}
    public static void noop() {}

    public static Object NULL() {
        if (Defeat.TRUE()) {
            return null;
        }
        else {
            return null;
        }
    }
}
