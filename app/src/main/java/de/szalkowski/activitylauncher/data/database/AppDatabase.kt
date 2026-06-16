package de.szalkowski.activitylauncher.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PackageDao {
    @Transaction
    @Query("SELECT * FROM packages ORDER BY name COLLATE NOCASE ASC, packageName ASC")
    abstract fun getAllPackagesFlow(): Flow<List<PackageWithActivities>>

    @Query("SELECT * FROM packages WHERE packageName = :packageName")
    abstract suspend fun getPackage(packageName: String): AppPackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPackage(pkg: AppPackageEntity): Long

    @Delete
    abstract suspend fun deletePackage(pkg: AppPackageEntity): Int

    @Transaction
    @Query("DELETE FROM packages WHERE packageName = :packageName")
    abstract suspend fun deletePackageByName(packageName: String): Int

    @Query("SELECT * FROM packages WHERE isFullyLoaded = 0 ORDER BY name COLLATE NOCASE ASC, packageName ASC")
    abstract suspend fun getNotFullyLoadedPackages(): List<AppPackageEntity>

    @Transaction
    @Query("SELECT * FROM packages WHERE packageName = :packageName")
    abstract suspend fun getPackageWithActivities(packageName: String): PackageWithActivities?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivities(activities: List<ActivityEntity>): List<Long>

    @Transaction
    @Query("DELETE FROM activities WHERE packageName = :packageName")
    abstract suspend fun deleteActivitiesForPackage(packageName: String): Int

    @Transaction
    open suspend fun updatePackageDetails(pkg: AppPackageEntity, activities: List<ActivityEntity>): Int {
        insertPackage(pkg)
        deleteActivitiesForPackage(pkg.packageName)
        insertActivities(activities)
        return 0
    }

    @Query("DELETE FROM packages")
    abstract suspend fun deleteAllPackages(): Int
}

data class PackageWithActivities(
    @Embedded val pkg: AppPackageEntity,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName",
    )
    val activities: List<ActivityEntity>,
)

@Database(entities = [AppPackageEntity::class, ActivityEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageDao(): PackageDao
}
