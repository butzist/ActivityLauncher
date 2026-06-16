package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.data.PackageRepository
import de.szalkowski.activitylauncher.database.PackageWithActivities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface PackageListService {
    val packages: List<MyPackageInfo>
    val packagesFlow: StateFlow<List<MyPackageInfo>>
    val isLoaded: Boolean
    fun getPackage(packageName: String): MyPackageInfo?
    fun invalidate()
    fun sync()
}

@Singleton
class PackageListServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PackageRepository,
) : PackageListService {

    private val packageManager: PackageManager = context.packageManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _packagesFlow = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    override val packagesFlow = _packagesFlow.asStateFlow()

    @Volatile
    override var isLoaded: Boolean = false
        private set

    init {
        scope.launch {
            repository.allPackagesFlow.collect { entities ->
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
            repository.sync()
            repository.loadAllDetails()
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

data class MyPackageInfo(
    val id: Long,
    val packageName: String,
    val name: String,
    val version: String,
    val defaultActivityName: ActivityName?,
    val activityNames: List<ActivityName>,
    val icon: Drawable,
    val iconResourceName: String?,
    val isFullyLoaded: Boolean = true,
)

data class ActivityName(
    val name: String,
    val shortCls: String,
    val fullCls: String,
)
