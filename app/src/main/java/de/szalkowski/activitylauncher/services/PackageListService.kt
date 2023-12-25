package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface PackageListService {
    val packages: List<MyPackageInfo>
}

class PackageListServiceImpl @Inject constructor(@ApplicationContext context: Context, settingsService: SettingsService) :
    PackageListService {

    private val config: Configuration = settingsService.getLocaleConfiguration()
    private val packageManager: PackageManager = context.packageManager

    override val packages: List<MyPackageInfo>
        get() = packageManager.getInstalledPackages(0).map {
            getPackageInfo(it.packageName)
        }.sortedBy { it.name.lowercase() }

    private fun getPackageInfo(packageName: String): MyPackageInfo {
        val info = packageManager.getPackageInfo(packageName, 0)
        val app = info.applicationInfo ?: return MyPackageInfo(
            packageName, packageName, packageManager.defaultActivityIcon, null
        )

        val name = getLocalizedName(
            config, packageName, app
        )

        val icon = try {
            packageManager.getApplicationIcon(app)
        } catch (e: Exception) {
            packageManager.defaultActivityIcon
        }
        val iconResource = app.icon
        val iconResourceName = if (iconResource != 0) {
            try {
                packageManager.getResourcesForApplication(app).getResourceName(iconResource)
            } catch (ignored: Exception) {
                null
            }
        } else {
            null
        }

        return MyPackageInfo(packageName, name, icon, iconResourceName)
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getLocalizedName(
        config: Configuration, packageName: String, app: ApplicationInfo
    ): String {
        return try {
            val appRes = packageManager.getResourcesForApplication(packageName)
            appRes.updateConfiguration(config, DisplayMetrics())
            appRes.getString(app.labelRes)
        } catch (ignored: PackageManager.NameNotFoundException) {
            app.loadLabel(packageManager).toString()
        } catch (ignored: RuntimeException) {
            app.loadLabel(packageManager).toString()
        }
    }
}

data class MyPackageInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val iconResourceName: String?,
)

