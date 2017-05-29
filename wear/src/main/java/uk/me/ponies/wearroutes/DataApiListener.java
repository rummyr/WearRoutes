package uk.me.ponies.wearroutes;

import android.graphics.Color;
import android.os.Parcel;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

import uk.me.ponies.wearroutes.common.DataKeys;
import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Communication between Phone and Watch
 */
class DataApiListener implements DataApi.DataListener {
    private static final String TAG = "DataApiListener";
    private final WeakReference<MapSwipeToZoomFragment> mFragmentRef;
    private final File storageDirectory;
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);


    public DataApiListener(MapSwipeToZoomFragment mapFragment, File storageDirectory) {
        mFragmentRef = new WeakReference<>(mapFragment);
        this.storageDirectory = storageDirectory;
    }

    //@Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (tagEnabled(TAG)) Log.d(TAG, "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            String path = event.getDataItem().getUri().getPath();
            if (tagEnabled(TAG)) Log.d(TAG, "DataItem URI is" + event.getDataItem().getUri());
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (tagEnabled(TAG)) Log.d(TAG, "Received a Data Changed Event for path:" + path);
                if ("/WearRoutes/addRoute".equals(path)) {


                    // unpack the message
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    String name = dataMapItem.getDataMap()
                            .getString(DataKeys.DATA_KEY_NAME);

                    long dateReceived = dataMapItem.getDataMap().getLong(DataKeys.DATA_KEY_DATE_RECEIVED);
                    final Date receivedDate;
                    if (dateReceived <= 0) {
                        receivedDate = new Date();
                    } else {
                        receivedDate = new Date(dateReceived);
                    }

                    String polyLineStr;
                    if (true) {
                        polyLineStr = dataMapItem.getDataMap().getString(DataKeys.DATA_KEY_ENCODED_POLYLINE_STR);
                    } else {
                        byte[] marshalledData = dataMapItem.getDataMap()
                                .getByteArray("LatLngList");
                        final Parcel parcel = Parcel.obtain();
                        parcel.unmarshall(marshalledData, 0, marshalledData.length);
                        //TODO: is this really important, apparently it is
                        // TODO: STOP abusing parcel
                        parcel.setDataPosition(0); // this is extremely important!
                        ArrayList<LatLng> myList = new ArrayList<>();
                        parcel.readList(myList, this.getClass().getClassLoader());
                        parcel.recycle();
                        polyLineStr = PolyUtil.encode(myList);

                        if (tagEnabled(TAG)) Log.d(TAG, "Received " + name + " With " + myList.size() + " points");
                    }


                    //TODO: persist to cache?
                    // String routeStorageDirectory = getFilesDir();
                    String baseName;
                    if (name.endsWith(".gpx")) {
                        baseName = name.substring(0, name.lastIndexOf('.')) + ".route";
                    } else {
                        baseName = name + ".route";
                    }

                    File routeFilename = new File(storageDirectory, baseName);
                    StoredRoute data = new StoredRoute(name, receivedDate, polyLineStr);


                    try {
                        FileWriter writer = new FileWriter(routeFilename);
                        writer.write(data.toJSON().toString());
                        writer.close();
                        StoredRoute looped = StoredRoute.fromJSON(data.toJSON().toString());
                        Defeat.noop(looped);
                        //ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(routeFilename));
                        //out.writeObject(data);
                        //out.close();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Unable to save " + name + " because " + ioe);
                        /*TODO: show a dialog
                        mDialog
                                .setTitle(android.R.string.dialog_alert_title)
                                .setMessage("Problems saving Route: baseName")
                                .setCancelable(true)
                                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //TODO: do we need to dismiss the dialog?
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .create()
                                .show();
                       */
                    } catch (JSONException jse) {
                        Log.e(TAG, "Writing json failed because:" + jse);
                    }

                    PolylineOptions rectOptions = new PolylineOptions().geodesic(false).addAll(data.getPoints());
                    rectOptions.color(Color.RED); // TODO: make configurable
                    rectOptions.width(5); // TODO: make configurable

                    //Change the padding as per needed

                    MapSwipeToZoomFragment mapFragmentContainer = mFragmentRef.get();
                    if (mapFragmentContainer == null) {
                        Log.e(TAG, "mapFragmentContainer has been garbage collected!");
                    } else {
                        mapFragmentContainer.addPolyline(rectOptions, name, data.getBounds(), true); // true = and zoom to
                    }
                } else {
                    Log.w(TAG, "Unrecognised path:" + path);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                if (tagEnabled(TAG)) Log.d(TAG, "Received a Data Deleted Event for path:" + path);
            } else {
                if (tagEnabled(TAG)) Log.d(TAG, "Received an unknown Data Event for path:" + path);
            }
        }
    }
}
