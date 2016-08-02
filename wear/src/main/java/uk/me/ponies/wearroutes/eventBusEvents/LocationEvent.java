package uk.me.ponies.wearroutes.eventBusEvents;

import android.location.Location;

/**
 * Created by rummy on 26/07/2016.
 */
public class LocationEvent {
    public Location getLocation() {
        return location;
    }

    public LocationEvent(Location location) {

        this.location = location;
    }

    final Location location;
}
