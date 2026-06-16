package de.szalkowski.activitylauncher.data.packages

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import de.szalkowski.activitylauncher.domain.model.ActivityName
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: PackageDataSource,
) : PackageRepository {

    private val packageManager: PackageManager = context.packageManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _packagesFlow = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    override val packagesFlow: StateFlow<List<MyPackageInfo>> = _packagesFlow.asStateFlow()

    @Volatile
    override var isLoaded: Boolean = false
        private set

    init {
        scope.launch {
            dataSource.allPackagesFlow.collect { entities ->
                val myPackages = entities.map { it.toMyPackageInfo() }
                _packagesFlow.value = myPackages.sortedBy { it.name.lowercase() }
                isLoaded = true
            }
        }
        sync()
    }

    override val packages: List<MyPackageInfo>
        get() = _packagesFlow.value

    override fun sync() {
        scope.launch {
            dataSource.sync()
            dataSource.loadAllDetails()
        }
    }

    private fun PackageWithActivities.toMyPackageInfo(): MyPackageInfo {
        val app = runCatching { packageManager.getApplicationInfo(pkg.packageName, 0) }.getOrNull()
        val icon = if (app != null) {
            packageManager.getApplicationIcon(app)
        } else {
            packageManager.defaultActivityIcon
        }

        val activityNames = activities.filter { !it.isDefault }.map {
            ActivityName(it.name, it.shortCls, it.fullCls)
        }
        val defaultActivityName = activities.find { it.isDefault }?.let {
            ActivityName(it.name, it.shortCls, it.fullCls)
        }

        return MyPackageInfo(
            id = pkg.packageName.hashCode().toLong(),
            packageName = pkg.packageName,
            name = pkg.name,
            version = pkg.version,
            defaultActivityName = defaultActivityName,
            activityNames = activityNames,
            icon = icon,
            iconResourceName = pkg.iconResourceName,
            isFullyLoaded = pkg.isFullyLoaded,
        )
    }

    override fun getPackage(packageName: String): MyPackageInfo? {
        return packages.find { it.packageName == packageName }
    }

    override fun invalidate() {
        sync()
    }
}
