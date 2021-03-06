package uk.me.ponies.wearroutes.developerCommands;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;

import uk.me.ponies.wearroutes.R;


public class DeveloperCommandsActivity extends WearableActivity {



    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_gridview_pager);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);

        final Resources res = getResources();
        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Adjust page margins:
                //   A little extra horizontal spacing between pages looks a bit
                //   less crowded on a round display.
                final boolean round = insets.isRound();
                int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                int colMargin = res.getDimensionPixelOffset(round ?
                        R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                pager.setPageMargins(rowMargin, colMargin);

                // GridViewPager relies on insets to properly handle
                // layout for round displays. They must be explicitly
                // applied since this listener has taken them over.
                pager.onApplyWindowInsets(insets);
                return insets;
            }
        });

        DeveloperGridPagerAdapter manageRoutesGridPageAdapter = new DeveloperGridPagerAdapter(getFragmentManager(), getApplicationContext());
        pager.setAdapter(manageRoutesGridPageAdapter );
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(pager);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            if (mContainerView != null) {
                mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            }
            if (mTextView != null) {
                mTextView.setTextColor(getResources().getColor(android.R.color.white));
            }
            if (mClockView != null) {
                mClockView.setVisibility(View.VISIBLE);
            }

        } else {
            if (mContainerView != null) {
                mContainerView.setBackground(null);
            }
            if (mTextView != null) {
                mTextView.setTextColor(getResources().getColor(android.R.color.black));
            }
            if (mClockView != null) {
                mClockView.setVisibility(View.GONE);
            }
        }
    }
}
