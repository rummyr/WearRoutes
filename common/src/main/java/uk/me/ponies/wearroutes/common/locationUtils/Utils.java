package uk.me.ponies.wearroutes.common.locationUtils;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by rummy on 08/08/2016.
 */
public class Utils {
    private static final double TORAD = Math.PI / 180.0;
    private static final int REarthMeters = 6371 * 1000;

    /** Hidden constructor for utility method class */
    private Utils(){}

    /** Only accurate for short distances (don't use great circles! or any other stuff)
     *
     * @param latitude1
     * @param longitude1
     * @param latitude2
     * @param longitude2
     * @return
     */
    public static float shortDistanceBetween(double latitude1, double longitude1, double latitude2, double longitude2) {
        double λ1 = latitude1 * TORAD;
        double λ2 = latitude2 * TORAD;
        double φ1 = longitude1* TORAD;
        double φ2 = longitude2* TORAD;
        double Rmeters = 6371 * 1000;

        double x = (λ2-λ1) * Math.cos((φ1+φ2)/2);
        double y = (φ2-φ1);
        double d = Math.sqrt(x*x + y*y) * Rmeters;
        return (float) d;
    }


    public static float haversineDistanceBetween(LatLng ll1, LatLng ll2) {
        return haversineDistanceBetween(ll1.latitude, ll1.longitude, ll2.latitude, ll2.longitude);
    }

    public static float haversineDistanceBetween(double lat1, double lon1, double lat2, double lon2) {
        double φ1 = lat1 * TORAD;
        double φ2 = lat2* TORAD;
        double Δφ = (lat2-lat1)* TORAD;
        double Δλ = (lon2-lon1)* TORAD;

        double  a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (float) (REarthMeters * c);
    }
}
