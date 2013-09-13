package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;

public class MyPackageInfo {
	public MyPackageInfo(PackageInfo info, PackageManager pm, PackageManagerCache cache) {
		this.package_name = info.packageName;
		ApplicationInfo app = info.applicationInfo;

		if(app != null) {
			this.name = pm.getApplicationLabel(app).toString();
			try {
				this.icon = (BitmapDrawable) pm.getApplicationIcon(app);
			}
			catch(ClassCastException e) {
				this.icon = (BitmapDrawable)pm.getDefaultActivityIcon();
			}
			this.icon_resource = app.icon;
		} else {
			this.name = info.packageName;
			this.icon = (BitmapDrawable) pm.getDefaultActivityIcon();
			this.icon_resource = 0;
		}
	
		this.icon_resource_name = null;
		if(this.icon_resource != 0) {
			try {
				this.icon_resource_name = pm.getResourcesForApplication(app).getResourceName(this.icon_resource);
			} catch (Exception e) {}
		}
		
		if(info.activities == null) {
			this.activities = new MyActivityInfo[0];
		} else {
			int n_activities = countActivitiesFromInfo(info);
			int i = 0;
			
			this.activities = new MyActivityInfo[n_activities];
			
			for(ActivityInfo activity : info.activities) {
				if(activity.isEnabled() && activity.exported) {
					assert(activity.packageName.equals(info.packageName));
					ComponentName acomp = new ComponentName(activity.packageName, activity.name);
					this.activities[i++] = cache.getActivityInfo(acomp);
				}
			}
		}
	}
	
	private static int countActivitiesFromInfo(PackageInfo info) {
		int n_activities = 0;
		for(ActivityInfo activity : info.activities) {
			if(activity.isEnabled() && activity.exported) {
				n_activities++;
			}
		}
		return n_activities;
	}
	
	public int getActivitiesCount() {
		return activities.length;
	}
	
	public MyActivityInfo getActivity(int i) {
		return activities[i];		
	}
	
	
	public String getPackageName() {
		return package_name;
	}
	
	public BitmapDrawable getIcon() {
		return icon;
	}
	
	public String getName() {
		return name;
	}
	
	public String getIconResourceName() {
		return icon_resource_name;
	}
	
	protected String package_name;
	protected BitmapDrawable icon;
	protected int icon_resource;
	protected String icon_resource_name;
	protected String name;
	protected MyActivityInfo[] activities;
}
