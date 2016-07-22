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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;

import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.CPUMeasurer;

public class MapContainingFragment extends Fragment {
    private static final LatLng SHEFFIELD = new LatLng(53.5089423,-1.7025131);
    private  View myMapFragment;
    String fragmentName;
    MapFragment innerMapFragment;
    private GoogleMap map;

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

    int zoom = 1;
    int tilt = 0;
    private String timingLabel;
    private long startCPU;
    private long startTime;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        boolean sharedGMapFragments = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Keys.KEY_DEVOPT_PERF_REUSE_GMAP_FRAGMENTS, false );
        // doesn't work, because there is NO action bar
        setHasOptionsMenu(true); // doesn't but allows us to track some more lifecycle events
        if (sharedGMapFragments) {
            return onCreateViewEmptyFragmentAddMapProgramatticaly(inflater, container, savedInstanceState);
        } else {
            return onCreateViewFragmentInLayout(inflater, container, savedInstanceState);
        }
    }
    // @Override
    public View onCreateViewFragmentInLayout(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
        Log.d("TAG", "MapContainingFragment " + fragmentName + " onCreateView called");
        myMapFragment = null;
        if (myMapFragment == null) {
            myMapFragment = inflater.inflate(R.layout.mapfragment, container, false);
            // getFragmentManager().findFragmentById(R.id.mapfragment) returns null.

            innerMapFragment =  (MapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapfragment);
            Log.d("TAG", "MapContainingFragment (not the view) is " + innerMapFragment );

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final boolean dontInitMap = prefs.getBoolean(Keys.KEY_DEVOPT_PERF_DONT_INIT_MAP, false);
            if (dontInitMap) {

            } else {
                innerMapFragment.getMapAsync(new OnMapReadyCallbackImpl());
            }
            // com.google.android.gms.maps.MapContainingFragment f = (com.google.android.gms.maps.MapContainingFragment)myMapFragment;
            // mapFragment.setRetainInstance(true);// exception:  Can't retain fragements that are nested in other fragments
            this.setRetainInstance(true); // seems to stop map loading delay?

        }

        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!
        }
        //  myMapFragment.setRetainInstance(true);
        return  myMapFragment;
        // return inflater.inflate(R.layout.mapfragment, container, false);
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


        //BEGIN("inflate self");
        myMapFragment = inflater.inflate(R.layout.emptyfragment, container, false);
        //END();

        if (false){
            {

                final boolean reusingInnerMapFragment;
                final boolean mustGetMap;
                //BEGIN("inflate self");
                myMapFragment = inflater.inflate(R.layout.emptyfragment, container, false);
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

                int children = container.getChildCount();
                String.valueOf(children);
                for (int i=0;i<children;i++) {
                    Object o = container.getChildAt(i);
                    String.valueOf(o);
                }
                //BUG: preventing GCing of map fragments!
                // gcStopper.add(innerMapFragment);



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
        }
        else {
            myMapFragment = inflater.inflate(R.layout.emptyfragment, container, false);

           // createInnerGoogleMapFragment();
        }

        this.setRetainInstance(true); // seems to help with map loading delay?

        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!
        }
        //  myMapFragment.setRetainInstance(true);
        Log.d("CPU", "CreateView completed");
        return  myMapFragment;
        // return inflater.inflate(R.layout.mapfragment, container, false);
    }


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
        createInnerGoogleMapFragment();
        END();
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

    private class OnMapReadyCallbackImpl implements OnMapReadyCallback {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            setupMap();



        }// end onMapReady
    }

    // we may not have had to get the map, but we still need to set zoom etc
    public void setupMap() {
        Log.d("MAP", "SetupMap Called " + fragmentName);
        BEGIN("Setting up the Map, not moving camera");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String whenOnMapShowsVal = prefs.getString(Keys.KEY_WEAR_WHEN_ON_MAP_SHOWS, Keys.MAP_TRACK_VAL);
        final boolean showGrid = Keys.GRID_NO_TRACK_VAL.equals(whenOnMapShowsVal) || Keys.GRID_TRACK_VAL.equals(whenOnMapShowsVal);
        final boolean showTrack = Keys.GRID_TRACK_VAL.equals(whenOnMapShowsVal) || Keys.MAP_TRACK_VAL.equals(whenOnMapShowsVal);

        if (showGrid) {
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
        }
        else {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }


        map.getUiSettings().setZoomControlsEnabled(false); // for multi page UI
        CameraPosition camPos = CameraPosition.builder()
                .target(SHEFFIELD)
                .zoom(zoom)
                .bearing(0)
                .tilt(tilt)
                .build();

        CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);



        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.d("TAG", "Camera Changed for Map " + fragmentName);

                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        Log.d("MAP", "MapContainingFragment" + fragmentName + " Loaded");
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

        // map.animateCamera(cam);
        Log.d("MAP", "SetupMap " + fragmentName + " moving camera to zoom" + zoom);

        END();
        BEGIN("Moving Camera..possibly");
        map.moveCamera(cam);
        END();
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
        if (map != null) {
            CameraPosition camPos = CameraPosition.builder()
                    .target(SHEFFIELD)
                    .zoom(zoom)
                    .bearing(0)
                    .tilt(tilt)
                    .build();

            CameraUpdate cam = CameraUpdateFactory.newCameraPosition(camPos);
            Log.d("MAP", "setZoom " + fragmentName + " moving camera to zoom" + zoom);
            map.moveCamera(cam);
        }
    }
    public int getZoom() {
        return zoom;
    }
}



