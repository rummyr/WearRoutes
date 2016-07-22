package uk.me.ponies.wearroutes.mainactionpages.unused;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.ActionChooserView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.me.ponies.wearroutes.R;


/**
 * Created by rummy on 06/07/2016.
 */
public class ActionPageStopResumeFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                    Bundle savedInstanceState) {
        return onCreateView_acv(inflater,container,savedInstanceState);
    }
    public View onCreateView_normal(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        View page =inflater.inflate(R.layout.action_pause_stop, container, false);
        return page;
    }
    public View onCreateView_acv(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View page =inflater.inflate(R.layout.action_pause_stop_acv, container, false);

        android.support.wearable.view.ActionChooserView  acv = (android.support.wearable.view.ActionChooserView) page.findViewById(R.id.action_pause_stop_acv);
        acv.setEnabled(false);
        acv.setOption(ActionChooserView.OPTION_START, ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.materialdesignicons_clock_start_green), Color.GREEN);
        acv.setOption(ActionChooserView.OPTION_END, ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.materialdesignicons_clock_end_red), Color.RED
        );
        return page;
    }
}