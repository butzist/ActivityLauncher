package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

public class MyActivityInfo implements Comparable<MyActivityInfo> {
    protected Drawable icon;
    protected String name;
    ComponentName component_name;
    int icon_resource;
    String icon_resource_name;
    boolean is_private;

    public static MyActivityInfo fromComponentName(PackageManager pm, ComponentName activity, Configuration config) {
        var info = new MyActivityInfo();
        info.component_name = activity;

        ActivityInfo act;
        try {
            act = pm.getActivityInfo(activity, 0);


            info.name = getActivityName(config,pm,activity,act); //act.loadLabel(pm).toString();
            try {
                info.icon = act.loadIcon(pm);
            } catch (Exception e) {
                info.icon = pm.getDefaultActivityIcon();
            }
            info.icon_resource = act.getIconResource();
        } catch (Exception e) {
            info.name = activity.getShortClassName();
            info.icon = pm.getDefaultActivityIcon();
            info.icon_resource = 0;
        }

        info.icon_resource_name = null;
        if (info.icon_resource != 0) {
            try {
                info.icon_resource_name = pm.getResourcesForActivity(activity).getResourceName(info.icon_resource);
            } catch (Exception ignored) {
            }
        }

        return info;
    }

    public ComponentName getComponentName() {
        return component_name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getIconResouceName() {
        return icon_resource_name;
    }

    private static String getActivityName(Configuration config, PackageManager pm, ComponentName activityComponent, ActivityInfo activity) throws PackageManager.NameNotFoundException {
        Resources appRes = pm.getResourcesForApplication(activityComponent.getPackageName());
        appRes.updateConfiguration(config, new DisplayMetrics());
        return appRes.getString(activity.labelRes);
    }
    @Override
    public int compareTo(MyActivityInfo another) {
        int cmp_name = this.name.compareTo(another.name);
        if (cmp_name != 0) return cmp_name;

        return this.component_name.compareTo(another.component_name);
    }

    @Override
    public boolean equals(Object other) {
        if (!other.getClass().equals(MyActivityInfo.class)) {
            return false;
        }

        MyActivityInfo other_info = (MyActivityInfo) other;
        return this.component_name.equals(other_info.component_name);
    }

    public void setPrivate(boolean isPrivate) {
        this.is_private = isPrivate;
    }
}
