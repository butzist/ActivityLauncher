package de.szalkowski.activitylauncher.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import de.szalkowski.activitylauncher.app.di.DatabaseModule
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.FavoriteDao
import de.szalkowski.activitylauncher.data.database.PackageDao
import de.szalkowski.activitylauncher.data.database.RecentDao
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @Provides
    fun providePackageDao(database: AppDatabase): PackageDao {
        return database.packageDao()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideRecentDao(database: AppDatabase): RecentDao {
        return database.recentDao()
    }
}
