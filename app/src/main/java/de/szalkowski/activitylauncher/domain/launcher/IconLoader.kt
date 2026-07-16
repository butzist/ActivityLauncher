package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.IconInfo
import de.szalkowski.activitylauncher.presentation.common.AsyncProvider
import de.szalkowski.activitylauncher.presentation.common.IconListAdapter

interface IconLoader {
    fun getIcon(uri: android.net.Uri): Result<ActivityIcon>
    fun getIcon(iconResourceString: String): ActivityIcon
    fun getIcon(componentName: ComponentName): ActivityIcon
    fun getPackageIcon(packageName: String): ActivityIcon
    fun tryGetIcon(iconResourceString: String): Result<ActivityIcon>
    fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconInfo>

    class NullResourceException : Exception("Resource ID is zero")
}
