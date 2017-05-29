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
import android.os.Bundle;
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

import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.Stats;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;
import uk.me.ponies.wearroutes.utils.FragmentLifecycleLogger;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class StatsPanel1 extends FragmentLifecycleLogger implements IGridViewPagerListener {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private static final String TAG = "StatsPanel1";

    private final NumberFormat TTFFormat = new DecimalFormat("##.# " + " sec");

    private TextView mLastTTF;
    private TextView mLastTTFLabel;

    private TextView mWakeLockTime;
    private TextView mWakeLockTimeLabel;

    private TextView mBGFixCount;
    private TextView mBGFixCountLabel;


    private TextView mBGPollInterval;
    private TextView mBGPollIntervalLabel;

    private TextView mFGFixCount;
    private TextView mFGFixCountLabel;

    private TextView mPollingMode;
    private TextView mPollingModeLabel;


    private TextView mSkippedFixes;
    private TextView mSkippedFixesLabel;

    private TextView mTimedOutFixes;
    private TextView mTimedOutFixesLabel;

    private TableLayout mTable;
    private Rect tmpVisibilityRect = new Rect();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.stats_panel1_with_labels, container, false);
        //TODO: make labels optional and make "gone" if disabled
        mTable = (TableLayout) v.findViewById(R.id.statsPanel1TableLayout);


        mLastTTFLabel = (TextView) v.findViewById(R.id.statsPanel1LastTTFLabel);
        mLastTTF = (TextView) v.findViewById(R.id.statsPanel1LastTTFValue);

        mWakeLockTime = (TextView) v.findViewById(R.id.statsPanelWakeLockTimeValue);
        mWakeLockTimeLabel = (TextView) v.findViewById(R.id.statsPanelWakeLockTimeLabel);

        mBGFixCount = (TextView) v.findViewById(R.id.statsPanelBackgroundFixCountValue);
        mBGFixCountLabel = (TextView) v.findViewById(R.id.statsPanelBackgroundFixCountLabel);
        mBGPollInterval = (TextView) v.findViewById(R.id.statsPanelBackgroundPollIntervalValue);
        mBGPollIntervalLabel = (TextView) v.findViewById(R.id.statsPanelBackgroundPollIntervalLabel);

        mFGFixCount = (TextView) v.findViewById(R.id.statsPanelForegroundFixCountValue);
        mFGFixCountLabel = (TextView) v.findViewById(R.id.statsPanelForegroundFixCountLabel);


        mPollingMode = (TextView) v.findViewById(R.id.statsPanelPollingModeValue);
        mPollingModeLabel = (TextView) v.findViewById(R.id.statsPanelPollingModeLabel);

        mSkippedFixes = (TextView) v.findViewById(R.id.statsPanelSkippedFixCountValue);
        mSkippedFixesLabel = (TextView) v.findViewById(R.id.statsPanelSkippedFixCountLabel);

        mTimedOutFixes= (TextView) v.findViewById(R.id.statsPanelFixTimeoutValue);
        mTimedOutFixesLabel= (TextView) v.findViewById(R.id.statsPanelFixTimeoutLabel);
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

        eventUpdateDisplayedData(null);

    }

    @Override

    public void onOffScreenPage() {
    }


    @Subscribe
    public void event(AmbientEvent evt) {
        switch (evt.getType()) {
            case AmbientEvent.ENTER_AMBIENT: {
                // set colours to white on black, no anti alias
                adjustColoursAndAntiAlias(Color.WHITE, Color.BLACK, false);
                break;
            }
            case AmbientEvent.LEAVE_AMBIENT: {
                // set colours to black on white , yes anti alias
                adjustColoursAndAntiAlias(Color.BLACK, Color.WHITE, true);
                break;
            }
            case AmbientEvent.UPDATE: {
                break;
            }
        }
    }

    /** simple method to group all colour/antialias changes into one place */
    private void adjustColoursAndAntiAlias(int fgColor, int bgColor, boolean antialias) {
        mTable.setBackgroundColor(bgColor);


        mLastTTF.setTextColor(fgColor);
        mLastTTF.getPaint().setAntiAlias(antialias);
        mLastTTFLabel.setTextColor(fgColor);
        mLastTTFLabel.getPaint().setAntiAlias(antialias);

        mWakeLockTime.setTextColor(fgColor);
        mWakeLockTime.getPaint().setAntiAlias(antialias);
        mWakeLockTimeLabel.setTextColor(fgColor);
        mWakeLockTimeLabel.getPaint().setAntiAlias(antialias);

        mBGFixCount.setTextColor(fgColor);
        mBGFixCount.getPaint().setAntiAlias(antialias);
        mBGFixCountLabel.setTextColor(fgColor);
        mBGFixCountLabel.getPaint().setAntiAlias(antialias);

        mBGPollInterval.setTextColor(fgColor);
        mBGPollInterval.getPaint().setAntiAlias(antialias);
        mBGPollIntervalLabel.setTextColor(fgColor);
        mBGPollIntervalLabel.getPaint().setAntiAlias(antialias);

        mFGFixCount.setTextColor(fgColor);
        mFGFixCount.getPaint().setAntiAlias(antialias);
        mFGFixCountLabel.setTextColor(fgColor);
        mBGFixCount.getPaint().setAntiAlias(antialias);

        mPollingMode.setTextColor(fgColor);
        mPollingMode.getPaint().setAntiAlias(antialias);
        mPollingModeLabel.setTextColor(fgColor);
        mPollingModeLabel.getPaint().setAntiAlias(antialias);

        mSkippedFixes.setTextColor(fgColor);
        mSkippedFixes.getPaint().setAntiAlias(antialias);
        mSkippedFixesLabel.setTextColor(fgColor);
        mSkippedFixesLabel.getPaint().setAntiAlias(antialias);

        mTimedOutFixes.setTextColor(fgColor);
        mTimedOutFixes.getPaint().setAntiAlias(antialias);
        mTimedOutFixesLabel.setTextColor(fgColor);
        mTimedOutFixesLabel.getPaint().setAntiAlias(antialias);

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
        Controller c = Controller.getInstance();
        if (c == null) {
            return;
        }


        String newText = TTFFormat.format(Stats.getLastBackgroundTTFMs()/1000.0);
        if (!newText.equals(mLastTTF.getText())) {
            mLastTTF.setText(newText);
        }

        newText = String.valueOf(Stats.getBackgroundFixCount());
        if (!newText.equals(mBGFixCount.getText())) {
            mBGFixCount.setText(newText);
        }

        newText = String.valueOf(Stats.getBackgroundPollingIntervalSecs());
        if (!newText.equals(mBGPollInterval.getText())) {
            mBGPollInterval.setText(newText);
        }

        newText = String.valueOf(Stats.getForegroundFixCount());
        if (!newText.equals(mFGFixCount.getText())) {
            mFGFixCount.setText(newText);
        }


        newText = Stats.getCurrentPollingMode();
        if (!newText.equals(mPollingMode.getText())) {
            mPollingMode.setText(newText);
        }

        newText = String.valueOf(Stats.getUnacceptableLocations());
        if (!newText.equals(mSkippedFixes.getText())) {
            mSkippedFixes.setText(newText);
        }

        newText = String.valueOf(Stats.getBackgroundFixTimeoutCount());
        if (!newText.equals(mTimedOutFixes.getText())) {
            mTimedOutFixes.setText(newText);
        }

        long wakelockTimeMs = Stats.getWakeLockHeldTime();
        newText = DateUtils.formatElapsedTime(wakelockTimeMs / 1000);
        if (!mWakeLockTime.getText().equals(newText)) {
            mWakeLockTime.setText(newText);
        }

    }// end eventUpdateDisplayedData


}
