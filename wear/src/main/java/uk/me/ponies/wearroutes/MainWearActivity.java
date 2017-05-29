/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.me.ponies.wearroutes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.LoggingEventBus;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.ControllerState;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.historylogger.LogEvent;
import uk.me.ponies.wearroutes.historylogger.SimpleTextLogger;
import uk.me.ponies.wearroutes.locationService.LocationPollingService;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;
import uk.me.ponies.wearroutes.utils.StoredRoutesUtils;
import uk.me.ponies.wearroutes.utils.UnexpectedExceptionHandler;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class MainWearActivity extends WearableActivity {
    private static final String TAG = "MainWearActivity";

    private static boolean staticComponentsActive = false;

    /** A class to hold references to objects that are <b>only</b> held to shutdown Garbage Collection.
     * makes freeing off a single = null call. */
    private static class GCStoppers {
        private static final String TAG = "GCStoppers";
        Set<Object> pin = Collections.synchronizedSet(new HashSet<>());

        public void add(Object o) {
            if (pin.contains(o)) {
                Log.w(TAG, "adding same object to GCStopper:" + o.getClass().getName() + "/" + o);
            }
            pin.add(o);
        }

        public void clear() {
            pin.clear();
            connectionCallBacksHandler = null;
            connectionFailedListener = null;

        }

        private ConnectionCallBacksHandler connectionCallBacksHandler;
        private ConnectionFailedListener connectionFailedListener;
    }

    private GCStoppers gcPreventer;
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);


    private DataApi.DataListener mDataApiListener;
    private CapabilityApi.CapabilityListener mCapabilityApiListener;
    private MainGridPagerAdapter sampleGridPagerAdapter;
    private GoogleApiClient mGoogleApiClient;
    private Controller mController;

    private SimpleTextLogger mSimpleTextLogger;

    private LocationPollingService mLocationPollingService;


    //BUG: multiple instances are launched? Or perhaps just MULTIPLE instances of existing components!

    @Override
    protected void onDestroy() {



        mGoogleApiClient.unregisterConnectionCallbacks(gcPreventer.connectionCallBacksHandler);
        mGoogleApiClient.unregisterConnectionFailedListener(gcPreventer.connectionFailedListener);
        mGoogleApiClient.disconnect();

        gcPreventer.clear(); // probably not required.
        gcPreventer = null;
        super.onDestroy();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }




    @Override
    /* One example of where this is called is when the StopRecording yes/no page pops up */
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("SomethingToPutInSavedInstanceState", true);
    }

    @Override
    public void onStateNotSaved() {
        super.onStateNotSaved();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "restarted");
        EventBus.getDefault().post(new LogEvent("Application Re-Started","PWR"));
        super.onRestart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeStaticComponent();

        final Thread currentThread = Thread.currentThread();
        Thread backgroundMonitor = new Thread() {
            public void run() {
                try {
                    Log.d("MainThreadMonitor","\n\n\n\n\n\n\n\n");
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        Thread.sleep(10);
                        State state = currentThread.getState();
                        Log.d("MainThreadMonitor", "in state:" + state);
                        StackTraceElement[] stack = currentThread.getStackTrace();
                        StringBuilder sb = new StringBuilder(1000);
                        for (int i=0;i<100 && i<stack.length;i++) {
                            sb.append(stack[i]).append("\n");
                        }
                        Log.d("MainThreadMonitor", "executing:" + sb);
                    }
                } catch (InterruptedException ie) {
                    // just stop
                }
            }
        };
        Defeat.noop(backgroundMonitor);
        //backgroundMonitor.start();

        Log.e(TAG, "onCreate called savedinstanceState is null:" + (savedInstanceState == null));
        if (gcPreventer == null) {
            gcPreventer = new GCStoppers();
        }
        else {
            Log.w(TAG,"gcPreventer is NOT null in onCreate, probably didn't go through destroy?");
        }


        //TODO: re-enable strict mode when it works again!!!
        if (Options.DEVELOPER_MODE && Options.DEVELOPER_STRICT_MODE && Defeat.FALSE()) {

            StrictMode.ThreadPolicy.Builder builder = new StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .detectCustomSlowCalls()
                    .penaltyDialog();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.detectResourceMismatches();
            }
            StrictMode.setThreadPolicy(builder.build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .detectLeakedRegistrationObjects()
                    .penaltyDeath()
                    .build());
        }
/*
        DebugEnabled.enableTag("WearMainActivity");
        DebugEnabled.enableTag("MapSwipeToZoomFrag");
        DebugEnabled.enableTag("SpeedPanel1");
        DebugEnabled.enableTag("ActivityLifeCycleLogger");
        DebugEnabled.enableTag("GridViewPagerNotifier");
        DebugEnabled.enableTag("Loader");
        DebugEnabled.enableTag("ColumnHistorian");
        DebugEnabled.enableTag("MainLocationHandler");
        DebugEnabled.enableTag("FragmentLifecycleLogger");
        DebugEnabled.enableTag("BearingSectorizer");
        DebugEnabled.enableTag("MainGridPagerAdapter");
        DebugEnabled.enableTag("MyGridViewPager");
*/
    //TODO: make a preference page for all of these!
        DebugEnabled.disableTag("MainGridPagerAdapter");

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean defaultDebugLogEnablement = sharedPrefs.getBoolean(Keys.KEY_DEVOPT_DEBUG_LOG_ENABLED, true);
        DebugEnabled.setDefaultEnablement(defaultDebugLogEnablement);
        MyPreferenceChangeListener noGCSharedPreferenceListener = new MyPreferenceChangeListener();
        gcPreventer.add(noGCSharedPreferenceListener);
        sharedPrefs.registerOnSharedPreferenceChangeListener(noGCSharedPreferenceListener);


        super.onCreate(savedInstanceState);
        setAmbientEnabled();


        setContentView(R.layout.activity_main);
        final Resources res = getResources();
        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        //BUG: Is it sensible to massively increase the offscreen page count to allow the mapFragment to live on?
        // Answer , yes and no .. it puts heavy up-front load, but keeping the offscreenpagecount at 1
        // causes delays every time the map fragment needs to be loaded again
        pager.setOffscreenPageCount(100);
        pager.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Adjust page margins:
                //   A little extra horizontal spacing between pages looks a bit
                //   less crowded on a round display.
                final boolean round = insets.isRound();
                int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                int colMargin = res.getDimensionPixelOffset(round ?
                        R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                pager.setPageMargins(rowMargin, colMargin);

                // GridViewPager relies on insets to properly handle
                // layout for round displays. They must be explicitly
                // applied since this listener has taken them over.
                pager.onApplyWindowInsets(insets);
                return insets;
            }
        });

        sampleGridPagerAdapter = new MainGridPagerAdapter(this, getFragmentManager());

        Controller.startup(sampleGridPagerAdapter, getApplicationContext());
        mController = Controller.getInstance();
        // actually in service mode this is probably not going to work because the location service
        // isn't yet started
        if (mController == null) {
            Log.e(TAG, "No Controller, what!");
        }
        else {
            mController.leaveSilentMode();
        }


        final MainGridPagerAdapter.ColumnHistorian columnHistorian = sampleGridPagerAdapter.getColumnHistorian();
        columnHistorian.setPager(pager);
        pager.setOnPageChangeListener(columnHistorian);
        pager.setAdapter(sampleGridPagerAdapter);

        GridViewPagerListenerNotifier noGCGridViewPagerListenerNotifier = new GridViewPagerListenerNotifier(pager);
        gcPreventer.add(noGCGridViewPagerListenerNotifier);
        pager.setOnPageChangeListener(noGCGridViewPagerListenerNotifier);

        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(pager);


        pager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int row, int col, float v, float v1, int i2, int i3) {
                if (tagEnabled(TAG)) Log.d(TAG, "onPageScrolled: " + row + "," + col);
            }

            @Override
            public void onPageSelected(int row, int col) {
                if (tagEnabled(TAG)) Log.d(TAG, "onPageSelected: " + row + "," + col);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                String[] states = {"IDLE", "DRAGGING", "SETTLING", "SCROLL_STATE_CONTENT_SETTLING", "UNKNOWN4"};
                if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChanged state: " + state + " " + states[state]);

                if (state == 2) {
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Dump of Google maps fragments created");
                    // Dump out state of mapFragments
                    int gcd = 0;
                    for (String index : GoogleMapFragmentPool.googleMapFragmentRecording.keySet()) {
                        WeakReference<MapFragment> ref = GoogleMapFragmentPool.googleMapFragmentRecording.get(index);
                        MapFragment f = ref.get();
                        if (f == null) {
                            if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps fragment " + index + " has been GC'd");
                            gcd++;
                        } else {
                            if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps fragment " + index + (f) + " Parent Fragment is " + f.getParentFragment());
                        }
                    }
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps fragments created contains: " + GoogleMapFragmentPool.googleMapFragmentRecording.size());
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps innerMap Pool is pooling: " + GoogleMapFragmentPool.innerMapFragmentPool.size());
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps innerMap Pool has had " + gcd + " garbage collected");
                }
                if (state == 2) { // settling - doesn't work!
                    if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChanged, SETTLING, current position is " + pager.getCurrentItem());
                    // doesn't work if we change the position here
                }

                if (state == 0) { // idle
                    if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChanged, IDLE, current position is " + pager.getCurrentItem());
                    // we can reFlip aka change the position/the pages here
                         {
                        final int row = pager.getCurrentItem().y;
                        final int column = pager.getCurrentItem().x;

                        final Point newPoint = columnHistorian.detectAndZoom(row, column);
                        if (newPoint != null) {
                            // setCurrentItem direct doesn't properly update
                            // so we'll try to run it later
                            // did not work pager.getAdapter().notifyDataSetChanged(); // apparently this work https://code.google.com/p/android/issues/detail?id=76028
                            // trying adding a layout changed listener! seems to work .. not 100% sure yet
                            pager.setCurrentItem(newPoint.y, newPoint.x, false);
                            // pager.getAdapter().notifyDataSetChanged();
                        }
                    }
                }
            }
        }); // end pager onPageChangeListener

        MapSwipeToZoomFragment noGCMapFragmentContainer = sampleGridPagerAdapter.getMapFragment();
        gcPreventer.add(noGCMapFragmentContainer);


        // service should have been started with application
        // but we should get a handle to it so we can adjust its "mode" (foreground, background, possibly recording)

        // perhaps we should "bind" to the service?
        /* pure start mode
        Intent startIntent = new Intent(this, LocationPollingService.class);
        this.startService(startIntent);
         */
        // bind approach
        if (mLocationPollingService == null) {
            bindService(new Intent(this,
                    LocationPollingService.class), mLocationPollingServiceConnectionListener, Context.BIND_AUTO_CREATE);
            //mLocationPollingServiceConnectionListener will be called when bound?
        }
        else {
            Log.e(TAG, "nor re-binding because it seems we already have a binding, this is probably wrong!");
        }


        ConnectionCallBacksHandler noGCConnectionCallBacksHandler = new ConnectionCallBacksHandler();
        gcPreventer.connectionCallBacksHandler = noGCConnectionCallBacksHandler;
        ConnectionFailedListener noGCConnectionFailedListener = new ConnectionFailedListener();
        gcPreventer.connectionFailedListener = noGCConnectionFailedListener;
        mDataApiListener = new DataApiListener(noGCMapFragmentContainer, getFilesDir());
        mCapabilityApiListener = new CapabilityApiListener();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(noGCConnectionCallBacksHandler)
                .addOnConnectionFailedListener(noGCConnectionFailedListener)
                .build();
        mGoogleApiClient.connect();


        doPermissionsCheck(this);

        for (StoredRoute route : StoredRoutesUtils.readStoredRoutes(getApplicationContext(), getFilesDir())) {
            if (noGCMapFragmentContainer != null) {
                noGCMapFragmentContainer.addRoute(route);
            }
        }



        /* Snackbars don't work on Wear
        Snackbar.make(mainPagerView, R.string.intro_text, Snackbar.LENGTH_INDEFINITE)
                // .action
                // setActionTextColour
            .show();
        */
        // in all likelihood I'm going to have to overlay a Card and update / show / hide it for tutorials and in-app notifications

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        if (tagEnabled(TAG)) Log.d(TAG, "before onEnterAmbient current isAmbient state is " + isAmbient());
        super.onEnterAmbient(ambientDetails);
        EventBus.getDefault().post(new LogEvent("Ambient Entered", "PWR"));
        EventBus.getDefault().post(new AmbientEvent(AmbientEvent.ENTER_AMBIENT, ambientDetails));

        if (tagEnabled(TAG)) Log.d(TAG, "after onEnterAmbient current isAmbient state is " + isAmbient());
    }

    @Override
    public void onExitAmbient() {
        if (tagEnabled(TAG)) Log.d(TAG, "before onExitAmbient current isAmbient state is " + isAmbient());
        EventBus.getDefault().post(new LogEvent("Ambient Exited", "PWR"));
        EventBus.getDefault().post(new AmbientEvent(AmbientEvent.LEAVE_AMBIENT, null));
        super.onExitAmbient();
        if (tagEnabled(TAG)) Log.d(TAG, "after onExitAmbient current isAmbient state is " + isAmbient());


        if (tagEnabled(TAG)) Log.d(TAG, "Posting a invalidate event to force full color mode to workaround gridViewPager half colour mode issue:");
        MainWearActivity.this.findViewById(android.R.id.content).getRootView().invalidate();

    }

    @Override
    public void onUpdateAmbient() {
        if (tagEnabled(TAG)) Log.d(TAG, "onUpdateAmbient Called");
        super.onUpdateAmbient();
        EventBus.getDefault().post(new AmbientEvent(AmbientEvent.UPDATE, null));
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



    @Override
    protected void onStart() {
        super.onStart();
        // cancel any displayed notification
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(1);

        File logDir = new File(Environment.getExternalStorageDirectory().getPath() + "/wearRoutes/");
        if (!logDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            logDir.mkdirs();
        }

        if (!SimpleTextLogger.isStarted()) {
            mSimpleTextLogger = SimpleTextLogger.create(logDir, "TextLogger");
            mSimpleTextLogger.setLogging(true);
        }
        else {
            mSimpleTextLogger = SimpleTextLogger.getInstance();
        }
        EventBus.getDefault().post(new LogEvent("MainWearActivity onCreate Complete", "ACT"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        // dismiss our notification?
        // is it even visible?
    }


    @Override
    protected void onStop() {
        if (tagEnabled(TAG)) Log.d(TAG, "onStop called");


        // loose some basic references that will be re-created in onCreate
        sampleGridPagerAdapter = null;
        if (mController != null) {
            mController.enterNoUI();
        }


        // IF we don't have a controller then we cant get recording state
        // assume we're stopped already.. no real idea how this happens TBH
        if (mController!=null && mController.getRecordingState() != ControllerState.StateConstants.STATE_STOPPED) {
            // if we are recording DON'T shutdown properly
            //TODO: implement a "recording" service to handle the stuff that needs to go on in the background, there isn't a huge amount

            Notification n = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Wear Routes")
                    .setContentText("Recording in background")
                    // Set a content intent to return to this sample
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
                            new Intent(getApplicationContext(), MainWearActivity.class), 0))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setOngoing(true) // cant dismiss?
                    //TODO: only api 21 .setCategory(Notification.CATEGORY_STATUS)
                    // .setWhen().setShowWhen() // TODO:later api
                    .build();
            // and now display it
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .notify(1, n);

            mController.enterSilentMode();

            // and unbind, we'll recreate when next started
            if (Options.LOCATION_HANDLER_AS_SERVICE) {
                //stop the service?
                if (mLocationPollingService != null) {
                    // always Detach our existing connection.
                    unbindService(mLocationPollingServiceConnectionListener);
                    mLocationPollingService = null;
                }
            }


            EventBus.getDefault().post(new LogEvent("MainWearActivity onStop Continuing in background","ACT"));

        } else { // we are full stopping!
            Log.w(TAG, "Finishing activity in onStop");
            EventBus.getDefault().post(new LogEvent("MainWearActivity onStop Full Stopping","ACT"));

            // text logger is *only* stopped on complete exit
            // otherwise we have issues logging when background recording!
            if (mSimpleTextLogger != null) {
                mSimpleTextLogger.setLogging(false);
                mSimpleTextLogger.destroy();
                mSimpleTextLogger = null;
            }

            if (mController != null) {
                mController.shutdown();
                mController = null;
            }

            //stop the service polling and unbind
            // responsibility for fully stopping the service is currently in the application
            //BUG: service should be stopped and started by the app
            // .. remembering that it may continue running when this app is no longer active/visible
            if (mLocationPollingService != null) {
                //TODO: do we need to stop the service or is unbind enough?
                mLocationPollingService.stopPolling();
                // Detach our existing connection.
                unbindService(mLocationPollingServiceConnectionListener);
                mLocationPollingService = null;

            }




            //DEBUG see what's left hanging in the EventBus
            EventBus.getDefault().hasSubscriberForEvent(Object.class);

            if (Options.DEVELOPER_MODE) {
                UnexpectedExceptionHandler.unregister();
            }

            //finish();
        }
        super.onStop();

    }

    private class ConnectionCallBacksHandler implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (tagEnabled(TAG)) Log.d(TAG, "onConnected(): Successfully connected to Google API client");
            Wearable.DataApi.addListener(mGoogleApiClient, MainWearActivity.this.mDataApiListener);
            //Wearable.MessageApi.addListener(mGoogleApiClient, this);

            Wearable.CapabilityApi.addListener(
                    mGoogleApiClient, MainWearActivity.this.mCapabilityApiListener, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
        } // end onConnected


        @Override
        public void onConnectionSuspended(int cause) {
            if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

    }

    private static void initializeStaticComponent() {
        if (staticComponentsActive) {
            return;
        }
        try {
            //EventBus.builder().installDefaultEventBus();
            LoggingEventBus.loggingBuilder().installDefaultEventBus();
        } catch (EventBusException ebe) {
            Log.e(TAG, "Bollocks it seems Event bus is already running, probably starting again while recording?", ebe);
        }
        UnexpectedExceptionHandler.register();
        staticComponentsActive = true;

    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
        }
    }

    private class CapabilityApiListener implements CapabilityApi.CapabilityListener {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
            if (tagEnabled(TAG)) Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
            /// mDataFragment.appendItem("onCapabilityChanged", capabilityInfo.toString());
        }
    }

    private class MyPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Keys.KEY_DEVOPT_DEBUG_LOG_ENABLED.equals(key)) {
                boolean defaultDebugLogEnablement = sharedPreferences.getBoolean(key, true);
                DebugEnabled.setDefaultEnablement(defaultDebugLogEnablement);
            } // end DEBUG_LOG_ENABLED
        } // end onSharedPreferenceChanged

    } // end inner class MyPreferenceChangeListener

    /** a receiver to be notified when the LocationPolling Service is bound */
    private ServiceConnection mLocationPollingServiceConnectionListener = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mLocationPollingService = ((LocationPollingService.ServiceBinder)service).getService();
            mLocationPollingService.beginPolling();
            //TODO: probably should restore desired state, unless the controller does that
            if (mController == null) {
                Log.e(TAG, "Polling service connected, but NO Controller!");
            }
            else {
                mController.leaveSilentMode();
            }
            Log.i(TAG, "Location Polling Service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mLocationPollingService = null;
            Log.i(TAG, "Location Polling Service disconnected");

        }
    };
}