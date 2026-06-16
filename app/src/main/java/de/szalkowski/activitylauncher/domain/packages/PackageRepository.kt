package de.szalkowski.activitylauncher.domain.packages

import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import kotlinx.coroutines.flow.StateFlow

interface PackageRepository {
    val packages: List<MyPackageInfo>
    val packagesFlow: StateFlow<List<MyPackageInfo>>
    val isLoaded: Boolean
    fun getPackage(packageName: String): MyPackageInfo?
    fun getIcon(packageName: String): android.graphics.drawable.Drawable
    fun invalidate()
    fun sync()
}
