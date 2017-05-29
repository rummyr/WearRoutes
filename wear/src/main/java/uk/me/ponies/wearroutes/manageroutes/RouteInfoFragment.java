package uk.me.ponies.wearroutes.manageroutes;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;

import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.keys.WearUIKeys;

/**
 * Displays simple info about a route
 */
public class RouteInfoFragment  extends Fragment{
    StoredRoute info;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.manage_routes_routeinfo_table, container, false);
        // replace hyphens with a space
        String deHyphenated = info.getName().replace('-',' ');
        // replace egbPeover with egb(zero width space) Peover - works!
        deHyphenated = deHyphenated.replaceAll("([a-z])([A-Z])", "$1\u200B$2");
        // remove .gpx to free up a little space
        deHyphenated = deHyphenated.replaceAll("\\.[gG][pP][xX]$", "");
        ((TextView)v.findViewById(R.id.RouteName)).setText(deHyphenated);

        // shortish date format
        DateFormat sdf = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        String dateStr = sdf.format(info.getReceivedDate());
        ((TextView)v.findViewById(R.id.DateReceived)).setText(dateStr);
        v.findViewById(R.id.deleteRouteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouteInfoFragment.this.deleteRoute();
            }
        });

        // Hide the routeName Label on square devices .. its mainly there for padding
        final TextView routeNameLabel = (TextView) v.findViewById(R.id.routeNameLabel);
        routeNameLabel.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    routeNameLabel.setVisibility(View.GONE);
                }
                return insets;
            }});
        // point count
        ((TextView)v.findViewById(R.id.pointCount)).setText(String.valueOf(info.getPoints().size()));

        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public void setInfo(StoredRoute info) {
        this.info = info;
    }

    public void deleteRoute() {
        File f = info.getTFile();
        long len = f.length();
        boolean canWrite = f.canWrite();
        boolean canRead = f.canRead();
        Defeat.noop(""+canWrite+canRead+len);
        f.delete();
        // and clear it from the shared prefs
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.remove(WearUIKeys.HIDE_PREFIX + info.getName());
        editor.apply();
    }
}
