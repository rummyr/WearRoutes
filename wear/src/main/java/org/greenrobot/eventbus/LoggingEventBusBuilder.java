package org.greenrobot.eventbus;

/**
 * Builder to create a LoggingEventBus instead of the usual EventBux
 */
class LoggingEventBusBuilder extends EventBusBuilder {
    LoggingEventBusBuilder() {
        super();
    }

    @Override
    public EventBus build() {
        return new LoggingEventBus(this);
    }
}
