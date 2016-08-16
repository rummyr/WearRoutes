package uk.me.ponies.wearroutes.eventBusEvents;

import android.location.Location;

/**
 * Created by rummy on 26/07/2016.
 */
public class LogEvent {
    final String line;
    final String type;
    final long time;

    public LogEvent(String line, String type) {
        this.line= line;
        this.type = type;
        this.time = System.currentTimeMillis();
    }

    public String getLine() {
        return line;
    }
    public String getType() {
        return type;
    }
    public long getTimeMs() {
        return time;
    }



}
