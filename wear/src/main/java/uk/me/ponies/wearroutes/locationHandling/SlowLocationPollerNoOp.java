package uk.me.ponies.wearroutes.locationHandling;

import android.content.Context;

public class SlowLocationPollerNoOp extends SlowLocationPollerBase {
    public SlowLocationPollerNoOp(LocationHandler master, Context context) {
        super(master, context);
    }

    @Override
    public void scheduleNext() {
    }

    @Override
    public void stop() {
    }
}