package de.szalkowski.activitylauncher.data.packages

import de.szalkowski.activitylauncher.data.database.*
import de.szalkowski.activitylauncher.domain.packages.SystemPackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageDataSource @Inject constructor(
    private val packageDao: PackageDao,
    private val systemPackageRepository: SystemPackageRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val loadingPackages = mutableSetOf<String>()

    val allPackagesFlow: Flow<List<PackageWithActivities>> = packageDao.getAllPackagesFlow()

    suspend fun sync() = withContext(Dispatchers.IO) {
        val installedPackages = systemPackageRepository.getInstalledPackages()
        val dbPackages = packageDao.getAllPackagesFlow().first().associateBy { it.pkg.packageName }

        installedPackages.forEach { installed ->
            val dbPkg = dbPackages[installed.packageName]?.pkg
            if (dbPkg == null || dbPkg.version != installed.version) {
                val newPkg = AppPackageEntity(
                    packageName = installed.packageName,
                    name = installed.name,
                    version = installed.version,
                    iconResourceName = installed.iconResourceName,
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
            val systemPackage = systemPackageRepository.getPackageDetails(packageName)
            if (systemPackage == null) {
                packageDao.deletePackageByName(packageName)
                return@withContext
            }

            val systemActivities = systemPackageRepository.getActivities(packageName)
            val filteredActivities = systemActivities.filter {
                !settingsRepository.hidePrivate || !it.isPrivate
            }

            val activityEntities = filteredActivities.map { activity ->
                ActivityEntity(
                    id = 0,
                    packageName = packageName,
                    name = activity.name,
                    shortCls = activity.componentName.className.substringAfterLast('.'),
                    fullCls = activity.componentName.className,
                    isDefault = activity.isDefault,
                    isPrivate = activity.isPrivate,
                    iconResourceName = activity.iconResourceName,
                )
            }

            val updatedPkg = AppPackageEntity(
                packageName = packageName,
                name = systemPackage.name,
                version = systemPackage.version,
                iconResourceName = systemPackage.iconResourceName,
                isFullyLoaded = true,
                lastUpdated = System.currentTimeMillis(),
            )

            // Split DAO calls to avoid Mockito issues with 'open' methods in abstract classes
            packageDao.insertPackage(updatedPkg)
            packageDao.deleteActivitiesForPackage(packageName)
            packageDao.insertActivities(activityEntities)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            markAsLoaded(packageName)
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
}
