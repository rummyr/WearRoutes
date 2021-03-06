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

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.me.ponies.wearroutes.common.Defeat;
import uk.me.ponies.wearroutes.manageroutes.ManageRoutesActivity;

public class ActionPageManageRoutesFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        android.support.wearable.view.ActionPage preferencesPage =
                (android.support.wearable.view.ActionPage)inflater.inflate(R.layout.fragment_action_page_manage_routes, container, false);
        Object o = preferencesPage.getButton().getImageDrawable();
        Defeat.noop(o);


        if (false) {
            BitmapDrawable bitmapD = (BitmapDrawable) preferencesPage.getButton().getImageDrawable();
            Bitmap bitmap = bitmapD.getBitmap();

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setScale(1, 1, 1, 2);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            bitmapD.setColorFilter(filter);
            // I'm sure there's an easier way than this, but I can't find the right combination
            //bitmapD.setColorFilter(Color.parseColor("#FF010101"), PorterDuff.Mode.MULTIPLY);
        }

        preferencesPage.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Intent intent;
                        intent = new Intent(getActivity(), ManageRoutesActivity.class);
                        startActivity(intent);
                    }
                }

        );
        return preferencesPage;
    }
}
