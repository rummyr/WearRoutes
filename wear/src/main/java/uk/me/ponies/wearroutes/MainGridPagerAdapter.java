/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.me.ponies.wearroutes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.developerCommands.ActionPageLaunchDeveloperCommandsFragment;
import uk.me.ponies.wearroutes.mainactionpages.ActionPageControlButtonsFragment;
import uk.me.ponies.wearroutes.mainactionpages.ActionPageFragment;
import uk.me.ponies.wearroutes.mainactionpages.unused.ActionPageStartFragment;
import uk.me.ponies.wearroutes.mainactionpages.unused.ActionPageStopFragment;
import uk.me.ponies.wearroutes.utils.SingleInstanceChecker;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Constructs fragments as requested by the GridViewPager. For each row a different background is
 * provided.
 * <p/>
 * Always avoid loading resources from the main thread. In this sample, the background images are
 * loaded from an background task and then updated using {@link #notifyRowBackgroundChanged(int)}
 * and {@link #notifyPageBackgroundChanged(int, int)}.
 */
public class MainGridPagerAdapter extends FragmentGridPagerAdapter {
    @SuppressWarnings("unused")
    private SingleInstanceChecker sic = new SingleInstanceChecker(this);

    private static final int TRANSITION_DURATION_MILLIS = 100;
    private static String TAG = "MainGridPagerAdapter";
    private ActionPageStartFragment mStartCard;
    private ActionPageStopFragment mStopCard;
    private final ActionPageControlButtonsFragment mControlButtonsCard;
    private final SpeedPanel1 mPanel1;
    private final StatsPanel1 mStatsPanel1;
    int ONE_MAP_AND_ZOOM_ROW = -1;
    private ColumnHistorian mHistorian;
    private final Context mContext;
    private List<Row> mRows;
    private int UNTILTED_ROW = -1;
    private int TILTED_ROW = -1;
    private ColorDrawable mDefaultBg;

    @Override
    protected void applyItemPosition(Object object, Point position) {
        if (tagEnabled(TAG)) Log.d(TAG, "GridPageAdapter, applyItemPosition called for " + object + " position " + position);
        super.applyItemPosition(object, position);
    }

    private ColorDrawable mClearBg;

    MainGridPagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);

        // add my column historian
        this.setColumnHistorian(new ColumnHistorian());

        mContext = ctx;
        MapSwipeToZoomFragment mf00 = new MapSwipeToZoomFragment();
        mf00.fragmentName = "Map #00";
        mf00.tilt = 70;

        mRows = new ArrayList<>();

        // mStartCard = new ActionPageStartFragment();
        // mStopCard = new ActionPageStopFragment();
        mControlButtonsCard = new ActionPageControlButtonsFragment();
        mPanel1 = new SpeedPanel1();
        mStatsPanel1 = new StatsPanel1();


        mRows.add(new Row(
                /*firstCard, */ mControlButtonsCard,
                new ActionPageManageRoutesFragment(),
                new ActionPagePreferencesFragment().setUseDenly(true),
                new ActionPageManageHistoryFragment(),
                new ActionPageLaunchDeveloperCommandsFragment(),
                //TODO: tutorial fragment here .. icon is a blackboard/presentation screen?
                cardFragment(R.string.welcome_title, R.string.welcome_text)));

        mRows.add(new Row(
                mPanel1,
                mStatsPanel1,
                CardFragment.create("Information Row", "This would show speed etc")
        ));

        mRows.add(new Row(
                mf00
        ));
        ONE_MAP_AND_ZOOM_ROW = mRows.size() - 1;

        mRows.add(new Row(CardFragment.create("Tutorial", "Possibly put a start tutorial item here?")));


        CardFragment lastCard = (CardFragment) cardFragment(R.string.dismiss_title, R.string.dismiss_text);
        mRows.add(new Row(lastCard));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDefaultBg = new ColorDrawable(ctx.getResources().getColor(R.color.dark_grey, ctx.getTheme()));
            mClearBg = new ColorDrawable(ctx.getResources().getColor(android.R.color.transparent, ctx.getTheme()));
        } else {
            mDefaultBg = new ColorDrawable(ctx.getResources().getColor(R.color.dark_grey));
            mClearBg = new ColorDrawable(ctx.getResources().getColor(android.R.color.transparent));
        }


    }

    private LruCache<Integer, Drawable> mRowBackgrounds = new LruCache<Integer, Drawable>(3) {
        @Override
        protected Drawable create(final Integer row) {
            int resid = BG_IMAGES[row % BG_IMAGES.length];
            new DrawableLoadingTask(mContext) {
                @Override
                protected void onPostExecute(Drawable result) {
                    TransitionDrawable background = new TransitionDrawable(new Drawable[]{
                            mDefaultBg,
                            result
                    });
                    mRowBackgrounds.put(row, background);
                    notifyRowBackgroundChanged(row);
                    background.startTransition(TRANSITION_DURATION_MILLIS);
                }
            }.execute(resid);
            return mDefaultBg;
        }
    };

    private LruCache<Point, Drawable> mPageBackgrounds = new LruCache<Point, Drawable>(3) {
        @Override
        protected Drawable create(final Point page) {
            // place bugdroid as the background at row 2, column 1
            if (page.y == 3 && page.x == 1) {
                int resid = R.drawable.bugdroid_large;
                new DrawableLoadingTask(mContext) {
                    @Override
                    protected void onPostExecute(Drawable result) {
                        TransitionDrawable background = new TransitionDrawable(new Drawable[]{
                                mClearBg,
                                result
                        });
                        mPageBackgrounds.put(page, background);
                        notifyPageBackgroundChanged(page.y, page.x);
                        background.startTransition(TRANSITION_DURATION_MILLIS);
                    }
                }.execute(resid);
            }
            return GridPagerAdapter.BACKGROUND_NONE;
        }
    };

    private Fragment cardFragment(int titleRes, int textRes) {
        Resources res = mContext.getResources();
        CardFragment fragment =
                CardFragment.create(res.getText(titleRes), res.getText(textRes));
        // Add some extra bottom margin to leave room for the page indicator
        fragment.setCardMarginBottom(res.getDimensionPixelSize(R.dimen.card_margin_bottom));

        return fragment;
    }

    private static final int[] BG_IMAGES = new int[]{
            R.drawable.debug_background_1,
            R.drawable.debug_background_2,
            R.drawable.debug_background_3,
            R.drawable.debug_background_4,
            R.drawable.debug_background_5
    };


    /* so it can be added to appropriate listeners.
    Though it may be better if the fragment does all of this itself!
     */

    @Nullable
    MapSwipeToZoomFragment getMapFragment() {
        if (ONE_MAP_AND_ZOOM_ROW >= 0) {
            return (MapSwipeToZoomFragment) getFragment(ONE_MAP_AND_ZOOM_ROW, 0);
        } else {
            return null;
        }
    }

    public ActionPageFragment getStartRecordingFragment() {
        return mStartCard;
    }

    public ActionPageFragment getStopRecordingFragment() {
        return mStopCard;
    }

    public ActionPageControlButtonsFragment getControlButtonsFragment() {
        return mControlButtonsCard;
    }


    /**
     * A convenient container for a row of fragments.
     */
    private class Row {
        final List<Fragment> columns = new ArrayList<>();

        private Row(Fragment... fragments) {
            for (Fragment f : fragments) {
                add(f);
            }
        }

        public void add(Fragment f) {
            columns.add(f);
        }

        Fragment getColumn(int i) {
            return columns.get(i);
        }

        private int getColumnCount() {
            return columns.size();
        }
    }

    @Override
    public Fragment getFragment(int row, int col) {
        if (tagEnabled(TAG))            Log.d(TAG, "getFragment called for row:" + row + " column:" + col);
        Row adapterRow = mRows.get(row);
        Fragment nextFragment = adapterRow.getColumn(col);
        Defeat.noop(nextFragment);
        return nextFragment;
    }

    @Override
    protected void restoreFragment(Fragment fragment, FragmentTransaction transaction) {
        super.restoreFragment(fragment, transaction);
    }

    @Override
    public Fragment instantiateItem(ViewGroup container, int row, int column) {
        return super.instantiateItem(container, row, column);
    }

    @Override
    public long getFragmentId(int row, int column) {
        return super.getFragmentId(row, column);
    }

    @Override
    public Drawable getBackgroundForRow(final int row) {
        return mRowBackgrounds.get(row);
    }

    @Override
    public Drawable getBackgroundForPage(final int row, final int column) {
        return mPageBackgrounds.get(new Point(column, row));
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mRows.get(rowNum).getColumnCount();
    }

    private class DrawableLoadingTask extends AsyncTask<Integer, Void, Drawable> {
        private static final String TAG = "Loader";
        private Context context;

        DrawableLoadingTask(Context context) {
            this.context = context;
        }

        @Override
        protected Drawable doInBackground(Integer... params) {
            if (tagEnabled(TAG))Log.d(TAG, "Loading asset 0x" + Integer.toHexString(params[0]));

            return context.getResources().getDrawable(params[0],context.getTheme());
        }
    }

    @Override
    public void startUpdate(ViewGroup container) {
        long st = System.currentTimeMillis();
        super.startUpdate(container);
        long end = System.currentTimeMillis();
        if (tagEnabled(TAG))            Log.d(TAG, "GridPagerAdapter: startUpdate Called id:" + container.getId() + " took " + (end-st) + "ms");
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        long st = System.currentTimeMillis();
        super.finishUpdate(container);
        long end = System.currentTimeMillis();
        if (tagEnabled(TAG))            Log.d(TAG, "GridPagerAdapter: finishUpdate Called id:" + container.getId() + " took " + (end-st) + "ms");
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        //if (tagEnabled(TAG)) Log.d(TAG, "isViewFromObject called");
        return super.isViewFromObject(view, object);
    }

    @Override
    public Fragment findExistingFragment(int row, int column) {
        if (tagEnabled(TAG))            Log.d(TAG, "GridPagerAdapter: findExistingFragment " + row + "," + column + " called");
        return super.findExistingFragment(row, column);
    }

    @Override
    public int getCurrentColumnForRow(int row, int currentColumn) {
        /// if (tagEnabled(TAG))Log.d(TAG, "GridPagerAdapter: getCurrentColumnForRow" + row + "," + currentColumn + " called - it is to find is a scroll up/down doesn't go to the first column");
        // return super.getCurrentColumnForRow(row, currentColumn);
        return mHistorian.getColumnForRow(row);
    }

    private void setColumnHistorian(ColumnHistorian historian) {
        mHistorian = historian;

        // register that these 2 rows are in fact synchronised
        if (UNTILTED_ROW >= 0 && TILTED_ROW >= 0 && UNTILTED_ROW != TILTED_ROW)
            mHistorian.setSyncedRows(UNTILTED_ROW, UNTILTED_ROW + 1);
    }

    ColumnHistorian getColumnHistorian() {
        return mHistorian;
    }


    /**
     * Created by rummy on 22/06/2016.
     */
    public class ColumnHistorian implements GridViewPager.OnPageChangeListener {
        private String TAG = "ColumnHistorian";
        HashMap<Integer, Integer> history = new HashMap<>();

        HashMap<Integer, Integer> syncThisRowWithThat = new HashMap<>();
        HashMap<Integer, Integer> syncThatRowWithThis = new HashMap<>();
        private GridViewPager mPager;

        @Override
        public void onPageScrolled(int positionX, int positionY, float offsetX, float offsetY, int offsetLeftPx, int offsetTopPx) {
            // EMPTY Listener Method, no need to log pageScrolled it floods the logs to little benefit
            /* if (tagEnabled(TAG)) Log.d(TAG, "onPageScrolled: Pos:" + positionX + "," + positionY
                    + " Off:" + offsetX + "," + offsetY
                    + "PxLeftTopOff:" + offsetLeftPx + "," + offsetTopPx);
            */
        }

        @Override
        public void onPageSelected(int row, int column) {
            if (tagEnabled(TAG)) Log.d(TAG, "Selected Page is " + row + "," + column);
            history.put(row, column);
            if (syncThatRowWithThis.containsKey(row)) {
                history.put(syncThatRowWithThis.get(row), column);
            }
            if (syncThisRowWithThat.containsKey(row)) {
                history.put(syncThisRowWithThat.get(row), column);
            }


        }

        int getColumnForRow(int row) {
            if (history.containsKey(row)) {
                return history.get(row);
            }
            // we may have visited the other row in a pair of rows, but not this one yet
            // actually not sure that is really possible
            else if (history.containsKey(syncThisRowWithThat.get(row))) {
                return history.get(syncThisRowWithThat.get(row));
            }
            return 0;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        void setSyncedRows(int thisRow, int thatRow) {
            syncThisRowWithThat.put(thisRow, thatRow);
            syncThatRowWithThis.put(thatRow, thisRow);
        }

        void setPager(GridViewPager pager) {
            mPager = pager;
        }

        /*
        @return null if unchanged
         */
        Point detectAndZoom(int row, int column) {
            // zoom out change when first column is selected
            // really need to work on this UI component!
            if ((column == 0 || column == 2)
                    && (row == ONE_MAP_AND_ZOOM_ROW)) {

                final int newColumn;
                if (column == 0) {
                    newColumn = column + 1;
                } else {
                    newColumn = column - 1;
                }


                if (mRows.get(row).columns.size() > newColumn) {
                    Fragment f = mRows.get(row).getColumn(newColumn);
                    if (f instanceof MapSwipeToZoomFragment) {
                        MapSwipeToZoomFragment mCFrag = (MapSwipeToZoomFragment) f;
                        final int newZoom;
                        if (column == 0) {
                            newZoom = mCFrag.getZoom() - 1;
                        } else {
                            newZoom = mCFrag.getZoom() + 1;
                        }

                        if (tagEnabled(TAG)) Log.d(TAG, "Zooming " + mCFrag.fragmentName + " to zoom:" + newZoom);
                        mCFrag.setZoom(newZoom);
                        if (mPager != null) {
                            // mPager.scrollTo(column + 1,row); // yes, X,Y - column, row!
                            Defeat.noop();
                        } else {
                            Log.w("ColumnHistorian", "No pager set");
                        }
                        return new Point(newColumn, row);
                    } else {
                        Log.e("ColumnHistorian", "Card to right is not a map fragment!");
                    }
                }
            }
            return null;
        } // end detectAndZoom
    }// end Column Historian


}
