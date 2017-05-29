package uk.me.ponies.wearroutes.locationHandling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

import java.util.Date;

import uk.me.ponies.wearroutes.common.logging.DebugEnabled;

class SlowLocationPollerAlarmWakeup extends SlowLocationPollerBase {
    private final String TAG = getClass().getSimpleName();
    private AlarmRecieverInner alarmReceiverInner;
    private final IntentFilter myFilter;
    private final PendingIntent pi;
    private final AlarmManager alarmManager;

    boolean mReceiverRegistered;

    public SlowLocationPollerAlarmWakeup(LocationHandler master, Context context) {
        super(master, context);
        alarmReceiverInner = new AlarmRecieverInner();
        Intent intent = new Intent("uk.me.ponies.wearroutes.LocationAlarm");
        myFilter = new IntentFilter(intent.getAction());
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getBroadcast(context, 0, intent, 0 /*PendingIntent.FLAG_ONE_SHOT*/); // PendingIntent.FLAG_ONE_SHOT seems to fire only once, could be code issue though
    }

    //THANKS TO http://stackoverflow.com/questions/4660823/android-alarm-not-working
    public void scheduleNext() {

        long millisInFuture = super.sleepTimeMs();

        //NOTE: unregistered when the alarm is received and in cancelAlarm
        synchronized (this) {
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "Registering the alarm receiver:" + alarmReceiverInner);
            super.getContext().registerReceiver(alarmReceiverInner, myFilter);
            mReceiverRegistered = true;
        }

        if (DebugEnabled.tagEnabled(TAG)) {
            if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "Scheduling an alarm for "
                    + millisInFuture + " ms in the future aka "
                    + new Date(System.currentTimeMillis() + millisInFuture));
        }


//       alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
//                next-1000, 2000,
//                pi
//                );
        // can't repeat faster than 60 seconds
        // Suspiciously short interval 12000 millis; expanding to 60 seconds
//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
//                , TimeUnit.SECONDS.toMillis(seconds), pi);


        // alarmManager.setExactAndAllowWhileIdle();
        //alarmManager.setAndAllowWhileIdle();
        if (false) { // .set flavour
            // DOCS - Note: Beginning in API 19, the trigger time passed to this method is treated as inexact
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + millisInFuture,
                    pi);
        }
        if (true) { // setWindow flavour (similar to setExact but some flexibility allowed
            long pollErrorMs = super.getAcceptablePollTimeErrorMs();
            alarmManager.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + millisInFuture - pollErrorMs,
                    (pollErrorMs << 1),
                    pi);
        }

    }


    /**
     * called from the inner class that actually receives an alarm,
     * contract is that the device is kept awake.
     *
     * @param context ?
     * @param intent  the intent that woke us up, not used.
     */
    public void onReceive(Context context, Intent intent) {
        if (DebugEnabled.tagEnabled(TAG)) Log.d(TAG, "Received Alarm1");

        super.commencePollingAsync(TAG);

        synchronized (this) {
            if (mReceiverRegistered) {
                if (DebugEnabled.tagEnabled(TAG))
                    Log.d(TAG, "UNRegistering the receiver because the alarm has triggered:" + alarmReceiverInner);
                mReceiverRegistered = false;
                context.unregisterReceiver(alarmReceiverInner);
            }
        }

        // Put here YOUR code.
        ////Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_SHORT).show(); // For example
    }

    @Override
    public void stop() {
        cancelAlarm();
        super.stop();
    }

    @Override
    public void destroy() {
        cancelAlarm();
        alarmReceiverInner = null;
        super.destroy();
    }

    public void cancelAlarm() {
        alarmManager.cancel(pi);
        synchronized (this) {
            if (mReceiverRegistered) {
                if (DebugEnabled.tagEnabled(TAG))
                    Log.d(TAG, "UNRegistering the receiver because we're cancelling the alarm (probably stop):" + alarmReceiverInner);
                mReceiverRegistered = false;
                super.getContext().unregisterReceiver(alarmReceiverInner);
            }
        }
    }


    public class AlarmRecieverInner extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SlowLocationPollerAlarmWakeup.this.onReceive(context, intent);
        }

    }
}