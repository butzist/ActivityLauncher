package de.szalkowski.activitylauncher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import org.thirdparty.LauncherIconCreator;

public class AllTasksListFragment extends Fragment implements AllTasksListAsyncProvider.Listener<AllTasksListAdapter> {
    protected ExpandableListView list;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_list, null);

        this.list = view.findViewById(R.id.expandableListView1);

        this.list.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            ExpandableListAdapter adapter = parent.getExpandableListAdapter();
            MyActivityInfo info = (MyActivityInfo) adapter.getChild(groupPosition, childPosition);
            LauncherIconCreator.launchActivity(getActivity(), info.component_name);
            return false;
        });

        AllTasksListAsyncProvider provider = new AllTasksListAsyncProvider(this.getActivity(), this);
        provider.execute();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.registerForContextMenu(this.list);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.context_action_shortcut);
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.context_action_launch);

        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        ExpandableListView list = getView().findViewById(R.id.expandableListView1);

        switch (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                MyActivityInfo activity = (MyActivityInfo) list.getExpandableListAdapter().getChild(
                        ExpandableListView.getPackedPositionGroup(info.packedPosition),
                        ExpandableListView.getPackedPositionChild(info.packedPosition));
                menu.setHeaderIcon(activity.icon);
                menu.setHeaderTitle(activity.name);
                menu.add(Menu.NONE, 2, Menu.NONE, R.string.context_action_edit);
                break;
            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                MyPackageInfo pack = (MyPackageInfo) list.getExpandableListAdapter().getGroup(
                        ExpandableListView.getPackedPositionGroup(info.packedPosition));
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
                MyActivityInfo activity = (MyActivityInfo) list.getExpandableListAdapter().getChild(
                        ExpandableListView.getPackedPositionGroup(info.packedPosition),
                        ExpandableListView.getPackedPositionChild(info.packedPosition));
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
                        dialog.show(this.getFragmentManager(), "ShortcutEditor");
                        break;
                }
                break;

            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                MyPackageInfo pack = (MyPackageInfo) list.getExpandableListAdapter().getGroup(
                        ExpandableListView.getPackedPositionGroup(info.packedPosition));
                switch (item.getItemId()) {
                    case 0:
                        LauncherIconCreator.createLauncherIcon(getActivity(), pack);
                        break;
                    case 1:
                        PackageManager pm = getActivity().getPackageManager();
                        Intent intent = pm.getLaunchIntentForPackage(pack.package_name);
                        Toast.makeText(getActivity(),
                                String.format(getText(R.string.starting_application).toString(), pack.name),
                                Toast.LENGTH_LONG).show();
                        getActivity().startActivity(intent);
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
            Toast.makeText(this.getActivity(), R.string.error_tasks, Toast.LENGTH_SHORT).show();
        }
    }
}
