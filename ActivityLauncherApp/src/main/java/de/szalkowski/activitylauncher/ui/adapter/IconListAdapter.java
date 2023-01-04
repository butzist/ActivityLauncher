package de.szalkowski.activitylauncher.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.preference.PreferenceManager;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import de.szalkowski.activitylauncher.constant.Constants;
import de.szalkowski.activitylauncher.util.IconLoader;
import de.szalkowski.activitylauncher.util.SettingsUtils;
import de.szalkowski.activitylauncher.async.IconListAsyncProvider;
import de.szalkowski.activitylauncher.manager.PackageManagerCache;
import de.szalkowski.activitylauncher.object.MyPackageInfo;

public class IconListAdapter extends BaseAdapter {
    private final PackageManager pm;
    private final Context context;
    private final IconLoader loader;
    private final SharedPreferences prefs;
    private String[] icons;

    public IconListAdapter(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.loader = new IconLoader(context);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(context));
    }

    public void resolve(IconListAsyncProvider.Updater updater) {
        TreeSet<String> icons = new TreeSet<>();
        List<PackageInfo> all_packages = this.pm.getInstalledPackages(0);
        Configuration locale = SettingsUtils.createLocaleConfiguration(prefs.getString(Constants.PREF_LANGUAGE, "System Default"));
        updater.updateMax(all_packages.size());
        updater.update(0);

        PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(this.pm);

        for (int i = 0; i < all_packages.size(); ++i) {
            updater.update(i + 1);

            PackageInfo pack = all_packages.get(i);
            try {
                MyPackageInfo myPack = cache.getPackageInfo(pack.packageName, locale);

                for (int j = 0; j < myPack.getActivitiesCount(); ++j) {
                    String icon_resource_name = myPack.getActivity(j).getIconResourceName();
                    if (icon_resource_name != null) {
                        icons.add(icon_resource_name);
                    }
                }
            } catch (NameNotFoundException | RuntimeException ignored) {
            }
        }

        this.icons = new String[icons.size()];
        this.icons = icons.toArray(this.icons);
    }

    @Override
    public int getCount() {
        return icons.length;
    }

    @Override
    public Object getItem(int position) {
        return icons[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView view = new ImageView(this.context);
        AbsListView.LayoutParams layout = new AbsListView.LayoutParams(50, 50);
        view.setLayoutParams(layout);
        String icon_resource_string = this.icons[position];
        Drawable icon = loader.getIcon(icon_resource_string);
        view.setImageDrawable(icon);
        return view;
    }
}
