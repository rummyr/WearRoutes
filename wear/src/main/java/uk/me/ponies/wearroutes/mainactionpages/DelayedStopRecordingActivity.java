package uk.me.ponies.wearroutes.mainactionpages;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.CircularButton;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.utils.IsNullAndLog;

/**
 * Shows a page with 2 buttons, cancel stop recording, and DO stop recording
 */
    public class DelayedStopRecordingActivity extends Activity {
    static String TAG = "DelayedStopRecordingAct";

    private DelayedConfirmationView mDelayedConfirmationView;
    private long stopTime;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            stopTime = SystemClock.elapsedRealtime();
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_delayed_stop_recording_2buttons);
            mDelayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.timer);
            CircularButton mStopNowButton = (CircularButton) findViewById(R.id.stopNow);
            Rect r = mDelayedConfirmationView.getImageDrawable().getBounds();
            Defeat.noop(r);
            //mDelayedConfirmationView.getImageDrawable().setBounds();
            mStopNowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doStop(v);
                }
            });

            startConfirmationTimer();
        }

        private void startConfirmationTimer() {
            mDelayedConfirmationView.setTotalTimeMs(Options.STOP_CONFIRMATION_DELAY);
            mDelayedConfirmationView.setListener(
                    new DelayedConfirmationView.DelayedConfirmationListener() {
                        @Override
                        /* Timer selected means CANCEL the shutdown. */
                        public void onTimerSelected(View view) {

                            // simplest approach is to just STOP!
                            // that should return them to the previous screen which will probably continue to show "shutdown"
                            // cancel the timer!
                            mDelayedConfirmationView.reset();
                            finish();

                        }

                        @Override
                        /* Timer finished means Cancel the shutdown */
                        public void onTimerFinished(View view) {
                            doStop(view);
                        }
                    }
            );

            mDelayedConfirmationView.start();
        }

        private void doStop(@SuppressWarnings("UnusedParameters") View view) {
            // tell them we've stopped
            Controller controller = Controller.getInstance();
            IsNullAndLog.logNull(TAG, "controller", controller);
            if (controller == null) {
                Log.e(TAG, "Controller is null, how are we supposed to stop recording");
                return;
            }
            controller.stopRecording(stopTime);
            mDelayedConfirmationView.setTotalTimeMs(0); // to shutdown the cancel event firing later


            Intent intent = new Intent(DelayedStopRecordingActivity.this, ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                    "STOPPED");
            startActivity(intent);
            finish();
        }
    }

