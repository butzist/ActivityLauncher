package de.szalkowski.activitylauncher.data.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.IconInfo
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.presentation.common.AsyncProvider
import de.szalkowski.activitylauncher.presentation.common.IconListAdapter
import java.util.TreeMap
import javax.inject.Inject

class IconLoaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageRepository: PackageRepository,
    private val activityRepository: ActivityRepository,
    settingsRepository: SettingsRepository,
) : IconLoader {
    private val pm: PackageManager = context.packageManager
    private val configuration = settingsRepository.getLocaleConfiguration()

    override fun getIcon(iconResourceString: String): Drawable =
        tryGetIcon(iconResourceString).getOrElse {
            val errorText = when (it) {
                is IconLoader.NullResourceException -> R.string.error_invalid_icon_resource
                is NameNotFoundException -> R.string.error_invalid_icon_resource
                else -> R.string.error_invalid_icon_format
            }

            Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
            pm.defaultActivityIcon
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
        val icons: TreeMap<String, Drawable> = TreeMap()

        val packages = packageRepository.packages
        updater?.updateMax(packages.size)
        updater?.update(0)

        for (pack in packages.withIndex()) {
            updater?.update(pack.index + 1)

            runCatching {
                val activities = activityRepository.getActivities(pack.value.packageName)
                for (activity in listOfNotNull(activities.defaultActivity) + activities.activities) {
                    activity.iconResourceName?.let { icons[it] = activity.icon }
                }
            }
        }

        return icons.map { entry -> IconInfo(entry.key, entry.value) }.toList()
    }
}
