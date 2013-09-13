package de.szalkowski.activitylauncher;

import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class IconListAdapter extends BaseAdapter {
	private String[] icons;
	private Context context;
	private PackageManager pm;

	public IconListAdapter(Context context, IconListAsyncProvider.Updater updater) {
		TreeSet<String> icons = new TreeSet<String>();
		
		this.context = context;
		this.pm = context.getPackageManager();
		List<PackageInfo> all_packages = this.pm.getInstalledPackages(0);
		updater.updateMax(all_packages.size());
		updater.update(0);
		
		PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(this.pm);

		for(int i=0; i < all_packages.size(); ++i) {
			updater.update(i+1);
			
			PackageInfo pack = all_packages.get(i);			
			try {
				MyPackageInfo mypack = cache.getPackageInfo(pack.packageName);

				for(int j=0; j < mypack.getActivitiesCount(); ++j) {
					String icon_resource_name = mypack.getActivity(j).getIconResouceName();
					if(icon_resource_name != null) {
						icons.add(icon_resource_name);
					}
				}
			} catch (NameNotFoundException e) {	}

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

	static public Drawable getIcon(String icon_resource_string, PackageManager pm) {
		try {
			String pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':'));
			String type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'));
			String name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1, icon_resource_string.length());
			Resources res = pm.getResourcesForApplication(pack);
			Drawable icon = res.getDrawable(res.getIdentifier(name, type, pack));
			return icon;
		} catch(Exception e) {
			return pm.getDefaultActivityIcon();
		}
		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView view = new ImageView(this.context);
		AbsListView.LayoutParams layout = new AbsListView.LayoutParams(50, 50);
		view.setLayoutParams(layout);
		String icon_resource_string = this.icons[position]; 
		view.setImageDrawable(IconListAdapter.getIcon(icon_resource_string, this.pm));
		return view;
	}
}
