package de.szalkowski.activitylauncher.todo;

import android.content.Context;

class IconListAsyncProvider extends AsyncProvider<IconListAdapter> {
    private final IconListAdapter adapter;

    IconListAsyncProvider(Context context, Listener<IconListAdapter> listener) {
        super(context, listener, false);
        this.adapter = new IconListAdapter(context);
    }

    @Override
    protected IconListAdapter run(Updater updater) {
        adapter.resolve(updater);
        return this.adapter;
    }
}
