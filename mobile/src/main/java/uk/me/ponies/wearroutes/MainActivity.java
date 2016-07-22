package uk.me.ponies.wearroutes;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.utils.ContentProviderUtils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , OnMapReadyCallback {

    static final String TAG = "MainActivityMobile";
    GoogleApiClient mGoogleApiClient;

    /** a pointer to the map */
    private GoogleMap mMap;

    @SuppressWarnings("FieldCanBeLocal")
    private TrackLocationAndBearingOnMap mMapLocationAndBearingTracker; // here to stop garbage collection


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setEnabled(false); // becomes enabled when the map is loaded
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // should have a built in file picker
                        fileChooserSelect(FileChooserType.BUILTIN);
                    }
                    else {
                        // go for the space cowboy, even though it has pink folders!
                        fileChooserSelect(FileChooserType.SpaceCowboyExplicit);
                    }
                    /*
                    Intent intent = new Intent()
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*" + "/*") // I wonder file/*?
                            .setAction(Intent.ACTION_GET_CONTENT
                                    //TODO: intent.addCategory(Intent.CATEGORY_OPENABLE); ??
                            );
                    */

                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.setDrawerListener(toggle); // add Drawer lister came in API 24
        }
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        doPermissionsCheck(this);
        setupDataAPI();



    }

    private void setupDataAPI() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API

                        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION);
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


                            /*--------------- Check for the watch being connected ------- */
                            // check for connected nodes
                            new WatchConnectedUtils(mGoogleApiClient)
                                    .isWatchConnectedAsync(new CheckNodesConnectedToaster());

                        }
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })

                // Request access only to the Wearable API
                //.addApiIfAvailable(Wearable.API)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();

        ContentProviderUtils.debugGetProviderIcons(getApplicationContext());
    }



    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //TODO: if (!mResolvingError) {
        //TODO: onStart or onResume
            mGoogleApiClient.connect();
        //}
    }

    @Override
    protected void onStop() {
        //TODO: if (!mResolvingError) {
            if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            //TODO: Wearable.DataApi.removeListener(mGoogleApiClient, this);
            // Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            // TODO: Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidMaint id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.action_settings: {
                startActivity(new Intent(getApplicationContext(), SettingsActivity2.class));
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {


        Log.d(TAG, "NavigationItemSelected");
        if (item.getItemId() == R.id.nav_view_settings) {
            startActivity(new Intent(this, SettingsActivity2.class));
        }
        else if (item.getItemId() == R.id.nav_manage) {
            //TODO: Manage tracks
            Snackbar.make(this.getCurrentFocus(), "Manage Tracks TBD", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
        else if (item.getItemId() == R.id.nav_import) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // should have a built in file picker
                fileChooserSelect(FileChooserType.BUILTIN);
            }
            else {
                // go for the space cowboy, even though it has pink folders!
                fileChooserSelect(FileChooserType.SpaceCowboyExplicit);
            }
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);



        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        else {
            Log.e(TAG, "Cant access FINE Location - Permission Denied");
        }

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        Log.d(TAG, "Got Activity Result - requestCode:" + requestCode + " resultCode:" + resultCode + " intent " + data);
        if (resultCode != RESULT_OK) {
            return; // no file selected?
        }

        Uri uri = data.getData();
        ContentResolver cR = getContentResolver();
        Log.d(TAG, "Got Activity Result - mimeType:" + cR.getType(uri));

        if (requestCode == 123 && resultCode == RESULT_OK) {
            // looking good!
            Uri chosenItem = data.getData();

            if (chosenItem.getPath().endsWith(".gpx")
                    || "application/gpx+xml".equals(cR.getType(uri)) //NOTE:Google Drive
                    || "text/xml".equals(cR.getType(uri)) //TODO: is this really valid? from google drive
                    ) {
                new LoadGPXAndAddToMap(this, mMap, mGoogleApiClient).execute(chosenItem);

            }

        }
    }

    private void doPermissionsCheck(Context context) {
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException nfe) {
            Log.e(TAG, "Couldnt find our own permissions to check in doPermissionsCheck!");
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
                Log.d(TAG, permission + " Granted");
            } else {
                //TODO: MUST INFORM USER!
                Log.e(TAG, permission + " Denied!!");
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        permission)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{permission},
                            234);

                } else {
                    Log.d(TAG, permission + " is a safe Permission");
                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{permission},
                            234);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }


            }
        }
    }

    /*
    //NOTE: * /* + ACTION_OPEN_DOCUMENT on my slimkat 4.4.4 it doesn't show the internal filesystem!
    //NOTE: while it does on API 23 emulator
    //NOTE: file/* + ACTION_OPEN_DOCUMENT on slimkat 4.4.4 only shows Drive and all are grey!

    //NOTE: * /* + ACTION_GET_CONTENT includes "Simple Explorer" in the view
    // Once I triggered SimpleExplorer, internal storage showed up
    // Filter to only show results that can be "opened", such as a file (as opposed to a list
    // of contacts or timezones)

    startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
    */
    private void fileChooserSelect(FileChooserType pFileChooser) {
        switch(pFileChooser) {
/*
            case AFileChooser: {
                // ipaulpro File Chooser version

                Intent intent = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent();
                intent.setType("file/*"); // * /* includes Movies and Music (and probably other stuff)
                startActivityForResult(intent, 123);
                break;
            }
*/
            case GenericSamsung: {
                //http://stackoverflow.com/questions/8945531/pick-any-kind-file-via-an-intent-on-android
                String minmeType = "*/*";
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(minmeType);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // special intent for Samsung file manager
                Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
                // if you want any file type, you can skip next line
                sIntent.putExtra("CONTENT_TYPE", minmeType);
                sIntent.addCategory(Intent.CATEGORY_DEFAULT);

                Intent chooserIntent;
                if (getPackageManager().resolveActivity(sIntent, 0) != null){
                    // it is device with samsung file manager
                    chooserIntent = Intent.createChooser(sIntent, "Open file");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
                }
                else {
                    chooserIntent = Intent.createChooser(intent, "Open file");
                }

                try {
                    startActivityForResult(chooserIntent, 123);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
/*
            case SpaceCowboyExplicit: {
                // This always works
                Intent i = new Intent(MainMobileActivity.this, FilePickerActivity.class); // even under 23 uses FilePicker
                // This works if you defined the intent filter
                // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                // Set these depending on your use case. These are the defaults.
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                // Configure initial directory by specifying a String.
                // You could specify a String like "/storage/emulated/0/", but that can
                // dangerous. Always use Android's API calls to get paths to the SD-card or
                // internal memory.
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

                startActivityForResult(i, 123);
                break;
            }
*/
/*
            case SpaceCowboyImplict: {
                // This always works
                // Intent i = new Intent(this, FilePickerActivity.class); // even under 23 uses FilePicker
                // This works if you defined the intent filter
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                // Set these depending on your use case. These are the defaults.
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                // Configure initial directory by specifying a String.
                // You could specify a String like "/storage/emulated/0/", but that can
                // dangerous. Always use Android's API calls to get paths to the SD-card or
                // internal memory.
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

                startActivityForResult(i, 123);
                break;
            }
*/
            case BUILTIN: {
                Intent intent = new Intent()
                        .setType("*" + "/*") // I wonder file/*?
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
                break;
            }
            default: {
                Log.e(TAG, "FileChooserType is unknown " + pFileChooser);
                break;
            }
        }
    }

    private class CheckNodesConnectedToaster implements WatchConnectedUtils.NodesListingCallback {
        public void availableNodes(final List<Node> pNodesFound) {
            // run on main thread so we can Toast
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CheckNodesConnectedToaster.this.toastNodeCount(pNodesFound);
                }
            });
        }
        public void toastNodeCount(List<Node>pNodesFound) {

            if (pNodesFound == null) {

            }
            else if (pNodesFound.size() > 0) {
                Toast.makeText(MainActivity.this, "There was at least one wearable found (" + pNodesFound.size() + ")", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(MainActivity.this, "There are no wearables found", Toast.LENGTH_LONG).show();

            }
        }
    }
}
