package uk.me.ponies.wearroutes;

import java.util.concurrent.TimeUnit;

/**
 * Created by rummy on 08/06/2016.
 */
public class Options {

    public static final long STOP_CONFIRMATION_DELAY = TimeUnit.SECONDS.toMillis(5);
    /* Should we exit our activity on STOP .. hopefully we won't be recording? */
    public static final boolean FINISH_ON_STOP = true;
    public static final int BEARING_SECTORS = 16;// 16 sectors = 22.5 degrees // 360/45; // 45 degree sectors = 8 sectors, 20 degree sectors = 18 sectors?
    public static final int WEAR_DEFAULT_STARTING_ZOOM = 15;
    //TODO: make MIN_ALLOWED_ACCURACY_METERS_RECORDING configurable
    public static final float MIN_ALLOWED_ACCURACY_METERS_NOT_RECORDING = 500; // may want this smaller or even configurable
    public static final float MIN_ALLOWED_ACCURACY_METERS_RECORDING = 100; // may want this smaller or even configurable
    public static  int OPTIONS_ROUTE_ZOOM_PADDING = 25;
    public static  float MIN_MOVE_DISTANCE_METERS = 4;
    public static  boolean ANIMATE_MOVES = false;
    public static  boolean NORTH_UP = false;

    public static final long DISPLAY_UPDATE_INTERVAL_WHEN_ON_SECS = 1;
    public static final long DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS = 30;
    public static final long LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS = 2;
    public static final long LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS = 30;
    public static final long LOCATION_UPDATE_INTERVAL_WHEN_RETRYING_SECS = 2;
    /** When we're retrying stop after a bit, in this case Ive set it to stop just before another loc update is due */
    public static final long LOCATION_WHEN_RETRYING_STOP_AFTER_SECS = DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS - 1 ;

    public static final long LOG_FLUSH_FREQUENCY_SECS = 60;


}
