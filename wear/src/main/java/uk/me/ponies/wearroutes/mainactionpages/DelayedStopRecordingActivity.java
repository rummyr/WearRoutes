package uk.me.ponies.wearroutes.mainactionpages;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.view.View;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.controller.Controller;

/**
 * Created by rummy on 15/07/2016.
 */
    public class DelayedStopRecordingActivity extends Activity {

        private DelayedConfirmationView mDelayedConfirmationView;
        private long stopTime;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            stopTime = SystemClock.elapsedRealtime();
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_delayed_stop_recording);
            mDelayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.timer);
            Rect r = mDelayedConfirmationView.getImageDrawable().getBounds();
            String.valueOf(r);
            //mDelayedConfirmationView.getImageDrawable().setBounds();

            startConfirmationTimer();
        }

        private void startConfirmationTimer() {
            mDelayedConfirmationView.setTotalTimeMs(Options.STOP_CONFIRMATION_DELAY);
            mDelayedConfirmationView.setListener(
                    new DelayedConfirmationView.DelayedConfirmationListener() {
                        @Override
                        /** Timer selected means CANCEL the stop */
                        public void onTimerSelected(View view) {

                            // simplest approach is to just STOP!
                            // that should return them to the previous screen which will probably continue to show "stop"
                            // cancel the timer!
                            mDelayedConfirmationView.reset();
                            finish();

                        }

                        @Override
                        /** Timer finished means Cancel the stop */
                        public void onTimerFinished(View view) {
                            // tell them we've stopped
                            Controller.getInstance().stopRecording(stopTime);
                            Intent intent = new Intent(DelayedStopRecordingActivity.this, ConfirmationActivity.class);
                            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                    ConfirmationActivity.SUCCESS_ANIMATION);
                            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                                    "STOPPED");
                            startActivity(intent);
                            finish();
                        }
                    }
            );

            mDelayedConfirmationView.start();
        }
    }

