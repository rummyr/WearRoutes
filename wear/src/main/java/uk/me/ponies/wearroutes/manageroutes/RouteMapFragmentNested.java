package uk.me.ponies.wearroutes.manageroutes;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.PolylineOptions;

import uk.me.ponies.wearroutes.Options;
import uk.me.ponies.wearroutes.R;
import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.keys.WearUIKeys;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class RouteMapFragmentNested extends Fragment {
    private  String TAG = getClass().getSimpleName();
    StoredRoute info;
    GoogleMap mMap;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.manage_route_map_fragment_in_layout, container, false);
        // inner map fragment is created in the layout
        MapFragment innerMapFragment = (MapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapfragment);
        if (tagEnabled(TAG))Log.d(TAG, "MapContainingFragment (not the view) is " + innerMapFragment);
        this.setRetainInstance(true);

        ToggleButton tb = ((ToggleButton)v.findViewById(R.id.hideRoute));

        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideButtonClicked(buttonView,isChecked);
            }
        });
        tb.setChecked(info.getTHidden());


        // can't do that here! Can't retain fragments that are nested in other fragments
        // // innerMapFragment.setRetainInstance(true);

        innerMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                mMap = googleMap;
                PolylineOptions popts = new PolylineOptions().addAll(info.getPoints()).color(Color.RED).width(5).geodesic(false);

                googleMap.addPolyline(popts);
                setupMap();

            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) {
            setupMap();
        }
    }

    private void setupMap() {
        mMap.setContentDescription(info.getName());
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(info.getBounds(), Options.OPTIONS_ROUTE_ZOOM_PADDING); // padding
        mMap.moveCamera(cu);
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }
        });
    }

    public void setInfo(StoredRoute info) {
        this.info = info;
    }

    public void hideButtonClicked(CompoundButton tb, boolean newState) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putBoolean(WearUIKeys.HIDE_PREFIX + info.getName(), newState);
        editor.apply();
        info.setTHidden(newState);
    }
}
