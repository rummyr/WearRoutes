package uk.me.ponies.wearroutes;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by rummy on 22/06/2016.
 */
public class GoogleMapFragmentPool {
    // a synchronized set of *all* mapFragments we have created.
    static Map<String,WeakReference<MapFragment>> googleMapFragmentRecording
            = Collections.synchronizedMap(
            new LinkedHashMap<String,WeakReference<MapFragment>>());

    static Set<MapFragment> gcStopper = Collections.synchronizedSet(new HashSet<MapFragment>());

    static final Set<MapFragment> innerMapFragmentPool = Collections.synchronizedSet(new HashSet<MapFragment>());
    static Map<MapFragment, GoogleMap> innerMapMapPool = Collections.synchronizedMap(new WeakHashMap<MapFragment, GoogleMap>());
    static int creationNumber;


    public static MapFragment getFragment() {
        MapFragment innerMapFragment = innerMapFragmentPool.iterator().next();
        innerMapFragmentPool.remove(innerMapFragment);
        return innerMapFragment;
    }
    public static GoogleMap getMapForFragment(MapFragment frag) {
        return innerMapMapPool.get(frag);
    }


    public static void returnFragment(MapFragment returnMe, GoogleMap map) {
        innerMapFragmentPool.add(returnMe);
        innerMapMapPool.put(returnMe,map);
    }


}
