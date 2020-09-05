package de.szalkowski.activitylauncher;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class IconPickerFragment extends Fragment implements IconListAsyncProvider.Listener<IconListAdapter> {
    protected GridView grid;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.icon_picker, null);

        this.grid = (GridView) view;
        this.grid.setOnItemClickListener((view1, item, index, id) ->
                Toast.makeText(getActivity(), view1.getAdapter().getItem(index).toString(), Toast.LENGTH_SHORT).show());

        return view;
    }

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);

        IconListAsyncProvider provider = new IconListAsyncProvider(this.getActivity(), this);
        provider.execute();
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

}
