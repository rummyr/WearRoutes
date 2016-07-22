package uk.me.ponies.wearroutes.mainactionpages;

import android.support.wearable.view.ActionPage;

/**
 * Created by rummy on 07/07/2016.
 */
public interface ActionPageFragment {
    void updatePage(int state);
    void updateButton(int state);
}
