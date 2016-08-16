package uk.me.ponies.wearroutes;

import android.app.Fragment;
import android.graphics.Point;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;

import java.util.Map;
import java.util.WeakHashMap;

import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Scans through the gridViewPager/gridViewPagerAdapter
 * telling the fragments if they are offScreen or possibly onScreen
 * Only works for WEAR devices where there is a SINGLE top-level-fragment visible at a time
 * Created by rummy on 15/07/2016.
 */
public class GridViewPagerListenerNotifier implements GridViewPager.OnPageChangeListener {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private static final String TAG = "GridViewPagerNotifier";
    private static final int UP = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 4;
    private static final int RIGHT = 8;
    GridViewPager mGridViewPager;
    // weak keys!!
    Map<Fragment, Boolean> visibleState = new WeakHashMap<>();

    public GridViewPagerListenerNotifier(GridViewPager mGridViewPager) {
        this.mGridViewPager = mGridViewPager;
    }

    @Override
    public void onPageScrolled(int row, int column, float offX, float offY, int i2, int i3) {
        // current Page is clearly onScreen
        // but we may need to flag the page that is being dragged onto screen
        if (offX != 0) {
            // TODO: technically over agressive swipe right doesn't need to update both sides!
            updateScreenVisibility(LEFT | RIGHT);
        }
        if (offY != 0) {
            updateScreenVisibility(UP | DOWN);
        }
    }

    @Override
    public void onPageSelected(int row, int column) {
        if (tagEnabled(TAG))            Log.d(TAG, "onPageSelected row:" + row + " column:" + column);
        updateScreenVisibility(UP | DOWN | LEFT | RIGHT); // to be entirely sure!
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (tagEnabled(TAG)) Log.d(TAG, "onPageScrollStateChanged state:" + state);
        switch (state) {
            case GridViewPager.SCROLL_STATE_CONTENT_SETTLING: {
                break;
            }
            case GridViewPager.SCROLL_STATE_DRAGGING: {
                break;
            }
            case GridViewPager.SCROLL_STATE_IDLE: {
                // this is the point at which any drags etc have stopped and there is only one
                // page visible
                updateScreenVisibility(UP | DOWN | LEFT | RIGHT);
                break;
            }
            case GridViewPager.SCROLL_STATE_SETTLING: {
                break;
            }
        }
    }

    private void updateScreenVisibility(int flags) {

        Point currentPoint = mGridViewPager.getCurrentItem();
        callOnScreen(currentPoint.y, currentPoint.x);
        // deactivate surrounding fragments

        int rowCount = mGridViewPager.getAdapter().getRowCount();

        // do current row
        int columnCount = mGridViewPager.getAdapter().getColumnCount(currentPoint.y);

        if (LEFT == (flags & LEFT)) {
            if (currentPoint.x > 0) { // left on same row
                callOffScreen(currentPoint.y, currentPoint.x - 1);
            }
        }
        if (RIGHT == (flags & RIGHT)) {
            if (currentPoint.x < columnCount - 1) { // Right on same row
                callOffScreen(currentPoint.y, currentPoint.x + 1);
            }
        }

        // do rows above and below
        if (UP == (flags & UP)) {
            if (currentPoint.y > 0) { // previous row
                callOffScreen(currentPoint.y - 1, mGridViewPager.getAdapter().getCurrentColumnForRow(currentPoint.y - 1, currentPoint.x));
            }
        }

        if (DOWN == (flags & DOWN)) {
            if (currentPoint.y < rowCount - 1) {
                callOffScreen(currentPoint.y + 1, mGridViewPager.getAdapter().getCurrentColumnForRow(currentPoint.y + 1, currentPoint.x));
            }
        }
    }

    private void callOnScreen(int y, int x) {
        FragmentGridPagerAdapter adapter = (FragmentGridPagerAdapter) mGridViewPager.getAdapter();
        Fragment f = adapter.getFragment(y, x);
        if (f instanceof IGridViewPagerListener) {
            if (!visibleState.containsKey(f)) { // lazy init
                visibleState.put(f, false);
            }
            if (!visibleState.get(f)) { // only send visible if it was previously "invisible"
                ((IGridViewPagerListener) f).onOnScreenPage();
                visibleState.put(f, true);
            }
        }
    }

    private void callOffScreen(int y, int x) {
        FragmentGridPagerAdapter adapter = (FragmentGridPagerAdapter) mGridViewPager.getAdapter();
        Fragment f = adapter.getFragment(y, x);
        if (f instanceof IGridViewPagerListener) {
            if (!visibleState.containsKey(f)) { // lazy init
                visibleState.put(f, true);
            }
            if (visibleState.get(f)) { // only send visible if it was previously "visible"
                ((IGridViewPagerListener) f).onOffScreenPage();
                visibleState.put(f, false);
            }
        }
    }

}
