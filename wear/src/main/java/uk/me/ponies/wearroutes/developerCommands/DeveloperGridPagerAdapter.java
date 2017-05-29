package uk.me.ponies.wearroutes.developerCommands;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.wearable.view.FragmentGridPagerAdapter;

import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;


public class DeveloperGridPagerAdapter extends FragmentGridPagerAdapter {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private final Context mContext;
//TODO: this would be better if the cache directory was passed in, not the context!
    DeveloperGridPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.mContext = context;
        //TODO: should be more dynamic!!!
        //TODO: don't read points, not required?
    }

    @Override
    public Fragment getFragment(int row, int column) {
        if (column == 0) {
            ActionPageClearCacheFragment ccap = new ActionPageClearCacheFragment();
            ccap.setContext(mContext);
            return ccap;
        }
        else {
            return null;
        }
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {
        return 1;
    }
}
