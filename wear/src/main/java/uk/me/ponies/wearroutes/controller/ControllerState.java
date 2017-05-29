package uk.me.ponies.wearroutes.controller;

/**
 * recording states
 */
public class ControllerState {
    static int recordingState = StateConstants.STATE_STOPPED;
    static long recordingStartTime = 0;
    static long recordingEndTime = 0;
    static long recordingPausedTime = 0;
    private ControllerState() {}

    /**
     * Constants for recording state.
     */
    public class StateConstants {
        public static final int STATE_STOPPED = 0;
        public static final int STATE_RECORDING = 1;
        public static final int STATE_PAUSED = 2;

        private StateConstants(){}
    }
}
