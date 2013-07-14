package de.szalkowski.activitylauncher;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RecentTaskListFragment extends ListFragment {
	protected class MyActivityInfo {
		public ComponentName componentName;
		public Drawable image;
		public String name;
	}
	
	final private String LOG = "de.szalkowski.activitylauncher";
	protected List<MyActivityInfo> activities;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.activities = getRunningActivities(this.getActivity());
		String[] activity_names = new String[this.activities.size()];
		for(int i=0; i<this.activities.size(); ++i) {
			activity_names[i] = this.activities.get(i).name;
		}

		setListAdapter(new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, activity_names) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				LayoutInflater inflater = (LayoutInflater)RecentTaskListFragment.this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(R.layout.activity_list_item, parent, false);
				
				TextView text = (TextView)view.findViewById(android.R.id.text1);
				ImageView image = (ImageView)view.findViewById(android.R.id.icon);
				
				text.setText(RecentTaskListFragment.this.activities.get(position).name);
				image.setImageDrawable(RecentTaskListFragment.this.activities.get(position).image);
				
				return view;
			}
			
			@Override
			public View getDropDownView(int position, View convertView,
					ViewGroup parent) {
				// TODO Auto-generated method stub
				return super.getDropDownView(position, convertView, parent);
			}
		});
	}

	
	protected List<MyActivityInfo> getRunningActivities(Context context) {
		PackageManager pm = this.getActivity().getPackageManager();
		ArrayList<MyActivityInfo> activities = new ArrayList<MyActivityInfo>();
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(100);
		for (ActivityManager.RunningTaskInfo task : tasks) {
			MyActivityInfo info = new MyActivityInfo();
			info.componentName = task.topActivity;
			info.name = getActivityName(task.topActivity,pm);
			try {
				info.image = pm.getActivityIcon(task.topActivity);
			} catch (NameNotFoundException e) {
				info.image = pm.getDefaultActivityIcon();
			}
			
			activities.add(info);
		}
		
		return activities;
	}
	
	protected String getActivityName(ComponentName activity, PackageManager pm) {
		String canonicalName =  activity.toShortString();
		PackageInfo pack;
		//ApplicationInfo app;
		
		try {
			//app = pm.getApplicationInfo(activity.getPackageName(), 0);
			pack = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
			return canonicalName;
		}

		if(pack.activities == null) return canonicalName;
		
		for(ActivityInfo act : pack.activities) {
			if(act.name.equals(activity.getClassName())) {
				return act.loadLabel(pm).toString();
				
				//CharSequence label = pm.getText(pack.packageName, act.labelRes, app);
				//if(label!=null) {
				//	return label.toString();
				//} else {
				//	return canonicalName;
				//}
			}
		}
		
		return canonicalName;
	}
}
