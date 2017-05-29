package uk.me.ponies.wearroutes.mainactionpages.unused;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.CustomUrlTileProvider;
import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.common.Defeat;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

public class MapsActivity extends WearableActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener

        ,GoogleApiClient.ConnectionCallbacks
        ,GoogleApiClient.OnConnectionFailedListener
         ,DataApi.DataListener
    ,CapabilityApi.CapabilityListener
 {

    private static String TAG = "RouteDisplayGMaps";
    /**
     * Overlay that shows a short help text when first launched. It also provides an option to
     * exit the app.
     */
    private DismissOverlayView mDismissOverlay;
    private GoogleApiClient mGoogleApiClient;
    @SuppressWarnings("FieldCanBeLocal")
    private TrackLocationAndBearingOnMap mMapLocationAndBearingTracker; // here to shutdown garbage collection


     /**
     * The map. It is initialized when the map has been fully loaded and is ready to be used.
     *
     * @see #onMapReady(com.google.android.gms.maps.GoogleMap)
     */
    private GoogleMap mMap;
     /* Needs to be a field, so onEnter/LeaveAmbient can do appropriate tweaks to the map. */
     private MapFragment mapFragment;

     public void onCreate(Bundle savedState) {

        super.onCreate(savedState);

        // Set the layout. It only contains a MapFragment and a DismissOverlay.
        setContentView(R.layout.activity_maps);

        // Enable ambient support, so the map remains visible in simplified, low-color display
        // when the user is no longer actively using the app but the app is still visible on the
        // watch face.
        setAmbientEnabled();

        // Retrieve the containers for the root of the layout and the map. Margins will need to be
        // set on them to account for the system window insets.
        final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.root_container);
        final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

        if (true) {
            // Set the system view insets on the containers when they become available.
            topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    // Call through to super implementation and apply insets
                    insets = topFrameLayout.onApplyWindowInsets(insets);

                    FrameLayout.LayoutParams mapFrameLayoutMarginParams =
                            (FrameLayout.LayoutParams) mapFrameLayout.getLayoutParams();

                    // Add Wearable insets to FrameLayout container holding map as margins
                    mapFrameLayoutMarginParams.setMargins(
                            // adjusting this to +100 shows the map in the right half of the screen
                            insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                    mapFrameLayout.setLayoutParams(mapFrameLayoutMarginParams);

                    return insets;
                }
            });

        }


        // Obtain the DismissOverlayView and display the introductory help text.
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.intro_text);
        mDismissOverlay.showIntroIfNecessary();

            // Obtain the MapFragment and set the async listener to be notified when the map is ready.
         mapFragment =  (MapFragment) getFragmentManager().findFragmentById(R.id.map);
         mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        doPermissionsCheck(this);
    }


     /**
      * Starts ambient mode on the map.
      * The API swaps to a non-interactive and low-color rendering of the map when the user is no
      * longer actively using the app.
      */
     @Override
     public void onEnterAmbient(Bundle ambientDetails) {
         Log.i(TAG, "onEnterAmbient");
         super.onEnterAmbient(ambientDetails);
         mapFragment.onEnterAmbient(ambientDetails);
         if (ambientDetails.getBoolean(EXTRA_LOWBIT_AMBIENT)) {
             Defeat.noop();
             //TODO: should disable anti aliasing on text fields
             //mStateTextView.setTextColor(Color.WHITE);
             //mStateTextView.getPaint().setAntiAlias(false);

             //TODO: should lower map update frequency (depending on speed?)
         }
     }

     /**
      * Exits ambient mode on the map.
      * The API swaps to the normal rendering of the map when the user starts actively using the app.
      */
     @Override
     public void onExitAmbient() {
         Log.i(TAG, "onExitAmbient");
         super.onExitAmbient();
         mapFragment.onExitAmbient();

        // should probably enable anti aliasing
     }


     @Override
     protected void onResume() {
         super.onResume();
         mGoogleApiClient.connect();
     }

     @Override
     protected void onPause() {
         super.onPause();
         if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
             Wearable.DataApi.removeListener(mGoogleApiClient, this);
             //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
             Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
             mGoogleApiClient.disconnect();
         }

     }

     @Override
     public void onConnected(Bundle connectionHint) {
         if (tagEnabled(TAG)) Log.d(TAG, "onConnected(): Successfully connected to Google API client");
         Wearable.DataApi.addListener(mGoogleApiClient, this);
         //Wearable.MessageApi.addListener(mGoogleApiClient, this);
         Wearable.CapabilityApi.addListener(
                 mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);

         int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
         if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
             // Create the LocationRequest object
             LocationRequest locationRequest = LocationRequest.create();
             // Use high accuracy
             locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
             // Set the update interval to 2 seconds
             locationRequest.setInterval(TimeUnit.SECONDS.toMillis(2));
             // Set the fastest update interval to 2 seconds
             locationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(2));
             // Set the minimum displacement
             locationRequest.setSmallestDisplacement(2);

             mMapLocationAndBearingTracker = new TrackLocationAndBearingOnMap(mMap);
             LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mMapLocationAndBearingTracker);
         }
     }

     @Override
     public void onConnectionSuspended(int cause) {
         if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
     }

     @Override
     public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
         Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
     }


     @Override
     public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
         if (tagEnabled(TAG)) Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
         /// mDataFragment.appendItem("onCapabilityChanged", capabilityInfo.toString());
     }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Map is ready to be used.
        mMap = googleMap;





        Log.i(TAG,"ZoomControlsEnabled is " + mMap.getUiSettings().isZoomControlsEnabled());
        Log.i(TAG,"RotateGesturesEnabled is " + mMap.getUiSettings().isRotateGesturesEnabled());
        Log.i(TAG,"CompassEnabled is " + mMap.getUiSettings().isCompassEnabled());
        Log.i(TAG,"ZoomGestures is " + mMap.getUiSettings().isZoomGesturesEnabled());
        Log.i(TAG,"ScrollGestures is " + mMap.getUiSettings().isScrollGesturesEnabled());
        Log.i(TAG,"TiltGestures is " + mMap.getUiSettings().isTiltGesturesEnabled());
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(false); // swipe functions work if this is disabled!

        // mMap.getUiSettings().setMyLocationButtonEnabled(true); // strangely doesn't actually show the button

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        Log.i(TAG,"ZoomControlsEnabled is " + mMap.getUiSettings().isZoomControlsEnabled());
        Log.i(TAG,"RotateGesturesEnabled is " + mMap.getUiSettings().isRotateGesturesEnabled());
        Log.i(TAG,"CompassEnabled is " + mMap.getUiSettings().isCompassEnabled());



        // Set the long click listener as a way to exit the map.
        mMap.setOnMapLongClickListener(this);

        // Add a marker in Sydney, Australia and move the camera.
        LatLng london = new LatLng(51.5074, 0.1278);
        mMap.addMarker(new MarkerOptions().position(london).title("Marker in London"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(london));

        //TODO: Temporary Tile overlay!
        if (false){
            // see http://gis.stackexchange.com/questions/104325/how-to-use-custom-map-tiles-with-the-google-map-api-v2-for-android
            // and source https://github.com/CUTR-at-USF/OpenTripPlanner-for-Android/blob/master/opentripplanner-android/src/main/res/values/donottranslate.xml
            String overlayString  = "http://a.tile.opencyclemap.org/cycle/{z}/{x}/{y}.png";
            int mMaxZoomLevel = 18; // Cyclemap max zoon
            int tile_width = 256;
            int tile_height = 256;
            int CUSTOM_MAP_TILE_Z_INDEX = -1;
            mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
            CustomUrlTileProvider mTileProvider = new CustomUrlTileProvider(
                    tile_width,
                    tile_height, overlayString);
            TileOverlay mSelectedTileOverlay = mMap.addTileOverlay(
                    new TileOverlayOptions().tileProvider(mTileProvider)
                            .zIndex(CUSTOM_MAP_TILE_Z_INDEX));
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // Display the dismiss overlay with a button to exit this activity.
        mDismissOverlay.show();
    }


    private void setupDataAPI() {
        if (tagEnabled(TAG)) Log.d(TAG, "setupDataAPI called");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        if (tagEnabled(TAG)) Log.d(TAG, "onConnected: " + connectionHint);
                        Wearable.DataApi.addListener(mGoogleApiClient, MapsActivity.this);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        if (tagEnabled(TAG)) Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                //.addApiIfAvailable(Wearable.API)
                .addApi(Wearable.API)
                .build();
    }

     //@Override
     public void onDataChanged(DataEventBuffer dataEvents) {
         if (tagEnabled(TAG)) Log.d(TAG, "onDataChanged(): " + dataEvents);

         for (DataEvent event : dataEvents) {
             String path = event.getDataItem().getUri().getPath();
             if (tagEnabled(TAG)) Log.d(TAG, "DataItem URI is" + event.getDataItem().getUri());
             if (event.getType() == DataEvent.TYPE_CHANGED) {
                 if (tagEnabled(TAG)) Log.d(TAG, "Received a Data Changed Event for path:" + path);
                 if ("/RouteDisplayGMapsWear/addRoute".equals(path)) {
                     // unpack the message
                     DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                     byte[] marshalledData = dataMapItem.getDataMap()
                             .getByteArray("LatLngList");
                     final Parcel parcel = Parcel.obtain();
                     parcel.unmarshall(marshalledData,0,marshalledData.length);
                     //TODO: is this really important, apparently it is
                     // TODO: STOP abusing parcel
                     parcel.setDataPosition(0); // this is extremely important!
                     List<LatLng> myList = new ArrayList<>();
                     parcel.readList(myList,this.getClassLoader());
                     parcel.recycle();
                     if (tagEnabled(TAG)) Log.d(TAG,"Received " + myList.size() + " points");

                     PolylineOptions rectOptions = new PolylineOptions().addAll(myList).geodesic(false);
                     rectOptions.color(Color.RED); // TODO: make configurable
                     rectOptions.width(5); // TODO: make configurable
                     //Polyline polyline =
                     mMap.addPolyline(rectOptions);
                     //Calculate the markers to get their position
                     LatLngBounds.Builder b = new LatLngBounds.Builder();
                     for (LatLng ll : myList) {
                         b.include(ll);
                     }
                     LatLngBounds bounds = b.build();
                     //Change the padding as per needed
                     CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, Options.OPTIONS_ROUTE_ZOOM_PADDING); // padding
                     mMap.animateCamera(cu);
                 }
                 else {
                     Log.w(TAG,"Unrecognised path:" + path);
                 }
             } else if (event.getType() == DataEvent.TYPE_DELETED) {
                 if (tagEnabled(TAG)) Log.d(TAG, "Received a Data Deleted Event for path:" + path);
             } else {
                 if (tagEnabled(TAG)) Log.d(TAG, "Received an unknown Data Event for path:" + path);
             }
         }
     }


     private void doPermissionsCheck(Context context) {
         PackageInfo info;
         try {
             info = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
         } catch (PackageManager.NameNotFoundException nfe) {
             Log.e(TAG, "Couldn't find our own permissions to check in doPermissionsCheck!");
             return; // !! Should never happen!
         }
         if (info == null) {
             Log.e(TAG, "Our permissions is null in doPermissionsCheck!");
             return;
         }

         for (String permission : info.requestedPermissions) {
             int permissionCheck = ContextCompat.checkSelfPermission(this,
                     permission);
             if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 if (tagEnabled(TAG)) Log.d(TAG, permission + " Granted");
             } else {
                 //TODO: MUST INFORM USER!
                 Log.e(TAG, permission + " Denied!!");
                 if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                         permission)) {

                     // Show an explanation to the user *asynchronously* -- don't block
                     // this thread waiting for the user's response! After the user
                     // sees the explanation, try again to request the permission.

                     ActivityCompat.requestPermissions(this,
                             new String[]{permission},
                             1234);

                 } else {
                     if (tagEnabled(TAG)) Log.d(TAG, permission + " is a safe Permission");
                     // No explanation needed, we can request the permission.

                     ActivityCompat.requestPermissions(this,
                             new String[]{permission},
                             1234);

                     // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                     // app-defined int constant. The callback method gets the
                     // result of the request.
                 }


             }
         }
     }
 }
