package uk.me.ponies.wearroutes.controller;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.MainGridPagerAdapter;
import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.FlushLogsEvent;
import uk.me.ponies.wearroutes.eventBusEvents.LocationPollingStateEvent;
import uk.me.ponies.wearroutes.historylogger.LogEvent;
import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;
import uk.me.ponies.wearroutes.historylogger.CSVLogger;
import uk.me.ponies.wearroutes.historylogger.GPXLogger;
import uk.me.ponies.wearroutes.historylogger.LatLngLogger;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;


/**
 * Handles quite a lot of the coordination
 */
public class Controller  {
    private static final String TAG = Controller.class.getSimpleName();
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    @SuppressWarnings("FieldCanBeLocal")
    private final String WAKELOCK_NAME = "RecordingWakelock";

    /** Singleton instance of this class.. persists for far too long :) . */
    private static Controller instance;

    /** The pagerAdapter, used to get various fragments. NOT final because the android lifecycle
    may require us to have a new one (possibly, I'm not very clear on this).
     */
    //BUG: we really should get the pager or it's internal components to handle button state changes!!!
    private MainGridPagerAdapter mPagerAdapter;

    private boolean isAmbient = false;
    private boolean isSilent = false; // indicates TOTAL background mode


    private CSVLogger mCSVLogger;
    private GPXLogger mGPXLogger;


    private PowerManager.WakeLock recordingWakeLock;
    private Ticker mUpdateDisplayTicker;
    private Ticker mFlushLogsTicker;
    private FlushLogsEvent FLUSH_LOGS_EVENT = new FlushLogsEvent();
    private Runnable sendFlushLogsEvent = new Runnable() { public void run() {
        if (EventBus.getDefault().hasSubscriberForEvent(FlushLogsEvent.class)) {
            EventBus.getDefault().post(FLUSH_LOGS_EVENT);
        }}};

    private WeakReference<Context> mWeakContext = null;
    private static final boolean NOT_BACKGROUNDED = false;

    @Nullable
    public LatLngLogger getLatLngLogger() {
        return mLatLngLogger;
    }

    private LatLngLogger mLatLngLogger;

    private Controller(final MainGridPagerAdapter pagerAdapter, Context context) {
        super();
        setPagerAdapter(pagerAdapter);
        mWeakContext = new WeakReference<>(context);
        updateLocationPollerState(isSilent, "Controller.<init>");
    }

    private void setPagerAdapter(MainGridPagerAdapter pagerAdapter) {
        mPagerAdapter = pagerAdapter;
    }



    public static synchronized void startup(MainGridPagerAdapter pagerAdapter, Context context)  {
        if (instance == null) {
            instance = new Controller(pagerAdapter, context);
            // potentially we could shutdown the tickers
            // e.g. instance.mUpdateDisplayTicker.stopTicker();

            instance.startUpdateDisplayEventTicker();
            instance.startFlushLogsTicker();
            // register ourselves with the EventBus .. at a minimum we want to know when we go ambient
            EventBus.getDefault().register(instance);
        }
        else {
            // we already exist, just change the main configurable item
            // tickers etc can continue to run.
            instance.setPagerAdapter(pagerAdapter);
            instance.mWeakContext = new WeakReference<>(context);
        }


    }
    @Nullable  public static synchronized Controller getInstance() {
        if (instance == null) {
            Log.e(TAG, "NO INSTANCE of Controller, that's pretty fatal!");
        }
        return instance;
    }

    public int getRecordingState() {
        return ControllerState.recordingState;
    }

    public boolean isRecording() {
        return ControllerState.StateConstants.STATE_RECORDING == ControllerState.recordingState;
    }

    public void startRecording() {
        ControllerState.recordingState = ControllerState.StateConstants.STATE_RECORDING;
        EventBus.getDefault().post(new LogEvent("Starting to record","ACT"));
        updateLocationPollerState(NOT_BACKGROUNDED, "Controller.startRecording");
        
        if (mWeakContext.get() != null) {
            boolean keepAwakeWhenRecording = PreferenceManager.getDefaultSharedPreferences(mWeakContext.get())
                    .getBoolean(Keys.KEY_KEEP_AWAKE_WHEN_RECORDING, false);
            if (keepAwakeWhenRecording) {
                acquireRecordingPartialWakeLock();
                EventBus.getDefault().post(new LogEvent("Acquired keep on on when recording lock","PWR"));
            }
        }

        if (mPagerAdapter.getStartRecordingFragment() != null) {  // UI reconfig may make this null
            mPagerAdapter.getStartRecordingFragment().updateButton(ControllerState.recordingState);
        }
        if (mPagerAdapter.getStopRecordingFragment() != null) {// UI reconfig may make this null
            mPagerAdapter.getStopRecordingFragment().updateButton(ControllerState.recordingState);
        }
        if (mPagerAdapter.getControlButtonsFragment() != null) {// UI reconfig may make this null
            mPagerAdapter.getControlButtonsFragment().updatePage(ControllerState.recordingState);
        }

        ControllerState.recordingPausedTime = 0;
        ControllerState.recordingStartTime = SystemClock.elapsedRealtime();



        File logDir = new File(Environment.getExternalStorageDirectory().getPath() + "/wearRoutes/");
        if (!logDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            logDir.mkdirs();
        }

        //DISABLED mCSVLogger = new CSVLogger(logDir);
        mLatLngLogger = new LatLngLogger();
        mGPXLogger = new GPXLogger(logDir);

        if (mCSVLogger != null) mCSVLogger.setLogging(true);
        mLatLngLogger.setLogging(true);
        mGPXLogger.setLogging(true);
        EventBus.getDefault().register(mLatLngLogger);
        if (mCSVLogger != null) EventBus.getDefault().register(mCSVLogger);
        EventBus.getDefault().register(mGPXLogger);

        //TODO: flip the view to the speedPanel??
    }

    public void stopRecording(long stopTime) {
        ControllerState.recordingState = ControllerState.StateConstants.STATE_STOPPED;
        EventBus.getDefault().post(new LogEvent("Stopping recording","ACT"));
        updateLocationPollerState(isSilent, "Controller.stopRecording");

        releaseRecordingPartialWakeLock();
        EventBus.getDefault().post(new LogEvent("Released keep on on when recording lock","PWR"));

        if (mPagerAdapter.getStartRecordingFragment() != null) {  // UI reconfig may make this null
            mPagerAdapter.getStartRecordingFragment().updateButton(ControllerState.recordingState);
        }
        if (mPagerAdapter.getStopRecordingFragment() != null) {// UI reconfig may make this null
            mPagerAdapter.getStopRecordingFragment().updateButton(ControllerState.recordingState);
        }
        if (mPagerAdapter.getControlButtonsFragment() != null) {// UI reconfig may make this null
            mPagerAdapter.getControlButtonsFragment().updatePage(ControllerState.recordingState);
        }

        ControllerState.recordingEndTime = SystemClock.elapsedRealtime();

        //TODO: much more stuff here
        // -- perhaps see http://stackoverflow.com/questions/27945471/showing-android-wear-style-alertdialog
        // for a layout
        if (mCSVLogger != null) mCSVLogger.setLogging(false);
        if (mLatLngLogger != null) mLatLngLogger.setLogging(false);
        if (mGPXLogger != null) mGPXLogger.setLogging(false);

        sendFlushLogsEvent.run(); // ensure the data is written .. MUST do before unsubscribing :)

        if (mLatLngLogger != null) EventBus.getDefault().unregister(mLatLngLogger);
        if (mCSVLogger != null) EventBus.getDefault().unregister(mCSVLogger);
        if (mGPXLogger != null) EventBus.getDefault().unregister(mGPXLogger);

    }

    private void startUpdateDisplayEventTicker() {
        final UpdateDisplayedData mEvent = new UpdateDisplayedData();
        mUpdateDisplayTicker = new Ticker(new Runnable() { public void run() {
            if (EventBus.getDefault().hasSubscriberForEvent(UpdateDisplayedData.class)) {
                EventBus.getDefault().post(mEvent);
            }
        }}, TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_ON_SECS));
        mUpdateDisplayTicker.startTicker();
    }

    private void changeUpdateDisplayTickerFrequencyMs(long newFrequencyMs) {
        mUpdateDisplayTicker.changeTickerFrequencyMs(newFrequencyMs);
    }


    private void startFlushLogsTicker() {
        long flushInterval =  Options.LOG_FLUSH_FREQUENCY_SECS;
        if (Build.MODEL.startsWith("sdk_")) flushInterval = 1;
        mFlushLogsTicker = new Ticker(sendFlushLogsEvent, TimeUnit.SECONDS.toMillis(flushInterval));
        mFlushLogsTicker.startTicker();
    }


    public long getRecordingDurationMs() {
        if (isRecording()) {
            return SystemClock.elapsedRealtime() - ControllerState.recordingStartTime;
        }
        else if (ControllerState.recordingStartTime == 0) {
            return 0;
        }
        else {
            return ControllerState.recordingEndTime - ControllerState.recordingStartTime;
        }
    }

    @Subscribe
    public void onAmbientEvent(AmbientEvent evt) {
        switch (evt.getType()) {
            case AmbientEvent.ENTER_AMBIENT: {
                isAmbient = true;
                changeUpdateDisplayTickerFrequencyMs(TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS));
                break;
            }
            case AmbientEvent.LEAVE_AMBIENT: {
                isAmbient = false;
                changeUpdateDisplayTickerFrequencyMs(TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_ON_SECS));

                // and tickle the ticker
                break;
            }
            case AmbientEvent.UPDATE: {

                // hmm!
                break;
            }
        }// end switch
        
        // now update the poller
        if (isSilent) {
            updateLocationPollerState(isSilent, "Controller.onAmbientEvent");
        }
        else {
            updateLocationPollerState(isAmbient, "Controller.onAmbientEvent");
        }
    }

    private void acquireRecordingPartialWakeLock() {
        if (recordingWakeLock == null) {
            // lockStatic doesn't exist yet, try to create it
            if (mWeakContext.get() == null) {
                return; // no context, no way we can create a wakelock!
            }
            // have a context and no lockStatic, create one
            PowerManager powerManager = (PowerManager) mWeakContext.get().getSystemService(Context.POWER_SERVICE);
            recordingWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_NAME);
            if (recordingWakeLock != null) {
                recordingWakeLock.setReferenceCounted(false);
            }
        }

        if (recordingWakeLock != null) { // redundant isNullAndLog, but what the hell!
            recordingWakeLock.acquire();
        }
    }

    private void updateLocationPollerState(boolean backgrounded, String source) {
        final LocationPollingStateEvent newPollingState;
        if (!backgrounded) {
            // always fast when screen is on
            newPollingState = new LocationPollingStateEvent(LocationPollingStateEvent.FAST, "Controller.updateLocationPollerState/" + source);
        }
        else { // screen off (or backgrounded) is either fast, slow or very slow
            boolean updateLessOftenInAmbient = PreferenceManager.getDefaultSharedPreferences(mWeakContext.get()).getBoolean(Keys.KEY_GPS_UPDATES_LESS_IN_AMBIENT, true);

            if (isRecording()) {
                if (!updateLessOftenInAmbient) {
                    newPollingState = new LocationPollingStateEvent(LocationPollingStateEvent.FAST, "Controller.updateLocationPollerState/" + source);
                }
                else {
                    newPollingState = new LocationPollingStateEvent(LocationPollingStateEvent.SLOW, "Controller.updateLocationPollerState/" + source);
                }
            } else { // not recording AND ambient -- very slow
                newPollingState = new LocationPollingStateEvent(LocationPollingStateEvent.VERY_SLOW, "Controller.updateLocationPollerState/" + source);
            }
        }
        EventBus.getDefault().post(newPollingState);
    }
    private void releaseRecordingPartialWakeLock() {
        if (recordingWakeLock != null) {
            recordingWakeLock.release();
        }
    }

    // BUG: NYI
    public void enterSilentMode() {
        // entering silent mode may be called after noUI,
        // so we may not have an updateDisplay Ticker
        if (mUpdateDisplayTicker != null) {
            mUpdateDisplayTicker.stopTicker();
        }
        isSilent = true;
        updateLocationPollerState(isSilent, "Controller.enterSilentMode");
    }

    //BUG: NYI

    public void leaveSilentMode() {
        isSilent = false;
        // can be called on resume from "minimize", so some items can be missing
        startUpdateDisplayEventTicker();
        updateLocationPollerState(isSilent, "Controller.leaveSilentMode");
    }

    /** Frees off the UI aspects, there's only a few */
    public void enterNoUI() {
        mPagerAdapter = null;
        if (mUpdateDisplayTicker != null) {
            mUpdateDisplayTicker.stopTicker();
        }
        mUpdateDisplayTicker = null;
    }


    /* called by Main.onStop usually IFF not recording. */
    public void shutdown() {
        enterNoUI();
        // stop the locationHandler service from polling, we are shutting down
        EventBus.getDefault().post(new LocationPollingStateEvent(LocationPollingStateEvent.OFF, "Controller.shutdown"));

        releaseRecordingPartialWakeLock();
        EventBus.getDefault().unregister(instance);
        recordingWakeLock = null;
        mCSVLogger  = null;
        mGPXLogger = null;

        if (mFlushLogsTicker != null) {
            mFlushLogsTicker.stopTicker();
        }
        mFlushLogsTicker = null;
        FLUSH_LOGS_EVENT = null;

        // and actually quite critical
        // because it is a STATIC until the class is GC'd this doesn't go away!
        instance = null;
    }

    /* Record some Stats for TTF in the background */
    public void statsBackgroundTTFTimeMs(long ttf) {
        Stats.addLastBackgroundTTFMs(ttf);
    }
}
