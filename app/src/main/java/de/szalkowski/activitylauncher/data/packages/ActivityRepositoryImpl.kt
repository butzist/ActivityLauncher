package de.szalkowski.activitylauncher.data.packages

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.componentName
import de.szalkowski.activitylauncher.core.util.isPrivate
import de.szalkowski.activitylauncher.domain.model.*
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository,
    private val packageRepository: PackageRepository,
) : ActivityRepository {

    private val packageManager = context.packageManager

    override fun getActivities(packageName: String): PackageActivities {
        val infos = runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
        }.getOrNull().orEmpty().associateBy { i -> i.name }

        val pack = this.packageRepository.getPackage(packageName)
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
                .map { (name, info) -> getActivityInfo(info!!, name) },
        )
    }

    override fun getActivity(componentName: ComponentName): MyActivityInfo {
        val pack = this.packageRepository.getPackage(componentName.packageName)
        val activityInfo = runCatching {
            packageManager.getActivityInfo(componentName, 0)
        }.getOrNull()

        val names = pack?.let { listOfNotNull(it.defaultActivityName) + it.activityNames }
        val name = names?.find { n -> n.fullCls == componentName.className }

        if (activityInfo == null || name == null) {
            return MyActivityInfo(
                componentName,
                createNameFromClass(componentName.className),
                null,
                false,
            )
        }

        return getActivityInfo(activityInfo, name)
    }

    override fun getIcon(componentName: ComponentName): Drawable {
        return runCatching {
            val activityInfo = packageManager.getActivityInfo(componentName, 0)
            activityInfo.loadIcon(packageManager)
        }.getOrElse {
            packageManager.defaultActivityIcon
        }
    }

    override fun invalidate() {
        this.packageRepository.invalidate()
    }

    private fun getActivityInfo(
        activityInfo: ActivityInfo,
        nameInfo: ActivityName,
    ): MyActivityInfo {
        val componentName = activityInfo.componentName
        val name = nameInfo.name
        val iconResourceName = getIconResourceName(activityInfo)
        val isPrivate = activityInfo.isPrivate(packageManager)

        return MyActivityInfo(
            componentName,
            name,
            iconResourceName,
            isPrivate,
        )
    }

    private fun getIconResourceName(
        activityInfo: ActivityInfo,
    ): String? {
        if (activityInfo.iconResource == 0) {
            return null
        }

        return runCatching {
            packageManager.getResourcesForActivity(activityInfo.componentName)
                .getResourceName(activityInfo.iconResource)
        }.getOrNull()
    }

    private fun createNameFromClass(cls: String): String {
        val name = cls.substringAfterLast('.')
        val config = settingsRepository.getLocaleConfiguration()
        val locale = androidx.core.os.ConfigurationCompat.getLocales(config).get(0) ?: java.util.Locale.getDefault()
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}
