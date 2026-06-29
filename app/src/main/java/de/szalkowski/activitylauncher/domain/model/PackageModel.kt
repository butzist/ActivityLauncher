package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName

data class MyPackageInfo(
    val id: Long,
    val packageName: String,
    val name: String,
    val version: String,
    val defaultActivityName: ActivityName?,
    val activityNames: List<ActivityName>,
    val iconResourceName: String?,
    val isFullyLoaded: Boolean = true,
)

data class ActivityName(
    val name: String,
    val shortCls: String,
    val fullCls: String,
    val isPrivate: Boolean,
    val iconResourceName: String?,
)

data class PackageActivities(
    val packageName: String,
    val name: String,
    val defaultActivity: MyActivityInfo?,
    val activities: List<MyActivityInfo>,
)

data class MyActivityInfo(
    val componentName: ComponentName,
    val name: String,
    val iconResourceName: String?,
    val isPrivate: Boolean,
    val isDefault: Boolean = false,
)

data class SystemPackage(
    val packageName: String,
    val name: String,
    val version: String,
    val iconResourceName: String?,
)

data class IconInfo(
    val iconResourceName: String,
)
