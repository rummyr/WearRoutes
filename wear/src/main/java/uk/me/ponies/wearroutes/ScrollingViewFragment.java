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
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;


public class ScrollingViewFragment extends Fragment {
    private static final LatLng SHEFFIELD = new LatLng(53.5054747,-1.6915511);
    private  View myMapFragment;


    int zoom = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
        Log.d("TAG", "ScrollingViewFragment onCreateView called");
        myMapFragment = inflater.inflate(R.layout.scrollable_fragment, container, false);


        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!
        }
      //  myMapFragment.setRetainInstance(true);
        return  myMapFragment;
        // return inflater.inflate(R.layout.mapfragment, container, false);
    }



    @Override
    public void onAttach(Context context) {
        Log.d("TAG", "ScrollingViewFragment: onAttach called");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.d("TAG","ScrollingViewFragment: onDetach called");
        super.onDetach();
    }

    @Override
    public void onStart() {
        Log.d("TAG","ScrollingViewFragment: onStart called with zoom:" + zoom);
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d("TAG","ScrollingViewFragment: onResume called");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d("TAG","ScrollingViewFragment: onPause called");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("TAG","ScrollingViewFragment: onStop called");
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("TAG","ScrollingViewFragment: onSaveInstanceState called");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        Log.d("TAG","ScrollingViewFragment: onDestroyView called");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d("TAG","ScrollingViewFragment: onDestroy called");
        super.onDestroy();
    }

    public String toString() {
        return "ScrollingViewFragment";
    }

}
