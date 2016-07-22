package uk.me.ponies.wearroutes;

import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility Class to detect if the watch is connected and has our app installed
 */
public class WatchConnectedUtils {
    private static final long CONNECTION_TIME_OUT_MS = 1000;
    /* Weak, because that's how I like things */
    private WeakReference<GoogleApiClient> mGoogleApiClientRef;
    public WatchConnectedUtils(GoogleApiClient pGoogleApiClient) {
        mGoogleApiClientRef = new WeakReference<GoogleApiClient>(pGoogleApiClient);
    }

    public void isWatchConnectedAsync(final NodesListingCallback callback) {
        final GoogleApiClient client = mGoogleApiClientRef.get();
        if (null == mGoogleApiClientRef) {
            callback.availableNodes(Collections.EMPTY_LIST);
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                // going to assume we're going to
                callback.availableNodes(nodes);
                client.disconnect();
            }
        }).start();
    }

    public static interface NodesListingCallback {
        public void availableNodes(List<Node> pNodesFound);
    }
}
