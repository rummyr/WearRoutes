package uk.me.ponies.wearroutes.mainactionpages.unused;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.view.ActionPage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.ControllerState;
import uk.me.ponies.wearroutes.mainactionpages.ActionPageFragment;


public class ActionPageStartFragment extends Fragment implements ActionPageFragment {

    private static final String TAG = ActionPageStartFragment.class.getSimpleName();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ActionPage page =(ActionPage)inflater.inflate(R.layout.action_start, container, false);

        updateButton(page, Controller.getInstance().getRecordingState());

        page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Controller.getInstance().startRecording();
            }
        });

        return page;
    }

    @Override
    public void updatePage(int state) {
        updateButton(state);
    }
    @Override
    public void updateButton(int state) {
        View v = getView();
        if (v == null) {
            Log.e(TAG, "expecting an action page, got NULL");
            return;
        }
        else if (!(v instanceof ActionPage) ) {
            Log.e(TAG, "expecting an action page, got a " + v.getClass().getSimpleName());
            return;
        }
        updateButton(v, state);
    }
    private void updateButton(@NonNull  View v, int state) {
        if (!(v instanceof ActionPage) ){
            Log.e(TAG, "expecting an action page, got a " + v.getClass().getSimpleName());
            return;
        }
        ActionPage page = (ActionPage) v;
        if (ControllerState.StateConstants.STATE_STOPPED == state) {
            // not started, hence enabled
           // page.setAlpha(1.0f);
            page.setText("Start");
            page.setImageResource(R.drawable.materialdesignicons_clock_start_green);
            page.setEnabled(true);
        }
        else {
            // disabled
          //  page.setAlpha(0.26f); // 0.26 for light theme and 0.3 for dark theme .. can;t
            page.setText("RECORDING");
            page.setImageResource(R.drawable.materialdesignicons_clock_start_grey);
            page.setEnabled(false);
        }
    }
}