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
import android.os.SystemClock;
import android.transition.Transition;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextClock;


public class SpeedPanel1 extends Fragment implements GridViewPagerListener {
    private static final String TAG = "SpeedPanel1";
    TextClock mClockView; // probably not required
    Chronometer mChronometer;
    private boolean mPaused;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.speed_panel1, container, false);
        mClockView = ((TextClock)v.findViewById(R.id.panel1TextClock));
        mClockView.setVisibility(View.VISIBLE);
        mChronometer = ((Chronometer)v.findViewById(R.id.panel1Chronometer));
        mChronometer.setBase(SystemClock.elapsedRealtime() - 1000*60*60);
        // mChronometer.start();
        return v;
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.d(TAG,"onHiddenChanged called" );
        super.onHiddenChanged(hidden);
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        Log.d(TAG,"setMenuVisibility called" );
        super.setMenuVisibility(menuVisible);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.d(TAG,"setUserVisibleHint called" );
        super.setUserVisibleHint(isVisibleToUser);
    }


    @Override
    public void onAttach(Context context) {
        Log.d(TAG,"onAttach called" );
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        Log.d(TAG,"onStart called" );
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG,"onStop called" );
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy called" );
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView called" );
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.d(TAG,"onDetach called" );
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG,"onCreateOptionsMenu called" );
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG,"onPrepareOptionsMenu called" );
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(TAG,"onCreateContextMenu called" );
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void setEnterTransition(Transition transition) {
        Log.d(TAG,"setEnterTransition called" );
        super.setEnterTransition(transition);
    }

    @Override
    public void setReturnTransition(Transition transition) {
        Log.d(TAG,"setReturnTransition called" );
        super.setReturnTransition(transition);
    }

    @Override
    public void setExitTransition(Transition transition) {
        Log.d(TAG,"setExitTransition called" );
        super.setExitTransition(transition);
    }

    @Override
    public void setReenterTransition(Transition transition) {
        Log.d(TAG,"setReenterTransition called" );
        super.setReenterTransition(transition);
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause called" );
        super.onPause();
        mPaused = true;
        // should we also pause the Clock and Chronometer and so on?
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume called" );
        super.onResume();
        mPaused = false;
    }

    public void startChronometer(long baseTime) {
        mChronometer.setBase(baseTime);
        mChronometer.start();
    }

    public void stopChronometer() {
        mChronometer.stop();
    }

    /* restarts the clock. This is CRUDELY done by setting the clock to VISIBLE */
    @Override
    public void onOnScreenPage() {
        mClockView.setVisibility(View.VISIBLE);
    }

    @Override
    /* stops the clock. This is CRUDELY done by setting the clock to GONE */
    public void onOffScreenPage() {
        mClockView.setVisibility(View.GONE);
    }
}
