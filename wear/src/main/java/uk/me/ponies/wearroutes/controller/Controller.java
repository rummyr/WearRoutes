package uk.me.ponies.wearroutes.controller;

import android.os.SystemClock;
import android.util.Log;

import uk.me.ponies.wearroutes.MainGridPagerAdapter;

/**
 * Created by rummy on 07/07/2016.
 */
public class Controller {
    private static final String TAG = Controller.class.getSimpleName();
    private static Controller instance;
    final MainGridPagerAdapter mPagerAdapter;

    private Controller(final MainGridPagerAdapter pagerAdapter) {
        mPagerAdapter = pagerAdapter;
    }

    public static synchronized void startup(MainGridPagerAdapter pagerAdapter)  {
        instance = new Controller(pagerAdapter);
    }
    public static synchronized Controller getInstance() {
        if (instance == null) {
            Log.e(TAG, "NO INSTANCE of Controller, that's pretty fatal!");
        }
        return instance;
    }

    public int getRecordingState() {
        return State.recordingState;
    }

    public boolean isRecording() {
        return StateConstants.STATE_RECORDING == State.recordingState;
    }

    public void startRecording() {
        State.recordingState = StateConstants.STATE_RECORDING;
        mPagerAdapter.getStartRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getStopRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getControlButtonsFragment().updatePage(State.recordingState);

        State.recordingPausedTime = 0;
        State.recordingStartTime = SystemClock.elapsedRealtime();
        mPagerAdapter.getPanel1().startChronometer(State.recordingStartTime);
        //TODO: much more stuff here
        //TODO: set the average speed and distance to recording
        //TODO: set the log to recording
    }

    public void stopRecording(long stopTime) {
        State.recordingState = StateConstants.STATE_STOPPED;
        mPagerAdapter.getStartRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getStopRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getControlButtonsFragment().updatePage(State.recordingState);
        mPagerAdapter.getPanel1().stopChronometer();
        //TODO: much more stuff here
        //TODO: stop the log recording
        //TODO: stop the average speed and distance to recording
        //TODO: Confirmation dialog to the user!
        // -- perhaps see http://stackoverflow.com/questions/27945471/showing-android-wear-style-alertdialog
        // for a layout

    }


    private long getChronometerTimeBase() {
        //TODO: fix this for "paused" calculations
        return SystemClock.elapsedRealtime();
    }
}
