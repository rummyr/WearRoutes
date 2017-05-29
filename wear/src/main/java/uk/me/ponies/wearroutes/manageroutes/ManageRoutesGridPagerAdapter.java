package uk.me.ponies.wearroutes.manageroutes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;

import java.io.File;
import java.util.List;

import uk.me.ponies.wearroutes.common.StoredRoute;
import uk.me.ponies.wearroutes.utils.StoredRoutesUtils;

/**
 * GridPagerAdapter for Manage Routes
 */
public class ManageRoutesGridPagerAdapter extends FragmentGridPagerAdapter {
    final List<StoredRoute> routes;

    public ManageRoutesGridPagerAdapter(FragmentManager fm, Context context, File routesDirectory) {
        super(fm);
        //TODO: should be more dynamic!!!
        //TODO: don't read points, not required?
        routes = StoredRoutesUtils.readStoredRoutes(context, routesDirectory);
    }

    @Override
    public Fragment getFragment(int row, int column) {
        StoredRoute info = routes.get(row);
        if (column == 0) {
            RouteInfoFragment rif = new RouteInfoFragment();
            rif.setInfo(info);
            Bundle args = new Bundle();
            args.putCharSequence("routeName", info.getName());
            args.putInt("routePoints", info.getNumPoints());
            args.putLong("routeReceivedDate", info.getReceivedDate().getTime());
            rif.setArguments(args);
            return rif;
        }
        else {
            RouteMapFragmentNested rmn = new RouteMapFragmentNested();
            rmn.setInfo(info);
            return rmn;
        }
    }

    @Override
    public int getRowCount() {
        return routes.size();
    }

    @Override
    public int getColumnCount(int i) {
        return 2; // at the moment just 2 columns
    }
}
