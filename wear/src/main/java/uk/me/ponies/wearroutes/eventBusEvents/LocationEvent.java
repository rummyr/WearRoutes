package uk.me.ponies.wearroutes.eventBusEvents;

import android.location.Location;

/**
 * contains a location and additional extra information such as source.
 */
public class LocationEvent {
    private final Location location;
    private final String source;

    public LocationEvent(Location location, String source) {
        this.location = location;
        this.source = source;
    }

    public Location getLocation() {
        return location;
    }
    public String getSource() { return source;}



}
