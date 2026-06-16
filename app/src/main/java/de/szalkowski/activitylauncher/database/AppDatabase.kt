package de.szalkowski.activitylauncher.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {
    @Transaction
    @Query("SELECT * FROM packages ORDER BY name COLLATE NOCASE ASC")
    fun getAllPackagesFlow(): Flow<List<PackageWithActivities>>

    @Query("SELECT * FROM packages WHERE packageName = :packageName")
    suspend fun getPackage(packageName: String): AppPackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: AppPackageEntity): Long

    @Delete
    suspend fun deletePackage(pkg: AppPackageEntity): Int

    @Transaction
    @Query("DELETE FROM packages WHERE packageName = :packageName")
    suspend fun deletePackageByName(packageName: String): Int

    @Query("SELECT * FROM packages WHERE isFullyLoaded = 0")
    suspend fun getNotFullyLoadedPackages(): List<AppPackageEntity>

    @Transaction
    @Query("SELECT * FROM packages WHERE packageName = :packageName")
    suspend fun getPackageWithActivities(packageName: String): PackageWithActivities?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>): List<Long>

    @Transaction
    @Query("DELETE FROM activities WHERE packageName = :packageName")
    suspend fun deleteActivitiesForPackage(packageName: String): Int
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
