package de.szalkowski.activitylauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AllTasksListAdapter extends BaseExpandableListAdapter {
	protected List<MyPackageInfo> packages = null;
	protected Context context;
	
	public AllTasksListAdapter(Context context, AllTasksListAsyncProvider.Updater updater) {
		this.context = context;
		PackageManager pm = context.getPackageManager();
		PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(pm);
		List<PackageInfo> all_packages = pm.getInstalledPackages(0);
		this.packages = new ArrayList<MyPackageInfo>(all_packages.size());
		updater.updateMax(all_packages.size());
		updater.update(0);
		
		for(int i=0; i < all_packages.size(); ++i) {
			updater.update(i+1);
			PackageInfo pack = all_packages.get(i);
			MyPackageInfo mypack;
			try {
				mypack = cache.getPackageInfo(pack.packageName);
				if (mypack.getActivitiesCount() > 0) {
					this.packages.add(mypack);
				}
			} catch (NameNotFoundException e) {}
		}
		
		Collections.sort(this.packages);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return this.packages.get(groupPosition).getActivity(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView (int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		MyActivityInfo activity = (MyActivityInfo)getChild(groupPosition, childPosition);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.all_activities_child_item, null);
		
		TextView text1 = (TextView) view.findViewById(android.R.id.text1);
		text1.setText(activity.getName());
	
		TextView text2 = (TextView) view.findViewById(android.R.id.text2);
		text2.setText(activity.getComponentName().getClassName());
	
		ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
		icon.setImageDrawable(activity.getIcon());

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return this.packages.get(groupPosition).getActivitiesCount();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return this.packages.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return this.packages.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		MyPackageInfo pack = (MyPackageInfo)getGroup(groupPosition);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.all_activities_group_item, null);
		
		TextView text = (TextView) view.findViewById(android.R.id.text1);
		text.setText(pack.getName());
		
		ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
		icon.setImageDrawable(pack.getIcon());
		
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

}
