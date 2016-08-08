package uk.me.ponies.wearroutes.utils;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import static uk.me.ponies.wearroutes.common.logging.DebugEnabled.tagEnabled;

/**
 * Created by rummy on 08/08/2016.
 */
public class FragmentLifecycleLogger extends Fragment {
    public static String TAG = "FragmentLifecycleLogger";

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onHiddenChanged called");
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onStart() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onStart called");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onResume called");
        super.onResume();
    }

    @Override
    public void onPause() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onPause called");
        super.onPause();
    }

    @Override
    public void onStop() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onStop called");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onDestroyView called");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onDestroy called");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onDetach called");
        super.onDetach();
    }

    @Override
    public void onAttach(Context context) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onAttach called");
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onCreate called");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onCreateView called");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onCreateOptionsMenu called");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (tagEnabled(TAG))
            Log.d(TAG, getClass().getSimpleName() + " onPrepareOptionsMenu called");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDestroyOptionsMenu() {
        if (tagEnabled(TAG))
            Log.d(TAG, getClass().getSimpleName() + " onDestroyOptionsMenu called");
        super.onDestroyOptionsMenu();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onCreateContextMenu called");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onViewStateRestored called");
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onActivityCreated called");
        super.onActivityCreated(savedInstanceState);
    }


    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onViewCreated called");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onInflate called");
        super.onInflate(context, attrs, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (tagEnabled(TAG)) Log.d(TAG, getClass().getSimpleName() + " onSaveInstanceState called");
        super.onSaveInstanceState(outState);
    }


}
