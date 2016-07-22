package uk.me.ponies.wearroutes;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.kml.KmlContainer;
import com.google.maps.android.kml.KmlGeometry;
import com.google.maps.android.kml.KmlLayer;
import com.google.maps.android.kml.KmlPlacemark;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Async tasks that loads a GPX and adds it to the map as a polyline
 */
    public class LoadKMLAndAddToMap extends AsyncTask<Integer, Void, Integer> {
    private static final float ROUTE_WIDTH = 5; // smaller than the overly fat default
    private static final int ROUTE_COLOR = Color.RED;
    GoogleMap map; // cleaned up in postExecute
        /** Context is needed for the ContentResolver */
        Context context;

    public LoadKMLAndAddToMap(Context context, GoogleMap map) {
        this.context = context;
        this.map = map;
    }

    private static final String TAG = "RouteFollowerLoadKML";
        public Integer doInBackground(Integer  ... ids) {
            return doInBackground(ids[0]);
        }

        public Integer doInBackground(Integer id) {
             return id;
        }


        /* Update the map, needs to be done on main thread, hence here */
        public void onPostExecute(Integer  id) {
            KmlLayer layer;
            try {
                layer = new KmlLayer(map, id,
                        context);
                layer.addLayerToMap();
                //TODO: zoom in?
                //Calculate the markers to get their position
                LatLngBounds.Builder b = new LatLngBounds.Builder();
                for (KmlContainer c : layer.getContainers()) {
                    for (KmlPlacemark p : c.getPlacemarks()){
                        KmlGeometry g = p.getGeometry();
                        if(g.getGeometryType().equals("Point")) {
                            LatLng point = (LatLng) g.getGeometryObject();
                            b.include(point);
                        }
                    }
                }
                LatLngBounds bounds = b.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 25); // padding
                map.animateCamera(cu);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                map = null; // remove potentially unwanted reference (though in truth unlikely to be required)
                context = null;
            }
        }





    }




