package de.szalkowski.activitylauncher.domain.packages

import de.szalkowski.activitylauncher.data.database.AppPackageEntity
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import kotlinx.coroutines.flow.Flow

interface LocalPackageRepository {
    fun getAllPackagesFlow(): Flow<List<PackageWithActivities>>
    suspend fun insertPackage(pkg: AppPackageEntity)
    suspend fun deletePackageByName(packageName: String)
    // ... other methods needed for details
}
