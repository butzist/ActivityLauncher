package de.szalkowski.activitylauncher.ui.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.szalkowski.activitylauncher.R;
import de.szalkowski.activitylauncher.async.AsyncProvider;
import de.szalkowski.activitylauncher.async.IconListAsyncProvider;
import de.szalkowski.activitylauncher.ui.adapter.IconListAdapter;

public class IconPickerDialogFragment extends DialogFragment implements IconListAsyncProvider.Listener<IconListAdapter> {
    private GridView grid;
    private IconPickerListener listener = null;

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);

        IconListAsyncProvider provider = new IconListAsyncProvider(this.getActivity(), this);
        provider.execute();
    }

    public void attachIconPickerListener(IconPickerListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.icon_picker, null);

        this.grid = (GridView) view;
        this.grid.setOnItemClickListener((view1, item, index, id) -> {
            if (IconPickerDialogFragment.this.listener != null) {
                IconPickerDialogFragment.this.listener.iconPicked(view1.getAdapter().getItem(index).toString());
                IconPickerDialogFragment.this.requireDialog().dismiss();
            }
        });

        builder.setTitle(R.string.title_dialog_icon_picker)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> IconPickerDialogFragment.this.requireDialog().cancel());

        return builder.create();
    }

    @Override
    public void onProviderFinished(AsyncProvider<IconListAdapter> task,
                                   IconListAdapter value) {
        try {
            this.grid.setAdapter(value);
        } catch (Exception e) {
            Toast.makeText(this.getActivity(), R.string.error_icons, Toast.LENGTH_SHORT).show();
        }
    }

    public interface IconPickerListener {
        void iconPicked(String icon);
    }
}
