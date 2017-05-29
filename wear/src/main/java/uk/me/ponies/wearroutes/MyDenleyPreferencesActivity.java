package uk.me.ponies.wearroutes;

import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

import preference.WearPreferenceActivity;
import uk.me.ponies.wearroutes.common.Defeat;

/**
 * Hangover debug class.. though might be re-instated at some point
 */
public class MyDenleyPreferencesActivity extends WearPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WearableListView list = (WearableListView) findViewById(android.R.id.list);
        Defeat.noop(list);
        addPreferencesFromResource(R.xml.denley_preferences);
        // TODO: should surely be pulled from the theme!
        //list.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));


        if (true) return;

        Object o = findViewById(me.denley.wearpreferenceactivity.R.id.heading);
        Defeat.noop(o);
        ((TextView)o).setTextColor(android.R.color.holo_blue_light);

        o = list.findViewById(me.denley.wearpreferenceactivity.R.id.title);
        Defeat.noop(o);
        ((TextView)o).setTextColor(android.R.color.holo_green_light);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (true) return;
        WearableListView list = (WearableListView) findViewById(android.R.id.list);
        Defeat.noop(list);
        Object o = list.findViewById(me.denley.wearpreferenceactivity.R.id.title);
        Defeat.noop(o);
        o = findViewById(me.denley.wearpreferenceactivity.R.id.heading);
        Defeat.noop(o);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (true) return;
        WearableListView list = (WearableListView) findViewById(android.R.id.list);
        Defeat.noop(list);
        Object o = list.findViewById(me.denley.wearpreferenceactivity.R.id.title);
        Defeat.noop(o);
        o = findViewById(me.denley.wearpreferenceactivity.R.id.heading);
        Defeat.noop(o);
        //((TextView)o).setTextColor(android.R.color.holo_green_light);

    }
}
