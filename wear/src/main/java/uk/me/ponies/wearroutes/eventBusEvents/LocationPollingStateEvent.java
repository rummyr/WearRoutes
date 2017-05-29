package uk.me.ponies.wearroutes.eventBusEvents;

/**
 * Event to change to polling frequency / FAST, SLOW, NO, etc
 */
public class LocationPollingStateEvent {
    int type;
    // states can be:
    // off,
    // not recording and screen off,
    // not recording screen on
    // recording screen off
    // recording screen on

    /** no polling at all */
    public static final int OFF = 0;
    /** poll infrequently , eg. every few minutes */
    public static final int FAST = 16;
    public static final int SLOW = 32;
    public static final int VERY_SLOW = 64;


    private int state;
    private String source;

    public LocationPollingStateEvent(int newState, String source) {
        switch (newState) {
            case OFF:
            case FAST:
            case SLOW:
            case VERY_SLOW:{
                this.state = newState;
                this.source = source;
                break;
            }
            default:throw new IllegalArgumentException("isForegrounded must be one of OFF, FAST, SLOW, VERY_SLOW");
        }
    }

    public int getState() {
        return state;
    }

    public String toString() {
        String msg = this.getClass().getSimpleName() + " state is :";
        switch (state) {
            case OFF:
                msg += "OFF";
                break;
            case FAST:
                msg += "FAST";
                break;
            case SLOW:
                msg += "SLOW";
                break;
            case VERY_SLOW:
                msg += "VERY_SLOW";
                break;
            default:
                msg += "unknown " + state;
                break;
        }
        return msg + " source:" + source;
    }
}
