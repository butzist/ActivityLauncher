package de.szalkowski.activitylauncher.data.packages

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.isPrivate
import de.szalkowski.activitylauncher.data.database.*
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageDao: PackageDao,
    private val settingsRepository: SettingsRepository,
) {
    private val packageManager: PackageManager = context.packageManager
    private val loadingPackages = mutableSetOf<String>()

    val allPackagesFlow: Flow<List<PackageWithActivities>> = packageDao.getAllPackagesFlow()

    suspend fun sync() = withContext(Dispatchers.IO) {
        val installedPackages = getInstalledPackages()
        val dbPackages = packageDao.getAllPackagesFlow().first().associateBy { it.pkg.packageName }

        installedPackages.forEach { installed ->
            val dbPkg = dbPackages[installed.packageName]?.pkg
            val currentVersion = getVersion(installed)
            if (dbPkg == null || dbPkg.version != currentVersion) {
                val name = installed.applicationInfo?.loadLabel(packageManager)?.toString() ?: installed.packageName
                val newPkg = AppPackageEntity(
                    packageName = installed.packageName,
                    name = name,
                    version = currentVersion,
                    iconResourceName = null,
                    isFullyLoaded = false,
                    lastUpdated = System.currentTimeMillis(),
                )
                packageDao.insertPackage(newPkg)
            }
        }

        val installedPackageNames = installedPackages.map { it.packageName }.toSet()
        dbPackages.keys.filter { it !in installedPackageNames }.forEach {
            packageDao.deletePackageByName(it)
        }
    }

    private fun getInstalledPackages(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packageManager.getInstalledPackages(
                PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS,
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)
        }
    }

    suspend fun loadAllDetails(onProgress: (Int, Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val packagesToLoad = packageDao.getNotFullyLoadedPackages()
        val total = packagesToLoad.size
        packagesToLoad.forEachIndexed { index, pkg ->
            loadDetails(pkg.packageName)
            onProgress(index + 1, total)
        }
    }

    suspend fun loadDetails(packageName: String) = withContext(Dispatchers.IO) {
        synchronized(loadingPackages) {
            if (!loadingPackages.add(packageName)) return@withContext
        }

        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.GET_ACTIVITIES or PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_ACTIVITIES
            }
            val info = packageManager.getPackageInfo(packageName, flags)
            val app = info.applicationInfo
            if (app == null) {
                markAsLoaded(packageName)
                return@withContext
            }

            val appRes = getLocalizedResources(packageName)
            val infoActivities = info.activities
            val activities = if (infoActivities == null) {
                emptyList()
            } else {
                infoActivities.filter { !settingsRepository.hidePrivate || !it.isPrivate(packageManager) }
            }

            val activityEntities = activities.map { activity ->
                val name = if (appRes != null) {
                    runCatching { appRes.getString(activity.labelRes) }.getOrElse { activity.loadLabel(packageManager).toString() }
                } else {
                    activity.loadLabel(packageManager).toString()
                }
                ActivityEntity(
                    id = 0,
                    packageName = packageName,
                    name = name,
                    shortCls = activity.name.substringAfterLast('.'),
                    fullCls = activity.name,
                    isDefault = false,
                )
            }

            val updatedPkg = AppPackageEntity(
                packageName = packageName,
                name = app.loadLabel(packageManager).toString(),
                version = getVersion(info),
                iconResourceName = runCatching { appRes?.getResourceName(app.icon) }.getOrNull(),
                isFullyLoaded = true,
                lastUpdated = System.currentTimeMillis(),
            )

            packageDao.updatePackageDetails(updatedPkg, activityEntities)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e

            if (e is PackageManager.NameNotFoundException) {
                packageDao.deletePackageByName(packageName)
            } else {
                markAsLoaded(packageName)
            }
        } finally {
            synchronized(loadingPackages) {
                loadingPackages.remove(packageName)
            }
        }
    }

    private suspend fun markAsLoaded(packageName: String) {
        packageDao.getPackage(packageName)?.let { existing ->
            packageDao.insertPackage(existing.copy(isFullyLoaded = true))
        }
    }

    suspend fun removePackage(packageName: String) = withContext(Dispatchers.IO) {
        packageDao.deletePackageByName(packageName)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        packageDao.deleteAllPackages()
    }

    private fun getVersion(info: PackageInfo): String =
        "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"

    private fun getLocalizedResources(packageName: String): Resources? {
        return runCatching {
            val appRes = packageManager.getResourcesForApplication(packageName)
            val config = settingsRepository.getLocaleConfiguration()
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(config, appRes.displayMetrics)
            appRes
        }.getOrNull()
    }
}
