package de.szalkowski.activitylauncher

import android.content.Context

class AllTasksListAsyncProvider internal constructor(
        context: Context,
        listener: AsyncProvider.Listener<AllTasksListAdapter>) : AsyncProvider<AllTasksListAdapter>(context, listener, true) {
    private val adapter: AllTasksListAdapter

    init {
        this.adapter = AllTasksListAdapter(context)
    }

    override fun run(updater: AsyncProvider<AllTasksListAdapter>.Updater): AllTasksListAdapter {
        this.adapter.resolve(updater)
        return this.adapter
    }
}
