package de.szalkowski.activitylauncher.domain.packages

import de.szalkowski.activitylauncher.domain.model.SystemActivity
import de.szalkowski.activitylauncher.domain.model.SystemPackage

interface SystemPackageRepository {
    fun getInstalledPackages(): List<SystemPackage>
    fun getPackageDetails(packageName: String): SystemPackage?
    fun getActivities(packageName: String): List<SystemActivity>
}
