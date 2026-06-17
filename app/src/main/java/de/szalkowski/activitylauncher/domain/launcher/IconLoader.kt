package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import androidx.core.graphics.drawable.IconCompat
import de.szalkowski.activitylauncher.domain.model.IconInfo
import de.szalkowski.activitylauncher.presentation.common.AsyncProvider
import de.szalkowski.activitylauncher.presentation.common.IconListAdapter

interface IconLoader {
    fun getIcon(iconResourceString: String): IconCompat
    fun getIcon(componentName: ComponentName): IconCompat
    fun getPackageIcon(packageName: String): IconCompat
    fun tryGetIcon(iconResourceString: String): Result<IconCompat>
    fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconInfo>

    class NullResourceException : Exception("Resource ID is zero")
}
