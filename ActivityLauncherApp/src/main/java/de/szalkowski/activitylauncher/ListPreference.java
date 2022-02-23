package de.szalkowski.activitylauncher;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

public class ListPreference extends androidx.preference.ListPreference {
    public ListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        int index = Arrays.asList(getEntryValues()).indexOf(getValue());
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(getDialogTitle())
                .setNegativeButton(getNegativeButtonText(), null)
                .setSingleChoiceItems(getEntries(), index, (dialog, which) -> {
                    setValue(getEntryValues()[which].toString());
                    dialog.dismiss();
                })
                .show();
    }
}
