package de.szalkowski.activitylauncher;

import java.util.ArrayList;
import java.util.List;

import org.thirdparty.LauncherIconCreator;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class RecentTaskListFragment extends ListFragment {
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
				View view = super.getView(position, convertView, parent);
				
				LayoutInflater inflater = (LayoutInflater)RecentTaskListFragment.this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.activity_list_item, parent, false);
				
				TextView text = (TextView)view.findViewById(android.R.id.text1);
				ImageView image = (ImageView)view.findViewById(android.R.id.icon);
				
				text.setText(RecentTaskListFragment.this.activities.get(position).name);
				image.setImageDrawable(RecentTaskListFragment.this.activities.get(position).icon);
				
				return view;
			}
		});
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		this.registerForContextMenu(getListView());
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ComponentName activity = RecentTaskListFragment.this.activities.get(position).componentName;
		Intent intent = LauncherIconCreator.getActivityIntent(activity);
		RecentTaskListFragment.this.getActivity().startActivity(intent);						
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, 0, Menu.NONE, R.string.context_action_shortcut);
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.context_action_launch);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		MyActivityInfo activity = RecentTaskListFragment.this.activities.get(info.position);

		switch(item.getItemId()) {
		case 0:
			LauncherIconCreator.createLauncherIcon(getActivity(), activity);
			break;
		case 1:
			Intent intent = LauncherIconCreator.getActivityIntent(activity.componentName);
			getActivity().startActivity(intent);
			break;
		}
		return super.onContextItemSelected(item);
	}

	protected List<MyActivityInfo> getRunningActivities(Context context) {
		PackageManager pm = this.getActivity().getPackageManager();
		ArrayList<MyActivityInfo> activities = new ArrayList<MyActivityInfo>();
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(100);
		for (ActivityManager.RunningTaskInfo task : tasks) {
			MyActivityInfo info = new MyActivityInfo(task.topActivity, pm);			
			activities.add(info);
		}
		
		return activities;
	}

}
