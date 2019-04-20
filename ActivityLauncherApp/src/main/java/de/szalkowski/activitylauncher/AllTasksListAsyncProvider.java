package de.szalkowski.activitylauncher;

import android.content.Context;

public class AllTasksListAsyncProvider extends AsyncProvider<AllTasksListAdapter> {
    private AllTasksListAdapter adapter;

    AllTasksListAsyncProvider(
            Context context,
            de.szalkowski.activitylauncher.AsyncProvider.Listener<AllTasksListAdapter> listener) {
        super(context, listener, true);
        this.adapter = new AllTasksListAdapter(context);
    }

    @Override
    protected AllTasksListAdapter run(Updater updater) {
        this.adapter.resolve(updater);
        return this.adapter;
    }
}
