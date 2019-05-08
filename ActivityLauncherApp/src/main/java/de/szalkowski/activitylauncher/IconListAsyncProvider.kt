package de.szalkowski.activitylauncher

import android.content.Context

internal class IconListAsyncProvider(context: Context, listener: AsyncProvider.Listener<IconListAdapter>) : AsyncProvider<IconListAdapter>(context, listener, false) {
    private val adapter: IconListAdapter

    init {
        this.adapter = IconListAdapter(context)
    }

    override fun run(updater: AsyncProvider<IconListAdapter>.Updater): IconListAdapter {
        adapter.resolve(updater)
        return this.adapter
    }
}
