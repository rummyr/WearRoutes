package uk.me.ponies.wearroutes.controller;

import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.MainGridPagerAdapter;
import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.FlushLogsEvent;
import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;
import uk.me.ponies.wearroutes.historylogger.CSVLogger;
import uk.me.ponies.wearroutes.historylogger.GPXLogger;
import uk.me.ponies.wearroutes.historylogger.LatLngLogger;

/**
 * Handles quite a lot of the coordination
 */
public class Controller {
    private static final String TAG = Controller.class.getSimpleName();
    private static Controller instance;
    final MainGridPagerAdapter mPagerAdapter;

    private CSVLogger mCSVLogger;
    private GPXLogger mGPXLogger;

    private Ticker mUpdateDisplayTicker;
    private Ticker mFlushLogsTicker;
    private static final FlushLogsEvent FLUSH_LOGS_EVENT = new FlushLogsEvent();
    private Runnable sendFlushLogsEvent = new Runnable() { public void run() {
        if (EventBus.getDefault().hasSubscriberForEvent(FlushLogsEvent.class)) {
            EventBus.getDefault().post(FLUSH_LOGS_EVENT);
        }}};

    @Nullable
    public LatLngLogger getLatLngLogger() {
        return mLatLngLogger;
    }

    private LatLngLogger mLatLngLogger;

    private Controller(final MainGridPagerAdapter pagerAdapter) {

        mPagerAdapter = pagerAdapter;
    }

    public static synchronized void startup(MainGridPagerAdapter pagerAdapter)  {
        instance = new Controller(pagerAdapter);

        instance.startUpdateDisplayEventTicker();
        instance.startFlushLogsTicker();
        // register ourselves with the EventBus .. at a minimum we want to know when we go ambient
        EventBus.getDefault().register(instance);

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

    private void changeEventTickerFrequencyMs(long newFrequencyMs) {
        mUpdateDisplayTicker.changeTickerFrequencyMs(newFrequencyMs);
    }


    public void startFlushLogsTicker() {
        mFlushLogsTicker = new Ticker(sendFlushLogsEvent, TimeUnit.SECONDS.toMillis(Options.LOG_FLUSH_FREQUENCY_SECS));
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
            case AmbientEvent.ENTER: {
                changeEventTickerFrequencyMs(TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS));
                break;
            }
            case AmbientEvent.LEAVE: {
                changeEventTickerFrequencyMs(TimeUnit.SECONDS.toMillis(Options.DISPLAY_UPDATE_INTERVAL_WHEN_ON_SECS));
                // and tickle the ticker
                break;
            }
            case AmbientEvent.UPDATE: {
                // hmm!
                break;
            }
        }
    }

}
