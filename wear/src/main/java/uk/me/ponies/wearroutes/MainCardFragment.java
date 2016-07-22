package uk.me.ponies.wearroutes;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by rummy on 06/07/2016.
 */
public class MainCardFragment extends Fragment {
    private View myFragmentView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myFragmentView = inflater.inflate(R.layout.main_card_fragment_layout, container, false);
        return myFragmentView;
    }

}
