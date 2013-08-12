package de.szalkowski.activitylauncher;

import java.util.List;
import java.util.TreeSet;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

	public IconListAdapter(Context context) {
		TreeSet<String> icons = new TreeSet<String>();
		
		this.context = context;
		this.pm = context.getPackageManager();
		List<PackageInfo> all_packages = this.pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		
		for(PackageInfo pack : all_packages) {
			if(pack.activities == null) continue;

			for(ActivityInfo activity : pack.activities) {
				try	{
					ComponentName acomp = new ComponentName(activity.packageName, activity.name);
					int icon_resource = activity.getIconResource();
					if(icon_resource != 0) {
						String icon_resource_name = this.pm.getResourcesForActivity(acomp).getResourceName(icon_resource);
						icons.add(icon_resource_name);
					}
				} catch(Exception e) {}
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
