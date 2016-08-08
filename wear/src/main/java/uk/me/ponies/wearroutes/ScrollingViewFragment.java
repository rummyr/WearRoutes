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

package uk.me.ponies.wearroutes;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.me.ponies.wearroutes.utils.FragmentLifecycleLogger;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;


public class ScrollingViewFragment extends FragmentLifecycleLogger {
    private View myMapFragment;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, "ScrollingViewFragment onCreateView called");
        myMapFragment = inflater.inflate(R.layout.scrollable_fragment, container, false);


        if (savedInstanceState == null) {
            // this is probably where we'd ACTUALLY do fragment stuff perhaps .. not sure!

        }
        return myMapFragment;

    }


    public String toString() {
        return "ScrollingViewFragment";
    }

}
