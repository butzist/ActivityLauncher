package de.szalkowski.activitylauncher.domain.packages

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.model.PackageActivities
import kotlinx.coroutines.flow.StateFlow

interface PackageRepository {
    val packages: List<MyPackageInfo>
    val packagesFlow: StateFlow<List<MyPackageInfo>>
    val isLoaded: Boolean
    val isSyncing: StateFlow<Boolean>
    fun getPackage(packageName: String): MyPackageInfo?
    fun getActivities(packageName: String): PackageActivities
    fun getActivity(componentName: ComponentName): MyActivityInfo
    fun invalidate()
    fun sync()
    fun loadDetails(packageName: String)
    fun removePackage(packageName: String)
}
