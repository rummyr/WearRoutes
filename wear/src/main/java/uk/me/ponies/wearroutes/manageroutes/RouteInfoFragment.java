package uk.me.ponies.wearroutes.manageroutes;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.keys.WearUIKeys;
import uk.me.ponies.wearroutes.common.StoredRoute;

/**
 * Created by rummy on 04/07/2016.
 */
public class RouteInfoFragment  extends Fragment{
    StoredRoute info;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.manage_routes_routeinfo_table, container, false);
        ((TextView)v.findViewById(R.id.RouteName)).setText(info.getName());
        ((TextView)v.findViewById(R.id.DateReceived)).setText(info.getReceivedDate().toString());
        v.findViewById(R.id.deleteRouteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouteInfoFragment.this.deleteRoute();
            }
        });
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
        String.valueOf(""+canWrite+canRead+len);
        boolean rv = f.delete();
        // and clear it from the shared prefs
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.remove(WearUIKeys.HIDE_PREFIX + info.getName());
        editor.apply();
    }
}
