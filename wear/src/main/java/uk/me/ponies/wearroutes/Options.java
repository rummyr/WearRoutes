package uk.me.ponies.wearroutes;

import java.util.concurrent.TimeUnit;

import uk.me.ponies.wearroutes.common.Defeat;


public class Options {
    public static final boolean DEVELOPER_MODE = true;
    public static final boolean DEVELOPER_STRICT_MODE = false;



    public static final long STOP_CONFIRMATION_DELAY = TimeUnit.SECONDS.toMillis(5);
    public static final int BEARING_SECTORS = 24; // should be divisible by 4 I think // 16 sectors = 22.5 degrees a little coarse// 360/45; // 45 degree sectors = 8 sectors, 20 degree sectors = 18 sectors?
    public static final int WEAR_DEFAULT_STARTING_ZOOM = 15;

    public static final int MIN_ALLOWED_ACCURACY_METERS_NOT_RECORDING = 500; // may want this smaller or even configurable
    public static final int MIN_ALLOWED_ACCURACY_METERS_RECORDING = 60; // is configurable (and smaller than the original 100)

    public static final long ACCEPTABLE_WAKEUP_INACCURACY_SECS = 5; // we will accept +- 5 seconds on the wakeup
    public static final long ACCEPTABLE_WAKEUP_INACCURACY_MILLIS = TimeUnit.SECONDS.toMillis(ACCEPTABLE_WAKEUP_INACCURACY_SECS);
    public static final boolean LOCATION_HANDLER_AS_SERVICE = Defeat.TRUE();

    public static  int OPTIONS_ROUTE_ZOOM_PADDING = 25;
    public static  float MIN_MOVE_DISTANCE_METERS = 4;
    public static  boolean ANIMATE_MOVES = false;
    public static  boolean NORTH_UP = false;

    public static final long DISPLAY_UPDATE_INTERVAL_WHEN_ON_SECS = 1;
    public static final long DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS = 30;
    public static final long LOCATION_UPDATE_INTERVAL_WHEN_ON_SECS = 2;
    public static final int LOCATION_UPDATE_INTERVAL_WHEN_AMBIENT_SECS = 30;
    // public static final int LOCATION_UPDATE_INTERVAL_WHEN_RETRYING_SECS = 2;
    /** When we're retrying shutdown after a bit, in this case Ive set it to shutdown just before another loc update is due */
    // public static final long LOCATION_WHEN_RETRYING_STOP_AFTER_SECS = DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS - 1 ;
    public static final long SlowPollingKeepAwakeForAtMostSecs = 2*DISPLAY_UPDATE_INTERVAL_WHEN_AMBIENT_SECS ;

    public static final long LOG_FLUSH_FREQUENCY_SECS = 60;


}
