package uk.me.ponies.wearroutes.historylogger;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.me.ponies.wearroutes.eventBusEvents.LocationEvent;

/**
 * Created by rummy on 30/07/2016.
 */
public class LatLngLogger {
    private boolean mIsLogging = false;
    private List<LatLng> mHistory = new ArrayList<>();
    private float mCumulativeDistance = 0;
    Location mPrevLocation = null;

    public boolean isLogging() {
        return mIsLogging;
    }

    public void setLogging(boolean logging) {
        if (logging && !mIsLogging) { // starting to log
            startLogging();
        } else if (!logging && mIsLogging) { // stopping logging
            stopLogging();
        }


        this.mIsLogging = logging;
    }

    private void startLogging() {

        mHistory = new ArrayList<LatLng>();
        mPrevLocation = null;
    }

    private void stopLogging() {
        //TODO: is anything needed when we stop logging to a polyLine?
        mPrevLocation = null;
    }

    public List<LatLng> getHistory() {
        return Collections.unmodifiableList(mHistory);
    }

    public float getCumulativeDistanceMeters() {
        return mCumulativeDistance;
    }

    @Subscribe
    public void newLocation(LocationEvent locationEvent) {
        if (!mIsLogging) {
            return;
        }
        Location l = locationEvent.getLocation();
        if (l == null) {
            return;
        }
        if (mPrevLocation != null) {
            float distance = l.distanceTo(mPrevLocation);
            mCumulativeDistance += distance;
        }
        mHistory.add(new LatLng(l.getLatitude(), l.getLongitude()));
        mPrevLocation = l;
    }
}
