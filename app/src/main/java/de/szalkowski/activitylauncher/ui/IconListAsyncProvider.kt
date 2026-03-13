package de.szalkowski.activitylauncher.ui

import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ActivityContext

class IconListAsyncProvider @AssistedInject constructor(
    @ActivityContext context: Context,
    private val adapter: IconListAdapter,
    @Assisted listener: Listener<IconListAdapter>?,
) : AsyncProvider<IconListAdapter>(context, listener, false) {
    @AssistedFactory
    interface IconListAsyncProviderFactory {
        fun create(listener: Listener<IconListAdapter>?): IconListAsyncProvider
    }

    override fun run(updater: Updater?): IconListAdapter {
        adapter.resolve(updater)
        return this.adapter
    }
}
