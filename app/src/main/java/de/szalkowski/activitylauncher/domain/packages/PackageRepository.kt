package de.szalkowski.activitylauncher.domain.packages

import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import kotlinx.coroutines.flow.StateFlow

interface PackageRepository {
    val packages: List<MyPackageInfo>
    val packagesFlow: StateFlow<List<MyPackageInfo>>
    val isLoaded: Boolean
    val isSyncing: StateFlow<Boolean>
    fun getPackage(packageName: String): MyPackageInfo?
    fun invalidate()
    fun sync()
    fun loadDetails(packageName: String)
    fun removePackage(packageName: String)
}
