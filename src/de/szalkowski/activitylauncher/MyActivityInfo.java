package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.BitmapDrawable;

public class MyActivityInfo {
	public MyActivityInfo(ComponentName activity, PackageManager pm) {
		this.component_name = activity;
		
		ActivityInfo act;
		try {
			act = pm.getActivityInfo(activity, 0);
			this.name = act.loadLabel(pm).toString();
			try {
				this.icon = (BitmapDrawable)act.loadIcon(pm);
			}
			catch(ClassCastException e) {
				this.icon = (BitmapDrawable)pm.getDefaultActivityIcon();
			}
			this.icon_resource = act.getIconResource();
		} catch (NameNotFoundException e) {
			this.name = activity.getShortClassName();
			this.icon = (BitmapDrawable)pm.getDefaultActivityIcon();
			this.icon_resource = 0;
		}
		
		if(this.icon_resource != 0) {
			try {
				this.icon_resource_name = pm.getResourcesForActivity(activity).getResourceName(this.icon_resource);
			} catch (Exception e) {}
		}
	}
	
	public ComponentName component_name;
	public BitmapDrawable icon;
	public int icon_resource;
	public String icon_resource_name;
	public String name;
	public int package_id;
};