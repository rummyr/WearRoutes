package uk.me.ponies.wearroutes.locationService;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import uk.me.ponies.wearroutes.locationHandling.LocationHandler;

/**
 * A Class that puts the Location Polling into a Service.
 * This will remove it from the Activity and hopefully make it more robust against
 * weird activity starts and ends.
 *
 *
 * NOTE: started in WearRoutesApplication!
 */
public class LocationPollingService extends Service {
    static final String TAG = "LocationPollingService";
    LocationHandler handler = null;
    ServiceBinder mBinder = new ServiceBinder();

    /** don't start anything here, just let it be created */
    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // return super.onStartCommand(intent, flags, startId);
        //TODO: check the flag needed here STICKY/NOT_STICKY etc

        // not sure, but it looks like START_STICKY causes WearRoutes to restart when killed
        // NOT_STICKY seems to allow it to be killed (kinda oops)
        Log.d(TAG, "Location Service onStartCommand");
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Location Service onDestroy");
        stopPolling();
        super.onDestroy();
    }

    @Subscribe
    public void onDummyEvent(Object o) {}

    /******************** Control APIs ****************/

    public void beginPolling() {
        if (handler == null) {
            handler = new LocationHandler(getApplicationContext(),LocationHandler.POLL_USING_ALARMS,LocationHandler.ACCURACY_VELOCITY_ADJUST);
            Log.d(TAG, "LocationHandler created");
        } else {
            Log.e(TAG, "begin Polling when already polling");
        }
    }

    public void stopPolling() {
        if (handler != null) {
            Log.d(TAG, "shutting down LocationHandler");
            handler.shutdown();
            handler = null;
        } else {
            Log.e(TAG, "stoppingPolling when not polling");
        }
        EventBus.getDefault().unregister(this);
    }


    public class ServiceBinder extends Binder {
        public LocationPollingService getService() {
            return LocationPollingService.this;
        }
    }
}
