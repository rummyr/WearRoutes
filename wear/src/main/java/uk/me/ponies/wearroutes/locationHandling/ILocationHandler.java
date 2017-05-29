package uk.me.ponies.wearroutes.locationHandling;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;


public interface ILocationHandler {
    void shutdown();

    PendingResult<Status> requestLocationUpdates(LocationRequest locRequest, LocationListener locationListener);
    PendingResult<Status> removeLocationUpdates(LocationListener locationListener);

    }
