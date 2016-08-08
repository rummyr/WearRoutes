package uk.me.ponies.wearroutes;

import android.content.Context;
import android.graphics.Point;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Created by rummy on 22/06/2016.
 */
public class MyGridViewPager extends GridViewPager {
    private static String TAG = "MyGridViewPager";

    ChainedOnPageListener masterListener = new ChainedOnPageListener();

    public MyGridViewPager(Context context) {
        super(context);
        super.setOnPageChangeListener(masterListener);
    }

    public MyGridViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnPageChangeListener(masterListener);
    }

    public MyGridViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnPageChangeListener(masterListener);
    }

    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        masterListener.addOnPageChangeListener(listener);
    }

    public void addOnPageChangeListener(OnPageChangeListener listener) {
        masterListener.addOnPageChangeListener(listener);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        Point currentItem = getCurrentItem();
        GridPagerAdapter adapter = getAdapter();
        if (adapter instanceof MainGridPagerAdapter) { // Sample doesn't really create fragments
            if (((MainGridPagerAdapter) adapter).ONE_MAP_AND_ZOOM_ROW == currentItem.y) {
                return true; // sort of BOLLOX, but not really , we're going to try to intercept in a bit!
            }
        }
        return super.canScrollHorizontally(direction);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Object o = getContext();
        String.valueOf(o);
        boolean isAmbient = false; // we might find it is, or not!
        final boolean rv;
        if (o instanceof WearableActivity) {
            isAmbient = ((WearableActivity) o).isAmbient();
        }
        if (tagEnabled(TAG)) Log.d(TAG, "onTouch ambient is " + isAmbient + "event was:" + ev + "context is " + o);

        if (!isAmbient) {
            rv = super.onTouchEvent(ev);
        } else {
            // suppress the onTouch event when in ambient mode .. just in case its messing up return from ambient.
            rv = false;
        }
        if (tagEnabled(TAG)) Log.d(TAG, "onTouchEvent is returning " + rv);
        return rv;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean rv = super.onInterceptTouchEvent(ev);
        if (tagEnabled(TAG)) Log.d(TAG, "oninterceptTouchEvent is returning " + rv);
        return rv;
    }


}
