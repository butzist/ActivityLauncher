package de.szalkowski.activitylauncher.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.ui.AsyncProvider
import de.szalkowski.activitylauncher.ui.IconListAdapter
import java.util.TreeMap
import javax.inject.Inject

interface IconLoaderService {
    fun getIcon(iconResourceString: String): Drawable
    fun tryGetIcon(iconResourceString: String): Result<Drawable>
    fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconInfo>

    data class IconInfo(
        val iconResourceName: String,
        val icon: Drawable,
    )

    class NullResourceException : Exception("Resource ID is zero")
}

class IconLoaderServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageListService: PackageListService,
    private val activityListService: ActivityListService,
    settingsService: SettingsService,
) : IconLoaderService {
    private val pm: PackageManager = context.packageManager
    private val configuration = settingsService.getLocaleConfiguration()

    override fun getIcon(iconResourceString: String): Drawable =
        tryGetIcon(iconResourceString).getOrElse {
            val errorText = when (it) {
                is IconLoaderService.NullResourceException -> R.string.error_invalid_icon_resource
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

            if (id == 0) throw IconLoaderService.NullResourceException()

            ResourcesCompat.getDrawable(res, id, context.theme) ?: throw IconLoaderService.NullResourceException()
        }
    }

    override fun loadIcons(updater: AsyncProvider<IconListAdapter>.Updater?): List<IconLoaderService.IconInfo> {
        val icons: TreeMap<String, Drawable> = TreeMap()

        val packages = packageListService.packages
        updater?.updateMax(packages.size)
        updater?.update(0)

        for (pack in packages.withIndex()) {
            updater?.update(pack.index + 1)

            runCatching {
                val activities = activityListService.getActivities(pack.value.packageName)
                for (activity in listOfNotNull(activities.defaultActivity) + activities.activities) {
                    activity.iconResourceName?.let { icons[it] = activity.icon }
                }
            }
        }

        return icons.map { entry -> IconLoaderService.IconInfo(entry.key, entry.value) }.toList()
    }
}
