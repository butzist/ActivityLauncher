package de.szalkowski.activitylauncher.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.szalkowski.activitylauncher.constant.Constants;
import de.szalkowski.activitylauncher.util.SettingsUtils;
import de.szalkowski.activitylauncher.async.AllTasksListAsyncProvider;
import de.szalkowski.activitylauncher.databinding.AllActivitiesChildItemBinding;
import de.szalkowski.activitylauncher.databinding.AllActivitiesGroupItemBinding;
import de.szalkowski.activitylauncher.manager.PackageManagerCache;
import de.szalkowski.activitylauncher.object.MyActivityInfo;
import de.szalkowski.activitylauncher.object.MyPackageInfo;

public class AllTasksListAdapter extends BaseExpandableListAdapter implements Filterable {
    private final PackageManager pm;
    private final LayoutInflater inflater;
    SharedPreferences prefs;
    private List<MyPackageInfo> packages;
    private List<MyPackageView> filtered;

    public AllTasksListAdapter(@NonNull Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.pm = context.getPackageManager();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void resolve(AllTasksListAsyncProvider.Updater updater) {
        PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(this.pm);
        List<PackageInfo> all_packages = this.pm.getInstalledPackages(0);
        Configuration locale = SettingsUtils.createLocaleConfiguration(prefs.getString("language", "System Default"));
        this.packages = new ArrayList<>(all_packages.size());
        updater.updateMax(all_packages.size());
        updater.update(0);

        for (int i = 0; i < all_packages.size(); ++i) {
            updater.update(i + 1);
            PackageInfo pack = all_packages.get(i);
            MyPackageInfo mypack;
            try {
                mypack = cache.getPackageInfo(pack.packageName, locale);
                if (mypack.getActivitiesCount() > 0) {
                    this.packages.add(mypack);
                }
            } catch (NameNotFoundException | RuntimeException ignored) {
            }
        }

        Collections.sort(this.packages);
        this.filtered = createFilterView("", this.prefs.getBoolean("hide_hide_private", true));
    }

    private List<MyPackageView> createFilterView(String query, boolean hidePrivate) {
        String q = query.toLowerCase();
        List<MyPackageView> result = new ArrayList<>();
        for (int j = 0; j < this.packages.size(); ++j) {
            MyPackageInfo parent = this.packages.get(j);
            MyPackageView entry = new MyPackageView(parent, j);

            for (int i = 0; i < parent.getActivitiesCount(); ++i) {
                MyActivityInfo child = parent.getActivity(i);
                if (
                        (child.getName().toLowerCase().contains(q) ||
                                child.getComponentName().flattenToString().toLowerCase().contains(q) ||
                                child.getIconResourceName() != null && child.getIconResourceName().toLowerCase().contains(q)
                        ) && (!hidePrivate || !child.isPrivate())
                ) {
                    entry.add(child, i);
                }
            }

            if (!entry.children.isEmpty()) {
                result.add(entry);
            }
        }

        return result;
    }


    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.filtered.get(groupPosition).children.get(childPosition).child;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return this.filtered.get(groupPosition).children.get(childPosition).id;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        MyActivityInfo activity = (MyActivityInfo) getChild(groupPosition, childPosition);
        AllActivitiesChildItemBinding binding = AllActivitiesChildItemBinding.inflate(inflater, parent, false);

        binding.text1.setText(activity.getName());

        binding.text2.setText(activity.getComponentName().getClassName());

        if (activity.isPrivate()) {
            binding.icon1.setVisibility(View.VISIBLE);
        }

        binding.icon.setImageDrawable(activity.getIcon());

        binding.button1.setOnClickListener(this::bringContextMenu);

        return binding.getRoot();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.filtered.get(groupPosition).children.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.filtered.get(groupPosition).parent;
    }

    @Override
    public int getGroupCount() {
        return this.filtered.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return this.filtered.get(groupPosition).id;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        MyPackageInfo pack = (MyPackageInfo) getGroup(groupPosition);
        AllActivitiesGroupItemBinding binding = AllActivitiesGroupItemBinding.inflate(inflater, parent, false);

        binding.text1.setText(pack.getName());

        binding.icon.setImageDrawable(pack.getIcon());

        binding.button1.setOnClickListener(this::bringContextMenu);

        // expand if filtered list is short enough
        if (this.filtered.size() < 10) {
            ExpandableListView expandableListView = (ExpandableListView) parent;
            expandableListView.expandGroup(groupPosition);
        }

        return binding.getRoot();
    }

    private void bringContextMenu(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            view.getParent().showContextMenuForChild(view, 0, 0);
        else view.performLongClick();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<MyPackageView> result = createFilterView(constraint.toString(), prefs.getBoolean(Constants.PREF_HIDE_HIDE_PRIVATE, true));
                FilterResults wrapped = new FilterResults();
                wrapped.values = result;
                wrapped.count = result.size();
                return wrapped;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null) {
                    AllTasksListAdapter.this.filtered = (List<MyPackageView>) results.values;
                    notifyDataSetChanged();
                }
            }
        };
    }

    private static class MyPackageView {
        MyPackageInfo parent;
        List<Child> children;
        long id;

        MyPackageView(MyPackageInfo parent, long id) {
            this.parent = parent;
            this.id = id;
            this.children = new ArrayList<>();
        }

        void add(MyActivityInfo activity, long id) {
            Child child = new Child();
            child.child = activity;
            child.id = id;
            this.children.add(child);
        }

        private static class Child {
            MyActivityInfo child;
            long id;
        }
    }
}
