package de.szalkowski.activitylauncher.data.launcher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.qualifiers.ApplicationContext
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

    override fun getIcon(iconResourceString: String): Drawable {
        return tryGetIcon(iconResourceString).getOrElse {
            pm.defaultActivityIcon
        }
    }

    override fun getIcon(componentName: ComponentName): Drawable {
        return runCatching {
            val activityInfo = pm.getActivityInfo(componentName, 0)
            activityInfo.loadIcon(pm)
        }.getOrElse {
            pm.defaultActivityIcon
        }
    }

    override fun getPackageIcon(packageName: String): Drawable {
        return runCatching {
            val app = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationIcon(app)
        }.getOrElse {
            pm.defaultActivityIcon
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun tryGetIcon(iconResourceString: String): Result<Drawable> {
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

            ResourcesCompat.getDrawable(res, id, context.theme) ?: throw IconLoader.NullResourceException()
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
