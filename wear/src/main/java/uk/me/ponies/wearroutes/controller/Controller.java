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
import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;
import uk.me.ponies.wearroutes.historylogger.CSVLogger;
import uk.me.ponies.wearroutes.historylogger.GPXLogger;
import uk.me.ponies.wearroutes.historylogger.LatLngLogger;
import uk.me.ponies.wearroutes.historylogger.SimpleTextLogger;
import uk.me.ponies.wearroutes.prefs.Keys;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

/**
 * Handles quite a lot of the coordination
 */
public class Controller  {
    private static final String TAG = Controller.class.getSimpleName();
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private final String WAKELOCK_NAME = "RecordingWakelock";

    /** Singleton instance of this class.. persists for far too long :) . */
    private static Controller instance;

    /** The pagerAdapter, used to get various fragments. NOT final because the android lifecycle
    may require us to have a new one (possibly, I'm not very clear on this).
     */
    MainGridPagerAdapter mPagerAdapter;



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

    @Nullable
    public LatLngLogger getLatLngLogger() {
        return mLatLngLogger;
    }

    private LatLngLogger mLatLngLogger;

    private Controller(final MainGridPagerAdapter pagerAdapter, Context context) {
        super();
        setPagerAdapter(pagerAdapter);
        mWeakContext = new WeakReference<>(context);
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
        }


    }
    @Nullable  public static synchronized Controller getInstance() {
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

        if (mWeakContext.get() != null) {
            boolean keepAwakeWhenRecording = PreferenceManager.getDefaultSharedPreferences(mWeakContext.get())
                    .getBoolean(Keys.KEY_KEEP_AWAKE_WHEN_RECORDING, false);
            if (keepAwakeWhenRecording) {
                acquireRecordingPartialWakeLock();
            }
        }

        mPagerAdapter.getStartRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getStopRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getControlButtonsFragment().updatePage(State.recordingState);

        State.recordingPausedTime = 0;
        State.recordingStartTime = SystemClock.elapsedRealtime();



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
        State.recordingState = StateConstants.STATE_STOPPED;

        releaseRecordingPartialWakeLock();

        mPagerAdapter.getStartRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getStopRecordingFragment().updateButton(State.recordingState);
        mPagerAdapter.getControlButtonsFragment().updatePage(State.recordingState);
        State.recordingEndTime = SystemClock.elapsedRealtime();

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

    public void startUpdateDisplayEventTicker() {
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


    public void startFlushLogsTicker() {
        long flushInterval =  Options.LOG_FLUSH_FREQUENCY_SECS;
        if (Build.MODEL.startsWith("sdk_")) flushInterval = 1;
        mFlushLogsTicker = new Ticker(sendFlushLogsEvent, TimeUnit.SECONDS.toMillis(flushInterval));
        mFlushLogsTicker.startTicker();
    }


    public long getRecordingDurationMs() {
        if (isRecording()) {
            return SystemClock.elapsedRealtime() - State.recordingStartTime;
        }
        else if (State.recordingStartTime == 0) {
            return 0;
        }
        else {
            return State.recordingEndTime - State.recordingStartTime;
        }
    }

    @Subscribe
    public void event(AmbientEvent evt) {
        switch (evt.getType()) {
            case AmbientEvent.ENTER_AMBIENT: {
                changeUpdateDisplayTickerFrequencyMs(TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS));
                break;
            }
            case AmbientEvent.LEAVE_AMBIENT: {
                changeUpdateDisplayTickerFrequencyMs(TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_ON_SECS));
                // and tickle the ticker
                break;
            }
            case AmbientEvent.UPDATE: {
                // hmm!
                break;
            }
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

        if (recordingWakeLock != null) { // redundant check, but what the hell!
            recordingWakeLock.acquire();
        }
    }
    private void releaseRecordingPartialWakeLock() {
        if (recordingWakeLock != null) {
            recordingWakeLock.release();
        }
    }

    // BUG: NYI
    public void enterBackgroundRecording() {
        EventBus.getDefault().unregister(instance);
        mUpdateDisplayTicker.stopTicker();
        mFlushLogsTicker.stopTicker();
    }

    //BUG: NYI
    public void leaveBackgroundRecording() {
    }

    /* called by Main.onStop usually IFF not recording. */
    public void shutdown() {
        releaseRecordingPartialWakeLock();
        EventBus.getDefault().unregister(instance);
        recordingWakeLock = null;
        mCSVLogger  = null;
        mGPXLogger = null;
        mPagerAdapter = null;
        if (mUpdateDisplayTicker != null) {
            mUpdateDisplayTicker.stopTicker();
        }
        mUpdateDisplayTicker = null;
        if (mFlushLogsTicker != null) {
            mFlushLogsTicker.stopTicker();
        }
        mFlushLogsTicker = null;
        FLUSH_LOGS_EVENT = null;

        // and actually quite critical
        // because it is a STATIC until the class is GC'd this doesn't go away!
        instance = null;
    }
}
