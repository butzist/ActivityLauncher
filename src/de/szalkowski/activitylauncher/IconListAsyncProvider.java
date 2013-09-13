package de.szalkowski.activitylauncher;

import android.content.Context;

public class IconListAsyncProvider extends AsyncProvider<IconListAdapter> {
	public IconListAsyncProvider(Context context, Listener<IconListAdapter> listener) {
		super(context, listener, false);
	}
	
	@Override
	protected IconListAdapter run(Updater updater) {
		IconListAdapter adapter = new IconListAdapter(this.context, updater);
		return adapter;
	}
}
