package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class MyPackageInfo(
    val id: Long,
    val packageName: String,
    val name: String,
    val version: String,
    val defaultActivityName: ActivityName?,
    val activityNames: List<ActivityName>,
    val icon: Drawable,
    val iconResourceName: String?,
    val isFullyLoaded: Boolean = true,
)

data class ActivityName(
    val name: String,
    val shortCls: String,
    val fullCls: String,
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
    val icon: Drawable,
    val iconResourceName: String?,
    val isPrivate: Boolean,
)

data class IconInfo(
    val iconResourceName: String,
    val icon: Drawable,
)
