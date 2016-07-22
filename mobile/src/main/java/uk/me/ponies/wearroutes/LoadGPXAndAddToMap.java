package uk.me.ponies.wearroutes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.maps.android.PolyUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import uk.me.ponies.wearroutes.tracksimplifaction.SimplifyV2;
import uk.me.ponies.wearroutes.common.DataKeys;

/**
 * Async tasks that loads a GPX and adds it to the map as a polyline
 */
public class LoadGPXAndAddToMap extends AsyncTask<Uri, Void, LoadGPXAndAddToMap.LoadGPXResult> {
    private static final float ROUTE_WIDTH = 5; // smaller than the overly fat default
    private static final int ROUTE_COLOR = Color.RED;


    GoogleMap map; // cleaned up in postExecute
    /**
     * Context is needed for the ContentResolver
     */
    AppCompatActivity context;
    /**
     * WTF is this doing in here!!!!
     */
    GoogleApiClient apiClient;
    /**
     * Created to try to work around NPE when creating/showing dialog
     */
    private AlertDialog.Builder mDialog;
    private static String TAG = "LoadGPXAndAddToMap";

    public LoadGPXAndAddToMap(AppCompatActivity context, GoogleMap map, GoogleApiClient apiClient) {
        this.context = context;
        this.map = map;
        this.apiClient = apiClient;
        mDialog = new AlertDialog.Builder(context);
    }

    public LoadGPXResult doInBackground(Uri... uris) {
        return doOneInBackground(uris[0]);
    }

    public LoadGPXResult doOneInBackground(Uri uri) {

        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            Cursor returnCursor = context.getContentResolver().query(uri,null,null,null,null);
            /*
                 * Get the column indexes of the data in the Cursor,
                 * move to the first row in the Cursor, get the data,
                 * and display it.
                 */
            final String fileName;
            final long fileSize;
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
                fileName = (returnCursor.getString(nameIndex));
                fileSize = returnCursor.getLong(sizeIndex);

                for (int i=0;i<returnCursor.getColumnCount();i++) {
                    int type = returnCursor.getType(i);
                    switch (type) {

                    }

                }
                returnCursor.close();
            }
            else if ("file".equals(uri.getScheme()) && "".equals(uri.getAuthority())){
                fileName = uri.getLastPathSegment();
                fileSize = new File(uri.getPath()).length(); // returnCursor.getLong(sizeIndex);
            }
            else {
                fileName = uri.getLastPathSegment();
                fileSize = -1; // returnCursor.getLong(sizeIndex);
            }
            String lastPastSegment = uri.getLastPathSegment();
            String schema = uri.getScheme();
            String authority = uri.getAuthority();
            String.valueOf(fileSize + schema + authority + fileName+ lastPastSegment);


            // for file in download (Emulator API ??): content://com.android.externalstorage.documents/document/primary%3ADownload%2Fbridleways_north_america(sic)_wgs84.gpx
            // for file in download (GenyMotion 4.4.4 API 19) selected via "File Manager" file:///storage/emulated/0/Download/rothwell-20km-Casper-Gem-2015-04-19.gpx
            // NOTE: file:/// does NOT resolve via ContentResolver
            // for google drive: content://com.google.android.apps.docs.storage/document/acc%3D1%3Bdoc%3D1352

            List<LatLng> locations = GPXFileParser.decodeGPX(in);
            locations.size();
            LoadGPXResult rv = new LoadGPXResult();
            rv.gpxPoints = locations;
            rv.name = fileName;
            return rv;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    /* Update the map, needs to be done on main thread, hence here */
    public void onPostExecute(LoadGPXResult result) {
        if (result == null) {
            return;
        }
        try {
            List<LatLng> srcPoints = result.gpxPoints;
            // Instantiates a new Polyline object and adds points to define a rectangle
            PolylineOptions rectOptions = new PolylineOptions().addAll(srcPoints);
            rectOptions.color(ROUTE_COLOR);
            rectOptions.width(ROUTE_WIDTH);
            //Polyline polyline =
            map.addPolyline(rectOptions);


            //Calculate the markers to get their position
            LatLngBounds.Builder b = new LatLngBounds.Builder();
            for (LatLng ll : srcPoints) {
                b.include(ll);
            }
            LatLngBounds bounds = b.build();
            //Change the padding as per needed
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 25); // padding
            map.animateCamera(cu);


            // also send to Watch
            //FIXME: MUST do this in a different class/method/mechanism!
            // need to either convert to multiple pairs of float and add and add again
            // or more simply putall X and putall Y

            Log.e(TAG, "WTF! We're sending stuff to the watch from here!!!");
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/WearRoutes/addRoute");
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putLong(DataKeys.DATA_KEY_TIME_SENT, System.currentTimeMillis());
            putDataMapReq.getDataMap().putString(DataKeys.DATA_KEY_NAME, result.name);
            putDataMapReq.getDataMap().putLong(DataKeys.DATA_KEY_DATE_RECEIVED,new Date().getTime());

            String encodedPolyLine = PolyUtil.encode(srcPoints);
            String simplifiedEncodedPolyLine = null;
            if (true) {
                if (encodedPolyLine.length() > 50000) {
                    // longer than 50k, simplify it
                    long start = System.currentTimeMillis();

                    int estimagedRequiredPoints = (int) ((srcPoints.size() * 50000.0) / encodedPolyLine.length());
                    List<LatLng> simplified = SimplifyV2.simplify(srcPoints, 0.000001, estimagedRequiredPoints); // 0.00001 halved the number of points in bridleways_north_america(sic)_wgs84.gpx
                    long end = System.currentTimeMillis();
                    Log.d(TAG, "Simplify took " + (end - start) + "millis");
                    PolylineOptions simplifiedPolyOpts = new PolylineOptions().addAll(simplified);
                    simplifiedPolyOpts.color(Color.BLUE);
                    simplifiedPolyOpts.width(1);
                    simplifiedPolyOpts.geodesic(false); // not doing LONG lines!
                    //Polyline polyline =
                    map.addPolyline(simplifiedPolyOpts);
                    Log.d(TAG, "Original has " + srcPoints.size() + "Points, and simplified has " + simplified.size() + " points");
                    simplifiedEncodedPolyLine = PolyUtil.encode(simplified);
                    putDataMapReq.getDataMap().putString(DataKeys.DATA_KEY_ENCODED_POLYLINE_STR, simplifiedEncodedPolyLine);
                }
                else {
                    putDataMapReq.getDataMap().putString(DataKeys.DATA_KEY_ENCODED_POLYLINE_STR, encodedPolyLine);
                }
            }
            else {
                Parcel parcel = Parcel.obtain();
                parcel.writeList(srcPoints);
                byte[] marshalledData = parcel.marshall();
                parcel.recycle(); // as required

                if (marshalledData.length > 100000) { // approx 100k?
                    // and add a "simplified" version for testing
                    //TODO: probably remove the simplified overlay
                    //  List<LatLng> simplified = SimplifyV1.simplify(srcPoints, 0.000001); // 0.00001 halved the number of points in bridleways_north_america(sic)_wgs84.gpx
                    long start = System.currentTimeMillis();

                    int estimagedRequiredPoints = (int) ((srcPoints.size() * 90000.0) / marshalledData.length);
                    List<LatLng> simplified = SimplifyV2.simplify(srcPoints, 0.000001, estimagedRequiredPoints); // 0.00001 halved the number of points in bridleways_north_america(sic)_wgs84.gpx
                    long end = System.currentTimeMillis();
                    Log.d(TAG, "Simplify took " + (end - start) + "millis");
                    PolylineOptions simplifiedPolyOpts = new PolylineOptions().addAll(simplified);
                    simplifiedPolyOpts.color(Color.BLUE);
                    simplifiedPolyOpts.width(1);
                    //Polyline polyline =
                    map.addPolyline(simplifiedPolyOpts);
                    Log.d(TAG, "Original has " + srcPoints.size() + "Points, and simplified has " + simplified.size() + " points");

                    parcel = Parcel.obtain();
                    parcel.writeList(simplified);
                    byte[] simplifiedMarshalledData = parcel.marshall();
                    parcel.recycle(); // as required

                    Log.d(TAG, "original marshalled to " + marshalledData.length + "bytes");
                    Log.d(TAG, "simplified marshalled to " + simplifiedMarshalledData.length + "bytes");
                    Log.d(TAG, "PolyLine Encoded original encodes to " + PolyUtil.encode(srcPoints).length() + " chars");
                    Log.d(TAG, "PolyLine Encoded simplified encodes to " + PolyUtil.encode(simplified).length() + " chars");

                    marshalledData = simplifiedMarshalledData; // replace the original data with the smaller version
                    putDataMapReq.getDataMap().putByteArray("LatLngList", marshalledData);
                } else {
                    Log.d(TAG, "Original is <10k (" + marshalledData.length + ") and has " + srcPoints.size() + "Points");
                }
            }






            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            //TODO: handle Large data, small data transfers OK, but big stuff ~100k! doesnt!
            //TODO: we could compress?
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(apiClient, putDataReq);
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {

                @Override
                public void onResult(@NonNull DataApi.DataItemResult result) {
                    Log.d(TAG, "putDataItem: resultCallback is " + result.toString() + " status: " + result.getStatus());
                    if (!result.getStatus().isSuccess()) {
                        // now new'd in constructor new AlertDialog.Builder(context)
                        mDialog
                                .setTitle(android.R.string.dialog_alert_title)
                                .setMessage(result.getStatus().getStatusMessage())
                                .setCancelable(true)
                                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //TODO: do we need to dismiss the dialog?
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .create()
                                .show();

                    }
                    mDialog = null; // NOW we can clear this after the onResult has been called
                }
            });
            Log.e(TAG, "MUST Check result of the resultCallBack somehow");

        } finally {
            map = null; // remove potentially unwanted reference (though in truth unlikely to be required)
            context = null;
            // mDialog =null; can't remove here .. or it won't be available on the result callback

        }
    }

    static class LoadGPXResult {
        private String name;
        private List<LatLng> gpxPoints;
    }
}




