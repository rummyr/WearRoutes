package uk.me.ponies.wearroutes.customviews;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by rummy on 15/07/2016.
 * Intended to be identical to DelayedConfirmationView, but the image in the middle resizes
 */
public class DelayedConfirmationViewResizing extends android.support.wearable.view.DelayedConfirmationView {
    public DelayedConfirmationViewResizing(Context context) {
        super(context);
    }

    public DelayedConfirmationViewResizing(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DelayedConfirmationViewResizing(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageCirclePercentage(float percentage) {
        super.setImageCirclePercentage(percentage);
    }

}
