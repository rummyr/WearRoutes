package uk.me.ponies.wearroutes;

import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by rummy on 20/06/2016.
 */
public class CustomUrlTileProvider extends UrlTileProvider {

    private String baseUrl;

    public CustomUrlTileProvider(int width, int height, String url) {
        super(width, height);
        this.baseUrl = url;
    }

    @Override
    public URL getTileUrl(int x, int y, int zoom) {
        try {
            return new URL(baseUrl.replace("{z}", "" + zoom).replace("{x}", "" + x)
                    .replace("{y}", "" + y));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

