package de.szalkowski.activitylauncher;

import java.util.HashSet;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class IconListAdapter extends BaseAdapter {
	private String[] icons;
	private Context context;
	private PackageManager pm;

	public IconListAdapter(Context context) {
		HashSet<String> icons = new HashSet<String>();

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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView view = new ImageView(this.context);
		String icon_resource_string = this.icons[position]; 
		String pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':') + 1);
		try {
			Resources res = this.pm.getResourcesForApplication(pack); 
			Drawable icon = res.getDrawable(res.getIdentifier(null, null, icon_resource_string));
			view.setImageDrawable(icon);
		} catch(Exception e) {
			view.setImageDrawable(this.pm.getDefaultActivityIcon());
		}
		return view;
	}

}
