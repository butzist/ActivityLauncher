package de.szalkowski.activitylauncher

import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.SystemPackage
import de.szalkowski.activitylauncher.domain.packages.SystemPackageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSystemPackageRepository @Inject constructor() : SystemPackageRepository {
    private val packages = mutableMapOf<String, SystemPackage>()
    private val activities = mutableMapOf<String, List<MyActivityInfo>>()

    fun addPackage(pkg: SystemPackage, pkgActivities: List<MyActivityInfo>) {
        packages[pkg.packageName] = pkg
        activities[pkg.packageName] = pkgActivities
    }

    fun clear() {
        packages.clear()
        activities.clear()
    }

    override fun getInstalledPackages(): List<SystemPackage> {
        return packages.values.toList()
    }

    override fun getPackageDetails(packageName: String): SystemPackage? {
        return packages[packageName]
    }

    override fun getActivities(packageName: String): List<MyActivityInfo> {
        return activities[packageName] ?: emptyList()
    }
}
