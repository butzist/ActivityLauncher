package de.szalkowski.activitylauncher.object;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
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
            info.name = getActivityName(config, pm, activity, act);
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

    public int getIconResource() {
        return icon_resource;
    }

    public String getIconResourceName() {
        return icon_resource_name;
    }

    public String getName() {
        return name;
    }

    public Boolean isPrivate() {
        return is_private;
    }

    private static String getActivityName(Configuration config, PackageManager pm, ComponentName activityComponent, ActivityInfo activity) throws PackageManager.NameNotFoundException {
        Resources appRes = pm.getResourcesForApplication(activityComponent.getPackageName());
        appRes.updateConfiguration(config, new DisplayMetrics());
        return appRes.getString(activity.labelRes);
    }

    public void setComponentName(ComponentName component_name) {
        this.component_name = component_name;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setIconResource(int icon_resource) {
        this.icon_resource = icon_resource;
    }

    public void setIconResourceName(String icon_resource_name) {
        this.icon_resource_name = icon_resource_name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivate(Boolean is_private) {
        this.is_private = is_private;
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
