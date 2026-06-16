package de.szalkowski.activitylauncher.domain.packages

import android.content.pm.PackageInfo

interface SystemPackageRepository {
    fun getInstalledPackages(): List<PackageInfo>
}
