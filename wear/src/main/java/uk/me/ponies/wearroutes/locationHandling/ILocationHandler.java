package uk.me.ponies.wearroutes.locationHandling;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * Created by rummy on 24/08/2016.
 */
public interface ILocationHandler {
    void shutdown();

    public PendingResult<Status> requestLocationUpdates(LocationRequest locRequest, LocationListener locationListener);
    public PendingResult<Status> removeLocationUpdates(LocationListener locationListener);

    }
