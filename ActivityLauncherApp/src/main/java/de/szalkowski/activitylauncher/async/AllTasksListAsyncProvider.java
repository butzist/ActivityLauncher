package de.szalkowski.activitylauncher.async;

import android.content.Context;

import de.szalkowski.activitylauncher.ui.adapter.AllTasksListAdapter;

public class AllTasksListAsyncProvider extends AsyncProvider<AllTasksListAdapter> {
    private final AllTasksListAdapter adapter;

    public AllTasksListAsyncProvider(
            Context context,
            AsyncProvider.Listener<AllTasksListAdapter> listener) {
        super(context, listener, true);
        this.adapter = new AllTasksListAdapter(context);
    }

    @Override
    protected AllTasksListAdapter run(Updater updater) {
        this.adapter.resolve(updater);
        return this.adapter;
    }
}
