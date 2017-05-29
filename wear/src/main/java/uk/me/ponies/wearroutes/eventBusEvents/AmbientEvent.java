package uk.me.ponies.wearroutes.eventBusEvents;

import android.os.Bundle;


public class AmbientEvent {
    public Bundle getBundle() {
        return bundle;
    }

    public int getType() {
        return type;
    }

    private Bundle bundle;
    private int type;
    public static final int ENTER_AMBIENT = 0;
    public static final int LEAVE_AMBIENT = 1;
    public static final int UPDATE= 2;

    public AmbientEvent(int type, Bundle bundle) {
        this.type = type;
        this.bundle = bundle;
    }

}
