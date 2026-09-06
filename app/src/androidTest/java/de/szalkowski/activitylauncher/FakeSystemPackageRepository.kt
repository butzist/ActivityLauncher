package de.szalkowski.activitylauncher

import de.szalkowski.activitylauncher.data.packages.SystemPackageRepositoryImpl
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.SystemPackage
import de.szalkowski.activitylauncher.domain.packages.SystemPackageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSystemPackageRepository @Inject constructor(
    private val realRepository: SystemPackageRepositoryImpl,
) : SystemPackageRepository {
    private val packages = mutableMapOf<String, SystemPackage>()
    private val activities = mutableMapOf<String, List<MyActivityInfo>>()

    var useReal: Boolean = false

    fun addPackage(pkg: SystemPackage, pkgActivities: List<MyActivityInfo>) {
        useReal = false
        packages[pkg.packageName] = pkg
        activities[pkg.packageName] = pkgActivities
    }

    fun clear() {
        useReal = false
        packages.clear()
        activities.clear()
    }

    override fun getInstalledPackages(): List<SystemPackage> {
        if (useReal || packages.isEmpty()) {
            return realRepository.getInstalledPackages()
        }
        return packages.values.toList()
    }

    override fun getPackageDetails(packageName: String): SystemPackage? {
        if (useReal || !packages.containsKey(packageName)) {
            return realRepository.getPackageDetails(packageName)
        }
        return packages[packageName]
    }

    override fun getActivities(packageName: String): List<MyActivityInfo> {
        if (useReal || !activities.containsKey(packageName)) {
            return realRepository.getActivities(packageName)
        }
        return activities[packageName] ?: emptyList()
    }
}
