package de.szalkowski.activitylauncher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllTasksListAdapter extends BaseExpandableListAdapter implements Filterable {
    private final PackageManager pm;
    private final LayoutInflater inflater;
    private List<MyPackageInfo> packages;
    private List<MyPackageView> filtered;

    AllTasksListAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.pm = context.getPackageManager();
    }

    void resolve(AllTasksListAsyncProvider.Updater updater) {
        PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(this.pm);
        List<PackageInfo> all_packages = this.pm.getInstalledPackages(0);
        this.packages = new ArrayList<>(all_packages.size());
        updater.updateMax(all_packages.size());
        updater.update(0);

        for (int i = 0; i < all_packages.size(); ++i) {
            updater.update(i + 1);
            PackageInfo pack = all_packages.get(i);
            MyPackageInfo mypack;
            try {
                mypack = cache.getPackageInfo(pack.packageName);
                if (mypack.getActivitiesCount() > 0) {
                    this.packages.add(mypack);
                }
            } catch (NameNotFoundException | RuntimeException ignored) {
            }
        }

        Collections.sort(this.packages);
        this.filtered = createFilterView("");

    }

    private List<MyPackageView> createFilterView(String query) {
        String q = query.toLowerCase();
        List<MyPackageView> result = new ArrayList<>();
        for (int j = 0; j < this.packages.size(); ++j) {
            MyPackageInfo parent = this.packages.get(j);
            MyPackageView entry = new MyPackageView(parent, j);

            for (int i = 0; i < parent.getActivitiesCount(); ++i) {
                MyActivityInfo child = parent.getActivity(i);
                if (child.name.toLowerCase().contains(q) ||
                        child.component_name.flattenToString().toLowerCase().contains(q) ||
                        child.icon_resource_name != null && child.icon_resource_name.toLowerCase().contains(q)) {
                    entry.add(child, i);
                }
            }

            if (!entry.children.isEmpty() ||
                    parent.name.toLowerCase().contains(q) ||
                    parent.package_name.toLowerCase().contains(q) ||
                    parent.icon_resource_name != null && parent.icon_resource_name.contains(q)
            ) {
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
        View view = this.inflater.inflate(R.layout.all_activities_child_item, parent, false);

        TextView text1 = view.findViewById(android.R.id.text1);
        text1.setText(activity.getName());

        TextView text2 = view.findViewById(android.R.id.text2);
        text2.setText(activity.getComponentName().getClassName());

        if (activity.is_private) {
            ImageView icon1 = view.findViewById(android.R.id.icon1);
            icon1.setVisibility(View.VISIBLE);
        }

        ImageView icon = view.findViewById(android.R.id.icon);
        icon.setImageDrawable(activity.getIcon());

        ImageButton button = view.findViewById(android.R.id.button1);
        button.setOnClickListener(view1 -> view.performLongClick());

        return view;
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
        View view = this.inflater.inflate(R.layout.all_activities_group_item, parent, false);

        TextView text = view.findViewById(android.R.id.text1);
        text.setText(pack.getName());

        ImageView icon = view.findViewById(android.R.id.icon);
        icon.setImageDrawable(pack.getIcon());

        ImageButton button = view.findViewById(android.R.id.button1);
        button.setOnClickListener(view1 -> view.performLongClick());

        // expand if filtered list is short enough
        if (filtered.size() < 10) {
            ExpandableListView expandableListView = (ExpandableListView) parent;
            expandableListView.expandGroup(groupPosition);
        }

        return view;
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
                List<MyPackageView> result = createFilterView(constraint.toString());
                FilterResults wrapped = new FilterResults();
                wrapped.values = result;
                wrapped.count = result.size();
                return wrapped;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null) {
                    filtered = (List<MyPackageView>) results.values;
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
