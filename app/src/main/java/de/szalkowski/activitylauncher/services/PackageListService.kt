package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.services.internal.isPrivate
import javax.inject.Inject

interface PackageListService {
    val packages: List<MyPackageInfo>
}

class PackageListServiceImpl @Inject constructor(
    @ApplicationContext context: Context, val settingsService: SettingsService
) : PackageListService {

    private val config: Configuration = settingsService.getLocaleConfiguration()
    private val packageManager: PackageManager = context.packageManager
    private val installedPackages: List<MyPackageInfo> =
        packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES).mapNotNull {
            getPackageInfo(it)
        }.sortedBy { it.name.lowercase() }

    override val packages: List<MyPackageInfo>
        get() = installedPackages

    private fun getPackageInfo(info: PackageInfo): MyPackageInfo? {
        val packageName = info.packageName as String? // do not trust Android implementations
        if (packageName.isNullOrEmpty()) {
            return null
        }

        val app = info.applicationInfo ?: return null
        val appRes = getLocalizedResources(packageName)

        val name = getName(app, appRes)
        val version = "${info.versionName} (${info.versionCode})"
        val icon = getIcon(app)
        val iconResourceName = getIconResourceName(app, appRes)
        val defaultActivityName = getDefaultActivityName(packageName, appRes)
        val activities = info.activities.orEmpty()
            .filter { !settingsService.hidePrivate || !it.isPrivate(packageManager) }
            .map { getActivityName(it, appRes) }
            .filter { it != defaultActivityName }

        return MyPackageInfo(
            packageName, name, version, defaultActivityName, activities, icon, iconResourceName
        )
    }

    private fun getDefaultActivityName(
        packageName: String, appRes: Resources?
    ): ActivityName? {
        if (appRes != null) {
            try {
                val defaultIntent = packageManager.getLaunchIntentForPackage(packageName)
                val activityInfo =
                    defaultIntent?.resolveActivityInfo(packageManager, 0) ?: return null
                val defaultActivityName = getActivityName(activityInfo, appRes)
                return defaultActivityName
            } catch (ignored: Exception) {
            }
        }

        return null
    }

    private fun getIcon(app: ApplicationInfo): Drawable {
        return try {
            packageManager.getApplicationIcon(app)
        } catch (ignored: Exception) {
            packageManager.defaultActivityIcon
        }
    }

    private fun getIconResourceName(
        app: ApplicationInfo, appRes: Resources?
    ): String? {
        val iconResource = app.icon

        if (iconResource != 0 && appRes != null) {
            try {
                return appRes.getResourceName(iconResource)
            } catch (ignored: Exception) {
            }
        }

        return null
    }


    private fun getName(app: ApplicationInfo, appRes: Resources?): String {
        var name = app.loadLabel(packageManager).toString()
        if (name == app.packageName) {
            name = createNameFromClass(name)
        }

        if (appRes != null) {
            try {
                name = appRes.getString(app.labelRes)
            } catch (ignored: Exception) {
            }
        }

        return name
    }

    private fun getActivityName(activity: ActivityInfo, appRes: Resources?): ActivityName {
        var name = createNameFromClass(activity.name)

        if (appRes != null) {
            try {
                name = appRes.getString(activity.labelRes)
            } catch (ignored: Exception) {
            }
        }

        val cls = activity.name.substringAfterLast('.')
        return ActivityName(name, cls, activity.name)
    }

    private fun getLocalizedResources(packageName: String): Resources? {
        try {
            val appRes = packageManager.getResourcesForApplication(packageName)
            appRes.updateConfiguration(config, DisplayMetrics())
            return appRes
        } catch (ignored: Exception) {
            return null
        }
    }

    private fun createNameFromClass(cls: String): String {
        val name = cls.substringAfterLast('.')
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(config.locale) else it.toString() }
    }
}

data class MyPackageInfo(
    val packageName: String,
    val name: String,
    val version: String,
    val defaultActivityName: ActivityName?,
    val activityNames: List<ActivityName>,
    val icon: Drawable,
    val iconResourceName: String?,
)

data class ActivityName(
    val name: String,
    val shortCls: String,
    val fullCls: String,
)