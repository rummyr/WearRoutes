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

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.me.ponies.wearroutes.common.BearingSectorizer;
import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.common.locationUtils.Utils;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationProcessedEvent;
import uk.me.ponies.wearroutes.historylogger.LatLngLogger;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.CPUMeasurer;
import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.utils.FragmentLifecycleLogger;
import uk.me.ponies.wearroutes.utils.MapScaleBarOverlay;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

public class MapSwipeToZoomFragment extends FragmentLifecycleLogger implements IGridViewPagerListener {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private static final LatLng SHEFFIELD = new LatLng(53.5089423, -1.7025131);
    private static final String TAG = "MapSwipeToZoomFrag";
    private static final int AMBIENT_TEXT_COLOR = Color.BLACK;
    private static final int ACTIVE_TEXT_COLOR = Color.RED;
    private static final String KEY_RESUME_CAM_POS = "camLastPos";
    String fragmentName;
    private MapFragment innerMapFragment;
    int tilt = 0;
    private View myMapFragment;
    private GoogleMap map;
    private EventBusLocationSourceForMap mLocationSource; // required so we can register/unregister
    // make permanent -- might be GCd, who can tell?
    private GestureDetector mGestureDetector;
    private final Map<String, PolylineOptions> mSrcPolylines = new LinkedHashMap<>();
    private final Map<String, Polyline> mMapPolyLines = new HashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<String, LatLngBounds> mPolyLineBounds = new HashMap<>();
    private CameraPosition mResumeCamPos;
    private boolean mVisible = false; // hopefully we will be called with visible before we actually ARE!
    private CameraUpdate mLastCamUpdate = null;
    private Polyline mHistoryPolyline;
    private TextView mTextViewZoomDisplay;
    private TextView mTextViewTiltDisplay;
    private TextView mTextViewMapNumber;
    private MapScaleBarOverlay mScaleBarOverlay;

    private final BearingSectorizer mBearingSectorizer = new BearingSectorizer(Options.BEARING_SECTORS); // 20 degree sectors
    private boolean firstLocation = true;
    // cached values
    private Rect mCachedMiddleScreeRect; // a rectangle boxing the area we want the point to stay within
    private Point mCachedScreenAnchor; // a point we want the blue dot to stay near
    private int mCachedScreenAnchorRadiusSquared; // how far we allow the point to deviate from the target anchor

    private int zoom = Options.WEAR_DEFAULT_STARTING_ZOOM;
    private String timingLabel;
    private long startCPU;
    private long startTime;
    /** For reuse purposes, to reduce GCs */
    private final Rect tmpVisibilityRect = new Rect();
    private final Point tmpIdealLocationPoint = new Point();


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, fragmentName + " " + "onViewCreatedCalled");
        super.onViewCreated(view, savedInstanceState);


        View mAttachListenerTo;
        ViewParent vp1 = myMapFragment.getParent(); // vp1 = MyGridViewPager << probably receives touch events
        ViewParent vp2 = vp1.getParent(); // vp2 = FrameLayout
        ViewParent vp3 = vp2.getParent(); // vp3 = SwipeDismissLayout

        @SuppressWarnings("UnusedAssignment") int idp1 = ((View) vp1).getId(); // vp1.id (MyGridViewPager) = 2131689504
        @SuppressWarnings("UnusedAssignment") int idp2 = ((View) vp2).getId(); // vp2.id (FrameLayout) = -1
        @SuppressWarnings("UnusedAssignment") int idp3 = ((View) vp3).getId(); // vp3.id (SwipeDismissLayout) = 16908290
        mAttachListenerTo = (View) vp1;


        mGestureDetector = new GestureDetector(getActivity(), new SwipeMapGestureDetector());

        mAttachListenerTo.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (tagEnabled(TAG)) Log.d(TAG, "onGenericMotionSeen");
                return false;
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mAttachListenerTo.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (tagEnabled(TAG)) Log.d(TAG, "onScrollChange seen");
                }
            });
        }

        mAttachListenerTo.setOnTouchListener(new View.OnTouchListener() {
            int prevAction = MotionEvent.ACTION_DOWN;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_MOVE) {
                    if (tagEnabled(TAG)) Log.d(TAG, "onTouch seen for event:" + event);
                }

                if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                        && event.getAction() == MotionEvent.ACTION_MOVE
                        && prevAction == MotionEvent.ACTION_UP
                        && event.getHistorySize() >= 1) { // only fake if we have a history!
                    //TODO: FIX This properly you lazy git!
                    // where did our down ACTION go!
                    // fake one up
                    float originalX = event.getX();
                    float originalY = event.getY();
                    long originalDownTime = event.getDownTime();
                    float previousX = event.getHistoricalX(0);
                    float previousY = event.getHistoricalY(0);
                    long previousTime = event.getHistoricalEventTime(0);

                    if (tagEnabled(TAG)) Log.d(TAG, "onMOVE seen without onDown, "
                                + " faking one up for " + (int) previousX + "," + (int) previousY
                                + "->" + (int) originalX + "," + (int) originalY);
                    MotionEvent fakedEvent = MotionEvent.obtain(originalDownTime, previousTime, MotionEvent.ACTION_DOWN,
                            previousX, previousY, 0);
                    mGestureDetector.onTouchEvent(fakedEvent);
                    // after faking one we send the genuine one
                    mGestureDetector.onTouchEvent(event);
                } else {
                    // pass GENUINE the data onto the gesture listener
                    mGestureDetector.onTouchEvent(event);
                }
                if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                    prevAction = event.getAction();
                }

                return false; // we don't need to override other behaviour at the moment
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // doesn't work, because there is NO action bar
        setHasOptionsMenu(true); // doesn't but allows us to track some more lifecycle events{
        View rv = onCreateViewEmptyFragmentAddMapProgramatticaly(inflater, container, savedInstanceState);

        mScaleBarOverlay = (MapScaleBarOverlay) rv.findViewById(R.id.customScaleBar);
        mTextViewZoomDisplay = (TextView) rv.findViewById(R.id.textViewZoomDisplay);
        mTextViewTiltDisplay = (TextView) rv.findViewById(R.id.textViewTiltDisplay);
        mTextViewMapNumber = ((TextView) rv.findViewById(R.id.textViewMapNumber));

        mLocationSource = new EventBusLocationSourceForMap();
        return rv;
    }


    // NOTES:
    // innerMapFragments *do* get GC'd, unless we dod the dirty and stop that happening
    // attempting to re-use *THIS* object in "onCreate" results in .. "empty" or non-displayable pages.
    // Using GetChildFragmentManager .. works
    // Using getFragmentManager ... only 1st card shows a map .. it seems we're messing up!
    // removing the innerMapFragment in on Destroy - java.lang.IllegalStateException: Activity has been destroyed!
    // removing the innerMapFragment in on DestroyView -- seems OK, though it is still reporting parent!
    // pooling the innerMapFragment works (returned in onDestroyView) .. no noticeable performance improvement yet
    // as above, but done in onStop() .. works, still slow .. dies if open another app
    // as above, but done in onPause() .. seems to work, and Works if open another app!
    // avoid camera updating if re-using .. doesn't work!
    //
    // Looking at the "middle third" when dragging .. seems to be MAP related .. card fragment is OK
    // @Override

    private View onCreateViewEmptyFragmentAddMapProgramatticaly(LayoutInflater inflater, ViewGroup container,
                                                               Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment " + fragmentName + " onCreateView called");


        myMapFragment = inflater.inflate(R.layout.emptyfragment, container, false);
        createInnerGoogleMapFragment();
        this.setRetainInstance(true); // seems to help with map loading delay?

        //noinspection StatementWithEmptyBody
        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!
        }

        if (tagEnabled(TAG)) Log.d(TAG, "CreateView completed");

        return myMapFragment;


    }

    public View onCreateViewFragmentInLayout(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment " + fragmentName + " onCreateView called");
        myMapFragment = inflater.inflate(R.layout.mapfragment, container, false);
        // inner map fragment is created in the layout
        innerMapFragment = (MapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapfragment);
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment (not the view) is " + innerMapFragment);
        this.setRetainInstance(true); // seems to stop map loading delay?
        innerMapFragment.setRetainInstance(true);


        //noinspection StatementWithEmptyBody
        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!

        } else {
            mResumeCamPos = savedInstanceState.getParcelable(KEY_RESUME_CAM_POS);
        }
        return myMapFragment;
    }

    // called in onResume .. frankly unsure if this is the right place .. onCreate seems more sensible to me
    private void createInnerGoogleMapFragment() {

        if (GoogleMapFragmentPool.innerMapFragmentPool.isEmpty()) {
            if (tagEnabled(TAG)) Log.d(TAG, "creating a new InnerMapFragment");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final boolean directionOfTravelUp = prefs.getBoolean(Keys.KEY_WEAR_DIRECTION_OF_TRAVEL_UP, true);

            GoogleMapOptions options;
            if (!directionOfTravelUp) {
                options = new GoogleMapOptions().liteMode(true);
            } else {
                options = new GoogleMapOptions();
            }

            innerMapFragment = MapFragment.newInstance(options);

            // setupMap will be called from the callback
            WeakReference<MapFragment> refToMap = new WeakReference<>(innerMapFragment);
            GoogleMapFragmentPool.googleMapFragmentRecording.put(String.valueOf(++GoogleMapFragmentPool.creationNumber), refToMap);
        } else {
            // get one from the pool
            synchronized (GoogleMapFragmentPool.innerMapFragmentPool) {
                if (tagEnabled(TAG)) Log.d(TAG, "getting an old InnerMapFragment");
                innerMapFragment = GoogleMapFragmentPool.getFragment();
                map = GoogleMapFragmentPool.getMapForFragment(innerMapFragment);
            }
        }

        // Then we add it using a FragmentTransaction.
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.emptyFragment, innerMapFragment); // could name it I suppose
        fragmentTransaction.commit();


        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment (not the view) is " + innerMapFragment);
        if (tagEnabled(TAG)) Log.d(TAG, "CreateView " + fragmentName + " getting MapAsync ");

        innerMapFragment.getMapAsync(new OnMapReadyCallbackImpl());


        // com.google.android.gms.maps.MapContainingFragment f = (com.google.android.gms.maps.MapContainingFragment)myMapFragment;
        // mapFragment.setRetainInstance(true);// exception:  Can't retain fragments that are nested in other fragments
        this.setRetainInstance(true); // seems to stop map loading delay?

    }


    private void BEGIN(String s) {
        timingLabel = s;
        if (tagEnabled(TAG)) Log.d(TAG, "Beginning step " + s);
        startCPU = CPUMeasurer.currentCPUUsed();
        startTime = System.currentTimeMillis();
    }

    private void END() {
        long endTime = System.currentTimeMillis();
        long endCPU = CPUMeasurer.currentCPUUsed();
        if (tagEnabled(TAG)) Log.d(TAG, "Step " + timingLabel + " ENDED used " + (endCPU - startCPU) + " took " + (endTime - startTime));
    }


    @Override
    public void onStart() {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onStart called with zoom:" + zoom);
        BEGIN("onStart");
        super.onStart();
        END();
    }

    @Override
    public void onResume() {
        BEGIN("onResume");
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onResume called, visible is " + isVisible());
        super.onResume();
        EventBus.getDefault().register(this);
        EventBus.getDefault().register(mLocationSource);

        // setup some quickie lookups
        int screenWidth = getActivity().getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getActivity().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
        // we use the middle 2/3 left to right
        // and from 1/6 of the bottom to 1/5 of the top .. not symmetrical because it's nicer to have near the bottom part of the screen
        mCachedMiddleScreeRect = new Rect(screenWidth / 6, screenHeight / 5, screenWidth - screenWidth / 6, screenHeight - screenHeight / 6);

        mCachedScreenAnchor = new Point(screenWidth/2, 2*screenHeight/3); // try to keep the point near the middle and 1/3rd up
        mCachedScreenAnchorRadiusSquared = screenWidth*screenWidth/9;

        END();

        // correct the dynamic parts of the UI (buttons, etc)
        matchMapUIToConfig();
        // adding the polylines back in on the Main Event Thread seems to work.
        //Most likely because what's actually happening is that it's running after the map's onResume
        //TODO: fix having to re-add polylines after a delay on main thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (map != null) {
                    //map.clear();
                    //mMapPolyLines.clear();
                    for (String name : mMapPolyLines.keySet()) {
                        mMapPolyLines.remove(name);
                    }
                    for (String routeName : mSrcPolylines.keySet()) {
                        PolylineOptions srcPolyline = mSrcPolylines.get(routeName);
                        Polyline p = map.addPolyline(srcPolyline);
                        mMapPolyLines.put(routeName, p);
                    }


                    // and set the camera zoom
                    map.stopAnimation();
                    map.moveCamera(CameraUpdateFactory.zoomTo(zoom));
                    // TODO: may need to update position also?
                }
            }
        });
    }


    @Override
    public void onPause() {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onPause called");
        // removing fragment here causes:
        removeInnerMapAndReturnToPool();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().unregister(mLocationSource);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onStop called");
        // remove and pool works but dies if another app is opened!
        // fragmentManager reports: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onSaveInstanceState called");
        super.onSaveInstanceState(outState);
        if (map != null) {
            outState.putParcelable(KEY_RESUME_CAM_POS, map.getCameraPosition());
        }
    }

    @Override
    public void onDestroyView() {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onDestroyView called");
        // could potentially remove our inner map fragment here, seems to work
        //NOTE: leaking memory like crazy when switch between apps?!
        //removeInnerMapAndReturnToPool();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment:" + fragmentName + " onDestroy called");
        // cant remove the innerMap here .. get an exception
        super.onDestroy();
    }

    public String toString() {
        return "MapContainingFragment:" + fragmentName;
    }

    private void removeInnerMapAndReturnToPool() {
        boolean sharedGMapFragments = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Keys.KEY_DEVOPT_PERF_REUSE_GMAP_FRAGMENTS, false);
        if (sharedGMapFragments) {
            if (tagEnabled(TAG)) Log.d(TAG, "Removing inner map");
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.remove(innerMapFragment);
            fragmentTransaction.commit();
            // return this to the pool

            GoogleMapFragmentPool.returnFragment(innerMapFragment, map);
        }
    }


    public void onEnterAmbient(Bundle ambientDetails) {
        if (tagEnabled(TAG)) Log.d(TAG, "Fragment:" + fragmentName + " onEnterAmbient");
        if (innerMapFragment != null) {
            innerMapFragment.onEnterAmbient(ambientDetails);
        }
        if (mTextViewZoomDisplay != null) {
            mTextViewZoomDisplay.setTextColor(AMBIENT_TEXT_COLOR);
        }
        if (mTextViewTiltDisplay != null) {
            mTextViewTiltDisplay.setTextColor(AMBIENT_TEXT_COLOR);
        }
        if (mTextViewMapNumber != null) {
            mTextViewMapNumber.setTextColor(AMBIENT_TEXT_COLOR);
        }

    }

    public void onExitAmbient() {
        if (tagEnabled(TAG)) Log.d(TAG, "Fragment:" + fragmentName + " onExitAmbient");

        if (innerMapFragment != null) {
            innerMapFragment.onExitAmbient();
        }
        if (mTextViewZoomDisplay != null) {
            mTextViewZoomDisplay.setTextColor(ACTIVE_TEXT_COLOR);
        }
        if (mTextViewTiltDisplay != null) {
            mTextViewTiltDisplay.setTextColor(ACTIVE_TEXT_COLOR);
        }
        if (mTextViewMapNumber != null) {
            mTextViewMapNumber.setTextColor(ACTIVE_TEXT_COLOR);
        }
    }


    public
    @Nullable
    CameraPosition getCameraPosition() {
        if (map != null) {
            return map.getCameraPosition();
        } else {
            return null;
        }
    }

    public void animateCamera(CameraUpdate newCam) {
        if (map == null) {
            if (tagEnabled(TAG)) Log.d(TAG, "Not animating camera as the map doesn't exist");
        }

        boolean isVisibleToUser;
        View v = getView();
        if (v == null) {
            isVisibleToUser = false; // no view, can't update
        } else {
            isVisibleToUser = v.getLocalVisibleRect(tmpVisibilityRect);
        }

        // only if visible!!!
        if (!isVisibleToUser) {
            if (tagEnabled(TAG)) Log.d(TAG, "Not animating camera as the map isn't visible");
        }
        if (map != null && isVisibleToUser) {
            if (tagEnabled(TAG)) Log.d(TAG, "moving camera as the map exists and is visible");
            map.animateCamera(newCam);
        }
        mLastCamUpdate = newCam;
    }

    public void moveCamera(CameraUpdate newCam) {
        if (map == null) {
            if (tagEnabled(TAG)) Log.d(TAG, "Not moving camera as the map doesn't exist");
        }

        boolean isVisibleToUser;
        View v = getView();
        if (v == null) {
            isVisibleToUser = false; // no view, can't update
        } else {
            isVisibleToUser = v.getLocalVisibleRect(tmpVisibilityRect);
        }

        if (!isVisibleToUser) {
            if (tagEnabled(TAG)) Log.d(TAG, "Not moving camera as the map isn't visible");
        }

        if (map != null && isVisibleToUser) {
            if (tagEnabled(TAG)) Log.d(TAG, "moving camera as the map exists and is visible");
            map.stopAnimation();
            map.moveCamera(newCam);
        }
        mLastCamUpdate = newCam;
    }

    public void addPolyline(PolylineOptions rectOptions, String name, LatLngBounds bounds, boolean zoomTo) {
        // save it in case we need to restore it!
        mSrcPolylines.put(name, rectOptions);
        mPolyLineBounds.put(name, bounds);
        if (map != null) {
            Polyline oldPolyLine = mMapPolyLines.get(name);
            if (oldPolyLine != null) {
                oldPolyLine.remove(); // clear the old one with the same name
            }
            Polyline newPolyLine = map.addPolyline(rectOptions);
            mMapPolyLines.put(name, newPolyLine);
            if (zoomTo) {
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, Options.OPTIONS_ROUTE_ZOOM_PADDING); // padding
                animateCamera(cu);
            }
        }
    }

    /**
     * deprecated FFS!
     */
    public GoogleMap getMap() {
        return map;
    }

    public void addRoute(StoredRoute route) {
        if (!route.getTHidden()) {
            PolylineOptions rectOptions = new PolylineOptions();
            rectOptions.addAll(route.getPoints())
            .color(Color.RED)   // TODO: make configurable
            .width(5)           // TODO: make configurable
            .geodesic(false);

            addPolyline(rectOptions, route.getName(), route.getBounds(), true);
        }
    }

    @Override
    public void onOnScreenPage() {
        boolean camWasVisible = mVisible;
        mVisible = true;

        if (map != null) {
            map.setMyLocationEnabled(true); // enable this
        }

        // if we have *become* visible, move the camera to the last known position
        if (!camWasVisible && mLastCamUpdate != null) {
            if (tagEnabled(TAG)) Log.d(TAG, "Map is now visible restoring last camera position");
            //BUG: should move not animate!
            animateCamera(mLastCamUpdate);
        }

    }

    @Override
    public void onOffScreenPage() {
        mVisible = false;

        if (map != null) {
            map.setMyLocationEnabled(false); // disable this, may update in the background?
        }

    }

    // we may not have had to get the map, but we still need to set zoom etc
    private  void setupMap() {
        if (tagEnabled(TAG)) Log.d(TAG, "SetupMap Called " + fragmentName);
        BEGIN("Setting up the Map, not moving camera");


        map.setLocationSource(mLocationSource);
        // left, top, right, bottom
        map.setPadding(0,0,0,0);

        CameraPosition camPos;

        if (mResumeCamPos != null) {
            camPos = mResumeCamPos;
        } else {
            camPos = CameraPosition.builder()
                    .target(SHEFFIELD)
                    .zoom(zoom)
                    .bearing(0)
                    .tilt(tilt)
                    .build();
        }

        CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);

        if (mScaleBarOverlay != null) {
            mScaleBarOverlay.setMap(map);
        }


        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (tagEnabled(TAG)) Log.d(TAG, "Camera Changed for Map " + fragmentName);
                // hide the zoom bar while it is (potentially) invalid
                if (mScaleBarOverlay != null) {
                    mScaleBarOverlay.mapZooming();
                }

                // register a callback for when the map has finished loading

                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        if (tagEnabled(TAG)) Log.d(TAG, "MapContainingFragment" + fragmentName + " onCameraChange Map now Loaded zoom is " + map.getCameraPosition().zoom + " wanted " + zoom + " on map:" + map);
                        if (mTextViewZoomDisplay != null) {
                            mTextViewZoomDisplay.setText(String.valueOf(map.getCameraPosition().zoom));
                        }
                        if (mTextViewTiltDisplay != null) {
                            mTextViewTiltDisplay.setText(String.valueOf(map.getCameraPosition().tilt));
                        }
                        if (mTextViewMapNumber != null) {
                            mTextViewMapNumber.setText(fragmentName);
                        }
                        if (mScaleBarOverlay != null) {
                            mScaleBarOverlay.mapLoaded();
                        }
                    }// end OnMapLoaded
                });//end set on mapLoadedCallback
            }// end onCameraChanged
        });// end setCameraChangedListener

        // animateCamera(cam);
        if (tagEnabled(TAG)) Log.d(TAG, "SetupMap " + fragmentName + " moving camera to zoom" + zoom);

        END();
        BEGIN("Moving Camera..possibly");
        moveCamera(cam);
        END();
    }

    private void matchMapUIToConfig() {
        if (map == null) {
            if (tagEnabled(TAG)) Log.d(TAG, "matchMapUIToConfig -- no map");
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String whenOnMapShowsVal = prefs.getString(Keys.KEY_WEAR_WHEN_ON_MAP_SHOWS, Keys.MAP_TRACK_VAL);
        final boolean showGrid = Keys.GRID_NO_TRACK_VAL.equals(whenOnMapShowsVal) || Keys.GRID_TRACK_VAL.equals(whenOnMapShowsVal);

        if (showGrid) {
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
        } else {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        boolean tapToZoom = prefs.getBoolean(Keys.KEY_MAP_TAP_ON_ZOOM, false);
        boolean showZoom = prefs.getBoolean(Keys.KEY_MAP_SHOW_ZOOM_BUTTONS, false);

        if (tagEnabled(TAG)) Log.d(TAG, "showZoom is " + showZoom);
        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(showZoom);
        map.getUiSettings().setZoomGesturesEnabled(tapToZoom);
        map.setBuildingsEnabled(false);
        map.setTrafficEnabled(false);
        map.setIndoorEnabled(false);
        //TODO: looks like we can set a location source for the map.. probably ought to use "ours"
        //map.setLocationSource();


        // map.setPadding(0,75,0,0);

        //map.getUiSettings().setScrollGesturesEnabled(false);
        //map.getUiSettings().setRotateGesturesEnabled(false);
        //map.getUiSettings().setTiltGesturesEnabled(false);

        // and also do the "dot"
        // from http://stackoverflow.com/questions/14376361/disable-center-button-in-mylocation-at-google-map-api-v2
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);

    }

    private void zoomBy(int zoomBy) {
        if (map != null) {
            CameraPosition currentCamPosition = map.getCameraPosition();
            zoom = (int) map.getCameraPosition().zoom + zoomBy;
            CameraPosition camPos = CameraPosition.builder(currentCamPosition)
                    .zoom(zoom)
                    .tilt(tilt) // always force the tilt
                    .build();

            CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);
            if (tagEnabled(TAG)) Log.d(TAG, "setZoom " + fragmentName + " moving camera to zoom" + zoom + " on Map:" + map);
            moveCamera(cam);

            mLastCamUpdate = cam;
        }
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
        if (map != null) {
            CameraPosition camPos = CameraPosition.builder(map.getCameraPosition())
                    .zoom(zoom)
                    .tilt(tilt) // always force the tilt
                    .build();

            CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);
            if (tagEnabled(TAG)) Log.d(TAG, "setZoom " + fragmentName + " moving camera to zoom" + zoom + " on Map:" + map);
            moveCamera(cam);
            mLastCamUpdate = cam;
        }
    }

    @Subscribe
    /** after a location event has been processed this will be called
     * we update the "history" trail polyline
     */
    public void onLocationProcessedEvent(LocationProcessedEvent dummy) {
        // TODO: cull offscreen (if it looks like the google maps api isn't efficient enough
        if (map == null) {
            return; // not doing anything on a null map!
        }
        LatLngLogger lll = Controller.getInstance().getLatLngLogger();
        if (lll == null) {
            return;
        }
        PolylineOptions plo = new PolylineOptions()
                .geodesic(false)
                .width(3)
                .color(Color.BLACK)
                .addAll(lll.getHistory());
        if (mHistoryPolyline != null) {
            mHistoryPolyline.remove();
        }
        mHistoryPolyline = map.addPolyline(plo);
    }

    @Subscribe
    public void newLocation(LocationEvent locationEvent) {
        boolean updateRequired = false;
        String updateReason = "";

        if (map == null) { // quick exit
            return;
        }
        Location l = locationEvent.getLocation();
        if (l == null) {
            return; // shouldn't happen, but lets check anyway
        }
        CameraPosition originalCameraPosition = getCameraPosition();

        if (originalCameraPosition != null) {
            CameraPosition.Builder targetPos = CameraPosition.builder(originalCameraPosition);
            float newBearing = mBearingSectorizer.convertToSectorDegrees(l.getBearing());
            Projection projection = null;


            if (Defeat.FALSE()) {
                if (projection == null) {
                    projection = map.getProjection();
                }
                // calculate a camera that would place the dot near the bottom .. avoiding chin
                //TODO: ideal camera location should NOT be hard coded!
                tmpIdealLocationPoint.set(mCachedMiddleScreeRect.centerX(), 100);
                LatLng idealLocation = projection.fromScreenLocation(tmpIdealLocationPoint);
                Point locationOnScreen = projection.toScreenLocation(new LatLng(l.getLatitude(), l.getLongitude()));
                LatLng reverseLatLng = projection.fromScreenLocation(locationOnScreen);
                String.valueOf(reverseLatLng);
                targetPos.target(idealLocation);
            }
            // update the targetPos and only use it if required
            targetPos.target(new LatLng(l.getLatitude(), l.getLongitude()));
            targetPos.bearing(newBearing); // can set this anyway
            targetPos.tilt(tilt); // always force the tilt


            // quick check .. is it the first location
            if (firstLocation) {
                updateRequired = true;
                firstLocation = false;
                updateReason = "First Location";
            } // end first time check

            // check to see if bearing has changed significantly
            // TODO: check user preferences!
            if (!updateRequired && !Options.NORTH_UP) {
                // check the bearing to see if it has changed
                if (newBearing != originalCameraPosition.bearing) {
                    updateRequired = true;
                    updateReason = "Bearing Change";
                }
            } // end bearing changed check

            // check to see if the camera has moved enough (with accuracy taken into account) to justify a move
            // only update target cameraPosition IFF it has moved by enough
            if (!updateRequired && Defeat.FALSE()) {
                // only update camera position if it has changed appropriately
                float distanceFromCam = Utils.haversineDistanceBetween(
                        originalCameraPosition.target.latitude, originalCameraPosition.target.longitude,
                        l.getLatitude(), l.getLongitude());
                if (distanceFromCam > Options.MIN_MOVE_DISTANCE_METERS + l.getAccuracy()) {
                    updateRequired = true;
                    updateReason = "Accuracy";
                }
            } // end moved by reasonable distance

            // pretty much guaranteed to need these fields
            LatLng tmpLatLng = new LatLng(l.getLatitude(), l.getLongitude());
            LatLngBounds latLngBounds = null;

            // check to see if dot is outside of the target area using screen pixels
            if (!updateRequired  && Defeat.TRUE()) { // check to see if it is near the edge using pixels
                if (projection == null) {
                    projection = map.getProjection();
                }

                Point dotOnScreenPoint = projection.toScreenLocation(tmpLatLng);
                if (!mCachedMiddleScreeRect.contains(dotOnScreenPoint.x, dotOnScreenPoint.y)) {
                    updateRequired = true;
                    updateReason = "dot outside middle pixels";
                    if (tagEnabled(TAG)) Log.d(TAG, "Location changed: Dot is outside of middle bounds:" +
                            "pos:" + dotOnScreenPoint
                            + "middle: " + mCachedMiddleScreeRect);
                } else {
                    if (tagEnabled(TAG)) Log.d(TAG, "Location changed: Dot is INSIDE of middle pixels:" +
                            "pos:" + dotOnScreenPoint
                            + "middle: " + mCachedMiddleScreeRect);
                }
            } // end if dot is outside middle region as specified by pixels


            // check to see if the dot is outside of the target area using the LatLngBounds
            if (!updateRequired) {

                // use this to see if map need fixing up!
                // try to ensure dot is on screen, ideally near the middle
                //noinspection ConstantConditions
                if (latLngBounds == null) {
                    if (projection == null) {
                        projection = map.getProjection();
                    }

                    latLngBounds = projection.getVisibleRegion().latLngBounds;
                }
                // if point is completely off screen then just update, no fancy checks
                if (!latLngBounds.contains(tmpLatLng)) {
                    updateRequired = true; // dot is completely off screen, bring it back!
                    updateReason = "Outside Bounds";
                    if (tagEnabled(TAG)) Log.d(TAG, "Location changed: Dot is outside of bounds:" +
                            "pos:" + tmpLatLng
                            + "bounds: " + latLngBounds);
                }
            } // end if outside latLngBounds



            if (!updateRequired && Defeat.FALSE()) { // check to see if it's near the edge using lat long
                //noinspection ConstantConditions
                if (latLngBounds == null) {
                    if (projection == null) {
                        projection = map.getProjection();
                    }

                    latLngBounds = projection.getVisibleRegion().latLngBounds;
                }

                double latSpan = latLngBounds.northeast.latitude - latLngBounds.southwest.latitude;
                double longSpan = latLngBounds.northeast.longitude - latLngBounds.southwest.longitude;
                LatLng center = latLngBounds.getCenter();
                double lat1 = center.latitude + latSpan / 3;
                double lat2 = center.latitude - latSpan / 3;
                double lon1 = center.longitude + longSpan / 3;
                double lon2 = center.longitude - longSpan / 3;
                // sort them
                if (lat1 > lat2) {
                    double tmp = lat1;
                    lat1 = lat2;
                    lat2 = tmp;
                }
                if (lon1 > lon2) {
                    double tmp = lon1;
                    lon1 = lon2;
                    lon2 = tmp;

                }

                LatLng midNorthEast = new LatLng(lat1, lon1);
                LatLng midSouthWest = new LatLng(lat2, lon2);
                LatLngBounds middleTwoThirds = new LatLngBounds(midNorthEast, midSouthWest);
                if (!middleTwoThirds.contains(tmpLatLng)) {
                    updateReason = "Outside Middle 2/3";
                    updateRequired = true;
                    if (tagEnabled(TAG)) Log.d(TAG, "Location changed: Dot is outside of middle bounds:" +
                                "pos:" + tmpLatLng
                                + "middle: " + middleTwoThirds
                                + "bounds: " + latLngBounds);
                } else {
                    if (tagEnabled(TAG)) Log.d(TAG, "Location changed: Dot is INSIDE of middle bounds:" +
                                "pos:" + tmpLatLng
                                + "middle: " + middleTwoThirds
                                + "bounds: " + latLngBounds);
                }

            } // end if dot is outside middle region according to LatLng


            CameraUpdate newCam = CameraUpdateFactory.newCameraPosition(targetPos.build());
            if (updateRequired) {
                if (tagEnabled(TAG)) Log.d(TAG, "Location changed, updating because " + updateReason);
                if (Options.ANIMATE_MOVES) {
                    animateCamera(newCam);
                } else {
                    moveCamera(newCam);
                }
            } // end if moved or rotated
        } else {
            Log.e(TAG, "no original camera position .. that's weird!");
        }


        // and don't forget to update the "trail"
        addLocationToTrail();
    }

    private void addLocationToTrail() {
        // TODO: cull offscreen (if it looks like the google maps api isn't efficient enough
        if (map == null) {
            return; // not doing anything on a null map!
        }
        LatLngLogger lll = Controller.getInstance().getLatLngLogger();
        if (lll == null) {
            return;
        }
        PolylineOptions plo = new PolylineOptions()
                .geodesic(false)
                .width(3)
                .color(Color.BLACK)
                .addAll(lll.getHistory());

        if (mHistoryPolyline != null) {
            mHistoryPolyline.remove();
        }
        mHistoryPolyline = map.addPolyline(plo);
    }

    private class OnMapReadyCallbackImpl implements OnMapReadyCallback {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            firstLocation = true;

            matchMapUIToConfig();
            setupMap();

        }// end onMapReady
    }

    private class EventBusLocationSourceForMap implements LocationSource {
        OnLocationChangedListener mLocationChangedListener = null;

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mLocationChangedListener = onLocationChangedListener;
            if (tagEnabled(TAG)) Log.d(TAG, "EventBusLocationSourceForMap activated for " + mLocationChangedListener);
        }

        @Override
        public void deactivate() {
            if (tagEnabled(TAG)) Log.d(TAG, "EventBusLocationSourceForMap deActivated for " + mLocationChangedListener);
            mLocationChangedListener = null;
        }

        @Subscribe
        public void newLocation(LocationEvent locationEvent) {
            Location l = locationEvent.getLocation();
            if (l == null) {
                if (tagEnabled(TAG))Log.d(TAG, "EventBusLocationSourceForMap received null location");
                return;
            }
            if (mLocationChangedListener == null) {
                if (tagEnabled(TAG)) Log.d(TAG, "EventBusLocationSourceForMap has no listener");
            } else {
                if (tagEnabled(TAG))Log.d(TAG, "EventBusLocationSourceForMap sending location to listener " + mLocationChangedListener);
                // notify the map's listener that the blue dot needs moving
                mLocationChangedListener.onLocationChanged(l);
            }
        }
    }

    private class SwipeMapGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private final int SWIPE_MIN_DISTANCE;
        private final int SWIPE_VELOCITY_THRESHOLD;


        public SwipeMapGestureDetector() {
            final ViewConfiguration vc = ViewConfiguration.get(getActivity());
            DisplayMetrics dm = getActivity().getResources().getDisplayMetrics();
            SWIPE_MIN_DISTANCE = (int) (vc.getScaledPagingTouchSlop() * dm.density);
            SWIPE_VELOCITY_THRESHOLD = vc.getScaledMinimumFlingVelocity();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector onFling seen!");
            // mainly left?
            // Code from stackOverflow http://stackoverflow.com/questions/32966069/how-implement-left-right-swipe-fling-on-layout-in-android
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                long deltaT = e2.getEventTime() - e1.getEventTime();

                if (tagEnabled(TAG))Log.d(TAG, "GestureDetector onFling diffX:" + diffX + "vs" + SWIPE_MIN_DISTANCE + " velocityX:" + velocityX + "vs" + SWIPE_VELOCITY_THRESHOLD);
                if (tagEnabled(TAG))                    Log.d(TAG, "GestureDetector onFling diffY:" + diffY + "vs" + SWIPE_MIN_DISTANCE + " velocityY:" + velocityY + "vs" + SWIPE_VELOCITY_THRESHOLD);

                if (getActivity() == null) {
                    if (tagEnabled(TAG)) Log.d(TAG, "getActivity is NULL!");
                }
                //TODO: activity seems to usually be null here, find an alternative!
                Activity activity = getActivity();
                if (activity != null) {
                    Context context = activity.getApplicationContext();
                    if (PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean(Keys.KEY_PREF_DEVOPT_MAP_TOAST_ON_FLING, false)) {
                        CharSequence text = "Fling! " + (int) e1.getX() + "->" + (int) e2.getX() + " dt:" + (int) deltaT;
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }
                }

                // max off path is very small, so no longer using it
                    /*
                    if (Math.abs(diffY) > MAX_OFF_PATH) {
                        if (tagEnabled(TAG))Log.d(TAG, "onFling Not straight enough");
                        result = false;
                    }
                    else if (Math.abs(diffX) > MAX_OFF_PATH) {
                        if (tagEnabled(TAG))Log.d(TAG, "onFling Not straight enough");
                        result = false;
                    }
                    */
                // instead I'm discriminating on if the deltaX is 2x better than delta Y
                // abs(x/y) > 1 for sideways
                // abs(x/y)< 1 for upwards
                // or without the divide diffX > 2*
                boolean isStraight = false;
                if (Math.abs(diffX) > 2 * Math.abs(diffY)) {
                    // mainly X
                    if (tagEnabled(TAG))                        Log.d(TAG, "GestureDetector onFling straight enoughX: " + diffX + "/" + diffY);
                    isStraight = true;
                }
                if (Math.abs(diffY) > 2 * Math.abs(diffX)) {// mainly Y
                    if (tagEnabled(TAG))                        Log.d(TAG, "GestureDetector onFling straight enoughY: " + diffX + "/" + diffY);
                    isStraight = true;
                }
                if (!isStraight) {
                    if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector onFling is NOT straight!");
                }

                if (!isStraight) {
                    result = false;
                } else if (Math.abs(diffX) > Math.abs(diffY)) { // mainly across
                    if (Math.abs(diffX) > SWIPE_MIN_DISTANCE) {
                        if (Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                        } else {
                            if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector onFling Not fast enough");
                        }
                    } else {
                        if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector onFling Not far enough");
                    }
                    result = true;
                } else if (Math.abs(diffY) > SWIPE_MIN_DISTANCE) {
                    if (Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) { // mainly up
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                    } else {
                        if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector onFling Not fast enough");
                    }
                } else {
                    if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector onFling Not far enough");
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

        private void onSwipeTop() {
            if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector  Swipe Top seen");
        }

        private void onSwipeBottom() {
            if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector  Swipe Bottom seen");
        }

        private void onSwipeLeft() {
            if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector  Swipe Left seen");
            zoomBy(+1);
        }

        private void onSwipeRight() {
            if (tagEnabled(TAG)) Log.d(TAG, "GestureDetector  Swipe Right seen");
            zoomBy(-1);
        }

    }
}



