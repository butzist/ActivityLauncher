package de.szalkowski.activitylauncher.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.FavoriteDao
import de.szalkowski.activitylauncher.data.database.PackageDao
import de.szalkowski.activitylauncher.data.database.RecentDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "activity_launcher.db",
        ).fallbackToDestructiveMigration().build()
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
