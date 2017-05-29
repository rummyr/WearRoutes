package uk.me.ponies.wearroutes.developerCommands;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import uk.me.ponies.wearroutes.R;



public class ActionPageLaunchDeveloperCommandsFragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        android.support.wearable.view.ActionPage preferencesPage =
                (android.support.wearable.view.ActionPage)inflater.inflate(R.layout.fragment_action_page_developer_commands, container, false);


        preferencesPage.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Intent intent;
                        intent = new Intent(getActivity(), DeveloperCommandsActivity.class);
                        startActivity(intent);
                    }
                }

        );
        return preferencesPage;
    }
}

