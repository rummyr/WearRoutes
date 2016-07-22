package uk.me.ponies.wearroutes;

/**
 * Created by rummy on 08/06/2016.
 */
public class Options {
    public static final boolean I_MOVE_MAP = true;
    public static final long STOP_CONFIRMATION_DELAY = 10 * 1000;
    /* Should we exit our activity on STOP .. hopefully we won't be recording? */
    public static final boolean FINISH_ON_STOP = true;
    public static final int BEARING_SECTORS = 360/45; // 45 degree sectors = 8 sectors, 20 degree sectors = 18 sectors?
    public static final int WEAR_DEFAULT_STARTING_ZOOM = 15;
    public static  int OPTIONS_ROUTE_ZOOM_PADDING = 25;
    public static  float MIN_MOVE_DISTANCE_METERS = 4;
    public static  boolean ANIMATE_MOVES = false;
    public static  boolean NORTH_UP = false;
}
