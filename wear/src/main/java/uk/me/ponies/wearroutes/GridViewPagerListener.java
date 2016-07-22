package uk.me.ponies.wearroutes;

/**
 * Created by rummy on 15/07/2016.
 */
public interface GridViewPagerListener {
    /* Fired when this page is *loosely* onScreen */
    public void onOnScreenPage();
    /* Fired when this page is definitely NOT onScreen */
    public void onOffScreenPage();
}
