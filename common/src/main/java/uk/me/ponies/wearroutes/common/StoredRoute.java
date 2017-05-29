package uk.me.ponies.wearroutes.common;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rummy on 04/07/2016.
 */
public class StoredRoute implements Serializable{
    private final String name;
    private final Date receivedDate;
    private final String polyLineStr;
    private transient List<LatLng> tPoints; // don't want this storing, we can recreate it from the polyLineStr if needed
    private transient LatLngBounds tBounds;
    private transient boolean tHidden = false; // by default it's shown

    //TODO: public fields! what a pile of CRAP!
    private transient File tFile;

    public StoredRoute(String name, Date receivedDate, String polyLineStr) {
        this.name = name;
        this.receivedDate = receivedDate;
        this.polyLineStr = polyLineStr;
    }

    public JSONObject toJSON() throws JSONException {

        JSONObject json = new JSONObject();
        json.put(DataKeys.DATA_KEY_NAME, name);
        json.put(DataKeys.DATA_KEY_DATE_RECEIVED, receivedDate.getTime());
        json.put(DataKeys.DATA_KEY_ENCODED_POLYLINE_STR, polyLineStr);
        return json;
    }

    public static StoredRoute fromJSON(String s) throws JSONException {
        JSONObject json = new JSONObject(s);
        Date receivedDate = new Date(json.getLong(DataKeys.DATA_KEY_DATE_RECEIVED));
        String polyLineStr = json.getString(DataKeys.DATA_KEY_ENCODED_POLYLINE_STR);
        String name = json.getString(DataKeys.DATA_KEY_NAME);
        return new StoredRoute(name, receivedDate, polyLineStr);
    }

    private JSONObject toJSON(LatLngBounds bounds) throws JSONException {
        JSONObject rv = new JSONObject();
        rv.put("northeast",toJSON(bounds.northeast));
        rv.put("southwest",toJSON(bounds.southwest));
        return rv;
    }
    private static LatLngBounds fromJSONLatLngBounds(JSONObject json) throws  JSONException{
        return new LatLngBounds(fromJSONLatLng(json.getJSONObject("southwest")),
                fromJSONLatLng(json.getJSONObject("northeast")));
    }

    private JSONArray toJSON(List<LatLng>points) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i=0;i<points.size();i++) {
                array.put(i, toJSON(points.get(i)));
        }
        return array;
    }
    private static List<LatLng> fromJSONLatLonArray(JSONArray json) throws JSONException {
        ArrayList<LatLng> rv = new ArrayList<>();
        int size = json.length();
        for (int i=0;i<size;i++) {
            LatLng ll = fromJSONLatLng(json.getJSONObject(i));
            rv.add(ll);
        }
        return rv;
    }

    private JSONObject toJSON(LatLng ll) throws  JSONException{
        JSONObject rv = new JSONObject();
        rv.put("latitude", ll.latitude);
        rv.put("longitude", ll.longitude);
        return rv;
    }

    private static LatLng fromJSONLatLng(JSONObject json) throws  JSONException{
        return new LatLng(json.getDouble("latitude"), json.getDouble("longitude"));
    }
    

    public String getName() {
        return name;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public int getNumPoints() {
        return getPoints().size();
    }

    public List<LatLng> getPoints() {
        if (tPoints == null) {
            tPoints = PolyUtil.decode(polyLineStr);
        }
        return tPoints;
    }
    public void setPoints(List<LatLng> points) {
        tPoints = points;
    }
    /** returns the LatLngBounds, generating on the fly if required */
    public LatLngBounds getBounds() {
        if (tBounds == null) {
            LatLngBounds.Builder b = new LatLngBounds.Builder();
            for (LatLng ll : getPoints()) {
                b.include(ll);
            }
            tBounds = b.build();
        }
        return tBounds;
    }

    public File getTFile() {
        return tFile;
    }
    public void setTFile(File file) {
        tFile = file;
    }


    public void setTHidden(boolean THidden) {
        this.tHidden = THidden;
    }

    public boolean getTHidden() {
        return tHidden;
    }
}
