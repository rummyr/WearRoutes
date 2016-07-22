package uk.me.ponies.wearroutes.prefs;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import uk.me.ponies.wearroutes.R;


/**
 * Created by rummy on 15/07/2016.
 */
public class MyPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_preference_map_and_track_display);
    }
}
