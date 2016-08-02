package uk.me.ponies.wearroutes;

import android.os.Bundle;
import android.support.annotation.CallSuper;

/**
 * classes that can handle ambient (and probably want to be notified of such
 */
public interface IAmbientHandler {
    void handleEnterAmbientEvent(Bundle ambientDetails) ;

    void handleUpdateAmbientEvent() ;

    void handleExitAmbientEvent() ;
}
