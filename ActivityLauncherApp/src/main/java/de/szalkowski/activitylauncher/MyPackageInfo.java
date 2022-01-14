package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;

import java.util.Arrays;

public class MyPackageInfo implements Comparable<MyPackageInfo> {
    protected String package_name;
    protected Drawable icon;
    protected int icon_resource;
    protected String icon_resource_name;
    protected String name;
    protected MyActivityInfo[] activities;

    public static MyPackageInfo fromPackageInfo(PackageManagerCache cache, PackageInfo info) {
        var pm = cache.getPackageManager();
        var myInfo = new MyPackageInfo();
        myInfo.package_name = info.packageName;
        ApplicationInfo app = info.applicationInfo;

        if (app != null) {
            myInfo.name = pm.getApplicationLabel(app).toString();
            try {
                myInfo.icon = pm.getApplicationIcon(app);
            } catch (Exception e) {
                myInfo.icon = pm.getDefaultActivityIcon();
            }
            myInfo.icon_resource = app.icon;
        } else {
            myInfo.name = info.packageName;
            myInfo.icon = pm.getDefaultActivityIcon();
            myInfo.icon_resource = 0;
        }

        myInfo.icon_resource_name = null;
        if (myInfo.icon_resource != 0) {
            try {
                myInfo.icon_resource_name = pm.getResourcesForApplication(app).getResourceName(myInfo.icon_resource);
            } catch (Exception ignored) {
            }
        }

        if (info.activities == null) {
            myInfo.activities = new MyActivityInfo[0];
        } else {
            int n_activities = countActivitiesFromInfo(info);
            int i = 0;

            myInfo.activities = new MyActivityInfo[n_activities];

            for (ActivityInfo activity : info.activities) {
                if (BuildConfig.DEBUG && !(activity.packageName.equals(info.packageName))) {
                    throw new AssertionError("Assertion failed");
                }
                ComponentName acomp = new ComponentName(activity.packageName, activity.name);
                MyActivityInfo myActivityInfo = cache.getActivityInfo(acomp);
                myActivityInfo.setPrivate(isPrivate(activity));
                myInfo.activities[i++] = myActivityInfo;
            }

            Arrays.sort(myInfo.activities);
        }

        return myInfo;
    }

    private static int countActivitiesFromInfo(PackageInfo info) {
        return info.activities.length;
    }

    private static boolean isPrivate(ActivityInfo activity) {
        return !activity.isEnabled() || !activity.exported;
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
