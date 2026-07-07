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

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE packageName = :packageName AND className = :className")
    suspend fun deleteByComponent(packageName: String, className: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE packageName = :packageName AND className = :className)")
    suspend fun isFavorite(packageName: String, className: String): Boolean
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recent: RecentEntity): Long

    @Query("DELETE FROM recents WHERE packageName = :packageName AND className = :className")
    suspend fun deleteByComponent(packageName: String, className: String): Int

    @Query("DELETE FROM recents WHERE (packageName, className) NOT IN (SELECT packageName, className FROM recents ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trim(limit: Int): Int
}

@Database(
    entities = [
        AppPackageEntity::class,
        ActivityEntity::class,
        FavoriteEntity::class,
        RecentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageDao(): PackageDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
}
