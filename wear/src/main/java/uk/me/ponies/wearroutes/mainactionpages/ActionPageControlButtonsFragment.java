package uk.me.ponies.wearroutes.mainactionpages;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.ActionPage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.ControllerState;

import static uk.me.ponies.wearroutes.utils.IsNullAndLog.logNull;

/**
 * Displays the start/stop recording buttons
 */
public class ActionPageControlButtonsFragment extends Fragment implements ActionPageFragment {

    private static final String TAG = ActionPageControlButtonsFragment.class.getSimpleName();
    ActionPage startButton;
    ActionPage stopButton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FrameLayout page =(FrameLayout)inflater.inflate(R.layout.action_control_buttons, container, false);
         startButton = (ActionPage)page.findViewById(R.id.action_start);
         stopButton = (ActionPage)page.findViewById(R.id.action_stop);

        final Controller controller = Controller.getInstance();
        logNull(TAG, "controller", controller);
        if (controller == null) {
            Log.wtf(TAG, "Controller is null, cant do show control buttons fragment!");
            return null;
        }
        updatePage(controller.getRecordingState());

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.startRecording();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // delayed Confirm dialog
                Intent intent = new Intent(getActivity(), DelayedStopRecordingActivity.class);
                startActivity(intent);
                // DelayedStopRecordingActivity will receive the STOP. Controller.getInstance().stopRecording();
            }
        });

        return page;
    }

    @Override
    public void updatePage(int state) {
        switch (state) {
            case ControllerState.StateConstants.STATE_STOPPED: {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.INVISIBLE);
                break;
            }
            case ControllerState.StateConstants.STATE_RECORDING: {
                startButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                break;
            }
            case ControllerState.StateConstants.STATE_PAUSED: {

            }
        } // end switch state
    }

    @Override
    public void updateButton(int state) {
        Log.e(TAG, "Update Button is not supported for ActionPageControlButtonsFragment");
    }
}