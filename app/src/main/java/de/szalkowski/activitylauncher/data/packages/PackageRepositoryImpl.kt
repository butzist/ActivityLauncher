package de.szalkowski.activitylauncher.data.packages

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import de.szalkowski.activitylauncher.domain.model.*
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.entrypoint.PackageChangeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: PackageDataSource,
    private val settingsRepository: SettingsRepository,
) : PackageRepository {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val _packagesFlow = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    override val packagesFlow: StateFlow<List<MyPackageInfo>> = _packagesFlow.asStateFlow()

    private val activeTasksCount = MutableStateFlow(0)
    override val isSyncing: StateFlow<Boolean> = activeTasksCount
        .map { it > 0 }
        .stateIn(scope, SharingStarted.Eagerly, false)

    @Volatile
    override var isLoaded: Boolean = false
        private set

    init {
        scope.launch {
            dataSource.allPackagesFlow.collect { entities ->
                val myPackages = entities.map { it.toMyPackageInfo() }
                _packagesFlow.value = myPackages.sortedWith(compareBy({ it.name.lowercase() }, { it.packageName }))
                isLoaded = true
            }
        }
        sync()

        registerPackageChangeReceiver()
    }

    private fun registerPackageChangeReceiver() {
        // Register dynamic receiver for package changes to ensure foreground updates
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(PackageChangeReceiver().apply { packageRepository = this@PackageRepositoryImpl }, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(PackageChangeReceiver().apply { packageRepository = this@PackageRepositoryImpl }, filter)
            }
        } catch (e: Exception) {
            // Registration might fail in unit tests or if context is restricted
        }
    }

    override val packages: List<MyPackageInfo>
        get() = _packagesFlow.value

    override fun sync() {
        scope.launch {
            activeTasksCount.update { it + 1 }
            try {
                performSync()
            } finally {
                activeTasksCount.update { it - 1 }
            }
        }
    }

    override fun loadDetails(packageName: String) {
        scope.launch {
            activeTasksCount.update { it + 1 }
            try {
                dataSource.loadDetails(packageName)
            } finally {
                activeTasksCount.update { it - 1 }
            }
        }
    }

    override fun removePackage(packageName: String) {
        scope.launch {
            activeTasksCount.update { it + 1 }
            try {
                dataSource.removePackage(packageName)
            } finally {
                activeTasksCount.update { it - 1 }
            }
        }
    }

    private suspend fun performSync() {
        dataSource.sync()
        dataSource.loadAllDetails()
    }

    private fun PackageWithActivities.toMyPackageInfo(): MyPackageInfo {
        val activityNames = activities.filter { !it.isDefault }.map {
            ActivityName(it.name, it.shortCls, it.fullCls, it.isPrivate, it.iconResourceName)
        }
        val defaultActivityName = activities.find { it.isDefault }?.let {
            ActivityName(it.name, it.shortCls, it.fullCls, it.isPrivate, it.iconResourceName)
        }

        return MyPackageInfo(
            id = pkg.packageName.hashCode().toLong(),
            packageName = pkg.packageName,
            name = pkg.name,
            version = pkg.version,
            defaultActivityName = defaultActivityName,
            activityNames = activityNames,
            iconResourceName = pkg.iconResourceName,
            isFullyLoaded = pkg.isFullyLoaded,
        )
    }

    override fun getPackage(packageName: String): MyPackageInfo? {
        return _packagesFlow.value.find { it.packageName == packageName }
    }

    override fun getActivities(packageName: String): PackageActivities {
        val pack = getPackage(packageName)
            ?: return PackageActivities(packageName, packageName, null, listOf())

        val activityNames = (listOfNotNull(pack.defaultActivityName) + pack.activityNames)
        val activities = activityNames.map { name ->
            SystemActivity(
                ComponentName(pack.packageName, name.fullCls),
                name.name,
                name.iconResourceName,
                name.isPrivate,
            )
        }

        val defaultActivity = pack.defaultActivityName?.let { name ->
            activities.find { it.componentName.className == name.fullCls }
        }

        return PackageActivities(
            pack.packageName,
            pack.name,
            defaultActivity,
            activities,
        )
    }

    override fun getActivity(componentName: ComponentName): SystemActivity {
        val pack = getPackage(componentName.packageName)
        val activityNames = pack?.let { listOfNotNull(it.defaultActivityName) + it.activityNames }
        val name = activityNames?.find { n -> n.fullCls == componentName.className }

        if (name == null) {
            return SystemActivity(
                componentName,
                createNameFromClass(componentName.className),
                null,
                false,
            )
        }

        return SystemActivity(
            componentName,
            name.name,
            name.iconResourceName,
            name.isPrivate,
        )
    }

    private fun createNameFromClass(cls: String): String {
        val name = cls.substringAfterLast('.')
        val locale = try {
            val config = settingsRepository.getLocaleConfiguration()
            androidx.core.os.ConfigurationCompat.getLocales(config).get(0)
        } catch (e: Exception) {
            null
        } ?: java.util.Locale.getDefault()
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    override fun invalidate() {
        scope.launch {
            activeTasksCount.update { it + 1 }
            try {
                dataSource.clear()
                performSync()
            } finally {
                activeTasksCount.update { it - 1 }
            }
        }
    }
}
