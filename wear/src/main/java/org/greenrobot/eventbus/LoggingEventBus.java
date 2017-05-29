package org.greenrobot.eventbus;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.WeakHashMap;


public class LoggingEventBus extends EventBus {
    private static final String TAG = "EventBusLogger";
    WeakHashMap<Object, String> registered = new WeakHashMap<>();

    public LoggingEventBus() {
        super();
    }

    LoggingEventBus(EventBusBuilder builder) {
        super(builder);
    }

    public static EventBusBuilder loggingBuilder() {
        return new LoggingEventBusBuilder();
    }
    @Override
    public void register(Object subscriber) {
        super.register(subscriber);
        registered.put(subscriber, subscriber.getClass().getSimpleName());
        Log.d(TAG, "Registering " + subscriber.getClass().getSimpleName());

    }

    @Override
    public synchronized void unregister(Object subscriber) {
        super.unregister(subscriber);
        registered.remove(subscriber);
        Log.d(TAG, "UnRegistering " + subscriber.getClass().getSimpleName());

    }

    public void dump() {
        for (Object subscriber : registered.keySet()) {
            Log.w(TAG, "dump: " + subscriber + " is still registered");
        }
    }
}
