package de.szalkowski.activitylauncher.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.content.res.ResourcesCompat;

public class IconLoader {
    private final PackageManager pm;
    private final Context context;

    public IconLoader(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
    }

    @TargetApi(19)
    static Drawable getDrawable(Resources res, int id) {
        return res.getDrawable(id);
    }

    @TargetApi(21)
    static Drawable getDrawable(Resources res, int id, Resources.Theme theme) {
        return ResourcesCompat.getDrawable(res,id, theme);
    }

    public Drawable getIcon(String icon_resource_string) {
        try {
            String pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':'));
            String type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'));
            String name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1);
            Resources res = this.pm.getResourcesForApplication(pack);
            int id = res.getIdentifier(name, type, pack);

            if (Build.VERSION.SDK_INT >= 21) {
                return IconLoader.getDrawable(res, id, this.context.getTheme());
            } else {
                return IconLoader.getDrawable(res, id);
            }
        } catch (Exception e) {
            return this.pm.getDefaultActivityIcon();
        }
    }
}
