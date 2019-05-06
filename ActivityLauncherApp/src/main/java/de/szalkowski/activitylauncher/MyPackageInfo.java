package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.Arrays;

public class MyPackageInfo implements Comparable<MyPackageInfo> {
    protected Drawable icon;
    protected String name;
    String package_name;
    private String icon_resource_name;
    private MyActivityInfo[] activities;

    MyPackageInfo(PackageInfo info, PackageManager pm, PackageManagerCache cache) {
        this.package_name = info.packageName;
        ApplicationInfo app = info.applicationInfo;

        int icon_resource;
        if (app != null) {
            this.name = pm.getApplicationLabel(app).toString();
            try {
                this.icon = pm.getApplicationIcon(app);
            } catch (ClassCastException e) {
                this.icon = pm.getDefaultActivityIcon();
            }
            icon_resource = app.icon;
        } else {
            this.name = info.packageName;
            this.icon = pm.getDefaultActivityIcon();
            icon_resource = 0;
        }

        this.icon_resource_name = null;
        if (icon_resource != 0) {
            try {
                this.icon_resource_name = pm.getResourcesForApplication(app).getResourceName(icon_resource);
            } catch (Exception ignored) {
            }
        }

        if (info.activities == null) {
            this.activities = new MyActivityInfo[0];
        } else {
            int n_activities = countActivitiesFromInfo(info);
            int i = 0;

            this.activities = new MyActivityInfo[n_activities];

            for (ActivityInfo activity : info.activities) {
                if (activity.isEnabled() && activity.exported) {
                    assert (activity.packageName.equals(info.packageName));
                    ComponentName acomp = new ComponentName(activity.packageName, activity.name);
                    this.activities[i++] = cache.getActivityInfo(acomp);
                }
            }

            Arrays.sort(this.activities);
        }
    }

    private static int countActivitiesFromInfo(PackageInfo info) {
        int n_activities = 0;
        for (ActivityInfo activity : info.activities) {
            if (activity.isEnabled() && activity.exported) {
                n_activities++;
            }
        }
        return n_activities;
    }

    int getActivitiesCount() {
        return activities.length;
    }

    MyActivityInfo getActivity(int i) {
        return activities[i];
    }

    public String getPackageName() {
        return package_name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getIconResourceName() {
        return icon_resource_name;
    }

    @Override
    public int compareTo(MyPackageInfo another) {
        int cmp_name = this.name.compareTo(another.name);
        if (cmp_name != 0) return cmp_name;

        return this.package_name.compareTo(another.package_name);
    }

    @Override
    public boolean equals(Object other) {
        if (!other.getClass().equals(MyPackageInfo.class)) {
            return false;
        }

        MyPackageInfo other_info = (MyPackageInfo) other;
        return this.package_name.equals(other_info.package_name);
    }
}
