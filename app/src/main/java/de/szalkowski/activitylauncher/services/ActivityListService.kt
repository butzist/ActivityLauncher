package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

interface ActivityListService {
    fun getActivities(
        packageName: String,
    ): List<MyActivityInfo>

    fun getActivity(
        componentName: ComponentName,
    ): MyActivityInfo
}

class ActivityListServiceImpl @Inject constructor(@ActivityContext context: Context, settingsService: SettingsService) :
    ActivityListService {

    private val config: Configuration = settingsService.getLocaleConfiguration()
    private val packageManager = context.packageManager

    override fun getActivities(packageName: String): List<MyActivityInfo> {
        val info = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        } catch (e: Exception) {
            return listOf()
        }

        return info.activities.orEmpty().map {
            getActivityInfo(it)
        }.sortedBy { it.name }
    }

    override fun getActivity(componentName: ComponentName): MyActivityInfo {
        val activityInfo = try {
            packageManager.getActivityInfo(componentName, 0)
        } catch (e: Exception) {
            return MyActivityInfo(
                componentName,
                componentName.shortClassName,
                packageManager.defaultActivityIcon,
                null,
                false
            )
        }

        return getActivityInfo(activityInfo)
    }

    private fun getActivityInfo(
        activityInfo: ActivityInfo
    ): MyActivityInfo {
        val componentName = getComponentName(activityInfo)
        val name = getLocalizedName(activityInfo)
        val icon = getIcon(activityInfo)
        val iconResourceName = getIconResourceName(activityInfo)
        val isPrivate = isPrivate(activityInfo)

        return MyActivityInfo(
            componentName,
            name,
            icon,
            iconResourceName,
            isPrivate,
        )
    }

    private fun getComponentName(activityInfo: ActivityInfo) =
        ComponentName(activityInfo.packageName, activityInfo.name)

    private fun getIconResourceName(
        activityInfo: ActivityInfo
    ): String? {
        if (activityInfo.iconResource == 0) {
            return null
        }

        return try {
            packageManager.getResourcesForActivity(getComponentName(activityInfo))
                .getResourceName(activityInfo.iconResource)
        } catch (ignored: Exception) {
            null
        }
    }

    private fun getIcon(activityInfo: ActivityInfo): Drawable = try {
        activityInfo.loadIcon(packageManager)
    } catch (e: Exception) {
        packageManager.defaultActivityIcon
    }

    private fun getLocalizedName(
        activityInfo: ActivityInfo
    ): String = try {
        val appRes = packageManager.getResourcesForApplication(activityInfo.packageName)
        appRes.updateConfiguration(config, DisplayMetrics())
        appRes.getString(activityInfo.labelRes)
    } catch (e: Exception) {
        getComponentName(activityInfo).shortClassName
    }

    private fun isPrivate(activityInfo: ActivityInfo): Boolean {
        if (!activityInfo.exported) return true

        val enabledState = packageManager.getComponentEnabledSetting(getComponentName(activityInfo))
        return when (enabledState) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> false
            else -> !activityInfo.isEnabled
        }
    }
}

data class MyActivityInfo(
    val componentName: ComponentName,
    val name: String,
    val icon: Drawable,
    val iconResourceName: String?,
    var isPrivate: Boolean,
)