package uk.me.ponies.wearroutes.controller;


public class Stats {
    private static long lastBackgroundTTFMs;
    private static long backgroundTTFTotal;
    private static int backgroundFixCount;
    private static int backgroundFixTimeoutCount;
    private static int backgroundPollingInterval;
    private static int foregroundPollingInterval;
    private static long lastForegroundFixTTFms;
    private static long foregroundTTFTotal;
    private static int foregroundFixCount;
    private static String currentPollingMode = "NotSet";
    private static int unacceptableLocations;

    private static long wakelockHeldTimeMs;

    public static void addLastBackgroundTTFMs(long ttf) {
        lastBackgroundTTFMs = ttf;
        backgroundFixCount++;
        backgroundTTFTotal += ttf;
    }

    public static void addLastForegroundTTFMs(long ttf) {
        lastForegroundFixTTFms = ttf;
        foregroundFixCount++;
        foregroundTTFTotal += ttf;
    }

    public static void addBackgroundFixTimeout() {
        backgroundFixTimeoutCount++;
    }

    public static void addWakeLockHeldTime(long ms) {
        wakelockHeldTimeMs += ms;
    }

    public static void updateBackgroundPollingInterval(int secs) {
        backgroundPollingInterval = secs;
    }

    public static void updateForegroundPollingInterval(int secs) {
        foregroundPollingInterval = secs;
    }

    public static long getLastBackgroundTTFMs() {
        return lastBackgroundTTFMs;
    }
    public static  int getBackgroundFixCount() {
        return backgroundFixCount;
    }

    public static long getBackgroundTTFTotal() {
        return backgroundTTFTotal;
    }

    public static int getBackgroundFixTimeoutCount() {
        return backgroundFixTimeoutCount;
    }
    public static int getBackgroundPollingIntervalSecs() { return backgroundPollingInterval;}
    public static int getForegroundPollingIntervalSecs() { return foregroundPollingInterval;}
    public static  int getForegroundFixCount() {
        return foregroundFixCount;
    }
    public static void setCurrentPollingMode(String mode) {
        currentPollingMode = mode;
    }

    public static String getCurrentPollingMode() {
        return currentPollingMode;
    }

    public static void addUnacceptableLocation() {
        unacceptableLocations++;
    }
    public static int getUnacceptableLocations() {
        return unacceptableLocations;
    }
    public static long getWakeLockHeldTime() { return wakelockHeldTimeMs;}
}
