package uk.me.ponies.wearroutes;

import android.support.wearable.view.GridViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

/**
 * On Page Listener that allows more than one listener to be registered
 */
public class ChainedOnPageListener implements GridViewPager.OnPageChangeListener {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    List<GridViewPager.OnPageChangeListener> listeners = Collections.synchronizedList(new ArrayList<GridViewPager.OnPageChangeListener>());

    @Override
    public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {
        for(GridViewPager.OnPageChangeListener l : listeners) {
            l.onPageScrolled(i, i1, v, v1, i2, i3);
        }
    }

    @Override
    public void onPageSelected(int i, int i1) {
        for(GridViewPager.OnPageChangeListener l : listeners) {
            l.onPageSelected(i, i1);
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        for (GridViewPager.OnPageChangeListener l : listeners) {
            l.onPageScrollStateChanged(i);
        }
    }

    //package visible -- oh yes .. or an inner class would be OK
    void addOnPageChangeListener(GridViewPager.OnPageChangeListener newL) {
        listeners.add(newL);
    }
}
