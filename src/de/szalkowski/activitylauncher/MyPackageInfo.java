package de.szalkowski.activitylauncher;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;

public class MyPackageInfo {
	public MyPackageInfo(PackageInfo info, PackageManager pm) {
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
	
		try {
			this.icon_resource_name = pm.getResourcesForApplication(app).getResourceName(this.icon_resource);
		} catch (Exception e) {}
	}
	
	public String package_name;
	public BitmapDrawable icon;
	public int icon_resource;
	public String icon_resource_name;
	public String name;
	public int n_children;
}
