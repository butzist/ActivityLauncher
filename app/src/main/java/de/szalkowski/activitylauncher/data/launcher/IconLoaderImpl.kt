package de.szalkowski.activitylauncher.data.launcher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.toIconCompat
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.IconInfo
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.presentation.common.AsyncProvider
import de.szalkowski.activitylauncher.presentation.common.IconListAdapter
import javax.inject.Inject

class IconLoaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageRepository: PackageRepository,
    private val activityRepository: ActivityRepository,
    settingsRepository: SettingsRepository,
) : IconLoader {
    private val pm: PackageManager = context.packageManager
    private val configuration = settingsRepository.getLocaleConfiguration()

    override fun getIcon(iconResourceString: String): IconCompat {
        return tryGetIcon(iconResourceString).getOrElse {
            pm.defaultActivityIcon.toIconCompat()
        }
    }

    override fun getIcon(componentName: ComponentName): IconCompat {
        return try {
            val activityInfo = pm.getActivityInfo(componentName, 0)
            if (activityInfo.iconResource != 0) {
                if (componentName.packageName == context.packageName) {
                    val packageContext = context.createPackageContext(componentName.packageName, 0)
                    IconCompat.createWithResource(packageContext, activityInfo.iconResource)
                } else {
                    val drawable = activityInfo.loadIcon(pm)
                    drawable.toIconCompat()
                }
            } else {
                getPackageIcon(componentName.packageName)
            }
        } catch (e: Exception) {
            pm.defaultActivityIcon.toIconCompat()
        }
    }

    override fun getPackageIcon(packageName: String): IconCompat {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if (packageName == context.packageName) {
                val packageContext = context.createPackageContext(packageName, 0)
                IconCompat.createWithResource(packageContext, appInfo.icon)
            } else {
                val drawable = appInfo.loadIcon(pm)
                drawable.toIconCompat()
            }
        } catch (e: Exception) {
            pm.defaultActivityIcon.toIconCompat()
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun tryGetIcon(iconResourceString: String): Result<IconCompat> {
        return runCatching {
            val pack = iconResourceString.substringBefore(":")
            val typeAndName = iconResourceString.substringAfter(":")
            val type = typeAndName.substringBefore("/")
            val name = typeAndName.substringAfter("/")

            val res = pm.getResourcesForApplication(pack)
            // TODO: Replace with createConfigurationContext when minSdk is high enough
            res.updateConfiguration(configuration, res.displayMetrics)
            val id = res.getIdentifier(name, type, pack)

            if (id == 0) throw IconLoader.NullResourceException()

            if (pack == context.packageName) {
                val packageContext = context.createPackageContext(pack, 0)
                IconCompat.createWithResource(packageContext, id)
            } else {
                val drawable = pm.getDrawable(pack, id, null)!!
                drawable.toIconCompat()
            }
        }
    }

    override fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconInfo> {
        val icons: java.util.TreeSet<String> = java.util.TreeSet()

        val packages = packageRepository.packages
        updater?.updateMax(packages.size)
        updater?.update(0)

        for (pack in packages.withIndex()) {
            updater?.update(pack.index + 1)

            runCatching {
                val activities = activityRepository.getActivities(pack.value.packageName)
                for (activity in listOfNotNull(activities.defaultActivity) + activities.activities) {
                    activity.iconResourceName?.let { icons.add(it) }
                }
            }
        }

        return icons.map { IconInfo(it) }.toList()
    }
}
