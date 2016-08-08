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

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.common.logging.DebugEnabled;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.StateConstants;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.utils.ActivityLifeCycleLogger;
import uk.me.ponies.wearroutes.utils.StoredRoutesUtils;
import uk.me.ponies.wearroutes.common.StoredRoute;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class MainWearActivity extends WearableActivity {
    private static DataApi.DataListener mDataApiListener;
    private static CapabilityApi.CapabilityListener mCapabilityApiListener;
    MainGridPagerAdapter sampleGridPagerAdapter;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WearMainActivity";
    private ConnectionCallBacksHandler mConnectionCallBacksHandler;
    private ConnectionFailedListener mConnectionFailedListener;
    private MainLocationHandler mMainLocationListener;
    private MapSwipeToZoomFragment mMapFragmentContainer;
    private Controller mController;
    private GridViewPagerListenerNotifier mGridViewPagerListenerNotifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getApplication().registerActivityLifecycleCallbacks(new ActivityLifeCycleLogger());

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
        DebugEnabled.setDefaultEnablement(true);


        super.onCreate(savedInstanceState);
        setAmbientEnabled();


        setContentView(R.layout.activity_main);
        final Resources res = getResources();
        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
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
        final MainGridPagerAdapter.ColumnHistorian columnHistorian = sampleGridPagerAdapter.getColumnHistorian();
        columnHistorian.setPager(pager);
        pager.setOnPageChangeListener(columnHistorian);
        pager.setAdapter(sampleGridPagerAdapter);

        mGridViewPagerListenerNotifier = new GridViewPagerListenerNotifier(pager);
        pager.setOnPageChangeListener(mGridViewPagerListenerNotifier);

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

            final boolean reFlipPages = false; // true if the row is zoomin, map, zoomout

            @Override
            public void onPageScrollStateChanged(int state) {
                String[] states = {"IDLE", "DRAGGING", "SETTLING", "SCROLL_STATE_CONTENT_SETTLING", "UNKNOWN4"};
                if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChanged state: " + state + " " + states[state]);

                if (state == 2) {
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Dump of Google maps fragments created");
                    // Dump out state of mapFragments
                    int gcd = 0;
                    for (String index : GoogleMapFragmentPool.googleMapFragmentRecording.keySet()) {
                        WeakReference<com.google.android.gms.maps.MapFragment> ref = GoogleMapFragmentPool.googleMapFragmentRecording.get(index);
                        com.google.android.gms.maps.MapFragment f = ref.get();
                        if (f == null) {
                            if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps fragment " + index + " has been GC'd");
                            gcd++;
                        } else {
                            if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps fragment " + index + (f) + " Parent Fragment is " + f.getParentFragment());
                        }
                    }
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps fragments created contains: " + GoogleMapFragmentPool.googleMapFragmentRecording.size());
                    if (tagEnabled(TAG)) Log.d("InnerMapFragment", "Google maps innerMap Pool is pooling: " + GoogleMapFragmentPool.innerMapFragmentPool.size());
                }
                if (state == 2) { // settling - doesn't work!
                    if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChaged, SETTLING, currentposition is " + pager.getCurrentItem());
                    if (false) { // doesn't work if we change the position here
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

                if (state == 0) { // idle
                    if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChaged, IDLE, currentposition is " + pager.getCurrentItem());
                    if (true && reFlipPages) { // WORKS
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

        mMapFragmentContainer = sampleGridPagerAdapter.getMapFragment();
        mMainLocationListener = new MainLocationHandler(this);
        mConnectionCallBacksHandler = new ConnectionCallBacksHandler();
        mConnectionFailedListener = new ConnectionFailedListener();
        mDataApiListener = new DataApiListener(mMapFragmentContainer, getFilesDir());
        mCapabilityApiListener = new CapabilityApiListener();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallBacksHandler)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();
        mGoogleApiClient.connect();


        doPermissionsCheck(this);

        for (StoredRoute route : StoredRoutesUtils.readStoredRoutes(getApplicationContext(), getFilesDir())) {
            if (mMapFragmentContainer != null) {
                mMapFragmentContainer.addRoute(route);
            }
        }

        Controller.startup(sampleGridPagerAdapter);
        mController = Controller.getInstance();
        // mController.setContext(getApplicationContext());

        /* Snackbars don't work on Wear
        Snackbar.make(mainPagerView, R.string.intro_text, Snackbar.LENGTH_INDEFINITE)
                // .action
                // setActionTextColour
            .show();
        */
        // in all liklihood I'm going to have to overlay a Card and update / show / hide it for tutorials and in-app notifications

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        if (tagEnabled(TAG)) Log.d(TAG, "before onEnterAmbient current isAmbient state is " + isAmbient());
        super.onEnterAmbient(ambientDetails);
        sampleGridPagerAdapter.onEnterAmbient(ambientDetails);
        EventBus.getDefault().post(new AmbientEvent(AmbientEvent.ENTER, ambientDetails));
        if (tagEnabled(TAG)) Log.d(TAG, "after onEnterAmbient current isAmbient state is " + isAmbient());
    }

    @Override
    public void onExitAmbient() {
        if (tagEnabled(TAG)) Log.d(TAG, "before onExitAmbient current isAmbient state is " + isAmbient());
        sampleGridPagerAdapter.onExitAmbient();
        EventBus.getDefault().post(new AmbientEvent(AmbientEvent.LEAVE, null));
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
                if (tagEnabled(TAG)) Log.d(TAG, permission + " Granted");
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
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(001);
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
        if (mController.getRecordingState() != StateConstants.STATE_STOPPED) {
            Notification n = new Notification.Builder(getApplicationContext())
                    .setContentTitle("TITLE:Simple Notification")
                    .setContentText("Context Text")
                    // Set a content intent to return to this sample
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
                            new Intent(getApplicationContext(), MainWearActivity.class), 0))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(Notification.PRIORITY_MAX) //TODO: only enabel if started
                    .setOngoing(true) // cant dismiss?
                    //TODO: only api 21 .setCategory(Notification.CATEGORY_STATUS)
                    // .setWhen().setShowWhen() // TODO:later api
                    .build();
            // and now display it
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .notify(001, n);
        }
        super.onStop();

        // exit completely if we aren't recording
        if (Options.FINISH_ON_STOP && mController.getRecordingState() == StateConstants.STATE_STOPPED) {
            finish();
        }
    }

    private class ConnectionCallBacksHandler implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (tagEnabled(TAG)) Log.d(TAG, "onConnected(): Successfully connected to Google API client");
            Wearable.DataApi.addListener(mGoogleApiClient, MainWearActivity.mDataApiListener);
            //Wearable.MessageApi.addListener(mGoogleApiClient, this);

            Wearable.CapabilityApi.addListener(
                    mGoogleApiClient, MainWearActivity.mCapabilityApiListener, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
        } // end onConnected


        @Override
        public void onConnectionSuspended(int cause) {
            if (tagEnabled(TAG)) Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
        }

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
}