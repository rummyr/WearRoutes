/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.me.ponies.wearroutes.developerCommands;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import uk.me.ponies.wearroutes.R;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class ActionPageClearCacheFragment extends Fragment {
    private final static String TAG = ActionPageClearCacheFragment.class.getSimpleName();
    Context mContext;


    public void setContext(Context c) {
        mContext = c;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        android.support.wearable.view.ActionPage preferencesPage =
                (android.support.wearable.view.ActionPage) inflater.inflate(R.layout.fragment_developer_commands_action_page_clear_cache, container, false);


        preferencesPage.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mContext != null) {
                            trimCache(mContext);
                        }
                    }
                }

        );
        return preferencesPage;
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child: children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        // but only if it looks to be worth deleting!!!
        // don't go wild!
        if (String.valueOf(dir).endsWith(".dex")) {
            // leave this alone!
            if (tagEnabled(TAG)) Log.d("TrimCache", "Not deleting " + dir);
            return true;
        } else if (dir != null){
            if (tagEnabled(TAG)) Log.d("TrimCache", "Deleting " + dir);
            return dir.delete();
        }
        else {
            return true;
        }
    }

}
