package de.szalkowski.activitylauncher;

import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ArrayAdapter;

public class MainActivity extends FragmentActivity implements
		ActionBar.OnNavigationListener {
	
	protected final String LOG = "de.szalkowski.activitylauncher.MainActivity";
	
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		PackageManager pm = this.getPackageManager();
		List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		for(PackageInfo pack : packages) {
			Log.v(LOG,"package: " + pack.packageName);
			
			try {
				ApplicationInfo app = pm.getApplicationInfo(pack.packageName, 0);
				if(pack.activities == null) continue;
				
				for(ActivityInfo activity : pack.activities) {
					CharSequence label = pm.getText(pack.packageName, activity.labelRes, app);
					if(label==null) {
						label = "(no label)";
					}
					Log.v(LOG,"   activity: " + activity.name + " (" + label + ") " + (activity.isEnabled() && activity.exported ? "":"(disabled)"));
				}
			} catch (NameNotFoundException e) {
				Log.e(LOG, "package without app");
			}
		}
		*/

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.title_section_recent),
								getString(R.string.title_section_all), }), this);
	}

	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		
		Fragment fragment = null;
		switch(position) {
		case 0:
			fragment = new RecentTaskListFragment(); 
			break;
		case 1:
			fragment = new AllTasksListFragment();
			break;
		}		
		
		//Bundle args = new Bundle();
		//args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
		//fragment.setArguments(args);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).commit();
		return true;
	}
}
