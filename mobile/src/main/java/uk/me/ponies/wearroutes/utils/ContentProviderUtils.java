package uk.me.ponies.wearroutes.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Created by rummy on 06/07/2016.
 */
public class ContentProviderUtils {
    public static void debugGetProviderIcons(Context context) {
        for (PackageInfo pack : context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
            ProviderInfo[] providers = pack.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    Log.d("Example", "provider: " + provider.authority);
                    if (provider.authority!= null && provider.authority.startsWith("com.google.android.apps.docs.storage")) {
                        Log.d("Example", "got google storage provider");
                    }
                }
            }
        }
        for (ProviderInfo provider : context.getPackageManager().queryContentProviders(null, 0, 0)) {
            Log.d("Example", "Content provider authority:" + provider);
            if (provider.authority!= null && provider.authority.equals("com.google.android.apps.docs.storage")) {
                int iconResource = provider.getIconResource();
                Drawable d = provider.loadIcon(context.getPackageManager());
                String.valueOf(""+d+iconResource);
            }

        }
    }
}
