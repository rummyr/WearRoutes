package uk.me.ponies.wearroutes.eventBusEvents;

import android.os.Bundle;

/**
 * Created by rummy on 01/08/2016.
 */
public class AmbientEvent {
    public Bundle getBundle() {
        return bundle;
    }

    public int getType() {
        return type;
    }

    private Bundle bundle;
    private int type;
    public static final int ENTER = 0;
    public static final int LEAVE = 1;
    public static final int UPDATE= 2;

    public AmbientEvent(int type, Bundle bundle) {
        this.type = type;
        this.bundle = bundle;
    }

}
