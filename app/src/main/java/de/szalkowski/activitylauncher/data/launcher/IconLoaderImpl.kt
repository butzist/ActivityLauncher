package de.szalkowski.activitylauncher.data.launcher

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.getLauncherLargeIconSize
import de.szalkowski.activitylauncher.core.util.resize
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.IconInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.presentation.common.AsyncProvider
import de.szalkowski.activitylauncher.presentation.common.IconListAdapter
import javax.inject.Inject

class IconLoaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageRepository: PackageRepository,
    settingsRepository: SettingsRepository,
) : IconLoader {
    private val pm: PackageManager = context.packageManager
    private val configuration = settingsRepository.getLocaleConfiguration()

    override fun getIcon(uri: android.net.Uri): Result<ActivityIcon> {
        return runCatching {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val resized = bitmap.resize(context.getLauncherLargeIconSize())
            ActivityIcon.BitmapIcon(resized, false)
        }
    }

    override fun getIcon(iconResourceString: String): ActivityIcon {
        return tryGetIcon(iconResourceString).getOrElse {
            ActivityIcon.from(pm.defaultActivityIcon, context.getLauncherLargeIconSize())
        }
    }

    override fun getIcon(componentName: ComponentName): ActivityIcon {
        return try {
            val activityInfo = pm.getActivityInfo(componentName, 0)
            if (activityInfo.iconResource != 0) {
                val res = pm.getResourcesForApplication(componentName.packageName)
                val resourceName = runCatching { res.getResourceName(activityInfo.iconResource) }.getOrNull()
                ActivityIcon.Resource(componentName.packageName, activityInfo.iconResource, resourceName)
            } else {
                val drawable = activityInfo.loadIcon(pm)
                ActivityIcon.from(drawable, context.getLauncherLargeIconSize())
            }
        } catch (e: Exception) {
            ActivityIcon.from(pm.defaultActivityIcon, context.getLauncherLargeIconSize())
        }
    }

    override fun getPackageIcon(packageName: String): ActivityIcon {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if (appInfo.icon != 0) {
                val res = pm.getResourcesForApplication(packageName)
                val resourceName = runCatching { res.getResourceName(appInfo.icon) }.getOrNull()
                ActivityIcon.Resource(packageName, appInfo.icon, resourceName)
            } else {
                val drawable = appInfo.loadIcon(pm)
                ActivityIcon.from(drawable, context.getLauncherLargeIconSize())
            }
        } catch (e: Exception) {
            ActivityIcon.from(pm.defaultActivityIcon, context.getLauncherLargeIconSize())
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun tryGetIcon(iconResourceString: String): Result<ActivityIcon> {
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

            ActivityIcon.Resource(pack, id, iconResourceString)
        }
    }

    override fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconInfo> {
        val icons: java.util.HashSet<String> = java.util.HashSet()

        val packages = packageRepository.packages
        updater?.updateMax(packages.size)
        updater?.update(0)

        for (pack in packages.withIndex()) {
            updater?.update(pack.index + 1)

            runCatching {
                val activities = packageRepository.getActivities(pack.value.packageName)
                for (activity in listOfNotNull(activities.defaultActivity) + activities.activities) {
                    activity.iconResourceName?.let { icons.add(it) }
                }
            }
        }

        return icons.map { IconInfo(it) }
            .sortedWith(
                compareBy(
                    { it.iconResourceName.substringAfterLast('/') },
                    { it.iconResourceName.substringBefore(':') },
                ),
            )
    }
}
