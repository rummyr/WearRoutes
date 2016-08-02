package uk.me.ponies.wearroutes.controller;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;

import uk.me.ponies.wearroutes.MainGridPagerAdapter;
import uk.me.ponies.wearroutes.eventBusEvents.AmbientEvent;
import uk.me.ponies.wearroutes.eventBusEvents.FlushLogsEvent;
import uk.me.ponies.wearroutes.eventBusEvents.UpdateDisplayedData;
import uk.me.ponies.wearroutes.historylogger.CSVLogger;
import uk.me.ponies.wearroutes.historylogger.GPXLogger;
import uk.me.ponies.wearroutes.historylogger.LatLngLogger;

/**
 * Created by rummy on 07/07/2016.
 */
public class Controller {
    private static final String TAG = Controller.class.getSimpleName();
    private static Controller instance;
    final MainGridPagerAdapter mPagerAdapter;
    private Context mContext;
    private CSVLogger mCSVLogger;
    private GPXLogger mGPXLogger;
    private final long UPDATE_INTERVAL_WHEN_ON = 1 * 1000;
    private final long UPDATE_INTERVAL_WHEN_AMBIENT = 30 * 1000;

    private long LOG_FLUSH_FREQUENCY_MS = 60*1000;
    private Ticker mUpdateDisplayTicker;
    private Ticker mFlushLogsTicker;
    private FlushLogsEvent flushLogsEvent = new FlushLogsEvent();
    private Runnable sendFlushLogsEvent = new Runnable() { public void run() {
        if (EventBus.getDefault().hasSubscriberForEvent(FlushLogsEvent.class)) {
            EventBus.getDefault().post(flushLogsEvent);
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
        // register ourselves with the eventbus .. at a minimum we want to know when we go ambient
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
            boolean ok = logDir.mkdirs();
            String.valueOf(ok); // optimizer dead variable defeater for debugging
        }

        //DISABLED mCSVLogger = new CSVLogger(logDir); // mContext.getFilesDir());
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
        // mPagerAdapter.getPanel1().stopChronometer();
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


    private long getChronometerTimeBase() {
        //TODO: fix this for "paused" calculations
        return SystemClock.elapsedRealtime();
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void startUpdateDisplayEventTicker() {
        final UpdateDisplayedData mEvent = new UpdateDisplayedData();
        mUpdateDisplayTicker = new Ticker(new Runnable() { public void run() {
            if (EventBus.getDefault().hasSubscriberForEvent(UpdateDisplayedData.class)) {
                EventBus.getDefault().post(mEvent);
            }
        }},UPDATE_INTERVAL_WHEN_ON);
        mUpdateDisplayTicker.startTicker();
    }

    private void changeEventTickerFrequencyMs(long newFrequencyMs) {
        mUpdateDisplayTicker.changeTickerFrequencyMs(newFrequencyMs);
    }


    public void startFlushLogsTicker() {
        final FlushLogsEvent mEvent = new FlushLogsEvent();
        mFlushLogsTicker = new Ticker(sendFlushLogsEvent,LOG_FLUSH_FREQUENCY_MS);
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
                changeEventTickerFrequencyMs(UPDATE_INTERVAL_WHEN_AMBIENT);
                break;
            }
            case AmbientEvent.LEAVE: {
                changeEventTickerFrequencyMs(UPDATE_INTERVAL_WHEN_ON);
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
