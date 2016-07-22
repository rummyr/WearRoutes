package uk.me.ponies.wearroutes.mainactionpages.unused;

import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.wearable.view.ActionPage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.controller.Controller;
import uk.me.ponies.wearroutes.controller.StateConstants;
import uk.me.ponies.wearroutes.mainactionpages.ActionPageFragment;

/**
 * Created by rummy on 06/07/2016.
 */
public class ActionPageStopFragment extends Fragment implements ActionPageFragment {

    private static final String TAG = ActionPageStopFragment.class.getSimpleName();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ActionPage page =(ActionPage) inflater.inflate(R.layout.action_stop, container, false);
        updateButton(page, Controller.getInstance().getRecordingState());

        page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Controller.getInstance().stopRecording(SystemClock.elapsedRealtime());
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

    private void updateButton(@NonNull View v, int state) {
        if (!(v instanceof ActionPage) ){
            Log.e(TAG, "expecting an action page, got a " + v.getClass().getSimpleName());
            return;
        }
        ActionPage page = (ActionPage) v;
        if (StateConstants.STATE_RECORDING == state) {
            // not started, hence disabled
            // page.setAlpha(1.0f);
            page.setText("NOT STARTED");
            page.setImageResource(R.drawable.materialdesignicons_clock_end_grey);
            page.setEnabled(false);
        }
        else {
            // disabled
            //  page.setAlpha(0.26f); // 0.26 for light theme and 0.3 for dark theme .. can;t
            page.setText("STOP RECORDING");
            page.setImageResource(R.drawable.materialdesignicons_clock_end_red);
            page.setEnabled(true);
        }
    }
}