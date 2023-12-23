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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.thirdparty.IconCreator;
import org.thirdparty.Launcher;

import de.szalkowski.activitylauncher.databinding.FragmentAllListBinding;

public class AllTasksListFragment extends Fragment implements AllTasksListAsyncProvider.Listener<AllTasksListAdapter>, Filterable {
    private FragmentAllListBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAllListBinding.inflate(inflater, container, false);

        binding.expandableListView1.setOnChildClickListener(
                (parent, v, groupPosition, childPosition, id) -> {
                    ExpandableListAdapter adapter = parent.getExpandableListAdapter();
                    MyActivityInfo info = (MyActivityInfo) adapter.getChild(groupPosition, childPosition);
                    var rooted = isRootAllowed();
                    Launcher.launchActivity(getActivity(), info.component_name, rooted && info.is_private, true);
                    return false;
                }
        );
        binding.expandableListView1.setTextFilterEnabled(true);
        registerForContextMenu(binding.expandableListView1);

        AllTasksListAsyncProvider provider = new AllTasksListAsyncProvider(getActivity(), this);
        provider.execute();

        return binding.getRoot();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    ContextMenuInfo menuInfo) {
        var rooted = isRootAllowed();

        menu.add(Menu.NONE, 0, Menu.NONE, R.string.context_action_shortcut);
        if (rooted) {
            menu.add(Menu.NONE, 1, Menu.NONE, R.string.context_action_shortcut_as_root);
        }
        menu.add(Menu.NONE, 2, Menu.NONE, R.string.context_action_launch);
        if (rooted) {
            menu.add(Menu.NONE, 3, Menu.NONE, R.string.context_action_launch_as_root);
        }
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

        switch (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                MyActivityInfo activity = (MyActivityInfo) binding.expandableListView1.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
                menu.setHeaderIcon(activity.icon);
                menu.setHeaderTitle(activity.name);
                menu.add(Menu.NONE, 4, Menu.NONE, R.string.context_action_edit);
                break;
            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                MyPackageInfo pack = (MyPackageInfo) binding.expandableListView1.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
                menu.setHeaderIcon(pack.icon);
                menu.setHeaderTitle(pack.name);
                break;
        }

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

        switch (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                MyActivityInfo activity = (MyActivityInfo) binding.expandableListView1.getExpandableListAdapter().getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition));
                switch (item.getItemId()) {
                    case 0:
                        IconCreator.createLauncherIcon(getActivity(), activity);
                        break;
                    case 1:
                        RootLauncherIconCreator.createLauncherIcon(getActivity(), activity);
                        break;
                    case 2:
                        Launcher.launchActivity(getActivity(), activity.component_name, false, true);
                        break;
                    case 3:
                        Launcher.launchActivity(getActivity(), activity.component_name, true, true);
                        break;
                    case 4:
                        DialogFragment dialog = new ShortcutEditDialogFragment();
                        Bundle args = new Bundle();
                        args.putParcelable("activity", activity.component_name);
                        args.putBoolean("as_root", activity.is_private);
                        dialog.setArguments(args);
                        dialog.show(getChildFragmentManager(), "ShortcutEditor");
                        break;
                }
                break;

            case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                MyPackageInfo pack = (MyPackageInfo) binding.expandableListView1.getExpandableListAdapter().getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition));
                switch (item.getItemId()) {
                    case 0:
                        IconCreator.createLauncherIcon(requireActivity(), pack);
                        Toast.makeText(getActivity(), getString(R.string.error_no_default_activity), Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        PackageManager pm = requireActivity().getPackageManager();
                        Intent intent = pm.getLaunchIntentForPackage(pack.package_name);
                        if (intent != null) {
                            Toast.makeText(getActivity(), String.format(getText(R.string.starting_application).toString(), pack.name), Toast.LENGTH_LONG).show();
                            requireActivity().startActivity(intent);
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
            binding.expandableListView1.setAdapter(value);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.error_tasks, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Filter getFilter() {
        AllTasksListAdapter adapter = (AllTasksListAdapter) binding.expandableListView1.getExpandableListAdapter();
        if (adapter != null) {
            return adapter.getFilter();
        } else {
            return null;
        }
    }

    private boolean isRootAllowed() {
        return ((MainActivity) requireActivity()).isRootAllowed();
    }
}
