package de.szalkowski.activitylauncher.todo;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import de.szalkowski.activitylauncher.R;

public class DisclaimerDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_dialog_disclaimer).setMessage(R.string.dialog_disclaimer).setPositiveButton(android.R.string.yes, (dialog, which) -> {
            SharedPreferences editor = PreferenceManager.getDefaultSharedPreferences(requireActivity().getBaseContext());
            editor.edit().putBoolean("disclaimer_accepted", true).apply();
        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            SharedPreferences editor = PreferenceManager.getDefaultSharedPreferences(requireActivity().getBaseContext());
            editor.edit().putBoolean("disclaimer_accepted", false).apply();
            requireActivity().finish();
        });

        return builder.create();
    }
}
