package de.szalkowski.activitylauncher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class AllTasksListAdapter extends BaseExpandableListAdapter {
	final static private String LOG = "de.szalkowski.activitylauncher.alltaskslistadapter";
	protected List<MyPackageInfo> packages = null;
	protected List<MyActivityInfo> activities = null;
	protected Map<Pair<Integer,Integer>,Integer> index = null;
	protected Context context;
	
	public AllTasksListAdapter(Context context) {
		this.context = context;
		PackageManager pm = context.getPackageManager();
		List<PackageInfo> all_packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		this.activities = new ArrayList<MyActivityInfo>(all_packages.size()*3);
		this.packages = new ArrayList<MyPackageInfo>(all_packages.size());
		this.index = new HashMap<Pair<Integer,Integer>, Integer>(all_packages.size()*3);
		
		for(PackageInfo pack : all_packages) {
			if(pack.activities == null) continue;
			int n_activities = 0;
			int pack_pos = this.packages.size();
			
			for(ActivityInfo activity : pack.activities) {
				if(activity.isEnabled()) {
					ComponentName acomp = new ComponentName(activity.packageName, activity.name);
					MyActivityInfo myactivity = new MyActivityInfo(acomp, pm);
					myactivity.package_id = pack_pos;
					int act_pos = this.activities.size();
					
					this.activities.add(myactivity);
					this.index.put(new Pair<Integer, Integer>(pack_pos, n_activities), act_pos);					
				}
			}
			if(n_activities > 0) {
				MyPackageInfo mypack = new MyPackageInfo(pack, pm);
				mypack.n_children = n_activities;
				this.packages.add(mypack);
			}
		}
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return activities.get(index.get(new Pair<Integer,Integer>(groupPosition,childPosition)));
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return index.get(new Pair<Integer,Integer>(groupPosition,childPosition));
	}

	@Override
	public View getChildView (int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		MyActivityInfo activity = activities.get(index.get(new Pair<Integer,Integer>(groupPosition,childPosition)));
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(android.R.layout.simple_expandable_list_item_2, null);
		
		TextView text = (TextView) view.findViewById(android.R.id.text1);
		text.setText(activity.name);
		
		Log.d(LOG, "createChild " + activity.name);

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return this.packages.get(groupPosition).n_children;
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
		MyPackageInfo pack = this.packages.get(groupPosition);
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
		
		TextView text = (TextView) view.findViewById(android.R.id.text1);
		text.setText(pack.name);
		Log.d(LOG, "createGroup " + pack.name);
		
		return view;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
