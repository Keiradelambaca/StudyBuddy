package com.example.studybuddy.util;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

    public interface Callback { void onSelected(); }

    private final Callback callback;

    public SimpleItemSelectedListener(Callback callback) {
        this.callback = callback;
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (callback != null) callback.onSelected();
    }

    @Override public void onNothingSelected(AdapterView<?> parent) {}
}
