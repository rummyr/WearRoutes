package uk.me.ponies.wearroutes.controller;

/**
 * Created by rummy on 06/07/2016.
 */
public  class State {
    static int recordingState = StateConstants.STATE_STOPPED;
    static long recordingStartTime = 0;
    static long recordingEndTime = 0;
    static long recordingPausedTime = 0;
    private State() {}
}
