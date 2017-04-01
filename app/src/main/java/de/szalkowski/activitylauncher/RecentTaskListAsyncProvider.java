package de.szalkowski.activitylauncher;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;

public class RecentTaskListAsyncProvider extends AsyncProvider<MyActivityInfo[]> {
	public RecentTaskListAsyncProvider(Context context, Listener<MyActivityInfo[]> listener) {
		super(context,listener,true);
		this.max = 1;
	}

	@Override
	protected MyActivityInfo[] run(Updater updater) {
		PackageManager pm = context.getPackageManager();
		PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(pm);
		ArrayList<MyActivityInfo> activities = new ArrayList<MyActivityInfo>();
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(200);
		this.max = tasks.size();
		this.publishProgress(0);
		
		for (int i=0; i < tasks.size(); ++i) {
			this.publishProgress(i+1);

			ActivityManager.RunningTaskInfo task = tasks.get(i);
			MyActivityInfo info = cache.getActivityInfo(task.topActivity);			
			activities.add(info);
		}
		
		MyActivityInfo[] activities2 = new MyActivityInfo[activities.size()];
		for(int i=0; i < activities.size(); ++i) {
			activities2[i] = activities.get(i);
		}
		
		return activities2;
	}
}
