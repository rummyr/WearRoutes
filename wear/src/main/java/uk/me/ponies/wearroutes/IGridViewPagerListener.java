package uk.me.ponies.wearroutes;

/**
 * Created by rummy on 15/07/2016.
 */
public interface IGridViewPagerListener {
    /* Fired when this page is *loosely* onScreen */
    void onOnScreenPage();
    /* Fired when this page is definitely NOT onScreen */
    void onOffScreenPage();
}