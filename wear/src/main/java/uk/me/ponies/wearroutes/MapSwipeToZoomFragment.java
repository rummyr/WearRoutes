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

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.CPUMeasurer;
import uk.me.ponies.wearroutes.common.StoredRoute;

public class MapSwipeToZoomFragment extends Fragment implements GridViewPagerListener {
    private static final LatLng SHEFFIELD = new LatLng(53.5089423,-1.7025131);
    private static final String TAG = "MapSwipeToZoomFrag";
    private  View myMapFragment;
    String fragmentName;
    MapFragment innerMapFragment;
    private GoogleMap map;
    // make permanent -- might be GCd, who can tell?
    private GestureDetector mGestureDetector;
    private Map<String,PolylineOptions> mSrcPolylines = new LinkedHashMap<>();
    private Map<String,Polyline> mMapPolyLines = new HashMap<>();
    private Map<String,LatLngBounds> mPolyLineBounds = new HashMap<>();

    private CameraPosition mResumeCamPos;
    private static final String KEY_RESUME_CAM_POS = "camLastPos";
    private boolean mVisible = false; // hopefully we will be called with visible before we actually ARE!
    private CameraUpdate mLastCamUpdate = null;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d("Lifecycle", fragmentName + " " +"onCreateOptionsMenu called");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.d("Lifecycle", fragmentName + " " +"onPrepareOptionsMenu called");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDestroyOptionsMenu() {
        Log.d("Lifecycle", fragmentName + " " +"onDestroyOptionsMenu called");
        super.onDestroyOptionsMenu();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d("Lifecycle", fragmentName + " " +"onCreateContextMenu called");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        Log.d("Lifecycle", fragmentName + " " +"onViewStateRestored called");
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d("Lifecycle", fragmentName + " " +"onActivityCreated called");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d("Lifecycle", fragmentName + " " +"onViewCreatedCalled");
        super.onViewCreated(view, savedInstanceState);


        View mAttachListenerTo = myMapFragment;
        ViewParent vp1 = myMapFragment.getParent(); // vp1 = MyGridViewPager << probably recieves touch events
        ViewParent vp2 = vp1.getParent(); // vp2 = FrameLayout
        ViewParent vp3 = vp2.getParent(); // vp3 = SwipeDismissLayout

        int idp1 = ((View)vp1).getId(); // vp1.id (MyGridViewPager) = 2131689504
        int idp2 = ((View)vp2).getId(); // vp2.id (FrameLayout) = -1
        int idp3 = ((View)vp3).getId(); // vp3.id (SwipeDismissLayout) = 16908290
        String.valueOf(idp1+idp2 + idp3);
        mAttachListenerTo = (View)vp1;



        mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            //TODO tune these to screen size?
            private int SWIPE_MIN_DISTANCE = 50;
            private int SWIPE_VELOCITY_THRESHOLD = 100;
            private int MAX_OFF_PATH = 100;

            { // pseudo constructor
                final ViewConfiguration vc = ViewConfiguration.get(getActivity());
                DisplayMetrics dm = getActivity().getResources().getDisplayMetrics();
                SWIPE_MIN_DISTANCE = (int)(vc.getScaledPagingTouchSlop() * dm.density);
                SWIPE_VELOCITY_THRESHOLD  = vc.getScaledMinimumFlingVelocity();
                MAX_OFF_PATH = SWIPE_MIN_DISTANCE * 2;
            }
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d(TAG, "GestureDetector onFling seen!");
                // mainly left?
                // Code from stackOverflow http://stackoverflow.com/questions/32966069/how-implement-left-right-swipe-fling-on-layout-in-android
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    long deltaT = e2.getEventTime() - e1.getEventTime();

                    Log.d(TAG, "GestureDetector onFling diffX:" + diffX +"vs"+ SWIPE_MIN_DISTANCE + " velocityX:"+velocityX+"vs"+SWIPE_VELOCITY_THRESHOLD);
                    Log.d(TAG, "GestureDetector onFling diffY:" + diffY +"vs"+ SWIPE_MIN_DISTANCE + " velocityY:"+velocityY+"vs"+SWIPE_VELOCITY_THRESHOLD);

                    if (getActivity() == null) {
                        Log.d(TAG,"getActivity is NULL!");
                    }
                    Context context = getActivity().getApplicationContext();
                    if (PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean(Keys.KEY_PREF_DEVOPT_MAP_TOAST_ON_FLING,false)) {
                        CharSequence text = "Fling! " + (int) e1.getX() + "->" + (int) e2.getX() + " dt:" + (int) deltaT;
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }

                    // max off path is very small, so no longer using it
                    /*
                    if (Math.abs(diffY) > MAX_OFF_PATH) {
                        Log.d(TAG, "onFling Not straight enough");
                        result = false;
                    }
                    else if (Math.abs(diffX) > MAX_OFF_PATH) {
                        Log.d(TAG, "onFling Not straight enough");
                        result = false;
                    }
                    */
                    // instead I'm discriminating on if the deltaX is 2x better than delta Y
                    // abs(x/y) > 1 for sideways
                    // abs(x/y)< 1 for upwards
                    // or without the divide diffX > 2*
                    boolean isStraight = false;
                    if (Math.abs(diffX) > 2*Math.abs(diffY)) {
                        // mainly X
                        Log.d(TAG, "GestureDetector onFling straight enoughX: " + diffX + "/" + diffY);
                        isStraight = true;
                    }
                    if (Math.abs(diffY) > 2*Math.abs(diffX)) {// mainly Y
                        Log.d(TAG, "GestureDetector onFling straight enoughY: " + diffX + "/" + diffY);
                        isStraight = true;
                    }
                    if (!isStraight) {
                        Log.d(TAG, "GestureDetector onFling is NOT sraight!");
                    }

                    if (!isStraight) {
                        result = false;
                    }
                    else if (Math.abs(diffX) > Math.abs(diffY)) { // mainly across
                        if (Math.abs(diffX) > SWIPE_MIN_DISTANCE) {
                            if (Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                if (diffX > 0) {
                                    onSwipeRight();
                                } else {
                                    onSwipeLeft();
                                }
                            }
                            else {
                                Log.d(TAG, "GestureDetector onFling Not fast enough");
                            }
                        }
                        else {
                            Log.d(TAG, "GestureDetector onFling Not far enough");
                        }
                        result = true;
                    }
                    else if (Math.abs(diffY) > SWIPE_MIN_DISTANCE) {
                        if (Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) { // mainly up
                            if (diffY > 0) {
                                onSwipeBottom();
                            } else {
                                onSwipeTop();
                            }
                        } else {
                            Log.d(TAG, "GestureDetector onFling Not fast enough");
                        }
                    }
                    else {
                        Log.d(TAG, "GestureDetector onFling Not far enough");
                    }
                    result = true;

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }

            private void onSwipeTop() {
                Log.d(TAG, "GestureDetector  Swipe Top seen");
            }
            private void onSwipeBottom() {
                Log.d(TAG, "GestureDetector  Swipe Bottom seen");
            }
            private void onSwipeLeft() {
                Log.d(TAG, "GestureDetector  Swipe Left seen"); zoomBy(+1);
            }
            private void onSwipeRight() {
                Log.d(TAG, "GestureDetector  Swipe Right seen"); zoomBy(-1);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Log.d(TAG, "GestureDetector onScroll seen!");
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

        });
        mAttachListenerTo.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                Log.d(TAG, "onGenericMotionSeen");
                return false;
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mAttachListenerTo.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    Log.d(MapSwipeToZoomFragment.class.getSimpleName(), "onScrollChange seen");
                }
            });
        }

        mAttachListenerTo.setOnTouchListener(new View.OnTouchListener() {
            int prevAction = MotionEvent.ACTION_DOWN;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_MOVE) {
                    Log.d(MapSwipeToZoomFragment.class.getSimpleName(), "onTouch seen for event:" + event);
                }
                final boolean gestureDetected;

                if (event.getToolType(0) == event.TOOL_TYPE_FINGER
                    && event.getAction() == event.ACTION_MOVE
                    && prevAction == event.ACTION_UP
                    && event.getHistorySize() >= 1) { // only fake if we have a history!
                    //TODO: FIX This properly you lazy git!
                    // where did our down ACTION go!
                    // fake one up
                    float originalX = event.getX();
                    float originalY = event.getY();
                    long originalTime = event.getEventTime();
                    long originalDownTime = event.getDownTime();
                    float previousX =event.getHistoricalX(0);
                    float previousY =event.getHistoricalY(0);
                    long previousTime =event.getHistoricalEventTime(0);
                    int originalAction = event.getAction();
                    Log.d(MapSwipeToZoomFragment.class.getSimpleName(), "onMOVE seen without onDown, "
                            +" faking one up for "+(int)previousX+","+(int)previousY
                            + "->" + (int)originalX+","+(int)originalY);
                    MotionEvent fakedEvent = MotionEvent.obtain(originalDownTime,previousTime,event.ACTION_DOWN,
                            previousX,previousY, 0);
                    //event.setT
                    String.valueOf(originalAction+originalDownTime+originalTime+originalX+originalY+previousTime+previousX+previousY);
                    mGestureDetector.onTouchEvent(fakedEvent);
                    // after faking one we send the genuine one
                    gestureDetected = mGestureDetector.onTouchEvent(event);
                }
                else {
                    // pass GENUINE the data onto the gesture listener
                    gestureDetected = mGestureDetector.onTouchEvent(event);
                }
                if (event.getToolType(0) == event.TOOL_TYPE_FINGER) {
                    prevAction = event.getAction();
                }

                return false; // we don't need to override other behaviour at the moment
            }
        });

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Lifecycle", fragmentName + " " +"onCreate called");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        Log.d("Lifecycle", fragmentName + " " +"onInflate called");
        super.onInflate(context, attrs, savedInstanceState);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.d("Lifecycle", fragmentName + " " +"onHiddenChanged called");
        super.onHiddenChanged(hidden);
    }

    //BUG: no way should this go live!
    private static int DBG_SLEEP_BETWEEN_STEPS = 0;

    private int zoom = Options.WEAR_DEFAULT_STARTING_ZOOM;
    int tilt = 0;
    private String timingLabel;
    private long startCPU;
    private long startTime;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // doesn't work, because there is NO action bar
        setHasOptionsMenu(true); // doesn't but allows us to track some more lifecycle events{
        return onCreateViewEmptyFragmentAddMapProgramatticaly(inflater, container, savedInstanceState);
    }



    // NOTES:
    // innerMapFragments *do* get GC'd, unless we dod the dirty and stop that happening
    // attempting to re-use *THIS* object in "onCreate" results in .. "empty" or non-displayable pages.
    // Using GetChildFragmentManager .. works
    // Using getFragmentManager ... only 1st card shows a map .. it seems we're messing up!
    // removing the innerMapFragment in on Destroy - java.lang.IllegalStateException: Activity has been destroyed!
    // removing the innerMapFragment in on DestroyView -- seems OK, though it is still reporting parent!
    // pooling the innerMapFragment works (returned in onDestroyView) .. no noticable performance improvement yet
    // as above, but done in onStop() .. works, still slow .. dies if open another app
    // as above, but done in onPause() .. seems to work, and Works if open another app!
    // avoid camera updating if re-using .. doesn't work!
    //
    // Looking at the "middle third" when dragging .. seems to be MAP related .. card fragment is OK
    // @Override

    public View onCreateViewEmptyFragmentAddMapProgramatticaly(LayoutInflater inflater, ViewGroup container,
                                                               Bundle savedInstanceState) {
        Log.d("TAG", "MapContainingFragment " + fragmentName + " onCreateView called");


        myMapFragment = inflater.inflate(R.layout.emptyfragment, container, false);
        createInnerGoogleMapFragment();
        this.setRetainInstance(true); // seems to help with map loading delay?

        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!
        }
        //  myMapFragment.setRetainInstance(true);
        Log.d("CPU", "CreateView completed");

        return  myMapFragment;


    }

    public View onCreateViewFragmentInLayout(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
        Log.d("TAG", "MapContainingFragment " + fragmentName + " onCreateView called");
        myMapFragment = inflater.inflate(R.layout.mapfragment, container, false);
        // inner map fragment is created in the layout
        innerMapFragment =  (MapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapfragment);
        Log.d("TAG", "MapContainingFragment (not the view) is " + innerMapFragment );
        this.setRetainInstance(true); // seems to stop map loading delay?
        innerMapFragment.setRetainInstance(true);


        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!

        }
        else {
            mResumeCamPos = savedInstanceState.getParcelable(KEY_RESUME_CAM_POS);
        }
        return  myMapFragment;
    }

// called in onResume .. frankly unsure if this is the right place .. onCreate seems more sensible to me
private void createInnerGoogleMapFragment() {

    final boolean reusingInnerMapFragment;
    final boolean mustGetMap;
    //BEGIN("inflate self");
    //myMapFragment = inflater.inflate(R.layout.emptyfragment, container, false);
    //END();

    if (GoogleMapFragmentPool.innerMapFragmentPool.isEmpty()) {
        Log.d("InnerMapFragment", "creating a new InnerMapFragment");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean directionOfTravelUp = prefs.getBoolean(Keys.KEY_WEAR_DIRECTION_OF_TRAVEL_UP, true);

        GoogleMapOptions options;
        if (!directionOfTravelUp) {
            options = new GoogleMapOptions().liteMode(true);
        }
        else {
            options = new GoogleMapOptions();
        }
        //BEGIN("newInstance of Google MapFragment");
        innerMapFragment = MapFragment.newInstance(options);
        //END();

        mustGetMap = true;
        // setupMap will be called from the callback

        WeakReference<MapFragment> refToMap = new WeakReference<MapFragment>(innerMapFragment);
        GoogleMapFragmentPool.googleMapFragmentRecording.put(String.valueOf(++GoogleMapFragmentPool.creationNumber), refToMap);
        reusingInnerMapFragment = false;
    }
    else {
        // get one from the pool
        synchronized (GoogleMapFragmentPool.innerMapFragmentPool) {
            Log.d("InnerMapFragment", "getting an old InnerMapFragment");
            innerMapFragment = GoogleMapFragmentPool.getFragment();
            map = GoogleMapFragmentPool.getMapForFragment(innerMapFragment);
            mustGetMap = true;
            reusingInnerMapFragment = true;
        }
    }

    // Then we add it using a FragmentTransaction.
    //BEGIN("add Google Map Fragment to self");
    FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
    //fragmentTransaction.add(R.id.emptyFragment, innerMapFragment); // could name it I suppose
    fragmentTransaction.add(R.id.emptyFragment, innerMapFragment); // could name it I suppose
    fragmentTransaction.commit();
    //END();





    Log.d("TAG", "MapContainingFragment (not the view) is " + innerMapFragment );
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    final boolean dontInitMap = prefs.getBoolean(Keys.KEY_DEVOPT_PERF_DONT_INIT_MAP, false);

    if (mustGetMap) { // we HAVE to get the map because we didn't get it from the pool
        Log.d("MAP", "CreateView " + fragmentName + " getting MapAsync ");
        BEGIN("getting the map once");
        innerMapFragment.getMapAsync(new OnMapReadyCallbackImpl());
        // map = innerMapFragment.getMap();
        END();
        //BUG: get async! innerMapFragment.getMapAsync(new OnMapReadyCallbackImpl());
    } else {
        setupMap();
    }

    // com.google.android.gms.maps.MapContainingFragment f = (com.google.android.gms.maps.MapContainingFragment)myMapFragment;
    // mapFragment.setRetainInstance(true);// exception:  Can't retain fragements that are nested in other fragments
    this.setRetainInstance(true); // seems to stop map loading delay?

}


    private void BEGIN(String s) {
        timingLabel = s;
        Log.d("CPU", "Beginning step " + s);
        startCPU = CPUMeasurer.currentCPUUsed();
        startTime = System.currentTimeMillis();
    }

    private void END() {
        long endTime = System.currentTimeMillis();
        long endCPU = CPUMeasurer.currentCPUUsed();
        Log.d("CPU", "Step " + timingLabel + " ENDED used " + (endCPU - startCPU) + " took " + (endTime - startTime));

        if (DBG_SLEEP_BETWEEN_STEPS > 0) {
            try {
                Thread.sleep(DBG_SLEEP_BETWEEN_STEPS/2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Log.d("CPU", "Blipping CPU for 500ms");
            //long timeNow = System.currentTimeMillis();
            //while (System.currentTimeMillis() - timeNow < 500) {
            //    Math.sqrt(Math.PI);
            //}
            try {
                Thread.sleep(DBG_SLEEP_BETWEEN_STEPS/2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onAttach(Context context) {
        Log.d("Lifecycle", fragmentName + " " +"MapContainingFragment:" + fragmentName + " onAttach called");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onDetach called");
        super.onDetach();
    }

    @Override
    public void onStart() {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onStart called with zoom:" + zoom);
        BEGIN("onStart");
        super.onStart();
        END();
    }

    @Override
    public void onResume() {
        BEGIN("onResume");
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onResume called, visible is " + isVisible());
        super.onResume();


        boolean b = getView().isFocusable();
        String.valueOf(b);
        b = getView().isClickable();
        String.valueOf(b);

        END();

        // correct the dynamic parts of the UI (buttons, etc)
        matchMapUIToConfig();
        // adding the polylines back in on the Main Event Thread seems to work.
        //Most likely because what's actually hapenning is that it's running after the map's onResume
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
                }
            }
        });
    }


    @Override
    public void onPause() {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onPause called");
        // removing fragment here causes:
        removeInnerMapAndReturnToPool();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onStop called");
        // remove and pool works but dies if another app is opened!
        // fragmentManager reports: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onSaveInstanceState called");
        super.onSaveInstanceState(outState);
        if (map != null) {
            outState.putParcelable(KEY_RESUME_CAM_POS, map.getCameraPosition());
        }
    }

    @Override
    public void onDestroyView() {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onDestroyView called");
        // could potentially remove our inner map fragment here, seems to work
        //NOTE: leaking memory like crazy when switch between apps?!
        //removeInnerMapAndReturnToPool();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d("Lifecycle","MapContainingFragment:"  + fragmentName + " onDestroy called");
        // cant remove the innerMap here .. get an exception
        super.onDestroy();
    }

    public String toString() {
        return "MapContainingFragment:" + fragmentName;
    }

    private void removeInnerMapAndReturnToPool() {
        boolean sharedGMapFragments = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Keys.KEY_DEVOPT_PERF_REUSE_GMAP_FRAGMENTS, false );
        if (sharedGMapFragments) {
            Log.d("InnerMapFragment", "Removing inner map");
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.remove(innerMapFragment);
            fragmentTransaction.commit();
            // return this to the pool

            GoogleMapFragmentPool.returnFragment(innerMapFragment, map);
        }
    }


    public void onEnterAmbient(Bundle ambientDetails) {
        Log.d("TAG", "Fragment:" + fragmentName + " onEnterAmbient");
        if (innerMapFragment != null) {
            innerMapFragment.onEnterAmbient(ambientDetails);
        }
    }
    public void onExitAmbient() {
        Log.d("TAG", "Fragment:" + fragmentName + " onExitAmbient");

        if (innerMapFragment != null) {
            innerMapFragment.onExitAmbient();
        }
    }


    public @Nullable CameraPosition getCameraPosition() {
        if (map != null) {
            return map.getCameraPosition();
        }
        else {
            return null;
        }
    }

    public void animateCamera(CameraUpdate newCam) {
        if (map == null) {
            Log.d(TAG, "Not animating camera as the map doesn't exist");
        }
        if (mVisible == false) {
            Log.d(TAG, "Not animating camera as the map isn't visible");
        }
        if (map != null && mVisible) {
            Log.d(TAG, "moving camera as the map exists and is visible");
            map.animateCamera(newCam);
        }
        mLastCamUpdate = newCam;
    }

    public void moveCamera(CameraUpdate newCam) {
        if (map == null) {
            Log.d(TAG, "Not moving camera as the map doesn't exist");
        }
        if (mVisible == false) {
            Log.d(TAG, "Not moving camera as the map isn't visible");
        }

        if (map != null && mVisible) {
            Log.d(TAG, "moving camera as the map exists and is visible");
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
     *      deprecated FFS!
     */
    public GoogleMap getMap() {
        return map;
    }

    public void addRoute(StoredRoute route) {
        if (!route.getTHidden()) {
            PolylineOptions rectOptions = new PolylineOptions().addAll(route.getPoints());
            rectOptions.color(Color.RED); // TODO: make configurable
            rectOptions.width(5); // TODO: make configurable

            addPolyline(rectOptions, route.getName(), route.getBounds(), true);
        }
    }

    @Override
    public void onOnScreenPage() {
        boolean camWasVisible = mVisible;
        mVisible = true;
        if (map != null) {
            map.setMyLocationEnabled(mVisible); // enable this
        }

        // if we have *become* visible, move the camera to the last known position
        if (!camWasVisible && mLastCamUpdate != null) {
            Log.d(TAG, "Map is now visible restoring last camera position");
            //BUG: should move not animate!
            animateCamera(mLastCamUpdate);
        }

    }

    @Override
    public void onOffScreenPage() {
        mVisible = false;
        if (map != null) {
            map.setMyLocationEnabled(mVisible); // disable this, may update in the background?
        }

    }

    private class OnMapReadyCallbackImpl implements OnMapReadyCallback {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;

            matchMapUIToConfig();
            setupMap();



        }// end onMapReady
    }

    // we may not have had to get the map, but we still need to set zoom etc
    public void setupMap() {
        Log.d("MAP", "SetupMap Called " + fragmentName);
        BEGIN("Setting up the Map, not moving camera");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        CameraPosition camPos;

        if (mResumeCamPos != null) {
            camPos = mResumeCamPos;
        }
        else {
            camPos = CameraPosition.builder()
                    .target(SHEFFIELD)
                    .zoom(zoom)
                    .bearing(0)
                    .tilt(tilt)
                    .build();
        }

        CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);



        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.d("TAG", "Camera Changed for Map " + fragmentName);

                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        Log.d("MAP", "MapContainingFragment" + fragmentName + " onCameraChange Map now Loaded zoom is " + map.getCameraPosition().zoom +" wanted " + zoom + " on map:"+map );
                        View view = getView();
                        TextView t;
                        if (view != null) {
                            t = (TextView) view.findViewById(R.id.textViewZoomDisplay);
                            if (t != null) {
                                t.setText(String.valueOf(map.getCameraPosition().zoom));
                            }
                            t = (TextView) view.findViewById(R.id.textViewTiltDisplay);
                            if (t != null) {
                                t.setText(String.valueOf(map.getCameraPosition().tilt));
                            }
                            t = ((TextView) view.findViewById(R.id.textViewMapNumber));
                            if (t != null) {
                                t.setText(fragmentName);
                            }
                        } // if view is not null
                    }// end OnMapLoaded
                });//end set on mapLoadedCallback
            }// end onCameraChanged
        });// end setCameraChangedListener

        // animateCamera(cam);
        Log.d("MAP", "SetupMap " + fragmentName + " moving camera to zoom" + zoom);

        END();
        BEGIN("Moving Camera..possibly");
        moveCamera(cam);
        END();
    }

    private void matchMapUIToConfig() {
        if (map == null) {
            Log.d(MapSwipeToZoomFragment.class.getSimpleName(), "matchMapUIToConfig -- no map");
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String whenOnMapShowsVal = prefs.getString(Keys.KEY_WEAR_WHEN_ON_MAP_SHOWS, Keys.MAP_TRACK_VAL);
        final boolean showGrid = Keys.GRID_NO_TRACK_VAL.equals(whenOnMapShowsVal) || Keys.GRID_TRACK_VAL.equals(whenOnMapShowsVal);

        if (showGrid) {
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
        }
        else {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        final boolean showTrack = Keys.GRID_TRACK_VAL.equals(whenOnMapShowsVal) || Keys.MAP_TRACK_VAL.equals(whenOnMapShowsVal);
        boolean tapToZoom = prefs.getBoolean(Keys.KEY_MAP_TAP_ON_ZOOM, false);
        boolean showZoom = prefs.getBoolean(Keys.KEY_MAP_SHOW_ZOOM_BUTTONS, false);

        Log.d(MapSwipeToZoomFragment.class.getSimpleName(), "showZoom is "+showZoom);
        Log.d(MapSwipeToZoomFragment.class.getSimpleName(), "showZoom is "+showZoom);
        map.getUiSettings().setAllGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(showZoom);
        map.getUiSettings().setZoomGesturesEnabled(tapToZoom);
        // map.setPadding(0,75,0,0);

        //map.getUiSettings().setScrollGesturesEnabled(false);
        //map.getUiSettings().setRotateGesturesEnabled(false);
        //map.getUiSettings().setTiltGesturesEnabled(false);

        // and also do the "dot"
        // from http://stackoverflow.com/questions/14376361/disable-center-button-in-mylocation-at-google-map-api-v2
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);

    }

    public void zoomBy(int zoomBy) {
        if (map != null) {
            CameraPosition currentCamPosition = map.getCameraPosition();
            zoom = (int)map.getCameraPosition().zoom + zoomBy;
            CameraPosition camPos = CameraPosition.builder(currentCamPosition)
                    .zoom(zoom)
                    .build();

            CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);
            Log.d("MAP", "setZoom " + fragmentName + " moving camera to zoom" + zoom + " on Map:" + map);
            moveCamera(cam);

            mLastCamUpdate = cam;
        }
    }
    public void setZoom(int zoom) {
        this.zoom = zoom;
        if (map != null) {
            CameraPosition camPos = CameraPosition.builder(map.getCameraPosition())
                    .zoom(zoom)
                    .build();

            CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);
            Log.d("MAP", "setZoom " + fragmentName + " moving camera to zoom" + zoom + " on Map:" + map);
            moveCamera(cam);
            mLastCamUpdate = cam;
        }
    }
    public int getZoom() {
        return zoom;
    }
}



