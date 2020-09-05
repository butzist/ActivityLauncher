package de.szalkowski.activitylauncher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.thirdparty.LauncherIconCreator;

public class AllTasksListFragment extends Fragment implements AllTasksListAsyncProvider.Listener<AllTasksListAdapter>, Filterable {
    private ExpandableListView list;

    AllTasksListFragment() {
        super();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frament_all_list, null);

        this.list = view.findViewById(R.id.expandableListView1);

        this.list.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                ExpandableListAdapter adapter = parent.getExpandableListAdapter();
                MyActivityInfo info = (MyActivityInfo) adapter.getChild(groupPosition, childPosition);
                LauncherIconCreator.launchActivity(getActivity(), info.component_name);
                return false;
            }
        });
        this.list.setTextFilterEnabled(true);

        AllTasksListAsyncProvider provider = new AllTasksListAsyncProvider(getActivity(), this);
        provider.execute();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        registerForContextMenu(this.list);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.context_action_shortcut);
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.context_action_launch);

        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        ExpandableListView list = getView().findViewById(R.id.expandableListView1);

        switch (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                MyActivityInfo activity = (MyActivityInfo) list.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
                menu.setHeaderIcon(activity.icon);
                menu.setHeaderTitle(activity.name);
                menu.add(Menu.NONE, 2, Menu.NONE, R.string.context_action_edit);
                break;
            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                MyPackageInfo pack = (MyPackageInfo) list.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
                menu.setHeaderIcon(pack.icon);
                menu.setHeaderTitle(pack.name);
                break;
        }

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        ExpandableListView list = getView().findViewById(R.id.expandableListView1);

        switch (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                MyActivityInfo activity = (MyActivityInfo) list.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
                switch (item.getItemId()) {
                    case 0:
                        LauncherIconCreator.createLauncherIcon(getActivity(), activity);
                        break;
                    case 1:
                        LauncherIconCreator.launchActivity(getActivity(), activity.component_name);
                        break;
                    case 2:
                        DialogFragment dialog = new ShortcutEditDialogFragment();
                        Bundle args = new Bundle();
                        args.putParcelable("activity", activity.component_name);
                        dialog.setArguments(args);
                        dialog.show(getChildFragmentManager(), "ShortcutEditor");
                        break;
                }
                break;

            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                MyPackageInfo pack = (MyPackageInfo) list.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
                switch (item.getItemId()) {
                    case 0:
                        boolean success = LauncherIconCreator.createLauncherIcon(getActivity(), pack);
                        Toast.makeText(getActivity(), getString(R.string.error_no_default_activity), Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        PackageManager pm = getActivity().getPackageManager();
                        Intent intent = pm.getLaunchIntentForPackage(pack.package_name);
                        if (intent != null) {
                            Toast.makeText(getActivity(), String.format(getText(R.string.starting_application).toString(), pack.name), Toast.LENGTH_LONG).show();
                            getActivity().startActivity(intent);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.error_no_default_activity), Toast.LENGTH_LONG).show();
                        }
                        break;
                }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onProviderFinished(AsyncProvider<AllTasksListAdapter> task, AllTasksListAdapter value) {
        try {
            this.list.setAdapter(value);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.error_tasks, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Filter getFilter() {
        AllTasksListAdapter adapter = (AllTasksListAdapter) this.list.getExpandableListAdapter();
        if (adapter != null) {
            return adapter.getFilter();
        } else {
            return null;
        }
    }
}