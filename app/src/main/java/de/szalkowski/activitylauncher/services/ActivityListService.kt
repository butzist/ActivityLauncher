package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ActivityContext
import de.szalkowski.activitylauncher.services.internal.componentName
import de.szalkowski.activitylauncher.services.internal.isPrivate
import javax.inject.Inject

interface ActivityListService {
    fun getActivities(
        packageName: String,
    ): PackageActivities

    fun getActivity(
        componentName: ComponentName,
    ): MyActivityInfo
}

class ActivityListServiceImpl @Inject constructor(
    @ActivityContext context: Context,
    settingsService: SettingsService,
    private val packageListService: PackageListService
) : ActivityListService {

    private val config: Configuration = settingsService.getLocaleConfiguration()
    private val packageManager = context.packageManager

    override fun getActivities(packageName: String): PackageActivities {
        val infos = runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
        }.getOrNull().orEmpty().associateBy { i -> i.name }

        val pack = this.packageListService.packages.find { p -> p.packageName == packageName }
            ?: return PackageActivities(packageName, packageName, null, listOf())
        val defaultActivity = pack.defaultActivityName?.let { name ->
            infos[name.fullCls]?.let { info -> getActivityInfo(info, name) }
        }

        return PackageActivities(
            pack.packageName,
            pack.name,
            defaultActivity,
            pack.activityNames.associateWith { n -> infos[n.fullCls] }
                .filterValues { v -> v != null }
                .map { (name, info) -> getActivityInfo(info!!, name) })
    }

    override fun getActivity(componentName: ComponentName): MyActivityInfo {
        val pack =
            this.packageListService.packages.find { p -> p.packageName == componentName.packageName }
        val activityInfo = runCatching {
            packageManager.getActivityInfo(componentName, 0)
        }.getOrNull()

        val names = pack?.let { listOfNotNull(it.defaultActivityName) + it.activityNames }
        val name = names?.find { n -> n.fullCls == componentName.className }

        if (activityInfo == null || name == null) return MyActivityInfo(
            componentName,
            createNameFromClass(componentName.className),
            packageManager.defaultActivityIcon,
            null,
            false
        )

        return getActivityInfo(activityInfo, name)
    }

    private fun getActivityInfo(
        activityInfo: ActivityInfo,
        nameInfo: ActivityName,
    ): MyActivityInfo {
        val componentName = activityInfo.componentName
        val name = nameInfo.name
        val icon = getIcon(activityInfo)
        val iconResourceName = getIconResourceName(activityInfo)
        val isPrivate = activityInfo.isPrivate(packageManager)

        return MyActivityInfo(
            componentName,
            name,
            icon,
            iconResourceName,
            isPrivate,
        )
    }


    private fun getIconResourceName(
        activityInfo: ActivityInfo
    ): String? {
        if (activityInfo.iconResource == 0) {
            return null
        }

        return runCatching {
            packageManager.getResourcesForActivity(activityInfo.componentName)
                .getResourceName(activityInfo.iconResource)
        }.getOrNull()
    }

    private fun getIcon(activityInfo: ActivityInfo): Drawable = runCatching {
        activityInfo.loadIcon(packageManager)
    }.getOrElse {
        packageManager.defaultActivityIcon
    }

    private fun createNameFromClass(cls: String): String {
        val name = cls.substringAfterLast('.')
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(config.locale) else it.toString() }
    }
}

data class PackageActivities(
    val packageName: String,
    val name: String,
    val defaultActivity: MyActivityInfo?,
    val activities: List<MyActivityInfo>
)

data class MyActivityInfo(
    val componentName: ComponentName,
    val name: String,
    val icon: Drawable,
    val iconResourceName: String?,
    val isPrivate: Boolean,
)