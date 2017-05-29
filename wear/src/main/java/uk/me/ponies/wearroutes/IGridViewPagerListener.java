package uk.me.ponies.wearroutes;


interface IGridViewPagerListener {
    /* Fired when this page is *loosely* onScreen */
    void onOnScreenPage();
    /* Fired when this page is definitely NOT onScreen */
    void onOffScreenPage();
}
