package uk.me.ponies.wearroutes.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;


/**
 * Created by rummy on 27/06/2016.
 */
public abstract class Keys {

    public static final String KEY_WEAR_WHEN_ON_MAP_SHOWS = "pref_wear_when_on_map_shows";
    public static final String KEY_WEAR_DIRECTION_OF_TRAVEL_UP = "pref_wear_direction_of_travel_up";
    public static final String KEY_DEVOPT_PERF_REUSE_GMAP_FRAGMENTS = "pref_developer_option_performance_reuse_gmap_fragments";
    public static final String KEY_DEVOPT_PERF_DONT_INIT_MAP = "pref_developer_option_performance_dont_get_map";
    public static final String KEY_DEVOPT_PERF_ONLY_TILTED_MAP = "pref_developer_option_performance_only_tilted_map";

    public static final String MAP_NO_TRACK_VAL = "MapNoTrack".trim();
    public static final String MAP_TRACK_VAL = "MapTrack".trim();
    public static final String GRID_NO_TRACK_VAL = "GridNoTrack".trim();
    public static final String GRID_TRACK_VAL = "GridTrack".trim();
    public static final String KEY_MAP_TAP_ON_ZOOM = "pref_wear_map_tap_to_zoom";
    public static final String KEY_MAP_SHOW_ZOOM_BUTTONS = "pref_wear_map_show_zoom";
    public static final String KEY_PREF_DEVOPT_MAP_TOAST_ON_FLING = "pref_developer_option_map_toast_on_fling";

    public static final String KEY_PREF_SPEED_UNITS = "pref_wear_speed_units";
    public static final String KEY_PREF_SPEED_UNIT_KPH = "kph";
    public static final String KEY_PREF_SPEED_UNIT_MPH = "mph";


    public static final String KEY_PREF_DISTANCE_UNITS = "pref_wear_distance_units";
    public static final String KEY_PREF_DISTANCE_UNIT_KM = "km";
    public static final String KEY_PREF_DISTANCE_UNIT_MILES = "miles";

    public static final String KEY_GPS_UPDATES_LESS_IN_AMBIENT = "pref_wear_ambient_few_updates";


    public static final String KEY_DEVOPT_DEBUG_LOG_ENABLED = "pref_wear_debug_log_enabled";
    public static final String KEY_KEEP_AWAKE_WHEN_RECORDING = "pref_developer_option_keep_awake_when_recording";


    public static final String KEY_PREF_WEAR_RECORDING_LOCATION_INTERVAL ="pref_wear_recording_location_interval";
    public static final String KEY_PREF_WEAR_RECORDING_LOCATION_ACCURACY = "pref_wear_recording_location_accuracy";

    public static final String KEY_PREF_WEAR_ACCURACY_STATEGY = "pref_wear_accuracy_strategy";

}
