package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

public class MyActivityInfo implements Comparable<MyActivityInfo> {
    protected Drawable icon;
    protected String name;
    ComponentName component_name;
    int icon_resource;
    String icon_resource_name;

    MyActivityInfo(ComponentName activity, PackageManager pm) {
        this.component_name = activity;

        ActivityInfo act;
        try {
            act = pm.getActivityInfo(activity, 0);
            this.name = act.loadLabel(pm).toString();
            try {
                this.icon = act.loadIcon(pm);
            } catch (ClassCastException e) {
                this.icon = pm.getDefaultActivityIcon();
            } catch (IllegalArgumentException e) {
                this.icon = pm.getDefaultActivityIcon();
            }
            this.icon_resource = act.getIconResource();
        } catch (NameNotFoundException e) {
            this.name = activity.getShortClassName();
            this.icon = pm.getDefaultActivityIcon();
            this.icon_resource = 0;
        }

        this.icon_resource_name = null;
        if (this.icon_resource != 0) {
            try {
                this.icon_resource_name = pm.getResourcesForActivity(activity).getResourceName(this.icon_resource);
            } catch (Exception ignored) {
            }
        }
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

    @Override
    public int compareTo(MyActivityInfo another) {
        int cmp_name = this.name.compareTo(another.name);
        if (cmp_name != 0) return cmp_name;

        return this.component_name.compareTo(another.component_name);
    }

    @Override
    public boolean equals(Object other) {
        if (!other.getClass().equals(MyPackageInfo.class)) {
            return false;
        }

        MyActivityInfo other_info = (MyActivityInfo) other;
        return this.component_name.equals(other_info.component_name);
    }
}