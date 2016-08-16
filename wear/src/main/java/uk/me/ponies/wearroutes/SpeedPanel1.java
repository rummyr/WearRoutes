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

import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;
import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;
import uk.me.ponies.wearroutes.historylogger.LatLngLogger;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.FragmentLifecycleLogger;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class SpeedPanel1 extends FragmentLifecycleLogger implements IGridViewPagerListener, IAmbientHandler {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private static final String TAG = "SpeedPanel1";
    //TextClock mClockView; // probably not required
    // mChronometer;
    TextView mSpeedInstant;
    TextView mSpeedAverage;
    TextView mDistance;

    private String mSavedPreviousSpeedString = "N/A";
    private final String KPH = "kph";
    private final String MPH = "mph";
    private final String KM = "km";
    private final String MILES = "mls";
    private final NumberFormat speedFormatKPH = new DecimalFormat("###.# " + KPH);
    private final NumberFormat speedFormatMPH = new DecimalFormat("###.# " + MPH);
    private final NumberFormat distanceFormatKM = new DecimalFormat("###.## " + KM);
    private final NumberFormat distanceFormatMiles = new DecimalFormat("###.## " + MILES);
    private final SimpleDateFormat chronometerTimeFormat = new SimpleDateFormat("HH:mm:ss");
    private TextView mSpeedInstantLabel;
    private TextView mDistanceLabel;
    private TextView mSpeedAverageLabel;
    private TextView mChronometerLabel;
    private TextView mClockLabel;

    {
        chronometerTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private String mSavedPreviousDistanceString = "-";
    private TableLayout mSpeedPanel1Table;
    private TextView mStaticClock; // only updated when we get a updateDisplayEvent
    private TextView mStaticChronometer; // only updated when we get a updateDisplayEvent
    private Calendar mCalendar = Calendar.getInstance(); // to avoid needing to get one too often
    private Rect tmpVisibilityRect = new Rect();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.speed_panel_with_labels, container, false);
        //TODO: make labels optional and make "gone" if disabled
        mSpeedPanel1Table = (TableLayout) v.findViewById(R.id.panelTableLayout);

//        mClockView = ((TextClock)v.findViewById(R.id.panel1TextClock));
//        mClockView.setVisibility(View.VISIBLE);
//        mChronometer = ((Chronometer)v.findViewById(R.id.panel1Chronometer));
//        mChronometer.setBase(SystemClock.elapsedRealtime() - 1000*60*60);

        mSpeedInstant = (TextView) v.findViewById(R.id.panel1SpeedInstant);
        mSpeedInstantLabel = (TextView) v.findViewById(R.id.labelSpeed);
        mSpeedAverage = (TextView) v.findViewById(R.id.panel1SpeedAverage);
        mSpeedAverageLabel = (TextView) v.findViewById(R.id.labelAvgSpeed);
        mDistance = (TextView) v.findViewById(R.id.panel1Distance);
        mDistanceLabel = (TextView)v.findViewById(R.id.labelDistance);


        mStaticClock = (TextView) v.findViewById(R.id.panel1StaticClock);
        mClockLabel = (TextView)v.findViewById(R.id.labelClock);
        mStaticChronometer = (TextView) v.findViewById(R.id.panel1StaticChronometer);
        mChronometerLabel = (TextView)v.findViewById(R.id.labelChronometer);
        // mChronometer.start();
        return v;
    }


    @Override
    public void onPause() {
        if (tagEnabled(TAG)) Log.d(TAG, "onPause called");
        super.onPause();
        EventBus.getDefault().unregister(this);
        // should we also pause the Clock and Chronometer and so on?
    }

    @Override
    public void onResume() {
        if (tagEnabled(TAG)) Log.d(TAG, "onResume called");
        EventBus.getDefault().register(this);
        super.onResume();
    }


    /* restarts the clock. This is CRUDELY done by setting the clock to VISIBLE */
    @Override
    public void onOnScreenPage() {
        if (mSpeedInstant != null) {
            mSpeedInstant.setText(mSavedPreviousSpeedString); // restore this
        }
        if (mDistance != null) {
            mDistance.setText(mSavedPreviousDistanceString);
        }

    }

    @Override
    /* stops the clock. This is CRUDELY done by setting the clock to GONE */
    public void onOffScreenPage() {
    }


    @Subscribe
    public void event(AmbientEvent evt) {
        switch (evt.getType()) {
            case AmbientEvent.ENTER_AMBIENT: {
                handleEnterAmbientEvent(evt.getBundle());
                break;
            }
            case AmbientEvent.LEAVE_AMBIENT: {
                handleExitAmbientEvent();
                break;
            }
            case AmbientEvent.UPDATE: {
                handleUpdateAmbientEvent();
                break;
            }
        }
    }

    @Override
    public void handleEnterAmbientEvent(Bundle ambientDetails) {
        // set colours to black on white
        int fgColor = Color.WHITE;
        int bgColor = Color.BLACK;
        // mChronometer.setTextColor(fgColor);
        mStaticChronometer.setTextColor(fgColor);
        mSpeedAverage.setTextColor(fgColor);
        mDistance.setTextColor(fgColor);
        mSpeedInstant.setTextColor(fgColor);
        // mClockView.setTextColor(fgColor);
        mStaticClock.setTextColor(fgColor);
        mSpeedPanel1Table.setBackgroundColor(bgColor);

        mSpeedInstantLabel.setTextColor(fgColor);
        mDistanceLabel.setTextColor(fgColor);
        mSpeedAverageLabel.setTextColor(fgColor);
        mChronometerLabel.setTextColor(fgColor);
        mClockLabel.setTextColor(fgColor);
    }

    @Override
    public void handleUpdateAmbientEvent() {

    }

    @Override
    public void handleExitAmbientEvent() {
        // set colours to black on white
        int fgColor = Color.BLACK;
        int bgColor = Color.WHITE;
        //mChronometer.setTextColor(fgColor);
        mStaticChronometer.setTextColor(fgColor);
        mSpeedAverage.setTextColor(fgColor);
        mDistance.setTextColor(fgColor);
        mSpeedInstant.setTextColor(fgColor);
        // mClockView.setTextColor(fgColor);
        mStaticClock.setTextColor(fgColor);
        mSpeedPanel1Table.setBackgroundColor(bgColor);
        mSpeedInstantLabel.setTextColor(fgColor);
        mDistanceLabel.setTextColor(fgColor);
        mSpeedAverageLabel.setTextColor(fgColor);
        mChronometerLabel.setTextColor(fgColor);
        mClockLabel.setTextColor(fgColor);
    }

    @Subscribe
    public void newLocation(LocationEvent locationEvent) {
        Location l = locationEvent.getLocation();
        final String speedUnits = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Keys.KEY_PREF_SPEED_UNITS, Keys.KEY_PREF_SPEED_UNIT_KPH);

        final String speedString;
        if (l.hasSpeed()) {
            float speedMetersPerSecond = l.getSpeed();
            if (Keys.KEY_PREF_SPEED_UNIT_KPH.equals(speedUnits)) {
                speedString = speedFormatKPH.format(speedMetersPerSecond * 3600 / 1000);
            } else {
                speedString = speedFormatMPH.format(speedMetersPerSecond * 2.2369);
            }
        } else {
            if (Keys.KEY_PREF_SPEED_UNIT_KPH.equals(speedUnits)) {
                speedString = "- " + KPH;
            } else {
                speedString = "- " + MPH;
            }
        }

        mSavedPreviousSpeedString = speedString; // for resume
        // update only if actually needed
        if (!speedString.equals(mSpeedInstant.getText())) {
            mSpeedInstant.setText(speedString);
        }

        // when the location changes, *that* is when we update the GPS related data
        updateGPSRelatedData();
    }


    private void updateGPSRelatedData() {
        final String distanceUnits = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Keys.KEY_PREF_DISTANCE_UNITS, Keys.KEY_PREF_DISTANCE_UNIT_KM);
        final String speedUnits = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Keys.KEY_PREF_SPEED_UNITS, Keys.KEY_PREF_SPEED_UNIT_KPH);


        LatLngLogger lll = Controller.getInstance().getLatLngLogger();

        float cumulativeDistanceMeters = lll == null ? 0 : lll.getCumulativeDistanceMeters();

        final String distanceString;
        if (Keys.KEY_PREF_DISTANCE_UNIT_KM.equals(distanceUnits)) {
            distanceString = lll == null ? "N/A " + KM : distanceFormatKM.format(cumulativeDistanceMeters * 0.001);
        } else {
            distanceString = lll == null ? "N/A " + MILES : distanceFormatMiles.format(cumulativeDistanceMeters * 0.000621371);
        }

        mSavedPreviousDistanceString = distanceString; // for resume


        // update only if actually needed
        if (!distanceString.equals(mDistance.getText())) {
            mDistance.setText(distanceString);
        }

        // update average speed
        double recordingTimeSeconds = Controller.getInstance().getRecordingDurationMs() / 1000.0;
        double averageSpeedMetersPerSecond = cumulativeDistanceMeters / recordingTimeSeconds;

        final String avgSpeedString;
        if (Keys.KEY_PREF_SPEED_UNIT_KPH.equals(speedUnits)) {
            avgSpeedString = lll == null ? "N/A " + KPH : speedFormatKPH.format(averageSpeedMetersPerSecond * 3600 / 1000);
        } else {
            avgSpeedString = lll == null ? "N/A " + MPH : speedFormatMPH.format(averageSpeedMetersPerSecond * 2.2369);
        }
        if (!avgSpeedString.equals(mSpeedAverage.getText())) {
            mSpeedAverage.setText(avgSpeedString);
        }
    }


    @Subscribe
    public void eventUpdateDisplayedData(UpdateDisplayedData notUsed) {

        View v = getView();
        if (v == null) {
            return; // no view, can't update
        }
        boolean isVisibleToUser = v.getLocalVisibleRect(tmpVisibilityRect);

        // only if visible!!!
        if (!isVisibleToUser) {
            return;
        }

        updateClockRelatedData();


        // distance and avg speed are updated when the location changes

    }// end eventUpdateDisplayedData

    private void updateClockRelatedData() {
        //TODO: Localization for Clock
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        mStaticClock.setText(android.text.format.DateFormat.format("HH:mm:ss", mCalendar));

        long recordingDuration = Controller.getInstance().getRecordingDurationMs();
        if (recordingDuration <= 0) {
            mStaticChronometer.setText(R.string.chronometerNotStarted);
        } else {
            CharSequence newText = DateUtils.formatElapsedTime(recordingDuration / 1000);
            if (!mStaticChronometer.getText().equals(newText)) {
                mStaticChronometer.setText(newText);
            }
        }
    }

}
