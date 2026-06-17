package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.graphics.drawable.Drawable
import de.szalkowski.activitylauncher.domain.model.IconInfo
import de.szalkowski.activitylauncher.presentation.common.AsyncProvider
import de.szalkowski.activitylauncher.presentation.common.IconListAdapter

interface IconLoader {
    fun getIcon(iconResourceString: String): Drawable
    fun getIcon(componentName: ComponentName): Drawable
    fun getPackageIcon(packageName: String): Drawable
    fun tryGetIcon(iconResourceString: String): Result<Drawable>
    fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconInfo>

    class NullResourceException : Exception("Resource ID is zero")
}
