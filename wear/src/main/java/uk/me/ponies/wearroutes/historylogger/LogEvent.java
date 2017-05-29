package uk.me.ponies.wearroutes.historylogger;

/**
 * Event containing information for the TextLogger
 */
public class LogEvent {
    private final String line;
    private final String type;
    private final long time;

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
